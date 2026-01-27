import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { Pinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import type { I18n } from 'vue-i18n'
import { computed, h } from 'vue'
import type { Component } from 'vue'
import { createVuetify } from 'vuetify'
import type { ProductDto } from '~~/shared/api-client'
import { useProductCompareStore } from '~/stores/useProductCompareStore'

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) => `${count}`,
  }),
}))

const createStub = (tag: string): Component => ({
  inheritAttrs: false,
  setup(_props, { slots, attrs }) {
    return () => h(tag, attrs, slots.default?.())
  },
})

const VBtnStub: Component = {
  inheritAttrs: false,
  props: {
    disabled: Boolean,
  },
  emits: ['click'],
  setup(props, { slots, attrs, emit }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: 'button',
          disabled: props.disabled,
          onClick: (event: MouseEvent) => {
            if (props.disabled) {
              event.preventDefault()
              return
            }
            emit('click', event)
          },
        },
        slots.default?.()
      )
  },
}

const VTooltipStub: Component = {
  inheritAttrs: false,
  setup(_props, { slots, attrs }) {
    return () =>
      h('div', attrs, [slots.activator?.({ props: {} }), slots.default?.()])
  },
}

const CompareToggleStub: Component = {
  inheritAttrs: false,
  props: {
    product: {
      type: Object,
      required: true,
    },
    size: {
      type: String,
      default: 'comfortable',
    },
  },
  setup(props) {
    const store = useProductCompareStore()
    const isSelected = computed(() =>
      store.hasProduct(props.product as ProductDto)
    )
    const isDisabled = computed(() => {
      if (isSelected.value) {
        return false
      }

      return !store.canAddProduct(props.product as ProductDto).success
    })

    const toggle = () => {
      if (isDisabled.value) {
        return
      }

      store.toggleProduct(props.product as ProductDto)
    }

    return () =>
      h('button', {
        type: 'button',
        'data-test': 'category-product-compare',
        disabled: isDisabled.value,
        onClick: toggle,
      })
  },
}

type VuetifyInstance = ReturnType<typeof createVuetify>

let pinia: Pinia
let i18n: I18n
let vuetify: VuetifyInstance

const mountGrid = async (
  products: ProductDto[],
  extraProps: Record<string, unknown> = {}
) => {
  const module = await import('./CategoryProductCardGrid.vue')
  const CategoryProductCardGrid = module.default

  return mount(CategoryProductCardGrid, {
    props: { products, ...extraProps },
    global: {
      plugins: [pinia, i18n, vuetify],
      stubs: {
        VRow: createStub('div'),
        VCol: createStub('div'),
        VCard: createStub('div'),
        VCardItem: createStub('div'),
        VImg: createStub('div'),
        VSkeletonLoader: createStub('div'),
        VChip: createStub('div'),
        VTooltip: VTooltipStub,
        VBtn: VBtnStub,
        VIcon: createStub('i'),
        ImpactScore: createStub('div'),
        NuxtLink: createStub('a'),
        CompareToggleButton: CompareToggleStub,
      },
    },
  })
}

const buildProduct = (overrides: Partial<ProductDto> = {}): ProductDto => ({
  gtin: overrides.gtin ?? 1,
  base: {
    vertical: overrides.base?.vertical ?? 'electronics',
    bestName: overrides.base?.bestName ?? 'Example product',
  },
  identity: {
    bestName: overrides.identity?.bestName ?? 'Example product',
  },
  resources: {
    coverImagePath: overrides.resources?.coverImagePath,
  },
  offers: overrides.offers,
  names: overrides.names,
  scores: overrides.scores,
  attributes: overrides.attributes,
  datasources: overrides.datasources,
  aiReview: overrides.aiReview,
  slug: overrides.slug,
  fullSlug: overrides.fullSlug,
})

describe('CategoryProductCardGrid', () => {
  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    localStorage.clear()
    vuetify = createVuetify()
    i18n = createI18n({
      legacy: false,
      locale: 'en-US',
      messages: {
        'en-US': {
          category: {
            products: {
              untitledProduct: 'Untitled product',
              notRated: 'Not rated yet',
              priceUnavailable: 'Price unavailable',
              conditions: {
                new: 'New',
                occasion: 'Occasion',
                unknown: 'Unknown',
                other: 'Other ({condition})',
              },
              pricing: {
                newOfferLabel: 'New',
                occasionOfferLabel: 'Second-hand',
                bestOfferLabel: 'Best',
                conditionLabel: 'Condition: {condition}',
                trends: {
                  decrease: 'Price decrease',
                  increase: 'Price increase',
                  stable: 'Price stable',
                },
              },
              compare: {
                addToList: 'Add to compare',
                removeFromList: 'Remove from compare',
                limitReached: 'Maximum reached {count}',
                differentCategory:
                  'Only compare products from the same category.',
                missingIdentifier: 'Cannot compare this product.',
              },
            },
          },
          common: {
            boolean: {
              true: 'Yes',
              false: 'No',
            },
          },
        },
      },
    })
  })

  it('toggles a product in the compare list', async () => {
    const product = buildProduct({ gtin: 1001 })
    const wrapper = await mountGrid([product])
    const store = useProductCompareStore()

    const button = wrapper.get('[data-test="category-product-compare"]')

    await button.trigger('click')
    expect(store.items).toHaveLength(1)

    await button.trigger('click')
    expect(store.items).toHaveLength(0)
  })

  it('disables the compare button for incompatible categories', async () => {
    const first = buildProduct({
      gtin: 2001,
      base: { vertical: 'electronics' } as ProductDto['base'],
    })
    const other = buildProduct({
      gtin: 2002,
      base: { vertical: 'kitchen' } as ProductDto['base'],
    })

    const wrapper = await mountGrid([first, other])
    const store = useProductCompareStore()

    const buttons = wrapper.findAll('[data-test="category-product-compare"]')
    expect(buttons).toHaveLength(2)

    const firstButton = buttons[0]!
    const secondButton = buttons[1]!

    await firstButton.trigger('click')
    expect(store.items).toHaveLength(1)

    expect((secondButton.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('applies disabled styling and nofollow links when requested', async () => {
    const product = buildProduct({ fullSlug: '/products/demo' })
    const wrapper = await mountGrid([product], {
      isCategoryDisabled: true,
      nofollowLinks: true,
    })

    const card = wrapper.get('.category-product-card-grid__card')
    expect(card.classes()).toContain(
      'category-product-card-grid__card--disabled'
    )
    expect(card.attributes('rel')).toBe('nofollow')
  })

  it('renders new and occasion prices in the microtable layout', async () => {
    const product = buildProduct({
      offers: {
        bestNewOffer: { price: 1068, currency: 'EUR', shortPrice: '1068 €' },
        bestOccasionOffer: { price: 690, currency: 'EUR', shortPrice: '690 €' },
      } as ProductDto['offers'],
    })

    const wrapper = await mountGrid([product])

    // Check implementation of microtable
    const pricingTable = wrapper.get(
      '.category-product-card-grid__pricing-table'
    )
    expect(pricingTable.exists()).toBe(true)

    const cells = wrapper.findAll('.category-product-card-grid__pricing-cell')
    expect(cells).toHaveLength(2)

    // Check classes for specific offer types
    expect(
      wrapper.find('.category-product-card-grid__pricing-cell--new').exists()
    ).toBe(true)
    expect(
      wrapper
        .find('.category-product-card-grid__pricing-cell--occasion')
        .exists()
    ).toBe(true)
  })
})

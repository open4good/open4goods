import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import type { Pinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import type { I18n } from 'vue-i18n'
import { createVuetify } from 'vuetify'
import { h, defineComponent, nextTick } from 'vue'
import type { Component } from 'vue'
import { useProductCompareStore } from '~/stores/useProductCompareStore'
import type { ProductDto } from '~~/shared/api-client'

const routerPush = vi.fn(async () => {})
const routerReplace = vi.fn(async () => {})

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
    replace: routerReplace,
  }),
  useRoute: () => ({
    path: '/compare',
    hash: '',
  }),
}))

vi.mock('~~/shared/utils/localized-routes', () => ({
  resolveLocalizedRoutePath: () => '/compare',
}))

vi.mock('~/composables/usePluralizedTranslation', () => ({
  usePluralizedTranslation: () => ({
    translatePlural: (_key: string, count: number) =>
      count === 1 ? `${count} product selected` : `${count} products selected`,
  }),
}))

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_props, { slots }) {
    return () => slots.default?.()
  },
})

const simpleStub = (tag: string): Component => ({
  inheritAttrs: false,
  setup(_props, { slots, attrs }) {
    return () => h(tag, attrs, slots.default?.())
  },
})

const ExpandTransitionStub = defineComponent({
  name: 'ExpandTransitionStub',
  setup(_props, { slots }) {
    return () => h('div', slots.default?.())
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

type VuetifyInstance = ReturnType<typeof createVuetify>

let sequence = 0
let pinia: Pinia
let i18n: I18n
let vuetify: VuetifyInstance

const createProduct = (overrides: Partial<ProductDto> = {}): ProductDto => ({
  gtin: overrides.gtin ?? ++sequence,
  base: {
    vertical: overrides.base?.vertical ?? 'electronics',
    bestName: overrides.base?.bestName ?? 'Example product',
  },
  identity: {
    bestName: overrides.identity?.bestName ?? 'Example product',
  },
  resources: {
    coverImagePath:
      overrides.resources?.coverImagePath ??
      'https://cdn.example.com/image.jpg',
  },
  slug: overrides.slug,
  fullSlug: overrides.fullSlug,
  offers: overrides.offers,
  names: overrides.names,
  scores: overrides.scores,
  attributes: overrides.attributes,
  datasources: overrides.datasources,
  aiReview: overrides.aiReview,
})

describe('CategoryComparePanel', () => {
  beforeEach(() => {
    routerPush.mockReset()
    routerReplace.mockReset()
    routerPush.mockResolvedValue(undefined)
    routerReplace.mockResolvedValue(undefined)
    pinia = createPinia()
    setActivePinia(pinia)
    localStorage.clear()
    sequence = 0
    i18n = createI18n({
      legacy: false,
      locale: 'en-US',
      messages: {
        'en-US': {
          category: {
            products: {
              compare: {
                title: 'Compare products',
                expandPanel: 'Show list',
                collapsePanel: 'Hide list',
                removeSingle: 'Remove {name}',
                addMoreHint: 'Select two products.',
                launchComparison: 'Compare now',
                regionLabel: 'Products selected for comparison',
                itemsCount: '{count} products selected',
                addToList: 'Add to compare',
                removeFromList: 'Remove from compare',
                limitReached: 'You can compare up to {count} products.',
                differentCategory:
                  'Only compare products from the same category.',
                missingIdentifier: 'Cannot compare this product.',
              },
              untitledProduct: 'Untitled product',
            },
          },
        },
      },
    })
    vuetify = createVuetify()
  })

  const mountPanel = async () => {
    const module = await import('./CategoryComparePanel.vue')
    const CategoryComparePanel = module.default

    return mount(CategoryComparePanel, {
      global: {
        plugins: [pinia, i18n, vuetify],
        stubs: {
          ClientOnly: ClientOnlyStub,
          VCard: simpleStub('div'),
          VBtn: VBtnStub,
          VAvatar: simpleStub('div'),
          VImg: simpleStub('div'),
          VSkeletonLoader: simpleStub('div'),
          VTooltip: simpleStub('div'),
          VExpandTransition: ExpandTransitionStub,
          VIcon: simpleStub('span'),
          NuxtLink: {
            props: {
              to: {
                type: [String, Object],
                default: '#',
              },
            },
            setup(props, { slots }) {
              return () =>
                h(
                  'a',
                  {
                    href: typeof props.to === 'string' ? props.to : '#',
                    role: 'link',
                  },
                  slots.default?.()
                )
            },
          },
        },
      },
    })
  }

  it('remains hidden when no products are selected', async () => {
    const wrapper = await mountPanel()

    expect(wrapper.find('.category-compare-panel').exists()).toBe(false)
  })

  it('renders selected items and allows removing them', async () => {
    const store = useProductCompareStore()
    store.addProduct(createProduct({ gtin: 101 }))
    store.addProduct(createProduct({ gtin: 102 }))

    const wrapper = await mountPanel()

    expect(wrapper.text()).toContain('Compare products')
    const removeButtons = wrapper.findAll(
      '.category-compare-panel__item-remove'
    )
    expect(removeButtons).toHaveLength(2)

    const firstRemoveButton = removeButtons[0]!

    await firstRemoveButton.trigger('click')
    await nextTick()
    expect(store.items).toHaveLength(1)
    expect(routerReplace).toHaveBeenLastCalledWith('/compare#102')
  })

  it('emits launch event when comparison is triggered', async () => {
    const store = useProductCompareStore()
    const first = createProduct({ gtin: 201 })
    const second = createProduct({ gtin: 202 })

    store.addProduct(first)
    store.addProduct(second)

    const wrapper = await mountPanel()

    const launchButton = wrapper.get('.category-compare-panel__cta')
    expect((launchButton.element as HTMLButtonElement).disabled).toBe(false)

    await launchButton.trigger('click')
    await nextTick()
    const emitted = wrapper.emitted('launch-comparison') as
      | unknown[][]
      | undefined
    expect(emitted).toBeTruthy()
    expect(emitted?.[0]?.[0]).toHaveLength(2)
    expect(routerPush).toHaveBeenLastCalledWith('/compare#201Vs202')
  })

  it('links to the product detail page when a slug is available', async () => {
    const store = useProductCompareStore()
    store.addProduct(
      createProduct({
        fullSlug: '/products/example-product',
        base: { bestName: 'Linked product' },
        identity: { bestName: 'Linked product' },
      })
    )

    const wrapper = await mountPanel()
    const link = wrapper.get('.category-compare-panel__item-name--link')

    expect(link.element.tagName).toBe('A')
    expect(link.attributes('href')).toBe('/products/example-product')
    expect(link.text()).toContain('Linked product')
  })
})

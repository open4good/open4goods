import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductStickyOffersBar from './ProductStickyOffersBar.vue'

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    smAndDown: { value: false },
    width: { value: 1280 },
  }),
}))

const VSelectStub = defineComponent({
  name: 'VSelectStub',
  props: {
    items: { type: Array, default: () => [] },
    modelValue: { type: [String, Number, null], default: null },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    return () =>
      h(
        'select',
        {
          class: 'v-select-stub',
          value: props.modelValue ?? '',
          onChange: (event: Event) => {
            const target = event.target as HTMLSelectElement
            emit('update:modelValue', target.value)
          },
        },
        (props.items as Array<Record<string, unknown>>).map((item) => {
          const valueKey = (attrs['item-value'] as string) ?? 'value'
          const titleKey = (attrs['item-title'] as string) ?? 'title'
          const value = (item[valueKey] as string) ?? ''
          const label = (item[titleKey] as string) ?? ''
          return h('option', { value }, label)
        }),
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        'button',
        {
          class: 'v-btn-stub',
          disabled: Boolean(attrs.disabled),
          'data-href': attrs.href as string | undefined,
          'data-to': attrs.to as string | undefined,
          'aria-label': attrs['aria-label'] as string | undefined,
        },
        slots.default?.(),
      )
  },
})

const stubs = {
  'v-select': VSelectStub,
  'v-btn': VBtnStub,
  'v-icon': defineComponent({
    name: 'VIconStub',
    props: { icon: { type: String, default: '' } },
    setup(props) {
      return () => h('span', { class: 'v-icon-stub' }, props.icon)
    },
  }),
  'v-container': defineComponent({
    name: 'VContainerStub',
    setup(_, { slots }) {
      return () => h('div', { class: 'v-container-stub' }, slots.default?.())
    },
  }),
}

describe('ProductStickyOffersBar', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          hero: {
            offerConditions: {
              occasion: 'Second-hand',
              new: 'New',
            },
          },
          stickyOffers: {
            message: 'Enjoying this? Support us by shopping via Nudger!',
            selectAria: 'Choose an offer for {condition}',
            buttonLabel: 'Shop this offer',
            buttonAria: 'Open the {condition} offer in the current tab',
          },
          price: {
            metrics: {
              unknownSource: 'Unknown source',
            },
          },
        },
      },
    },
  })

  const baseProduct = {
    offers: {
      offersCount: 4,
      bestPrice: {
        currency: 'EUR',
      },
      occasionOffers: [
        {
          price: 649,
          currency: 'EUR',
          datasourceName: 'Merchant U',
          url: 'https://merchant-u.example',
          favicon: 'https://merchant-u.example/favicon.ico',
        },
        {
          price: 699,
          currency: 'EUR',
          datasourceName: 'Vintage Shop',
          url: 'https://vintage.example',
        },
      ],
      newOffers: [
        {
          price: 799,
          currency: 'EUR',
          datasourceName: 'Shop',
          url: 'https://shop.example',
          favicon: 'https://shop.example/favicon.ico',
          affiliationToken: 'abc123',
        },
        {
          price: 829,
          currency: 'EUR',
          datasourceName: 'Retailer',
          url: 'https://retailer.example',
        },
      ],
    },
  } as unknown as ProductDto

  const mountComponent = async (props: Partial<{ product: ProductDto; visible: boolean }> = {}) =>
    mountSuspended(ProductStickyOffersBar, {
      props: {
        product: props.product ?? baseProduct,
        visible: props.visible ?? true,
      },
      global: {
        plugins: [i18n],
        stubs,
      },
    })

  it('renders segments with the lowest price selected by default', async () => {
    const wrapper = await mountComponent()

    const segments = wrapper.findAll('[data-testid^="sticky-offer-segment-"]')
    expect(segments).toHaveLength(2)
    expect(segments[0]?.text()).toContain('Merchant U')
    expect(segments[0]?.text()).toContain('649 €')
    expect(segments[1]?.text()).toContain('Shop')
    expect(segments[1]?.text()).toContain('799 €')

    const occasionButton = wrapper.get('[data-testid="sticky-offer-button-occasion"]')
    expect(occasionButton.attributes('data-href')).toBe('https://merchant-u.example')

    const newButton = wrapper.get('[data-testid="sticky-offer-button-new"]')
    expect(newButton.attributes('data-to')).toBe('/contrib/abc123')

    await wrapper.unmount()
  })

  it('hides the bar when visibility is false', async () => {
    const wrapper = await mountComponent({ visible: false })

    expect(wrapper.find('[data-testid="sticky-offers-bar"]').exists()).toBe(false)

    await wrapper.unmount()
  })
})

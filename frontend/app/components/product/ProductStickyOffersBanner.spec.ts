import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, watch } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductStickyOffersBanner from './ProductStickyOffersBanner.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        price: {
          metrics: {
            unknownSource: 'Unknown source',
          },
        },
        hero: {
          supportBanner: {
            ariaLabel: 'Support Nudger by choosing an offer',
            message: 'Love this? Support us by purchasing via Nudger!',
            selectPlaceholder: 'Select an offer',
            action: 'Go to offer',
            segment: {
              occasion: {
                title: 'Second-hand offers',
                selectAria: 'Choose a second-hand offer',
              },
              new: {
                title: 'New offers',
                selectAria: 'Choose a new offer',
              },
            },
          },
          offerConditions: {
            new: 'New',
            occasion: 'Second-hand',
          },
        },
      },
    },
  },
})

const SelectStub = defineComponent({
  name: 'ProductStickyOffersBannerSelect',
  props: {
    items: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    watch(
      () => props.items,
      (items) => {
        emit('update:modelValue', items[0] ?? null)
      },
      { immediate: true },
    )

    return () =>
      h(
        'div',
        { class: 'select-stub' },
        (props.items as { label: string }[]).map((item) => item.label).join(' | '),
      )
  },
})

const mountBanner = (props: Partial<{ heroExitedViewport: boolean; offers: NonNullable<ProductDto['offers']> }>) =>
  mountSuspended(ProductStickyOffersBanner, {
    props: {
      product: {
        offers: props.offers,
      } as ProductDto,
      heroExitedViewport: props.heroExitedViewport ?? false,
    },
    global: {
      plugins: [i18n],
      stubs: {
        Teleport: { template: '<div><slot /></div>' },
        transition: false,
        'v-icon': defineComponent({
          name: 'VIconStub',
          props: { icon: { type: String, default: '' } },
          setup(iconProps) {
            return () => h('span', { class: 'v-icon-stub' }, iconProps.icon)
          },
        }),
        'v-btn': defineComponent({
          name: 'VBtnStub',
          setup(_, { slots, attrs }) {
            return () =>
              h(
                'button',
                {
                  class: ['v-btn-stub', attrs.class],
                  disabled: attrs.disabled as boolean | undefined,
                  'data-to': attrs.to,
                  'data-href': attrs.href,
                },
                slots.default?.(),
              )
          },
        }),
        'v-expansion-panels': defineComponent({
          name: 'VExpansionPanelsStub',
          setup(_, { slots }) {
            return () => h('div', { class: 'expansion-panels-stub' }, slots.default?.())
          },
        }),
        'v-expansion-panel': defineComponent({
          name: 'VExpansionPanelStub',
          setup(_, { slots }) {
            return () => h('div', { class: 'expansion-panel-stub' }, slots.default?.())
          },
        }),
        'v-expansion-panel-title': defineComponent({
          name: 'VExpansionPanelTitleStub',
          setup(_, { slots }) {
            return () => h('div', { class: 'expansion-panel-title-stub' }, slots.default?.())
          },
        }),
        'v-expansion-panel-text': defineComponent({
          name: 'VExpansionPanelTextStub',
          setup(_, { slots }) {
            return () => h('div', { class: 'expansion-panel-text-stub' }, slots.default?.())
          },
        }),
        'v-divider': defineComponent({
          name: 'VDividerStub',
          setup(_, { attrs }) {
            return () => h('hr', { class: ['v-divider-stub', attrs.class] })
          },
        }),
        ProductStickyOffersBannerSelect: SelectStub,
      },
    },
  })

describe('ProductStickyOffersBanner', () => {
  it('does not render when the hero is visible', async () => {
    const wrapper = await mountBanner({ heroExitedViewport: false })

    expect(wrapper.find('.product-sticky-banner').exists()).toBe(false)

    await wrapper.unmount()
  })

  it('displays sorted offers and CTA links when visible', async () => {
    const wrapper = await mountBanner({
      heroExitedViewport: true,
      offers: {
        offersCount: 3,
        bestPrice: { price: 120, currency: 'EUR', datasourceName: 'Global Shop' },
        occasionOffers: [
          { price: 140, currency: 'EUR', datasourceName: 'Occasion B', url: 'https://example.org/b' },
          { price: 110, currency: 'EUR', datasourceName: 'Occasion A', url: 'https://example.org/a' },
        ],
        newOffers: [
          {
            price: 199,
            currency: 'EUR',
            datasourceName: 'New Deals',
            affiliationToken: 'token-123',
          },
        ],
      },
    })

    expect(wrapper.get('.product-sticky-banner__message').text()).toContain(
      'Love this? Support us by purchasing via Nudger!',
    )

    const selectValues = wrapper.findAll('.select-stub').map((select) => select.text())
    expect(selectValues[0]).toMatch(/Occasion A — 110/) // sorted ascending

    const ctas = wrapper.findAll('.product-sticky-banner__cta')
    expect(ctas[0]?.attributes('data-href')).toBe('https://example.org/a')
    expect(ctas[1]?.attributes('data-to')).toBe('/contrib/token-123')

    await wrapper.unmount()
  })
})

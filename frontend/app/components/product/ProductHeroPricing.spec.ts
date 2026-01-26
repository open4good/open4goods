import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import ProductHeroPricing from './ProductHeroPricing.vue'

describe('ProductHeroPricing', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          hero: {
            bestPriceTitle: 'Best price',
            viewSingleOffer: 'View the offer',
            viewOffersCount: 'View the {count} offers',
            offersCountLabel: '{count} offers',
            offerConditions: {
              occasion: 'Second-hand',
              new: 'New',
            },
            noOffers: {
              new: 'No new offers yet!',
              occasion: 'No second-hand offers yet!',
            },
            alternativeOffers: {
              label: 'Alternative offers',
              placeholder: 'Select another offer',
              unknownMerchant: 'Unknown merchant',
            },
            trendTooltip: 'Deviation of {deviation} over {period}',
            trendPeriodDays: '{count} day | {count} days',
            trendPeriodHours: '{count} hour | {count} hours',
            trendPeriodMinutes: '{count} minute | {count} minutes',
          },
          price: {
            trend: {
              decrease: 'Price drop of {amount}',
              increase: 'Price increase of {amount}',
              stable: 'Price unchanged',
            },
          },
        },
      },
    },
  })

  const createWrapper = async (offers: NonNullable<ProductDto['offers']>) =>
    mountSuspended(ProductHeroPricing, {
      props: {
        product: {
          offers,
        } as ProductDto,
      },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtLink: defineComponent({
            name: 'NuxtLinkStub',
            props: { to: { type: [String, Object], required: true } },
            setup(props, { slots, attrs }) {
              return () =>
                h(
                  'a',
                  {
                    class: ['nuxt-link-stub', attrs.class],
                    href: typeof props.to === 'string' ? props.to : '#',
                    target: attrs.target as string | undefined,
                    rel: attrs.rel as string | undefined,
                  },
                  slots.default?.()
                )
            },
          }),
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () =>
                h('div', { class: 'client-only-stub' }, slots.default?.())
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
                    type: 'button',
                    onClick: attrs.onClick as
                      | ((event: MouseEvent) => void)
                      | undefined,
                  },
                  slots.default?.()
                )
            },
          }),
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: { icon: { type: String, default: '' } },
            setup(props) {
              return () => h('span', { class: 'v-icon-stub' }, props.icon)
            },
          }),
          'v-tooltip': defineComponent({
            name: 'VTooltipStub',
            props: { text: { type: String, default: '' } },
            setup(_, { slots }) {
              return () =>
                h('div', { class: 'v-tooltip-stub' }, [
                  slots.activator
                    ? slots.activator({ props: {} })
                    : slots.default?.(),
                ])
            },
          }),
        },
      },
    })

  it('renders both condition panels with price and merchant', async () => {
    const wrapper = await createWrapper({
      offersCount: 3,
      bestPrice: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://shop.example',
        favicon: 'https://shop.example/favicon.ico',
        condition: 'NEW',
        offerName: 'Shop offer',
      },
      bestOccasionOffer: {
        price: 649,
        currency: 'EUR',
        datasourceName: 'Merchant U',
        url: 'https://merchant-u.example',
        favicon: 'https://merchant-u.example/favicon.ico',
        condition: 'OCCASION',
        offerName: 'Used offer',
      },
      bestNewOffer: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://shop.example',
        favicon: 'https://shop.example/favicon.ico',
        condition: 'NEW',
        offerName: 'Shop offer',
      },
      occasionTrend: {
        trend: 'PRICE_INCREASE',
        variation: 5,
        period: 86400000,
      },
      newTrend: {
        trend: 'PRICE_DECREASE',
        variation: -10,
        period: 3600000,
      },
    })

    const panels = wrapper.findAll('.product-hero__pricing-panel')
    expect(panels).toHaveLength(2)
    expect(panels[0]?.text()).toContain('Second-hand')
    expect(panels[0]?.text()).toContain('649')
    expect(panels[1]?.text()).toContain('New')
    expect(panels[1]?.text()).toContain('799')

    await wrapper.unmount()
  })

  it('renders empty state when a condition has no offers', async () => {
    const wrapper = await createWrapper({
      offersCount: 1,
      bestPrice: {
        price: 649,
        currency: 'EUR',
        datasourceName: 'Merchant U',
        url: 'https://merchant-u.example',
        favicon: 'https://merchant-u.example/favicon.ico',
        condition: 'OCCASION',
        offerName: 'Used offer',
      },
      bestOccasionOffer: {
        price: 649,
        currency: 'EUR',
        datasourceName: 'Merchant U',
        url: 'https://merchant-u.example',
        favicon: 'https://merchant-u.example/favicon.ico',
        condition: 'OCCASION',
        offerName: 'Used offer',
      },
      occasionTrend: {
        trend: 'PRICE_STABLE',
        variation: 0,
      },
    })

    const panels = wrapper.findAll('.product-hero__pricing-panel')
    expect(panels).toHaveLength(2)
    expect(panels[0]?.text()).toContain('649')
    expect(panels[1]?.text()).toContain('No new offers yet!')

    await wrapper.unmount()
  })
})

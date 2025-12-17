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
            offerConditions: {
              occasion: 'Second-hand',
              new: 'New',
            },
            offerConditionsToggleAria: 'Choose an offer condition',
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
        },
      },
    })

  it('defaults to occasion offer and toggles to new', async () => {
    const wrapper = await createWrapper({
      offersCount: 3,
      bestPrice: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://shop.example',
        favicon: 'https://shop.example/favicon.ico',
        condition: 'NEW',
      },
      bestOccasionOffer: {
        price: 649,
        currency: 'EUR',
        datasourceName: 'Merchant U',
        url: 'https://merchant-u.example',
        favicon: 'https://merchant-u.example/favicon.ico',
        condition: 'OCCASION',
      },
      bestNewOffer: {
        price: 799,
        currency: 'EUR',
        datasourceName: 'Shop',
        url: 'https://shop.example',
        favicon: 'https://shop.example/favicon.ico',
        condition: 'NEW',
      },
      occasionTrend: {
        trend: 'PRICE_INCREASE',
        variation: 5,
      },
      newTrend: {
        trend: 'PRICE_DECREASE',
        variation: -10,
      },
    })

    const chips = wrapper.findAll('.product-hero__price-chip')
    expect(chips).toHaveLength(2)
    expect(chips[0]?.classes()).toContain('product-hero__price-chip--active')
    expect(wrapper.get('.product-hero__price-value').text()).toBe('649')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('€')
    expect(wrapper.get('.product-hero__price-merchant-link').text()).toContain(
      'Merchant U'
    )

    const trendChip = wrapper.get('.product-hero__price-trend')
    expect(trendChip.classes()).toContain('product-hero__price-trend--increase')
    expect(trendChip.find('.v-icon-stub').text()).toBe('mdi-trending-up')
    expect(trendChip.text()).toContain('Price increase of €5.00')

    await chips[1]?.trigger('click')
    await wrapper.vm.$nextTick()

    expect(chips[1]?.classes()).toContain('product-hero__price-chip--active')
    expect(wrapper.get('.product-hero__price-value').text()).toBe('799')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('€')
    expect(wrapper.get('.product-hero__price-merchant-link').text()).toContain(
      'Shop'
    )

    const updatedTrendChip = wrapper.get('.product-hero__price-trend')
    expect(updatedTrendChip.classes()).toContain(
      'product-hero__price-trend--decrease'
    )
    expect(updatedTrendChip.find('.v-icon-stub').text()).toBe(
      'mdi-trending-down'
    )
    expect(updatedTrendChip.text()).toContain('Price drop of €10.00')

    await wrapper.unmount()
  })

  it('uses affiliation redirect for a single offer', async () => {
    const wrapper = await createWrapper({
      offersCount: 1,
      bestPrice: {
        price: 499,
        currency: 'EUR',
        datasourceName: 'OnlyShop',
        url: 'https://onlyshop.example',
        favicon: 'https://onlyshop.example/favicon.ico',
        condition: 'NEW',
        affiliationToken: 'abc123',
      },
      bestNewOffer: {
        price: 499,
        currency: 'EUR',
        datasourceName: 'OnlyShop',
        url: 'https://onlyshop.example',
        favicon: 'https://onlyshop.example/favicon.ico',
        condition: 'NEW',
        affiliationToken: 'abc123',
      },
    })

    const clientOnlyLink = wrapper.get(
      '.client-only-stub .product-hero__price-merchant-link'
    )
    expect(clientOnlyLink.attributes('href')).toBe('/contrib/abc123')
    expect(clientOnlyLink.attributes('target')).toBeUndefined()

    await wrapper.unmount()
  })

  it('falls back to currency code when highlighting non-euro prices', async () => {
    const wrapper = await createWrapper({
      offersCount: 2,
      bestPrice: {
        price: 120,
        currency: 'USD',
        datasourceName: 'US Shop',
        url: 'https://us-shop.example',
        favicon: 'https://us-shop.example/favicon.ico',
        condition: 'NEW',
      },
      bestNewOffer: {
        price: 120,
        currency: 'USD',
        datasourceName: 'US Shop',
        url: 'https://us-shop.example',
        favicon: 'https://us-shop.example/favicon.ico',
        condition: 'NEW',
      },
    })

    expect(wrapper.get('.product-hero__price-value').text()).toBe('$120')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('USD')

    await wrapper.unmount()
  })
})

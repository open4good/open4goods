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
          'v-tabs': defineComponent({
            name: 'VTabsStub',
            props: { modelValue: { type: String, default: '' } },
            emits: ['update:modelValue'],
            setup(props, { slots }) {
              return () =>
                h(
                  'div',
                  { class: 'v-tabs-stub' },
                  // Render slots and pass a way for children to update the model (simplified)
                  slots.default?.().map(vnode => {
                    // Check if vnode is a VTabStub (mocking internal logic)
                    if (vnode && typeof vnode === 'object') {
                      // We can't easily cloneVNode and intercept props here in a simple stub without more complex logic
                      // So we rely on the VTabStub to emit a custom event that we listen to, or we just rely on the test interacting with VTabStub directly
                      // But for v-model to work, VTabs usually orchestrates it.
                      // For this test, we can minimalistically implement the tab click moving up.
                      // However, easier is to let the test click the tab and have the tab emit 'click'
                      // But we need the parent to catch it?
                      // Let's implement a simple provide/inject or just let the test confirm props.
                      // Actually, sticking to simple:
                      return vnode
                    }
                    return vnode
                  })
                )
            },
          }),
          'v-tab': defineComponent({
            name: 'VTabStub',
            props: {
              value: { type: String, required: true },
            },
            setup(props, { slots, attrs }) {
              return () =>
                h(
                  'button',
                  {
                    class: ['product-hero__tab', attrs.class],
                    type: 'button',
                    onClick: () => {
                      // In a real app, v-tab communicates with v-tabs to update the model.
                      // For this unit test, we can't easily rely on that without a smarter stub.
                      // Tests usually trigger the update on the component or modify the prop.
                    },
                  },
                  slots.default?.()
                )
            },
          }),
          'v-row': defineComponent({
            name: 'VRowStub',
            setup(_, { slots }) {
              return () => h('div', { class: 'v-row-stub' }, slots.default?.())
            },
          }),
          'v-col': defineComponent({
            name: 'VColStub',
            setup(_, { slots }) {
              return () => h('div', { class: 'v-col-stub' }, slots.default?.())
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

    const tabs = wrapper.findAll('.product-hero__tab')
    expect(tabs).toHaveLength(2)
    // Checking active class might be harder with stubs if we don't implement the v-model logic in the stub.
    // Instead we can check the component's internal state OR manually trigger value update.

    // Check initial state (should be 'occasion' as per logic [fallback to first available])
    expect(wrapper.get('.product-hero__price-value').text()).toBe('649')

    // Simulate clicking the "new" tab (second one)
    // Since our stub doesn't automatically update the parent v-model, we simulate the effect by setting the component state directly
    // OR we can make the VTabStub emit an event that we listen for?
    // The easiest way is directly interacting with the vm for state changes that depend on UI library internals.
    // But better: let's update the 'selectedCondition' ref.
    // Note: The original test clicked the chip.
    // Let's manually set the 'selectedCondition' to 'new'    // Simulate switching logic by emitting update:modelValue from v-tabs
    const vTabs = wrapper.findComponent({ name: 'VTabsStub' })
    await vTabs.vm.$emit('update:modelValue', 'new')

    // Assert the view updated
    expect(wrapper.get('.product-hero__price-value').text()).toBe('799')
    expect(wrapper.get('.product-hero__price-currency').text()).toBe('€')
    expect(wrapper.get('.product-hero__price-merchant-link').text()).toContain(
      'Shop'
    )

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

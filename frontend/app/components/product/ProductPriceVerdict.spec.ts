import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import ProductPriceVerdict from './ProductPriceVerdict.vue'
import { createI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { attrs, slots }) {
    return () => h('div', { ...attrs, class: 'v-card-stub' }, slots.default?.())
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: { icon: String },
  setup(props, { attrs }) {
    return () =>
      h('span', { ...attrs, 'data-icon': props.icon, class: 'v-icon-stub' })
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { attrs, slots }) {
    return () =>
      h('span', { ...attrs, class: 'v-chip-stub' }, slots.default?.())
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      'product.verdict.priceVerdictTitle': 'Price Verdict',
      'product.verdict.currentPriceLabel': 'Current price',
      'product.verdict.medianPriceLabel': 'Historical median',
      'product.verdict.priceDescriptions.good':
        'The current price is {percent}% lower than the historical median price of this product.',
      'product.verdict.priceDescriptions.poor':
        'The current price is {percent}% higher than the historical median price of this product.',
      'product.verdict.priceDescriptions.fair':
        'The current price is close (deviation of {percent}%) to the historical median price of this product.',
      'product.verdict.priceDescriptions.insufficient':
        'We do not have enough historical data to evaluate this price.',
      'product.verdict.priceLevels.good': 'Good Price',
      'product.verdict.priceLevels.fair': 'Average Price',
      'product.verdict.priceLevels.poor': 'High Price',
      'product.verdict.priceLevels.insufficient': 'Insufficient data',
    },
  },
})

describe('ProductPriceVerdict', () => {
  const globalOptions = {
    plugins: [i18n],
    stubs: {
      VCard: VCardStub,
      'v-card': VCardStub,
      VIcon: VIconStub,
      'v-icon': VIconStub,
      VChip: VChipStub,
      'v-chip': VChipStub,
    },
  }

  const makeProductWithHistory = (
    currentPrice: number,
    entries: number[]
  ): ProductDto => {
    return {
      offers: {
        bestPrice: { price: currentPrice, currency: 'EUR' },
        newHistory: {
          entries: entries.map((price, idx) => ({
            timestamp: Date.now() - idx * 24 * 60 * 60 * 1000,
            price,
          })),
        },
      },
    } as ProductDto
  }

  it('renders nothing if no product is provided or no offers exist', () => {
    const wrapper = mount(ProductPriceVerdict, {
      props: {
        product: {} as ProductDto,
      },
      global: globalOptions,
    })
    expect(wrapper.html()).toBe('<!--v-if-->')
  })

  it('renders good price verdict when current price is lower than historical median', () => {
    // Median of [100, 110, 120] is 110. Current price is 90 (which is -18% deviation).
    const product = makeProductWithHistory(90, [100, 110, 120])
    const wrapper = mount(ProductPriceVerdict, {
      props: { product },
      global: globalOptions,
    })

    expect(wrapper.text()).toContain('Good Price')
    expect(wrapper.text()).toContain('18% lower')
  })

  it('renders high price verdict when current price is higher than historical median', () => {
    // Median of [100, 110, 120] is 110. Current price is 130 (which is +18% deviation).
    const product = makeProductWithHistory(130, [100, 110, 120])
    const wrapper = mount(ProductPriceVerdict, {
      props: { product },
      global: globalOptions,
    })

    expect(wrapper.text()).toContain('High Price')
    expect(wrapper.text()).toContain('18% higher')
  })

  it('renders fair price verdict when current price is close to historical median', () => {
    // Median of [100, 110, 120] is 110. Current price is 112 (which is +2% deviation, between -5% and 5%).
    const product = makeProductWithHistory(112, [100, 110, 120])
    const wrapper = mount(ProductPriceVerdict, {
      props: { product },
      global: globalOptions,
    })

    expect(wrapper.text()).toContain('Average Price')
    expect(wrapper.text()).toContain('deviation of 2%')
  })

  it('renders nothing (v-if hides it) when history entries count is less than 3', () => {
    const product = makeProductWithHistory(100, [100, 110])
    const wrapper = mount(ProductPriceVerdict, {
      props: { product },
      global: globalOptions,
    })

    expect(wrapper.html()).toBe('<!--v-if-->')
  })
})

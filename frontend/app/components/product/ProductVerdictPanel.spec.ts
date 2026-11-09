import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ProductVerdictPanel from './ProductVerdictPanel.vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      product: {
        verdict: {
          title: 'Verdict',
          dimensions: {
            impact: 'Impact Score',
            price: 'Price',
            reliability: 'Reliability',
          },
          levels: { insufficient: 'Weak', unrated: 'Not rated' },
          priceLevels: { insufficient: 'n/a' },
          reliabilityLevels: { insufficient: 'n/a' },
          vsMedian: 'vs median',
        },
      },
    },
  },
})

describe('ProductVerdictPanel', () => {
  it('shows "Not rated" instead of "Weak" for a product without a score', () => {
    const wrapper = mount(ProductVerdictPanel, {
      props: { product: {} as never },
      global: {
        plugins: [i18n],
        stubs: {
          'v-icon': true,
          ProductVerdictDimensionChip: {
            props: ['title', 'value', 'tooltip'],
            template: '<div class="chip">{{ title }}:{{ value }}</div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('Impact Score:Not rated')
    expect(wrapper.text()).not.toContain('Weak')
  })
})

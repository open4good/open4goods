import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactSubscoreCard from './ProductImpactSubscoreCard.vue'
import type { ScoreView } from './impact-types'

vi.mock('vue-echarts', () => ({
  default: defineComponent({
    name: 'VueEChartsStub',
    props: ['option'],
    setup() {
      return () => h('div', { class: 'echart-stub' })
    },
  }),
}))

describe('ProductImpactSubscoreCard', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            absoluteValue: 'Absolute value',
          },
        },
      },
    },
  })

  const score: ScoreView = {
    id: 'CO2',
    label: 'CO2 emissions',
    description: 'Lower is better',
    relativeValue: 3.6,
    absoluteValue: 42.5,
    energyLetter: 'A',
    distribution: [
      { label: '1', value: 5 },
      { label: '2', value: 10 },
    ],
  }

  it('renders score information and energy badge', () => {
    const wrapper = mount(ProductImpactSubscoreCard, {
      props: { score, productName: 'Demo Product' },
      global: {
        plugins: [i18n],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score'],
            setup(props) {
              return () => h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
        },
      },
    })

    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Absolute value')
    expect(wrapper.text()).toContain('42.5')
    expect(wrapper.text()).toContain('A')
  })
})

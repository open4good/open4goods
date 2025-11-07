import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactSubscoreGenericCard from './ProductImpactSubscoreGenericCard.vue'
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

describe('ProductImpactSubscoreGenericCard', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            absoluteValue: 'Absolute value',
            explanationTitle: 'Explanation',
            percentile: 'Percentile',
            weightChip: 'Counts for {value}% of the total score',
            subscoreDetailsToggle: 'View indicator details',
            tableHeaders: {
              ranking: 'Rank',
            },
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
    coefficient: 0.42,
    ranking: 3,
    on20: 17.3,
    metadatas: {
      coverage: 'High',
    },
  }

  it('renders the header, score, chart and explanation details', () => {
    const wrapper = mount(ProductImpactSubscoreGenericCard, {
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
          ImpactCoefficientBadge: defineComponent({
            name: 'ImpactCoefficientBadgeStub',
            props: ['value'],
            setup(props) {
              return () => h('span', { class: 'coefficient-stub' }, `coefficient:${props.value}`)
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
          ClientOnly: defineComponent({
            name: 'ClientOnlyStub',
            setup(_, { slots }) {
              return () => slots.default?.()
            },
          }),
        },
      },
    })

    expect(wrapper.text()).toContain('coefficient:0.42')
    expect(wrapper.text()).toContain('17.3')
    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Absolute value')
    expect(wrapper.text()).toContain('42.5')
    expect(wrapper.text()).toContain('Explanation')
    expect(wrapper.text()).toContain('Lower is better')
    expect(wrapper.text()).toContain('View indicator details')
    expect(wrapper.text()).not.toContain('Percentile')
    expect(wrapper.text()).toContain('Rank')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('High')
  })
})

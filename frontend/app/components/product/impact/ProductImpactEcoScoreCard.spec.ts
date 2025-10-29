import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactEcoScoreCard from './ProductImpactEcoScoreCard.vue'
import type { ScoreView } from './impact-types'

describe('ProductImpactEcoScoreCard', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            primaryScoreLabel: 'Overall impact',
            absoluteValue: 'Absolute value',
            noPrimaryScore: 'No score',
          },
        },
      },
    },
  })

  const stubScore: ScoreView = {
    id: 'ECOSCORE',
    label: 'Eco score',
    description: 'Overall environmental score',
    relativeValue: 4.2,
    absoluteValue: 78.123,
  }

  it('renders the impact score using ImpactScore component', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: stubScore },
      global: {
        plugins: [i18n],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score', 'max', 'showValue', 'size'],
            setup(props) {
              return () => h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
        },
      },
    })

    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('score:4.2')
    expect(wrapper.text()).not.toContain('Absolute value')
  })

  it('renders a placeholder message when the score is missing', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: null },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('No score')
  })
})

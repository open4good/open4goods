import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactEcoScoreCard from './ProductImpactEcoScoreCard.vue'
import type { ScoreView } from './impact-types'

describe('ProductImpactEcoScoreCard', () => {
  vi.stubGlobal('$fetch', vi.fn().mockResolvedValue({}))

  const i18n = createI18n({
    legacy: false,
    locale: 'en-US',
    messages: {
      'en-US': {
        product: {
          impact: {
            absoluteValue: 'Absolute value',
            noPrimaryScore: 'No score',
            notRated: {
              title: 'No score',
              description:
                "We don't have enough data to calculate an impact score for this product yet.",
            },
            methodologyLink: 'Access the methodology',
            methodologyLinkAria: 'Open the Impact Score methodology',
            showVirtualScores: 'Show virtual scores',
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
    value: 3.6,
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
              return () =>
                h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
          NuxtLink: defineComponent({
            name: 'NuxtLinkStub',
            props: ['to', 'ariaLabel'],
            setup(props, { slots }) {
              return () =>
                h(
                  'a',
                  {
                    class: 'nuxt-link-stub',
                    href: typeof props.to === 'string' ? props.to : '#',
                    'data-to': props.to,
                  },
                  slots.default?.()
                )
            },
          }),
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: ['icon', 'size'],
            template: '<span class="v-icon-stub"></span>',
          }),
          'v-chip-group': true,
          'v-chip': true,
          'v-checkbox': true,
          'v-btn': {
            template:
              '<button class="v-btn-stub" v-bind="$attrs"><slot /></button>',
          },
        },
      },
    })

    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('score:3.6')
    expect(wrapper.find('.v-btn-stub').text()).toContain(
      'Access the methodology'
    )
    expect(wrapper.text()).not.toContain('Absolute value')
  })

  it('renders a placeholder message when the score is missing', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: null },
      global: {
        plugins: [i18n],
        stubs: {
          NuxtLink: true,
        },
      },
    })

    expect(wrapper.text()).toContain('No score')
  })

  it('uses value (0-5) score directly', () => {
    const scoreWithDifferentValues: ScoreView = {
      ...stubScore,
      value: 3.5,
      on20: 20, // Should be ignored (20/4 = 5, but value is 3.5)
    }

    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: scoreWithDifferentValues },
      global: {
        plugins: [i18n],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score'],
            setup(props) {
              return () =>
                h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
          NuxtLink: true,
          RouterLink: true,
          'v-icon': true,
          'v-chip-group': true,
          'v-chip': true,
          'v-checkbox': true,
          'v-btn': true,
        },
      },
    })

    expect(wrapper.text()).toContain('score:3.5')
  })
})

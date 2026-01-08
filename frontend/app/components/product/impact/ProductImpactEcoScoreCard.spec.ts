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
          'v-btn': true,
          'v-expand-transition': true,
          'v-skeleton-loader': true,
        },
      },
    })

    expect(wrapper.find('.impact-score-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('score:3.6')
    expect(wrapper.find('.impact-ecoscore__cta').text()).toContain(
      'Access the methodology'
    )
    expect(wrapper.text()).not.toContain('Absolute value')
  })

  it('renders a placeholder message when the score is missing', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: null },
      global: { plugins: [i18n] },
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
          'v-icon': true,
          'v-chip-group': true,
          'v-chip': true,
          'v-checkbox': true,
          'v-btn': true,
          'v-expand-transition': true,
          'v-skeleton-loader': true,
        },
      },
    })

    expect(wrapper.text()).toContain('score:3.5')
  })

  it('hides virtual scores by default and shows them when toggled', async () => {
    const virtualScore: ScoreView = {
      ...stubScore,
      id: 'VIRTUAL',
      virtual: true,
    }
    const scores = [stubScore, virtualScore]

    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: {
        score: stubScore,
        detailScores: scores,
      },
      global: {
        plugins: [i18n],
        stubs: {
          ImpactScore: true,
          NuxtLink: true,
          'v-icon': true,
          'v-chip-group': true,
          'v-chip': true,
          'v-checkbox': {
            template:
              '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
            props: ['modelValue'],
          },
          'v-btn': true,
          'v-expand-transition': true,
          'v-skeleton-loader': true,
          ProductImpactDetailsTable: {
            props: ['scores'],
            template:
              '<div><div v-for="s in scores" :key="s.id" class="score-row">{{ s.id }}</div></div>',
          },
        },
      },
    })

    // Default: virtual score hidden
    // Only ECOSCORE is not virtual (stubScore) but ECOSCORE is usually filtered out by the table component itself if it matches ID,
    // but here we pass it as detailScores.
    // Let's see filtered logic: detailScores.value.filter...
    // The component filters by virtual.

    expect(wrapper.findAll('.score-row').length).toBe(1)
    expect(wrapper.text()).toContain('ECOSCORE')
    expect(wrapper.text()).not.toContain('VIRTUAL')

    // Toggle virtual scores
    await wrapper.find('input[type="checkbox"]').setValue(true)

    expect(wrapper.findAll('.score-row').length).toBe(2)
    expect(wrapper.text()).toContain('VIRTUAL')
  })
})

import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import ProductImpactEcoScoreCard from './ProductImpactEcoScoreCard.vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
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
          },
        },
        components: {
          impactScore: {
            tooltip: 'Score: {value} / {max}',
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
          NuxtLink: defineComponent({
            name: 'NuxtLinkStub',
            props: ['to', 'ariaLabel'],
            setup(props, { slots }) {
              return () =>
                h(
                  'a',
                  { class: 'nuxt-link-stub', href: typeof props.to === 'string' ? props.to : '#', 'data-to': props.to },
                  slots.default?.(),
                )
            },
          }),
          'v-tooltip': defineComponent({
            name: 'VTooltipStub',
            props: ['text'],
            setup(props, { slots }) {
              return () =>
                h(
                  'div',
                  { class: 'v-tooltip-stub', 'data-text': props.text },
                  slots.activator?.({ props: {} }) ?? slots.default?.(),
                )
            },
          }),
          'v-rating': defineComponent({
            name: 'VRatingStub',
            props: ['modelValue', 'length', 'size', 'color', 'bgColor', 'density', 'halfIncrements', 'readonly'],
            setup(props) {
              return () =>
                h('div', {
                  class: 'v-rating-stub',
                  'data-model-value': props.modelValue,
                  'data-length': props.length,
                  'data-size': props.size,
                  'data-color': props.color,
                  'data-bg-color': props.bgColor,
                  'data-density': props.density,
                  'data-half-increments': props.halfIncrements,
                  'data-readonly': props.readonly,
                })
            },
          }),
          'v-icon': defineComponent({
            name: 'VIconStub',
            props: ['icon', 'size'],
            template: '<span class="v-icon-stub"></span>',
          }),
        },
      },
    })

    const impactScore = wrapper.findComponent(ImpactScore)

    expect(impactScore.exists()).toBe(true)
    expect(wrapper.find('.impact-score').text()).toContain('3.6 / 5')
    expect(wrapper.find('.impact-score').attributes('aria-label')).toBe('Score: 3.6 / 5')
    expect(wrapper.find('.impact-ecoscore__cta').text()).toContain('Access the methodology')
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

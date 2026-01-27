import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import { createVuetify } from 'vuetify'
import ProductImpactEcoScoreCard from './ProductImpactEcoScoreCard.vue'
import type { ScoreView } from './impact-types'

describe('ProductImpactEcoScoreCard', () => {
  vi.stubGlobal('$fetch', vi.fn().mockResolvedValue({}))

  const vuetify = createVuetify()

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

  const NuxtLinkStub = defineComponent({
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
  })

  it('renders the impact score using ImpactScore component', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: {
        score: stubScore,
        showRadar: true,
        radarAxes: [
          { id: '1', name: 'A', attributeValue: null },
          { id: '2', name: 'B', attributeValue: null },
          { id: '3', name: 'C', attributeValue: null },
        ],
        chartSeries: [
          {
            label: 'S',
            values: [],
            lineColor: '',
            areaColor: '',
            symbolColor: '',
          },
        ],
      },
      global: {
        plugins: [i18n, vuetify],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score', 'max', 'showValue', 'size'],
            setup(props) {
              return () =>
                h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
          NuxtLink: NuxtLinkStub,
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
    expect(wrapper.text()).toContain('score:14.4')
    expect(wrapper.text()).not.toContain('Absolute value')
  })

  it('renders a placeholder message when the score is missing', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: { score: null },
      global: {
        plugins: [i18n, vuetify],
        stubs: {
          NuxtLink: NuxtLinkStub,
        },
      },
    })

    expect(wrapper.text()).toContain('No score')
    expect(wrapper.text()).toContain('Access the methodology')
  })

  it('uses value (0-5) score directly', () => {
    const wrapper = mount(ProductImpactEcoScoreCard, {
      props: {
        score: stubScore,
        showRadar: true,
        radarAxes: [
          { id: '1', name: 'A', attributeValue: null },
          { id: '2', name: 'B', attributeValue: null },
          { id: '3', name: 'C', attributeValue: null },
        ],
        chartSeries: [
          {
            label: 'S',
            values: [],
            lineColor: '',
            areaColor: '',
            symbolColor: '',
          },
        ],
      },
      global: {
        plugins: [i18n, vuetify],
        stubs: {
          ImpactScore: defineComponent({
            name: 'ImpactScoreStub',
            props: ['score'],
            setup(props) {
              return () =>
                h('div', { class: 'impact-score-stub' }, `score:${props.score}`)
            },
          }),
          NuxtLink: NuxtLinkStub,
          RouterLink: true,
          'v-icon': true,
          'v-chip-group': true,
          'v-chip': true,
          'v-checkbox': true,
          'v-btn': true,
        },
      },
    })

    // Component uses normalizedScore which is stubScore.value (3.6) * 4 = 14.4
    expect(wrapper.text()).toContain('score:14.4')
  })
})

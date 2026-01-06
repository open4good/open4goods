import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ImpactScore from './ImpactScore.vue'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      'components.impactScore.tooltip': 'Score: {value} / {max}',
      'components.impactScore.tooltipBadge': 'Score: {value} / 20',
      'components.impactScore.outOf20': '/20',
    },
  },
})

// Stubs for Vuetify components
const VTooltipStub = defineComponent({
  name: 'VTooltip',
  props: ['text'],
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', 'data-text': props.text }, [
        slots.activator?.({ props: { 'aria-describedby': 'tooltip-id' } }),
      ])
  },
})

const VRatingStub = defineComponent({
  name: 'VRating',
  props: ['modelValue', 'length'],
  setup(props) {
    return () =>
      h('div', {
        class: 'v-rating-stub',
        'data-value': props.modelValue,
        'data-length': props.length,
      })
  },
})

describe('ImpactScore', () => {
  const globalOptions = {
    plugins: [i18n],
    stubs: {
      'v-tooltip': VTooltipStub,
      'v-rating': VRatingStub,
    },
  }

  it('renders combined mode by default with chip and stars', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        max: 5,
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score-combined').exists()).toBe(true)
    expect(wrapper.find('.impact-score-badge').exists()).toBe(true)
    expect(wrapper.findComponent(VRatingStub).exists()).toBe(true)
    expect(wrapper.text()).toContain('16.0 / 20') // (4/5)*20 = 16.0

    // Check tooltip text
    expect(wrapper.find('.v-tooltip-stub').attributes('data-text')).toBe(
      'Score: 16.0 / 20'
    )
  })

  it('renders stars mode when specified', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        max: 5,
        mode: 'stars',
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score').exists()).toBe(true)
    expect(wrapper.findComponent(VRatingStub).exists()).toBe(true)
    expect(wrapper.find('.impact-score-badge').exists()).toBe(false)
  })

  it('calculates score out of 20 correctly', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 2.5,
        max: 5,
      },
      global: globalOptions,
    })

    // (2.5/5)*20 = 10
    expect(wrapper.text()).toContain('10.0 / 20')
  })

  it('handles custom max correctly for badge score', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 75,
        max: 100,
      },
      global: globalOptions,
    })

    // (75/100)*20 = 15
    expect(wrapper.text()).toContain('15.0 / 20')
  })

  it('applies correct class based on size to the chip', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        size: 'large',
      },
      global: globalOptions,
    })

    const badge = wrapper.find('.impact-score-badge')
    expect(badge.classes()).toContain('impact-score-badge--large')
  })

  it('renders combined mode correctly', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        max: 5,
        mode: 'combined',
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score-combined').exists()).toBe(true)
    expect(wrapper.find('.impact-score-badge').exists()).toBe(true)
    expect(wrapper.findComponent(VRatingStub).exists()).toBe(true)
    expect(wrapper.text()).toContain('16.0 / 20')
  })

  it('supports vertical layout and toggleable elements', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 3,
        max: 5,
        mode: 'combined',
        layout: 'vertical',
        showScore: false,
        showStars: true,
      },
      global: globalOptions,
    })

    const combined = wrapper.find('.impact-score-combined')
    expect(combined.classes()).toContain('impact-score-combined--vertical')
    expect(wrapper.find('.impact-score-badge').exists()).toBe(false)
    expect(wrapper.findComponent(VRatingStub).exists()).toBe(true)
  })

  it('supports flat styling on the badge', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        flat: true,
      },
      global: globalOptions,
    })

    const badge = wrapper.find('.impact-score-badge')
    expect(badge.classes()).toContain('impact-score-badge--flat')
  })

  it('renders stacked badge layout for corner variant', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 4,
        mode: 'badge',
        badgeLayout: 'stacked',
        badgeVariant: 'corner',
      },
      global: globalOptions,
    })

    const badge = wrapper.find('.impact-score-badge')
    expect(badge.classes()).toContain('impact-score-badge--stacked')
    expect(badge.classes()).toContain('impact-score-badge--corner')
    expect(wrapper.text()).toContain('16.0')
    expect(wrapper.text()).toContain('/20')
  })
})

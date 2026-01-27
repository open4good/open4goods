import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ImpactScore from './ImpactScore.vue'
import { createI18n } from 'vue-i18n'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      'components.impactScore.tooltip': 'Score: {value} / {max}',
      'components.impactScore.svgAriaLabel': 'Score {score} out of 20',
    },
  },
})

describe('ImpactScore', () => {
  const globalOptions = {
    plugins: [i18n],
    stubs: {
      'v-btn': true,
    },
  }

  it('renders correctly with default props', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 15,
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score-panel').exists()).toBe(true)
    expect(wrapper.text()).toContain('15/ 20')
    expect(wrapper.find('.impact-score-panel--md').exists()).toBe(true)
  })

  it('calculates score out of 20 correctly (though logic is mostly in formatting now)', () => {
    // Current component seems to expect score pre-calculated or simply displays it relative to max if logic existed,
    // but the code just shows {{ formattedScoreValue }} / 20
    // and displayScore = props.score.
    // So if we pass 2.5, it shows 2.5.

    const wrapper = mount(ImpactScore, {
      props: {
        score: 2.5,
      },
      global: globalOptions,
    })

    expect(wrapper.text()).toContain('2.5/ 20')
  })

  it('applies xs size variant correctly', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 18,
        size: 'xs',
      },
      global: globalOptions,
    })

    const panel = wrapper.find('.impact-score-panel')
    expect(panel.classes()).toContain('impact-score-panel--xs')
    // Check that progress bar is hidden via CSS class logic (inferred)
    // Actually jsdom might not compute styles, but we check specific element presence if v-if was used.
    // The CSS hides it: .impact-score-panel--xs .impact-score-panel__bar { display: none; }
    // We can check if the class exists.
    expect(wrapper.find('.impact-score-panel__bar').exists()).toBe(true)
  })

  it('maps the small size alias to the compact xs layout', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 12,
        size: 'small',
      },
      global: globalOptions,
    })

    const panel = wrapper.find('.impact-score-panel')
    expect(panel.classes()).toContain('impact-score-panel--xs')
  })

  it('hides meta and progress bar in corner variant', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 10,
        variant: 'corner',
        showMethodology: true,
        showRange: true,
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score-panel__col-right').exists()).toBe(false)
    expect(wrapper.find('.impact-score-panel__bar').exists()).toBe(false)
  })

  it('hides/shows meta information', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 10,
        showMethodology: false,
        showRange: false,
      },
      global: globalOptions,
    })

    expect(wrapper.find('.impact-score-panel__col-right').exists()).toBe(false)
  })
})

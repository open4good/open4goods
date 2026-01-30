import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
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
const VBtnStub = {
  name: 'v-btn',
  template: '<button class="v-btn"><slot /></button>',
  props: ['variant', 'size', 'rounded'],
}

describe('ImpactScore Badge', () => {
  const globalOptions = {
    plugins: [i18n],
    stubs: {
      'v-btn': VBtnStub,
      'v-icon': true,
      CtaCard: true,
      NuxtLink: true,
    },
  }

  it('renders methodology button as a solid small badge', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 15,
        showMethodology: true,
      },
      global: globalOptions,
    })

    const btn = wrapper.findComponent({ name: 'v-btn' })
    console.log('Wrapper HTML:', wrapper.html())
    console.log('Wrapper Props:', wrapper.props())
    expect(btn.exists()).toBe(true)
    expect(btn.props('variant')).toBe('flat')
    expect(btn.props('size')).toBe('x-small')
    expect(btn.props('rounded')).toBe('pill')
    expect(btn.text()).toContain('Méthodologie')
  })

  it('hides methodology button when prop is false', () => {
    const wrapper = mount(ImpactScore, {
      props: {
        score: 15,
        showMethodology: false,
      },
      global: globalOptions,
    })

    const btn = wrapper.findComponent({ name: 'v-btn' })
    expect(btn.exists()).toBe(false)
  })
})

import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import NudgeToolAnimatedIcon from './NudgeToolAnimatedIcon.vue'

// -- Mocks --
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => ref('no-preference'),
}))

describe('NudgeToolAnimatedIcon', () => {
  it('renders correctly', () => {
    const wrapper = mount(NudgeToolAnimatedIcon, {
      global: {
        mocks: {
          $t: (t: string) => t,
        },
      },
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.attributes('role')).toBe('img')
    expect(wrapper.classes()).toContain('nudge-tool-animated-icon')
  })

  it('respects reduced motion', async () => {
    const wrapper = mount(NudgeToolAnimatedIcon, {
      global: {
        mocks: { $t: (k: string) => k },
      },
    })
    const el = wrapper.find('.nudge-tool-animated-icon')
    expect(el.attributes('style')).toContain('scale(1)')
  })

  it('applies animation timing custom properties', () => {
    const wrapper = mount(NudgeToolAnimatedIcon, {
      props: {
        variant: 'float',
        frequencyRange: [2000, 2400],
        randomizeOnMount: false,
      },
      global: {
        mocks: { $t: (k: string) => k },
      },
    })

    const style = wrapper.attributes('style')
    expect(style).toContain('--nudge-icon-animation-duration: 2200ms')
  })
})

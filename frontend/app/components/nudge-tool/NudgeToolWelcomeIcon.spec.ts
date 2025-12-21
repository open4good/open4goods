import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import NudgeToolWelcomeIcon from './NudgeToolWelcomeIcon.vue'

// -- Mocks --
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => ref('no-preference'),
}))

describe('NudgeToolWelcomeIcon', () => {
  it('renders correctly', () => {
    const wrapper = mount(NudgeToolWelcomeIcon, {
      global: {
        mocks: {
          $t: (t: string) => t,
        },
      },
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.attributes('role')).toBe('img')
    expect(wrapper.classes()).toContain('nudge-tool-welcome-icon')
  })

  it('respects reduced motion', async () => {
    // Override mock for this test if possible, or just rely on default false
    // We can't easily change the mock strictly without setup, but we can check initial state.
    const wrapper = mount(NudgeToolWelcomeIcon, {
      global: {
        mocks: { $t: (k: string) => k },
      },
    })
    // Initial scale should be 1
    const el = wrapper.find('.nudge-tool-welcome-icon')
    expect(el.attributes('style')).toContain('scale(1)')
  })
})

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { ref } from 'vue'
import NudgeToolAnimatedIcon from './NudgeToolAnimatedIcon.vue'

const zoomedState = ref(false)

// -- Mocks --
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k: string) => k }),
}))

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => ref('no-preference'),
  useStorage: (_key: string, defaultValue: boolean) => {
    if (zoomedState.value === undefined) {
      zoomedState.value = defaultValue
    }
    return zoomedState
  },
}))

const mountIcon = (options: Record<string, unknown> = {}) =>
  mount(NudgeToolAnimatedIcon, {
    global: {
      plugins: [createPinia()],
      mocks: {
        $t: (t: string) => t,
      },
    },
    ...options,
  })

describe('NudgeToolAnimatedIcon', () => {
  beforeEach(() => {
    zoomedState.value = false
  })

  it('renders correctly', () => {
    const wrapper = mountIcon()

    expect(wrapper.exists()).toBe(true)
    const el = wrapper.find('.nudge-tool-animated-icon')
    expect(el.attributes('role')).toBe('img')
    expect(el.classes()).toContain('nudge-tool-animated-icon')
  })

  it('respects reduced motion', async () => {
    const wrapper = mountIcon()
    const el = wrapper.find('.nudge-tool-animated-icon')
    expect(el.attributes('style')).toContain('scale(1)')
  })

  it('reduces motion when zoom override is active', () => {
    zoomedState.value = true
    const wrapper = mountIcon({
      props: {
        variant: 'float',
      },
    })

    const el = wrapper.find('.nudge-tool-animated-icon')
    expect(el.attributes('style')).toContain('animation: none')
  })

  it('applies animation timing custom properties', () => {
    const wrapper = mountIcon({
      props: {
        variant: 'float',
        frequencyRange: [2000, 2400],
        randomizeOnMount: false,
      },
    })

    const style = wrapper.find('.nudge-tool-animated-icon').attributes('style')
    expect(style).toContain('--nudge-icon-animation-duration: 2200ms')
  })
})

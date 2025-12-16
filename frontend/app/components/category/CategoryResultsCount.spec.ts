import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { nextTick, ref } from 'vue'

import CategoryResultsCount from './CategoryResultsCount.vue'

const reducedMotionPreference = ref<'reduce' | 'no-preference'>('no-preference')

vi.mock('@vueuse/core', async () => {
  const actual =
    await vi.importActual<typeof import('@vueuse/core')>('@vueuse/core')

  return {
    ...actual,
    usePreferredReducedMotion: () => reducedMotionPreference,
  }
})

describe('CategoryResultsCount', () => {
  let originalRequestAnimationFrame: typeof requestAnimationFrame
  let originalCancelAnimationFrame: typeof cancelAnimationFrame
  let currentTime: number

  beforeEach(() => {
    vi.useFakeTimers()
    currentTime = 0
    reducedMotionPreference.value = 'no-preference'

    originalRequestAnimationFrame = globalThis.requestAnimationFrame
    originalCancelAnimationFrame = globalThis.cancelAnimationFrame

    globalThis.requestAnimationFrame = (callback: FrameRequestCallback) => {
      const timeoutId = setTimeout(() => {
        currentTime += 16
        callback(currentTime)
      }, 16)

      return timeoutId as unknown as number
    }

    globalThis.cancelAnimationFrame = (handle: number) => {
      clearTimeout(handle)
    }
  })

  afterEach(() => {
    vi.useRealTimers()
    globalThis.requestAnimationFrame = originalRequestAnimationFrame
    globalThis.cancelAnimationFrame = originalCancelAnimationFrame
  })

  const createWrapper = (count: number) => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en-US',
      messages: {
        'en-US': {
          category: {
            products: {
              resultsCount: {
                one: '{count} product',
                other: '{count} products',
              },
            },
          },
          filters: {
            ecoscore: {
              title: '',
              description: '',
              cta: '',
              ariaLabel: '',
            },
          },
        },
      },
    })

    return mount(CategoryResultsCount, {
      props: { count },
      global: { plugins: [i18n] },
    })
  }

  it('renders the initial count with pluralisation', () => {
    const wrapper = createWrapper(1)

    expect(wrapper.text()).toBe('1 product')
  })

  it('animates toward the next value when the count increases', async () => {
    const wrapper = createWrapper(5)

    await wrapper.setProps({ count: 12 })

    vi.advanceTimersByTime(620)
    await nextTick()

    expect(wrapper.text()).toBe('12 products')
  })

  it('animates toward the next value when the count decreases', async () => {
    const wrapper = createWrapper(20)

    await wrapper.setProps({ count: 3 })

    vi.advanceTimersByTime(620)
    await nextTick()

    expect(wrapper.text()).toBe('3 products')
  })

  it('updates instantly when reduced motion is preferred', async () => {
    reducedMotionPreference.value = 'reduce'
    const wrapper = createWrapper(8)

    await wrapper.setProps({ count: 15 })
    await nextTick()

    expect(wrapper.text()).toBe('15 products')
  })
})

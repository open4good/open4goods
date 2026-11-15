import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

import SectionReveal from './SectionReveal.vue'

const motion = vi.hoisted(() => ({ preference: 'reduce' }))

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => ref(motion.preference),
  useStorage: (_key: string, defaultValue: boolean) => ref(defaultValue),
}))

type ObserverCallback = (
  entries: Array<Pick<IntersectionObserverEntry, 'isIntersecting'>>,
  observer: IntersectionObserver
) => void

// The compiled component uses Vuetify's own v-intersect directive, which
// drives a real IntersectionObserver: fake that instead of the directive.
const observerCallbacks: ObserverCallback[] = []

class FakeIntersectionObserver {
  constructor(callback: ObserverCallback) {
    observerCallbacks.push(callback)
  }

  observe = vi.fn()
  unobserve = vi.fn()
  disconnect = vi.fn()
  takeRecords = vi.fn(() => [])
}

const mountReveal = (props: Record<string, unknown> = {}) => {
  const wrapper = mount(SectionReveal, {
    props,
    slots: {
      default: '<p>Editorial content</p>',
    },
    global: {
      plugins: [createPinia()],
    },
  })

  const intersect = async (isIntersecting: boolean) => {
    const observer = new FakeIntersectionObserver(() => {})
    observerCallbacks.forEach(callback =>
      callback(
        [{ isIntersecting }],
        observer as unknown as IntersectionObserver
      )
    )
    await wrapper.vm.$nextTick()
  }

  return { wrapper, intersect }
}

describe('SectionReveal', () => {
  beforeEach(() => {
    motion.preference = 'reduce'
    observerCallbacks.length = 0
    vi.stubGlobal('IntersectionObserver', FakeIntersectionObserver)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('does not force centered text or alignment classes on slot content', () => {
    const { wrapper } = mountReveal()

    const content = wrapper.get('.section-reveal__content')

    expect(content.classes()).not.toContain('text-center')
    expect(content.classes()).not.toContain('align-center')
  })

  it('keeps the content laid out before it scrolls into view (no layout shift)', () => {
    motion.preference = 'no-preference'
    const { wrapper } = mountReveal({ transition: 'slide-y' })

    const content = wrapper.get('.section-reveal__content')

    expect(content.text()).toBe('Editorial content')
    expect(content.attributes('style') ?? '').not.toContain('display: none')
    expect(content.classes()).toContain('section-reveal__content--hidden')
    expect(content.classes()).toContain('section-reveal__content--slide-y')
  })

  it('reveals the content once it intersects', async () => {
    motion.preference = 'no-preference'
    const { wrapper, intersect } = mountReveal()

    await intersect(false)
    expect(wrapper.get('.section-reveal__content').classes()).toContain(
      'section-reveal__content--hidden'
    )

    await intersect(true)
    expect(wrapper.get('.section-reveal__content').classes()).not.toContain(
      'section-reveal__content--hidden'
    )
  })

  it('shows the content immediately when motion is reduced', () => {
    const { wrapper } = mountReveal()

    const content = wrapper.get('.section-reveal__content')

    expect(content.classes()).not.toContain('section-reveal__content--hidden')
    expect(content.classes()).toContain('section-reveal__content--static')
  })
})

import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { useNuxtApp, useState } from '#app'
import HomeHeroSection from './HomeHeroSection.vue'
const messages: Record<string, unknown> = {
  'home.hero.title': 'Nudger : Le comparateur écologique',
  'home.hero.titleSubtitle': ['Acheter mieux. Sans dépenser plus.'],
  'home.hero.background': 'hero-background.webp',
}

const subtitleCollections = {
  default: messages['home.hero.titleSubtitle'] as string[],
}

const resetHeroSubtitleState = () => {
  const seedState = useState<Record<string, number>>(
    'home-hero-variant-seeds',
    () => ({})
  )

  seedState.value = {}

  const nuxtApp = useNuxtApp()
  const state = nuxtApp?.payload?.state

  if (!state) {
    return
  }

  Reflect.deleteProperty(
    state as Record<string, unknown>,
    'home-hero-variant-seeds'
  )
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (
      key: string,
      _choiceOrParams?: number | Record<string, unknown>,
      _params?: Record<string, unknown>
    ) => messages[key] ?? key,
    tm: (key: string) => {
      if (messages[key]) {
        return messages[key]
      }

      if (key === 'home.hero.titleSubtitle') {
        return subtitleCollections.default
      }

      return []
    },
    te: (key: string) => Boolean(messages[key]),
    locale: ref('fr-FR'),
  }),
}))

const createStub = (tag: string, className = '') =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_, { slots, attrs }) {
      return () =>
        h(
          tag,
          { class: [className, attrs.class], ...attrs },
          slots.default ? slots.default() : []
        )
    },
  })

const mountComponent = async () => {
  resetHeroSubtitleState()

  return mountSuspended(HomeHeroSection, {
    global: {
      stubs: {
        HeroSurface: createStub('section'),
        VFadeTransition: createStub('div'),
        VSkeletonLoader: createStub('div'),
        VContainer: createStub('div'),
        VRow: createStub('div'),
        VCol: createStub('div'),
      },
    },
  })
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('HomeHeroSection', () => {
  it('renders the title and subtitle from the home hero copy', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.find('.home-hero__title').text()).toBe(
      messages['home.hero.title']
    )
    expect(wrapper.find('.home-hero__title-subtitle').text()).toBe(
      subtitleCollections.default[0]
    )

    await wrapper.unmount()
  })

  it('uses the background image from the home hero background', async () => {
    const wrapper = await mountComponent()
    const background = wrapper.find('.home-hero__background-media')

    expect(background.attributes('src')).toContain(
      messages['home.hero.background']
    )

    await wrapper.unmount()
  })
})

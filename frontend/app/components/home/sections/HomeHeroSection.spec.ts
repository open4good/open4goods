import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { computed, defineComponent, h, ref } from 'vue'
import { useNuxtApp, useState } from '#app'
import HomeHeroSection from './HomeHeroSection.vue'
import type { EventPackName } from '~~/config/theme/event-packs'

const messages: Record<string, unknown> = {
  'packs.default.hero.title': "Réconcilier écologie et pouvoir d'achat",
  'packs.default.hero.titleSubtitle': ['Acheter mieux. Sans dépenser plus.'],
  'packs.default.hero.background': 'hero-background.webp',
}

const subtitleCollections = {
  default: messages['packs.default.hero.titleSubtitle'] as string[],
}

const activeEventPack = ref<EventPackName>('default')

const resetHeroSubtitleState = () => {
  const seedState = useState<Record<string, number>>(
    'event-pack-variant-seeds',
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
    'event-pack-variant-seeds'
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

      if (key === 'packs.default.hero.titleSubtitle') {
        return subtitleCollections.default
      }

      return []
    },
    te: (key: string) => Boolean(messages[key]),
    locale: ref('fr-FR'),
  }),
}))

vi.mock('~~/app/composables/useSeasonalEventPack', () => ({
  useSeasonalEventPack: () => computed(() => activeEventPack.value),
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
  activeEventPack.value = 'default'
  vi.restoreAllMocks()
})

describe('HomeHeroSection', () => {
  it('renders the title and subtitle from the event pack', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.find('.home-hero__title').text()).toBe(
      messages['packs.default.hero.title']
    )
    expect(wrapper.find('.home-hero__title-subtitle').text()).toBe(
      subtitleCollections.default[0]
    )

    await wrapper.unmount()
  })

  it('uses the background image from the pack override', async () => {
    const wrapper = await mountComponent()
    const background = wrapper.find('.home-hero__background-media')

    expect(background.attributes('src')).toContain(
      messages['packs.default.hero.background']
    )

    await wrapper.unmount()
  })
})

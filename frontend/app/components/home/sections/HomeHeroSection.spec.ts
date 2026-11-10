import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { useNuxtApp, useState } from '#app'
import HomeHeroSection from './HomeHeroSection.vue'
import frFR from '~~/i18n/locales/fr-FR.json'
import enUS from '~~/i18n/locales/en-US.json'
const messages: Record<string, unknown> = {
  'home.hero.title': 'Comparez les prix des marchands partenaires',
  'home.hero.titleSubtitle': [
    'Nudger rassemble les offres de ses marchands partenaires pour vous montrer où acheter au meilleur prix.',
  ],
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
    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 50))

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
    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 50))
    const background = wrapper.find('.home-hero__background-media')

    expect(background.attributes('src')).toContain(
      messages['home.hero.background']
    )

    await wrapper.unmount()
  })

  it('positions the home hero as a partner price comparator in both locales', () => {
    expect(frFR.home.hero.title).toBe(
      'Comparez les prix des marchands partenaires'
    )
    expect(frFR.home.hero.titleSubtitle[0]).toBe(
      'Nudger rassemble les offres de ses marchands partenaires pour vous montrer où acheter au meilleur prix.'
    )
    expect(frFR.pwa.landing.hero.subtitle).toBe(frFR.home.hero.titleSubtitle[0])
    expect(enUS.home.hero.title).toBe("Compare partner merchants' prices")
    expect(enUS.pwa.landing.hero.subtitle).toBe(enUS.home.hero.titleSubtitle[0])
  })
})

import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { computed, defineComponent, h, ref } from 'vue'
import { useNuxtApp, useState } from '#app'
import HomeHeroSection from './HomeHeroSection.vue'
import type { EventPackName } from '~~/config/theme/assets'

const messages: Record<string, unknown> = {
  'home.events.default.hero.eyebrow': 'notre comparateur',
  'home.events.default.hero.title': "Réconcilier écologie et pouvoir d'achat",
  'home.events.default.hero.subtitles': [
    'Gagne du temps. Choisis librement.',
    'Choisis librement.',
  ],
  'home.events.christmas.hero.subtitles': [
    'Noël responsable',
    'Cadeaux en accord avec tes valeurs',
  ],
  'home.events.default.hero.titleSubtitle': ['Acheter mieux. Sans dépenser plus.'],
  'home.events.default.hero.search.label': 'Tu sais déjà ce que tu cherches ?',
  'home.events.default.hero.search.placeholder':
    'Recherchez un produit ou une catégorie',
  'home.events.default.hero.search.ariaLabel': 'Rechercher un produit responsable',
  'home.events.default.hero.search.cta': 'NUDGER',
  'home.events.default.hero.search.partnerLinkLabel':
    '{formattedCount} partenaire | {formattedCount} partenaires',
  'home.events.default.hero.search.partnerLinkFallback': 'nos partenaires',
  'home.events.default.hero.search.helper': 'Comparateur indépendant',
  'home.events.default.hero.search.helpersTitle':
    'Offre avec intention. Compare avec impact.',
  'home.events.default.hero.search.helpers': [
    {
      icon: '🌿',
      label: 'Une évaluation écologique et environnementale unique',
      segments: [
        { text: 'Une évaluation écologique', to: '/impact-score' },
        { text: ' et environnementale unique' },
      ],
    },
    {
      icon: '🏷️',
      label: 'Sans se faire avoir sur les prix',
      segments: [
        { text: 'Sans se faire avoir sur les prix avec' },
        { text: '{partnersLink}', to: '/partenaires' },
      ],
    },
  ],
  'home.events.default.hero.iconAlt': 'Icône du lanceur PWA Nudger',
  'home.events.default.hero.context.ariaLabel':
    'Carte contexte du héros présentant la promesse Nudger',
}

const helperItems = messages['home.events.default.hero.search.helpers'] as unknown[]

const subtitleCollections = {
  default: messages['home.events.default.hero.subtitles'] as string[],
  events: {
    christmas: messages['home.events.christmas.hero.subtitles'] as string[],
  },
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
      choiceOrParams?: number | Record<string, unknown>,
      params?: Record<string, unknown>
    ) => {
      const resolvedParams =
        typeof choiceOrParams === 'object' && choiceOrParams != null
          ? choiceOrParams
          : (params ?? {})

      const count =
        typeof choiceOrParams === 'number'
          ? choiceOrParams
          : (resolvedParams as { count?: number }).count

      const value = messages[key] ?? key

      if (typeof value !== 'string') {
        return value
      }

      const template =
        value.includes('|') && typeof count === 'number'
          ? count <= 1
            ? value.split('|')[0]?.trim()
            : value.split('|')[1]?.trim()
          : value

      return template.replace(/\{(\w+)\}/g, (_match, token) => {
        const replacement = (resolvedParams as Record<string, unknown>)[token]
        return replacement != null ? String(replacement) : _match
      })
    },
    tm: (key: string) => {
      if (messages[key]) {
        return messages[key]
      }

      if (key === 'home.events.default.hero.search.helpers') {
        return helperItems
      }

      if (key === 'home.events.default.hero.subtitles') {
        return subtitleCollections.default
      }

      if (key === 'home.events.christmas.hero.subtitles') {
        return subtitleCollections.events.christmas
      }

      if (key === 'home.events.default.hero.titleSubtitle') {
        return ['Acheter mieux. Sans dépenser plus.']
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

const VImgStub = defineComponent({
  name: 'VImgStub',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        'img',
        { class: ['v-img-stub', attrs.class], ...attrs },
        slots.default ? slots.default() : []
      )
  },
})

const NuxtLinkStub = defineComponent({
  name: 'NuxtLinkStub',
  setup(_, { slots, attrs }) {
    return () =>
      h(
        'a',
        {
          class: attrs.class,
          href:
            (attrs as { to?: string; href?: string }).to ??
            (attrs as { href?: string }).href,
        },
        slots.default ? slots.default() : []
      )
  },
})

const mountComponent = async (options?: { variantSeeds?: Record<string, number> }) => {
  resetHeroSubtitleState()

  if (options?.variantSeeds) {
    const seedState = useState<Record<string, number>>(
      'event-pack-variant-seeds',
      () => ({})
    )
    seedState.value = { ...(options.variantSeeds ?? {}) }
  }

  return mountSuspended(HomeHeroSection, {
    props: {
      searchQuery: '',
      minSuggestionQueryLength: 2,
      partnersCount: 12,
    },
    global: {
      stubs: {
        HeroSurface: createStub('section'),
        NudgeToolWizard: createStub('div', 'nudge-tool-wizard-stub'),
        SearchSuggestField: createStub('div', 'search-suggest-field-stub'),
        VContainer: createStub('div'),
        VRow: createStub('div'),
        VCol: createStub('div'),
        VAvatar: createStub('div'),
        VImg: VImgStub,
        VBtn: createStub('button'),
        RoundedCornerCard: createStub('div', 'rounded-corner-card-stub'),
        NuxtLink: NuxtLinkStub,
      },
    },
  })
}

afterEach(() => {
  activeEventPack.value = 'default'
  vi.restoreAllMocks()
})

describe('HomeHeroSection', () => {
  it('renders the eyebrow copy without trailing punctuation and shows the launcher icon', async () => {
    vi.spyOn(Math, 'random').mockReturnValue(0.1)

    const wrapper = await mountComponent()
    const eyebrow = wrapper.find('.home-hero__eyebrow')
    const icon = wrapper.find('.home-hero__icon')

    expect(eyebrow.text()).toBe(messages['home.events.default.hero.eyebrow'])
    expect(icon.attributes('src')).toBe(
      '/pwa-assets/icons/android/android-launchericon-512-512.png'
    )
    expect(icon.attributes('alt')).toBe(messages['home.events.default.hero.iconAlt'])

    await wrapper.unmount()
  })

  it('applies a random animation class from the configured list', async () => {
    vi.spyOn(Math, 'random').mockReturnValue(0.95)

    const wrapper = await mountComponent()
    const icon = wrapper.find('.home-hero__icon')

    expect(icon.classes()).toContain('home-hero__icon--pulse')

    await wrapper.unmount()
  })

  it('renders helper text with linked segments', async () => {
    const wrapper = await mountComponent()
    const helpers = wrapper.findAll('.home-hero__helper')

    expect(helpers).toHaveLength(2)

    const impactHelperLink = helpers[0].find('.home-hero__helper-link')

    expect(impactHelperLink.exists()).toBe(true)
    expect(impactHelperLink.attributes('href')).toBe('/impact-score')

    const partnersHelper = helpers[1]
    const partnersHelperText = partnersHelper.find('.home-hero__helper-text')
    const partnersLink = partnersHelper.find('.home-hero__helper-link')

    expect(partnersHelperText.text()).toContain(
      'Sans se faire avoir sur les prix avec'
    )
    expect(partnersHelperText.text()).toContain('12 partenaires')
    expect(partnersLink.exists()).toBe(true)
    expect(partnersLink.attributes('href')).toBe('/partenaires')
    expect(partnersLink.text()).toContain('12 partenaires')

    await wrapper.unmount()
  })

  it('prefers event-specific subtitles when provided', async () => {
    activeEventPack.value = 'christmas'
    const wrapper = await mountComponent({
      variantSeeds: { 'home-hero-subtitles': 0.9 },
    })
    const subtitle = wrapper.find('.home-hero__subtitle')
    const variantState = useState<Record<string, number>>(
      'event-pack-variant-seeds'
    )

    expect(variantState.value['home-hero-subtitles']).toBeCloseTo(0.9)

    expect(subtitle.text()).toBe(subtitleCollections.events.christmas[1])

    await wrapper.unmount()
  })

  it('falls back to default subtitles when no event override exists', async () => {
    activeEventPack.value = 'default'
    const wrapper = await mountComponent({
      variantSeeds: { 'home-hero-subtitles': 0.6 },
    })
    const subtitle = wrapper.find('.home-hero__subtitle')

    expect(subtitle.text()).toBe(subtitleCollections.default[1])

    await wrapper.unmount()
  })

  it('randomises the subtitle per page view', async () => {
    const firstWrapper = await mountComponent({
      variantSeeds: { 'home-hero-subtitles': 0.01 },
    })
    const firstSubtitle = firstWrapper.find('.home-hero__subtitle').text()

    await firstWrapper.unmount()

    const secondWrapper = await mountComponent({
      variantSeeds: { 'home-hero-subtitles': 0.95 },
    })
    const secondSubtitle = secondWrapper.find('.home-hero__subtitle').text()

    expect(firstSubtitle).not.toBe(secondSubtitle)

    await secondWrapper.unmount()
  })
})

import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import HomeHeroSection from './HomeHeroSection.vue'

const messages: Record<string, string> = {
  'home.hero.eyebrow': 'notre comparateur',
  'home.hero.title': "Réconcilier écologie et pouvoir d'achat",
  'home.hero.subtitle': 'Gagne du temps. Choisis librement.',
  'home.hero.search.label': 'Tu sais déjà ce que tu cherches ?',
  'home.hero.search.placeholder': 'Recherchez un produit ou une catégorie',
  'home.hero.search.ariaLabel': 'Rechercher un produit responsable',
  'home.hero.search.cta': 'NUDGER',
  'home.hero.search.partnerLinkLabel': '{formattedCount} partenaire | {formattedCount} partenaires',
  'home.hero.search.partnerLinkFallback': 'nos partenaires',
  'home.hero.iconAlt': 'Icône du lanceur PWA Nudger',
}

const helperItems = [
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
]

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
          : params ?? {}

      const count =
        typeof choiceOrParams === 'number'
          ? choiceOrParams
          : (resolvedParams as { count?: number }).count

      const value = messages[key] ?? key

      if (typeof value !== 'string') {
        return value
      }

      const template = value.includes('|') && typeof count === 'number'
        ? (count <= 1 ? value.split('|')[0]?.trim() : value.split('|')[1]?.trim())
        : value

      return template.replace(/\{(\w+)\}/g, (_match, token) => {
        const replacement = (resolvedParams as Record<string, unknown>)[token]
        return replacement != null ? String(replacement) : _match
      })
    },
    tm: (key: string) =>
      key === 'home.hero.search.helpers' ? helperItems : [],
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

const mountComponent = async () =>
  mountSuspended(HomeHeroSection, {
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

afterEach(() => {
  vi.restoreAllMocks()
})

describe('HomeHeroSection', () => {
  it('renders the eyebrow copy without trailing punctuation and shows the launcher icon', async () => {
    vi.spyOn(Math, 'random').mockReturnValue(0.1)

    const wrapper = await mountComponent()
    const eyebrow = wrapper.find('.home-hero__eyebrow')
    const icon = wrapper.find('.home-hero__icon')

    expect(eyebrow.text()).toBe(messages['home.hero.eyebrow'])
    expect(icon.attributes('src')).toBe(
      '/pwa-assets/icons/android/android-launchericon-512-512.png'
    )
    expect(icon.attributes('alt')).toBe(messages['home.hero.iconAlt'])

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
})

import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi, afterEach } from 'vitest'
import { defineComponent, h } from 'vue'
import HomeHeroSection from './HomeHeroSection.vue'

const messages: Record<string, string> = {
  'home.hero.eyebrow': 'notre comparateur',
  'home.hero.title': 'Réconcilier écologie et pouvoir d\'achat',
  'home.hero.subtitle': 'Gagne du temps. Choisis librement.',
  'home.hero.search.label': 'Tu sais déjà ce que tu cherches ?',
  'home.hero.search.placeholder': 'Recherchez un produit ou une catégorie',
  'home.hero.search.ariaLabel': 'Rechercher un produit responsable',
  'home.hero.search.cta': 'NUDGER',
  'home.hero.iconAlt': 'Icône du lanceur PWA Nudger',
  'home.widgets.nudgeWizard.title': 'Guidage Nudger',
  'home.widgets.nudgeWizard.subtitle': 'Choisis ta catégorie et affine selon tes priorités',
  'home.widgets.search.title': 'Recherche directe',
  'home.widgets.search.subtitle': 'Retrouve un produit ou une catégorie en un clin d’œil',
}

const helperItems = [
  { icon: '🌿', label: 'Une évaluation écologique et environnementale unique' },
]

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
    tm: (key: string) => (key === 'home.hero.search.helpers' ? helperItems : []),
  }),
}))

const createStub = (tag: string, className = '') =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_, { slots, attrs }) {
      return () => h(tag, { class: [className, attrs.class], ...attrs }, slots.default ? slots.default() : [])
    },
  })

const VImgStub = defineComponent({
  name: 'VImgStub',
  setup(_, { slots, attrs }) {
    return () => h('img', { class: ['v-img-stub', attrs.class], ...attrs }, slots.default ? slots.default() : [])
  },
})

const mountComponent = async () =>
  mountSuspended(HomeHeroSection, {
    props: {
      searchQuery: '',
      minSuggestionQueryLength: 2,
    },
    global: {
      stubs: {
        HeroSurface: createStub('section'),
        NudgeToolWizard: createStub('div', 'nudge-tool-wizard-stub'),
        SearchSuggestField: createStub('div', 'search-suggest-field-stub'),
        HomeWidgetShell: createStub('div', 'home-widget-shell-stub'),
        VContainer: createStub('div'),
        VRow: createStub('div'),
        VCol: createStub('div'),
        VAvatar: createStub('div'),
        VImg: VImgStub,
        VBtn: createStub('button'),
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
    expect(icon.attributes('src')).toBe('/pwa-assets/icons/android/android-launchericon-512-512.png')
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
})

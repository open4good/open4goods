import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, ref } from 'vue'

const messages: Record<string, string> = {
  'home.hero.search.label': 'Search for a product',
  'home.hero.search.placeholder': 'Search a product',
  'home.hero.search.ariaLabel': 'Search input',
  'home.hero.search.cta': 'NUDGER',
  'home.hero.search.helper': '50M references',
  'home.hero.eyebrow': 'Responsible shopping',
  'home.hero.title': 'Responsible choices are not a luxury',
  'home.hero.subtitle': 'Save time, stay true to your values.',
  'home.hero.imageAlt': 'Hero illustration',
  'home.problems.title': 'Too many labels, not enough clarity?',
  'home.problems.items.labelsOverload': 'Lost in the jungle of labels? Hard to truly compare.',
  'home.problems.items.budgetVsEcology': 'Ecology versus budget? Tired of choosing between the two.',
  'home.problems.items.tooManyTabs': 'Eight tabs open? One place is enough.',
  'home.solution.title': 'Responsible shopping, minus the headache',
  'home.solution.description': 'Nudger combines environmental, technical and pricing insights to simplify every decision.',
  'home.solution.benefits.time': 'Save time — we analyse the data for you.',
  'home.solution.benefits.savings': 'Save money — price comparison built in.',
  'home.solution.benefits.planet': 'Shop better — spot the most responsible products.',
  'home.solution.benefits.trust': 'Trustworthy — open data, independent recommendations.',
  'home.features.title': 'Key features',
  'home.features.subtitle': 'See what Nudger puts at your fingertips.',
  'home.features.cards.impactScore.title': 'AI Impact Score',
  'home.features.cards.impactScore.description': 'A clear ecological score (5 criteria, AI used with restraint).',
  'home.features.cards.priceComparison.title': 'Price comparison + history',
  'home.features.cards.priceComparison.description': 'Pay the right price at the right time.',
  'home.features.cards.openIndependent.title': 'Open & independent',
  'home.features.cards.openIndependent.description': 'Open source, open data, no brand influence.',
  'home.features.cards.noTracking.title': 'Zero tracking',
  'home.features.cards.noTracking.description': 'We track products, not you. Cookies are for eating.',
  'home.features.cards.massiveBase.title': 'Massive dataset',
  'home.features.cards.massiveBase.description': 'The largest open-data catalogue on the market: 50 million references.',
  'home.categories.title': 'Browse categories',
  'home.categories.subtitle': 'Start with our most popular universes and refine in one click.',
  'home.categories.cta': 'Browse products',
  'home.categories.items.electronics.title': 'Electronics',
  'home.categories.items.electronics.description': 'Smartphones, TVs, audio…',
  'home.categories.items.appliances.title': 'Appliances',
  'home.categories.items.appliances.description': 'Washing machines, dishwashers, fridges…',
  'home.trust.title': 'Proof & trust',
  'home.trust.subtitle': 'Demanding partners and open data keep us accountable.',
  'home.trust.logos.ademe': 'ADEME logo',
  'home.trust.stats.references': '50,000,000+ open-data references',
  'home.trust.stats.updates': 'Updated on a regular basis',
  'home.blog.title': 'From the blog',
  'home.blog.cta': 'Browse all articles',
  'home.blog.readMore': 'Read article',
  'home.blog.items.first.title': 'How we keep our AI frugal',
  'home.blog.items.first.date': '15 Jan 2025',
  'home.blog.items.first.excerpt': 'The technical choices that keep Nudger’s computations low-carbon.',
  'home.blog.items.second.title': 'Prioritising durability and repairability',
  'home.blog.items.second.date': '8 Jan 2025',
  'home.blog.items.second.excerpt': 'Why repairability weighs more in the Impact Score.',
  'home.blog.items.third.title': 'Balancing price and impact, step by step',
  'home.blog.items.third.date': '20 Dec 2024',
  'home.blog.items.third.excerpt': 'Nudger’s method to align ecology and budget.',
  'home.objections.title': 'Straight questions, clear answers',
  'home.objections.subtitle': 'The topics we hear about the most.',
  'home.objections.items.aiEnergy.question': 'Does AI burn too much energy?',
  'home.objections.items.aiEnergy.answer': 'Frugal AI, optimised computations, net positive impact.',
  'home.objections.items.reuse.question': 'Is true frugality all about reuse?',
  'home.objections.items.reuse.answer': 'We promote using less first, then better.',
  'home.objections.items.independence.question': 'Are you independent?',
  'home.objections.items.independence.answer': 'Yes. Results stay neutral, code is open.',
  'home.faq.title': 'FAQ',
  'home.faq.subtitle': 'Quick answers to the most common questions.',
  'home.faq.items.free.question': 'Is Nudger free?',
  'home.faq.items.free.answer': 'Yes, searching is free to use.',
  'home.faq.items.account.question': 'Do I need an account?',
  'home.faq.items.account.answer': 'No for searching; an account is optional.',
  'home.faq.items.categories.question': 'Which categories are covered?',
  'home.faq.items.categories.answer': 'Electronics & Appliances today, more to come.',
  'home.faq.items.impactScore.question': 'How do you calculate the Impact Score?',
  'home.faq.items.impactScore.answer': 'Five weighted criteria + lean AI, fully open methodology.',
  'home.faq.items.dataFreshness.question': 'Are the data kept up to date?',
  'home.faq.items.dataFreshness.answer': 'Yes, refreshed regularly with quality checks.',
  'home.faq.items.suggestProduct.question': 'How can I suggest a product?',
  'home.faq.items.suggestProduct.answer': 'Use our dedicated contact form.',
  'home.cta.title': 'Ready to buy better without overspending?',
  'home.cta.subtitle': 'Restart a search or explore the analysed offers.',
  'home.cta.button': 'Start a search',
  'home.cta.altLink': 'Open the search page',
  'home.seo.title': 'Nudger – Responsible shopping made easy',
  'home.seo.description': 'Compare the environmental impact and prices of over 50 million products with Nudger.',
  'home.seo.imageAlt': 'Illustration of the Nudger dashboard',
  'siteIdentity.siteName': 'Nudger'
}

const translate = (key: string) => messages[key] ?? key

const localeRef = ref('fr-FR')
const headSpy = vi.fn()
const routerPush = vi.fn()
const routerReplace = vi.fn()
const localePathMock = vi.fn((to: unknown) => {
  if (typeof to === 'string') {
    return `/localized/${to}`
  }

  if (to && typeof to === 'object' && 'name' in to) {
    const name = String(to.name)
    const query = 'query' in to && to.query && typeof to.query === 'object' && 'q' in to.query
      ? `?q=${String((to.query as Record<string, unknown>).q ?? '')}`
      : ''
    return `/localized/${name}${query}`
  }

  return '/localized'
})

function useLocalePathMock() {
  return (to: unknown) => localePathMock(to)
}

mockNuxtImport('useI18n', () => () => ({
  t: (key: string) => translate(key),
  locale: localeRef,
  availableLocales: ['fr-FR', 'en-US'],
}))

mockNuxtImport('useRouter', () => () => ({
  push: routerPush,
  replace: routerReplace,
}))

mockNuxtImport('useLocalePath', () => useLocalePathMock)
mockNuxtImport('useRequestURL', () => () => new URL('https://nudger.test/fr/'))
mockNuxtImport('useSeoMeta', () => vi.fn())
mockNuxtImport('useHead', () => (input: unknown) => {
  const value = typeof input === 'function' ? input() : input
  headSpy(value)
  return value
})

const simpleStub = (tag: string) =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_props, { slots, attrs }) {
      return () => h(tag, attrs, slots.default?.())
    },
  })

const VTextFieldStub = defineComponent({
  name: 'VTextFieldStub',
  props: {
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    const onInput = (event: Event) => {
      const target = event.target as HTMLInputElement
      emit('update:modelValue', target.value)
    }

    return () => h('input', { ...attrs, value: props.modelValue, onInput })
  },
})

const SearchSuggestFieldStub = defineComponent({
  name: 'SearchSuggestFieldStub',
  props: {
    modelValue: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'clear', 'select-category', 'select-product', 'submit'],
  setup(props, { emit, attrs }) {
    const onInput = (event: Event) => {
      const target = event.target as HTMLInputElement
      emit('update:modelValue', target.value)
    }

    const onKeydown = (event: KeyboardEvent) => {
      if (event.key === 'Enter' && !event.isComposing) {
        emit('submit')
      }
    }

    return () =>
      h('input', {
        ...attrs,
        value: props.modelValue,
        onInput,
        onKeydown,
      })
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  props: {
    type: { type: String, default: 'button' },
  },
  setup(props, { slots, attrs }) {
    return () => h('button', { ...attrs, type: props.type }, slots.default?.())
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: String, default: '' },
  },
  setup(props, { attrs }) {
    return () => h('span', { ...attrs, 'data-icon': props.icon }, props.icon)
  },
})

const VImgStub = defineComponent({
  name: 'VImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props, { attrs }) {
    return () => h('img', { ...attrs, src: props.src, alt: props.alt })
  },
})

const NuxtLinkStub = defineComponent({
  name: 'NuxtLinkStub',
  props: {
    to: { type: [String, Object], default: '' },
  },
  setup(props, { slots, attrs }) {
    const href = typeof props.to === 'string' ? props.to : JSON.stringify(props.to)
    return () => h('a', { ...attrs, href }, slots.default?.())
  },
})

const mountHomePage = async () => {
  const component = (await import('./index.vue')).default
  return mountSuspended(component, {
    global: {
      stubs: {
        VContainer: simpleStub('div'),
        VRow: simpleStub('div'),
        VCol: simpleStub('div'),
        VCard: simpleStub('div'),
        VSheet: simpleStub('div'),
        VIcon: VIconStub,
        VBtn: VBtnStub,
        VTextField: VTextFieldStub,
        SearchSuggestField: SearchSuggestFieldStub,
        VImg: VImgStub,
        VDivider: simpleStub('hr'),
        VExpansionPanels: simpleStub('div'),
        VExpansionPanel: simpleStub('div'),
        VExpansionPanelTitle: simpleStub('div'),
        VExpansionPanelText: simpleStub('div'),
        NuxtLink: NuxtLinkStub,
      },
    },
  })
}

describe('Home page', () => {
  beforeEach(() => {
    routerPush.mockReset()
    routerReplace.mockReset()
    localePathMock.mockClear()
    headSpy.mockReset()
  })

  it('submits the search query using the localized search route', async () => {
    const wrapper = await mountHomePage()

    const searchForm = wrapper.find('section.home-hero form[role="search"]')
    const searchInput = searchForm.find('input')

    await searchInput.setValue('smart tv')
    await searchForm.trigger('submit.prevent')

    expect(localePathMock).toHaveBeenCalledWith({ name: 'search', query: { q: 'smart tv' } })
    expect(routerPush).toHaveBeenCalledWith('/localized/search?q=smart tv')
  })

  it('registers FAQ structured data in head tags', async () => {
    await mountHomePage()

    const headEntries = headSpy.mock.calls.map(([value]) => value)
    const scriptEntry = headEntries
      .flatMap((entry) => (entry && typeof entry === 'object' && 'script' in entry ? (entry.script as unknown[]) : []))
      .find((entry) => entry && typeof entry === 'object' && 'key' in entry && (entry as { key: string }).key === 'home-faq-jsonld') as
      | { children?: string }
      | undefined

    expect(scriptEntry).toBeTruthy()

    const json = scriptEntry?.children ? JSON.parse(scriptEntry.children) : null

    expect(json).toBeTruthy()
    expect(json).toMatchObject({
      '@type': 'FAQPage',
    })
    expect(Array.isArray(json?.mainEntity)).toBe(true)
    expect(json?.mainEntity).toHaveLength(6)
    expect(json?.mainEntity[0]).toMatchObject({
      '@type': 'Question',
      acceptedAnswer: { '@type': 'Answer' },
    })
  })
})

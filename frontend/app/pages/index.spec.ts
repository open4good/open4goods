import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { describe, it, expect, vi, beforeEach, afterAll } from 'vitest'
import { defineComponent, h, ref, computed } from 'vue'

const messages: Record<string, unknown> = {
  'packs.default.hero.search.label': 'Search for a product',
  'packs.default.hero.search.placeholder': 'Search a product',
  'packs.default.hero.search.ariaLabel': 'Search input',
  'packs.default.hero.search.cta': 'NUDGER',
  'packs.default.hero.search.helper': '50M references',
  'packs.default.hero.search.helpersTitle':
    'Shop with intention. Compare for impact.',
  'packs.default.hero.search.helpers': [
    {
      icon: '🌿',
      label: 'Impact Score: a unique ecological and environmental assessment',
      segments: [
        { text: 'Impact Score: a' },
        {
          text: 'unique ecological and environmental assessment',
          to: '/impact-score',
        },
      ],
    },
    {
      icon: '🏷️',
      label:
        '100% independent, open-source, and {millions}+ open-data products',
      segments: [
        { text: '100% independent, open-source, and' },
        { text: '{millions}+ open-data products' },
      ],
    },
    {
      icon: '🛡️',
      label: 'Independent & open',
      segments: [{ text: 'Independent & open' }],
    },
    {
      icon: '⚡',
      label: '50M references',
      segments: [{ text: '50M references' }],
    },
  ],
  'packs.default.hero.highlights': [
    {
      title: '🌿 ImpactScore: guiding your purchases',
      segments: [
        {
          text: 'Buy less harmful for the planet, thanks to our',
        },
        {
          text: 'environmental assessment',
          to: '/impact-score',
        },
        {
          text: 'covering',
        },
        {
          text: '{impactScoreProducts}',
          to: '/impact-score',
        },
        {
          text: 'products across',
        },
        {
          text: '{impactScoreCategories} categories',
          to: '/categories',
        },
      ],
    },
    {
      title: '🇫🇷 Open & ethical',
      segments: [
        {
          text: '100% independent. Made in France,',
          icon: 'breton-flag',
          iconPosition: 'after',
        },
        {
          text: 'hosted in Europe. Over',
        },
        {
          text: '{millions}M',
          to: '/opendata',
        },
        {
          text: 'products in',
        },
        {
          text: 'open data.',
          to: '/opendata',
        },
        {
          text: 'Nudger is also',
        },
        {
          text: 'open source',
          to: '/opensource',
        },
        {
          text: 'and collects no personal data.',
        },
      ],
    },
    {
      title: 'Price comparison',
      segments: [
        {
          text: 'Avoid price traps with the best offers from',
        },
        {
          text: '{partnersLink}',
          to: '/partners',
        },
        {
          text: 'merchants. Buy at the right time with new and used price history for',
        },
        {
          text: '{priceHistoryProducts}',
          to: '/categories',
        },
        {
          text: 'products.',
        },
      ],
    },
  ],
  'packs.default.hero.search.partnerLinkLabel':
    '{formattedCount} partner | {formattedCount} partners',
  'packs.default.hero.search.partnerLinkFallback': 'our partners',
  'packs.default.hero.eyebrow': 'Responsible shopping',
  'packs.default.hero.title': 'Nudger: The eco-friendly comparator',
  'packs.default.hero.subtitles': [
    'Save time, stay true to your values.',
    'Shop smarter without compromise.',
  ],
  'packs.default.hero.titleSubtitle': ['Buy better. Spend smarter.'],
  'packs.default.hero.imageAlt': 'Hero illustration',
  'packs.default.hero.iconAlt': 'Hero icon',
  'packs.default.hero.context.ariaLabel':
    'Hero context card summarising Nudger’s promise',
  'home.hero.search.label': 'Search for a product',
  'home.hero.search.placeholder': 'Search a product',
  'home.hero.search.ariaLabel': 'Search input',
  'home.hero.search.cta': 'NUDGER',
  'home.hero.search.helper': '50M references',
  'home.hero.search.helpers': [
    {
      icon: '🌿',
      label: 'Impact Score: a unique ecological and environmental assessment',
      segments: [
        { text: 'Impact Score: a' },
        {
          text: 'unique ecological and environmental assessment',
          to: '/impact-score',
        },
      ],
    },
    {
      icon: '🏷️',
      label:
        '100% independent, open-source, and {millions}+ open-data products',
      segments: [
        { text: '100% independent, open-source, and' },
        { text: '{millions}+ open-data products' },
      ],
    },
    {
      icon: '🛡️',
      label: 'Independent & open',
      segments: [{ text: 'Independent & open' }],
    },
    {
      icon: '⚡',
      label: '50M references',
      segments: [{ text: '50M references' }],
    },
  ],
  'home.hero.highlights': [
    {
      title: '🌿 ImpactScore: guiding your purchases',
      segments: [
        {
          text: 'Buy less harmful for the planet, thanks to our',
        },
        {
          text: 'environmental assessment',
          to: '/impact-score',
        },
        {
          text: 'covering',
        },
        {
          text: '{impactScoreProducts}',
          to: '/impact-score',
        },
        {
          text: 'products across',
        },
        {
          text: '{impactScoreCategories} categories',
          to: '/categories',
        },
      ],
    },
    {
      title: '🇫🇷 Open & ethical',
      segments: [
        {
          text: '100% independent. Made in France,',
          icon: 'breton-flag',
          iconPosition: 'after',
        },
        {
          text: 'hosted in Europe. Over',
        },
        {
          text: '{millions}M',
          to: '/opendata',
        },
        {
          text: 'products in',
        },
        {
          text: 'open data.',
          to: '/opendata',
        },
        {
          text: 'Nudger is also',
        },
        {
          text: 'open source',
          to: '/opensource',
        },
        {
          text: 'and collects no personal data.',
        },
      ],
    },
    {
      title: 'Price comparison',
      segments: [
        {
          text: 'Avoid price traps with the best offers from',
        },
        {
          text: '{partnersLink}',
          to: '/partners',
        },
        {
          text: 'merchants. Buy at the right time with new and used price history for',
        },
        {
          text: '{priceHistoryProducts}',
          to: '/categories',
        },
        {
          text: 'products.',
        },
      ],
    },
  ],
  'home.hero.aiSummary.title': 'Community AI summary',
  'home.hero.aiSummary.creditsLabel': 'Credits remaining: {count}',
  'home.hero.aiSummary.creditsFallback': 'Credits remaining: unavailable',
  'home.hero.aiSummary.description':
    'A powerful tool with real AI inside, giving you a panoramic view of your product. Free and community-driven.',
  'home.hero.search.partnerLinkLabel':
    '{formattedCount} partner | {formattedCount} partners',
  'home.hero.search.partnerLinkFallback': 'our partners',
  'home.hero.eyebrow': 'Responsible shopping',
  'home.hero.title': 'Nudger: The eco-friendly comparator',

  'home.hero.imageAlt': 'Hero illustration',
  'home.problems.title': 'Too many labels, not enough clarity?',
  'home.problems.items.labelsOverload':
    'Lost in the jungle of labels? Hard to truly compare.',
  'home.problems.items.budgetVsEcology':
    'Ecology versus budget? Tired of choosing between the two.',
  'home.problems.items.tooManyTabs': 'Eight tabs open? One place is enough.',
  'home.solution.title': 'Responsible shopping, minus the headache',
  'home.solution.description':
    'Nudger combines environmental, technical and pricing insights to simplify every decision.',
  'home.solution.benefits.time.title': 'Save time',
  'home.solution.benefits.time.description': 'We analyse the data for you.',
  'home.solution.benefits.savings.title': 'Save money',
  'home.solution.benefits.savings.description': 'Price comparison built in.',
  'home.solution.benefits.planet.title': 'Shop better',
  'home.solution.benefits.planet.description':
    'Spot the most responsible products.',
  'home.solution.benefits.trust.title': 'Trustworthy',
  'home.solution.benefits.trust.description':
    'Open data, independent recommendations.',
  'home.features.title': 'Key features',
  'home.features.subtitle': 'See what Nudger puts at your fingertips.',
  'home.features.cards.impactScore.title': 'AI Impact Score',
  'home.features.cards.impactScore.description':
    'A clear ecological score (5 criteria, AI used with restraint).',
  'home.features.cards.priceComparison.title': 'Price comparison + history',
  'home.features.cards.priceComparison.description':
    'Pay the right price at the right time.',
  'home.features.cards.openIndependent.title': 'Open & independent',
  'home.features.cards.openIndependent.description':
    'Open source, open data, no brand influence.',
  'home.features.cards.noTracking.title': 'Zero tracking',
  'home.features.cards.noTracking.description':
    'We track products, not you. Cookies are for eating.',
  'home.features.cards.massiveBase.title': 'Massive dataset',
  'home.features.cards.massiveBase.description':
    'The largest open-data catalogue on the market: 50 million references.',
  'home.categories.title': 'Popular categories',
  'home.categories.subtitle': '',
  'home.categories.cta': 'Browse products',
  'home.categories.impactLink': 'ImpactScore televisions',
  'home.categories.fallbackTitle': 'Category',
  'home.categories.fallbackDescription':
    'Fallback description for the category.',
  'home.categories.carouselAriaLabel': 'Categories carousel',
  'home.categories.bannerAriaLabel': 'Browse highlighted categories',
  'home.categories.scrollPrevious': 'Scroll to previous categories',
  'home.categories.scrollNext': 'Scroll to next categories',
  'home.categories.impactLinkAria': 'Open Impact Score details for {category}',
  'home.categories.emptyState': 'Categories coming soon.',
  'home.categories.items.electronics.title': 'Electronics',
  'home.categories.items.electronics.description': 'Smartphones, TVs, audio…',
  'home.categories.items.appliances.title': 'Appliances',
  'home.categories.items.appliances.description':
    'Washing machines, dishwashers, fridges…',
  'home.blog.title': 'Live from the blog',
  'home.blog.subtitle': 'Latest stories from the team.',
  'home.blog.cta': 'Browse all articles',
  'home.blog.readMore': 'Read article',
  'home.blog.emptyState': 'No blog posts yet.',
  'home.blog.carouselAriaLabel': 'Featured blog posts carousel',
  'home.blog.items.first.title': 'How we keep our AI frugal',
  'home.blog.items.first.date': '15 Jan 2025',
  'home.blog.items.first.excerpt':
    'The technical choices that keep Nudger’s computations low-carbon.',
  'home.blog.items.second.title': 'Prioritising durability and repairability',
  'home.blog.items.second.date': '8 Jan 2025',
  'home.blog.items.second.excerpt':
    'Why repairability weighs more in the Impact Score.',
  'home.blog.items.third.title': 'Balancing price and impact, step by step',
  'home.blog.items.third.date': '20 Dec 2024',
  'home.blog.items.third.excerpt':
    'Nudger’s method to align ecology and budget.',
  'home.objections.title': 'Straight questions, clear answers',
  'home.objections.subtitle': 'The topics we hear about the most.',
  'home.objections.items.aiEnergy.question': 'Does AI burn too much energy?',
  'home.objections.items.aiEnergy.answer':
    'Frugal AI, optimised computations, net positive impact.',
  'home.objections.items.reuse.question': 'Is true frugality all about reuse?',
  'home.objections.items.reuse.answer':
    'We promote using less first, then better.',
  'home.objections.items.independence.question': 'Are you independent?',
  'home.objections.items.independence.answer':
    'Yes. Results stay neutral, code is open.',
  'home.faq.title': 'FAQ',
  'home.faq.subtitle': 'Quick answers to the most common questions.',
  'home.faq.items.free.question': 'Is Nudger free?',
  'home.faq.items.free.answer':
    "Yes, Nudger is completely free for you!<br/><br/>We earn money through affiliate partnerships. That means we receive a commission when you click Nudger offers and complete your purchase with our partner merchants.<br/><br/>It's the same model used by comparison sites and it never changes the price of the products you see on Nudger.",
  'home.faq.items.account.question': 'Do I need an account?',
  'home.faq.items.account.answer':
    "<b>Not at all!</b><br/>Every feature is available without creating an account on our platform.<br/>That's how we protect your privacy, too.",
  'home.faq.items.impactScore.question':
    'How do you calculate the Impact Score?',
  'home.faq.items.impactScore.answer':
    'This is where Nudger shines.<br/><br/>We aim to guide you through the realm of possibilities rather than provide absolute judgments.<br/><ul><li>We analyse every data point we have related to ecological and societal impact.</li><li>We compare products against each other using our Impact Score.</li><li>Advanced AI systems adjust how each criterion is weighted.</li></ul>And the whole process stays fully transparent.',
  'home.faq.items.impactScore.cta': 'Learn more',
  'home.faq.items.impactScore.ctaAria': 'Open the detailed Impact Score page',
  'home.faq.items.dataFreshness.question': 'Are the data kept up to date?',
  'home.faq.items.dataFreshness.answer':
    'Yes, our product data is refreshed <b>twice a day</b> to track offer changes as closely as possible.',
  'home.faq.agent.eyebrow': 'Your AI result here',
  'home.faq.agent.title': 'Ask your question, we’ll pin it to the FAQ',
  'home.faq.agent.subtitle': 'Your question and the AI result will show below.',
  'home.faq.agent.dynamicAnswerWithLink': 'See the detailed AI answer: {link}',
  'home.faq.agent.dynamicAnswerWithState': 'Answer in progress ({state}).',
  'home.faq.agent.dynamicAnswerPending': 'Thanks! Your question is in queue.',
  'home.faq.agent.preview.eyebrow': 'Live AI feed',
  'home.faq.agent.preview.title': 'Latest AI answer',
  'home.faq.agent.preview.subtitle':
    'The freshest public question appears here.',
  'home.faq.agent.preview.label': 'Latest question',
  'home.faq.agent.preview.status': 'Status: {status}',
  'home.faq.agent.preview.statusPending': 'Status pending',
  'home.faq.agent.preview.cta': 'Open the result',
  'home.faq.agent.preview.helper': 'Opens the last public response',
  'home.faq.agent.preview.questionFallback':
    'Your AI question will surface here.',
  'home.cta.title': 'Ready to buy better without overspending?',
  'home.cta.subtitle': 'Restart a search or explore the analysed offers.',
  'home.cta.button': 'Start a search',
  'home.cta.searchSubmit': 'Launch search',
  'home.cta.browseTaxonomy': 'Explore categories',
  'home.seo.title': 'Nudger – Responsible shopping made easy',
  'home.seo.description':
    'Compare the environmental impact and prices of over 50 million products with Nudger.',
  'home.seo.imageAlt': 'Illustration of the Nudger dashboard',
  'siteIdentity.siteName': 'Nudger',
}

const interpolate = (template: string, params: Record<string, unknown> = {}) =>
  template.replace(/\{(\w+)\}/g, (match, token) => {
    const value = params[token]
    return value != null ? String(value) : match
  })

const selectPluralForm = (template: string, count?: number) => {
  if (!template.includes('|') || typeof count !== 'number') {
    return template
  }

  const [singular, plural] = template.split('|').map(part => part.trim())

  return count <= 1 ? singular : plural
}

const translate = (
  key: string,
  params: Record<string, unknown> = {},
  count?: number
) => {
  const value = messages[key]

  if (typeof value === 'string') {
    const template = selectPluralForm(value, count)
    return interpolate(template, params)
  }

  return typeof value === 'number' ? String(value) : key
}

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
    const query =
      'query' in to &&
      to.query &&
      typeof to.query === 'object' &&
      'q' in to.query
        ? `?q=${String((to.query as Record<string, unknown>).q ?? '')}`
        : ''
    return `/localized/${name}${query}`
  }

  return '/localized'
})

function useLocalePathMock() {
  return (to: unknown) => localePathMock(to)
}

const categoriesMockData = ref([
  {
    id: 'category-1',
    verticalHomeTitle: 'Electronics',
    verticalHomeDescription: 'Tech products',
    verticalHomeUrl: 'electronics',
    popular: true,
    enabled: true,
  },
  {
    id: 'category-2',
    verticalHomeTitle: 'Appliances',
    verticalHomeDescription: 'Home essentials',
    verticalHomeUrl: 'appliances',
    popular: false,
    enabled: true,
  },
])

const categoriesLoadingRef = ref(false)
const fetchCategoriesMock = vi.fn().mockResolvedValue(categoriesMockData.value)

const blogArticlesMock = ref([
  {
    url: 'latest-post',
    title: 'Latest post',
    summary: 'Fresh content from the blog.',
    createdMs: 1736899200000,
  },
  {
    url: 'another-post',
    title: 'Another story',
    summary: 'Second blog highlight.',
    createdMs: 1736208000000,
  },
])

const blogLoadingRef = ref(false)
const fetchArticlesMock = vi.fn().mockResolvedValue(blogArticlesMock.value)

function useCategoriesComposable() {
  return {
    categories: computed(() => categoriesMockData.value),
    fetchCategories: fetchCategoriesMock,
    loading: categoriesLoadingRef,
    error: computed(() => null),
    activeCategoryId: computed(() => null),
    currentCategory: computed(() => null),
    clearError: vi.fn(),
    resetCategorySelection: vi.fn(),
  }
}

function useBlogComposable() {
  return {
    paginatedArticles: computed(() => blogArticlesMock.value),
    fetchArticles: fetchArticlesMock,
    loading: blogLoadingRef,
    articles: computed(() => blogArticlesMock.value),
    currentArticle: computed(() => null),
    tags: computed(() => []),
    selectedTag: computed(() => null),
    changePage: vi.fn(),
    fetchTags: vi.fn(),
    selectTag: vi.fn(),
    fetchArticle: vi.fn(),
    clearCurrentArticle: vi.fn(),
    clearError: vi.fn(),
    error: computed(() => null),
    pagination: computed(() => ({
      page: 1,
      size: 6,
      totalElements: blogArticlesMock.value.length,
      totalPages: 1,
    })),
  }
}

mockNuxtImport('useI18n', () => () => ({
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
        : typeof params === 'number'
          ? params
          : (resolvedParams as { count?: number }).count

    return translate(key, resolvedParams, count)
  },
  tm: (key: string) => messages[key],
  te: (key: string) => Boolean(messages[key]),
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

const affiliationPartnersMock = [
  { id: 'partner-1', name: 'Partner 1' },
  { id: 'partner-2', name: 'Partner 2' },
  { id: 'partner-3', name: 'Partner 3' },
]

const fetchSpy = vi.fn((url: string) => {
  if (typeof url === 'string' && url.includes('/api/agents/templates')) {
    return Promise.resolve([
      {
        id: 'question',
        name: 'Question',
        description: 'Test agent',
        icon: 'mdi-robot',
        promptTemplates: [],
        tags: [],
        allowedRoles: [],
        attributes: [],
        publicPromptHistory: true,
        allowTemplateEditing: true,
      },
    ])
  }

  if (typeof url === 'string' && url.includes('/api/agents/activity')) {
    return Promise.resolve([])
  }

  return Promise.resolve(affiliationPartnersMock)
})

vi.stubGlobal('$fetch', fetchSpy)

vi.mock('~/composables/categories/useCategories', () => ({
  useCategories: useCategoriesComposable,
}))

vi.mock('~/composables/categories/useCategories.ts', () => ({
  useCategories: useCategoriesComposable,
}))

vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: useBlogComposable,
}))

vi.mock('~/composables/blog/useBlog.ts', () => ({
  useBlog: useBlogComposable,
}))

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
  emits: [
    'update:modelValue',
    'clear',
    'select-category',
    'select-product',
    'submit',
  ],
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

const AgentPromptInputStub = defineComponent({
  name: 'AgentPromptInputStub',
  emits: ['submit'],
  setup(_props, { emit }) {
    const onClick = () =>
      emit('submit', {
        prompt: 'stub question',
        isPrivate: false,
        attributeValues: {},
      })

    return () =>
      h(
        'button',
        { class: 'agent-prompt-input-stub', type: 'button', onClick },
        'Ask agent'
      )
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
    const href =
      typeof props.to === 'string' ? props.to : JSON.stringify(props.to)
    return () => h('a', { ...attrs, href }, slots.default?.())
  },
})

const HomeBlogCarouselStub = defineComponent({
  name: 'HomeBlogCarouselStub',
  props: {
    items: { type: Array, default: () => [] },
  },
  setup(props) {
    return () =>
      h(
        'div',
        { class: 'home-blog-carousel-stub' },
        (props.items as Array<{ title?: string }>)
          .map(item => item.title)
          .join('|')
      )
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
        VAlert: simpleStub('div'),
        VSkeletonLoader: simpleStub('div'),
        VIcon: VIconStub,
        VBtn: VBtnStub,
        VTextField: VTextFieldStub,
        VTextarea: simpleStub('div'),
        SearchSuggestField: SearchSuggestFieldStub,
        AgentPromptInput: AgentPromptInputStub,
        VImg: VImgStub,
        VDivider: simpleStub('hr'),
        VExpansionPanels: simpleStub('div'),
        VExpansionPanel: simpleStub('div'),
        VExpansionPanelTitle: simpleStub('div'),
        VExpansionPanelText: simpleStub('div'),
        NuxtLink: NuxtLinkStub,
        HomeBlogCarousel: HomeBlogCarouselStub,
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
    fetchSpy.mockClear()
    fetchCategoriesMock.mockClear()
    fetchArticlesMock.mockClear()
  })

  afterAll(() => {
    vi.unstubAllGlobals()
  })

  it('renders the hero title and subtitle', async () => {
    const wrapper = await mountHomePage()

    const heroTitle = wrapper.find('section.home-hero .home-hero__title')
    const heroSubtitle = wrapper.find(
      'section.home-hero .home-hero__title-subtitle'
    )

    expect(heroTitle.text()).toBe(messages['packs.default.hero.title'])
    expect(heroSubtitle.text()).toBe(
      (messages['packs.default.hero.titleSubtitle'] as string[])[0]
    )
  })

  it('registers FAQ structured data in head tags', async () => {
    await mountHomePage()

    const headEntries = headSpy.mock.calls.map(([value]) => value)
    const scriptEntry = headEntries
      .flatMap(entry =>
        entry && typeof entry === 'object' && 'script' in entry
          ? (entry.script as unknown[])
          : []
      )
      .find(
        entry =>
          entry &&
          typeof entry === 'object' &&
          'key' in entry &&
          (entry as { key: string }).key === 'home-faq-jsonld'
      ) as { children?: string } | undefined

    expect(scriptEntry).toBeTruthy()

    const json = scriptEntry?.children ? JSON.parse(scriptEntry.children) : null

    expect(json).toBeTruthy()
    expect(json).toMatchObject({
      '@type': 'FAQPage',
    })
    expect(Array.isArray(json?.mainEntity)).toBe(true)
    expect(json?.mainEntity).toHaveLength(4)
    expect(json?.mainEntity[0]).toMatchObject({
      '@type': 'Question',
      acceptedAnswer: { '@type': 'Answer' },
    })
  })
})

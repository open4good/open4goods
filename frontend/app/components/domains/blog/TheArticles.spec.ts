import { mountSuspended } from '@nuxt/test-utils/runtime'
import {
  type ComponentPublicInstance,
  defineComponent,
  h,
  nextTick,
  reactive,
  ref,
} from 'vue'
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest'
import TheArticles from './TheArticles.vue'

type BlogArticle = {
  url?: string | null
  title?: string | null
  summary?: string | null
  author?: string | null
  image?: string | null
  createdMs?: number | null
}

type BlogTag = {
  name?: string | null
  count?: number | null
}

const defaultArticles: BlogArticle[] = [
  {
    url: 'https://blog.example.com/articles/hello-world',
    title: 'Hello Nuxt World',
    summary: 'A welcome post for the blog.',
    author: 'Alice',
    image: 'https://cdn.example.com/images/hello.jpg',
    createdMs: Date.UTC(2024, 0, 15),
  },
  {
    url: '/articles/vue-insights',
    title: '  ',
    summary: 'Deep dive into Vue best practices.',
    author: 'Bob',
    image: 'https://cdn.example.com/images/vue-insights.jpg',
    createdMs: Date.UTC(2024, 2, 2),
  },
]

const defaultTags: BlogTag[] = [
  { name: '  Nuxt  ', count: 3 },
  { name: 'Vue', count: 5 },
  { name: '  ', count: 1 },
]

type FetchArticlesPayload = {
  page: number
  size: number
  tag: string | null
}

type FetchArticlesResult = {
  articles?: BlogArticle[]
  pagination?: Partial<{
    totalPages: number
    totalElements: number
  }>
}

type MockBlogConfig = {
  articles?: BlogArticle[]
  initialTags?: BlogTag[]
  resolveTags?: () => BlogTag[] | Promise<BlogTag[]>
  pagination?: Partial<{
    page: number
    size: number
    totalElements: number
    totalPages: number
  }>
  selectedTag?: string | null
  loading?: boolean
  error?: string | null
  onFetchArticles?: (
    payload: FetchArticlesPayload
  ) => FetchArticlesResult | Promise<FetchArticlesResult>
}

type TheArticlesPublicInstance = ComponentPublicInstance<
  Record<string, never>,
  {
    handlePageChange: (page: number) => Promise<void>
    handleTagSelection: (tag: string | null) => Promise<void>
    pageSeoTitle: string
    seoDescription: string
    canonicalUrl: string
    primaryArticleImage: string | null | undefined
    structuredData: Record<string, unknown> | null
  }
>

const useHeadMock = vi.fn()
const useSeoMetaMock = vi.fn()
let requestUrl = new URL('https://example.com/blog')

vi.mock('#imports', () => ({
  useHead: (...args: unknown[]) => useHeadMock(...args),
  useSeoMeta: (...args: unknown[]) => useSeoMetaMock(...args),
  useRequestURL: () => requestUrl,
  useAsyncData: async (_key: string, handler: () => Promise<unknown>) => {
    if (handler) {
      await handler()
    }
    return { data: { value: true } }
  },
}))

vi.mock('#i18n', () => ({
  useLocalePath: () => (input: unknown) =>
    typeof input === 'string' ? input : '/',
}))

const translations: Record<
  string,
  string | ((params: Record<string, unknown>) => string)
> = {
  'blog.list.tagsTitle': 'Browse by tag',
  'blog.list.tagsAriaLabel': 'Blog tags',
  'blog.list.tagsAll': 'All articles',
  'blog.list.tagsLoading': 'Loading tags…',
  'blog.list.loading': 'Loading articles…',
  'blog.list.readMore': 'Read more',
  'blog.list.tagWithCount': ({ tag, count }) => `${tag} (${count})`,
  'blog.hero.eyebrow': 'Nudger blog',
  'blog.hero.title': 'Fresh insights on responsible shopping',
  'blog.hero.subtitle': 'Short reads on sustainable appliances and nudges.',
  'blog.breadcrumbs.home': 'Home',
  'blog.breadcrumbs.blog': 'Blog',
  'blog.pagination.info': ({ current, total, count }) =>
    `Page ${current} of ${total} (${count})`,
  'blog.pagination.pageLink': ({ page }) => `Go to page ${page}`,
  'blog.seo.baseTitle': 'Open4Goods blog',
  'blog.seo.tagTitle': ({ tag }) => `Open4Goods blog – ${tag}`,
  'blog.seo.pageTitle': ({ title, page }) => `${title} – Page ${page}`,
  'blog.seo.description': 'Latest insights and company updates.',
  'blog.seo.tagDescription': ({ tag }) => `Articles about ${tag}.`,
  'common.actions.retry': 'Retry',
  'siteIdentity.siteName': 'Nudger',
  'siteIdentity.links.linkedin': 'https://www.linkedin.com/company/nudger/',
}

type TranslateParams = Record<string, unknown>

const translate = (key: string, params: TranslateParams = {}) => {
  const value = translations[key]

  if (typeof value === 'function') {
    return value(params)
  }

  if (typeof value === 'string') {
    return value
  }

  if (Object.keys(params).length > 0) {
    return `${key} ${JSON.stringify(params)}`
  }

  return key
}

const localeRef = ref('en')

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params: TranslateParams = {}) => translate(key, params),
    locale: localeRef,
  }),
}))

const createReactiveRoute = (query: Record<string, unknown> = {}) => {
  const baseQuery: Record<string, unknown> = { ...query }

  return reactive({
    path: '/blog',
    name: 'blog-index',
    fullPath: '/blog',
    params: {},
    query: reactive(baseQuery),
  })
}

let route = createReactiveRoute()
let routerPush: Mock

const updateRouteQuery = (nextQuery: Record<string, unknown> | undefined) => {
  const sanitizedEntries = Object.entries(nextQuery ?? {}).filter(
    ([, value]) => value !== undefined && value !== null
  )
  const sanitizedQuery = Object.fromEntries(sanitizedEntries)

  route.query = reactive(sanitizedQuery) as typeof route.query
}

vi.mock('#app', () => ({
  useRoute: () => route,
  useRouter: () => ({
    push: (to: unknown) => routerPush(to),
  }),
}))

type MockBlogComposable = ReturnType<typeof createMockBlogComposable>
let blogComposable: MockBlogComposable

vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => blogComposable,
}))

vi.mock('~/composables/useThemedAsset', async () => {
  const { ref } = await import('vue')
  return {
    useThemeAsset: () => ref('https://example.com/mock-bg.jpg'),
  }
})

const createMockBlogComposable = (config: MockBlogConfig = {}) => {
  const paginatedArticles = ref<BlogArticle[]>([
    ...(config.articles ?? defaultArticles),
  ])
  const tags = ref<BlogTag[]>([...(config.initialTags ?? [])])
  const loading = ref(config.loading ?? false)
  const error = ref<string | null>(config.error ?? null)
  const pagination = ref({
    page: config.pagination?.page ?? 1,
    size: config.pagination?.size ?? 6,
    totalElements:
      config.pagination?.totalElements ?? paginatedArticles.value.length,
    totalPages: config.pagination?.totalPages ?? 3,
  })
  const selectedTag = ref<string | null>(config.selectedTag ?? null)

  const fetchArticles = vi.fn(
    async (
      page: number = pagination.value.page,
      size: number = pagination.value.size,
      tag: string | null = selectedTag.value
    ) => {
      const sanitizedTag = tag ?? null

      pagination.value = {
        ...pagination.value,
        page,
        size,
        totalElements:
          config.pagination?.totalElements ?? pagination.value.totalElements,
        totalPages:
          config.pagination?.totalPages ?? pagination.value.totalPages,
      }

      selectedTag.value = sanitizedTag

      if (config.onFetchArticles) {
        const result = await config.onFetchArticles({
          page,
          size,
          tag: sanitizedTag,
        })

        if (result?.articles) {
          paginatedArticles.value = [...result.articles]
        }

        if (result?.pagination) {
          pagination.value = {
            ...pagination.value,
            ...result.pagination,
          }
        }
      }
    }
  )

  const fetchTags = vi.fn(async () => {
    const resolved = config.resolveTags
      ? await config.resolveTags()
      : (config.initialTags ?? defaultTags)

    tags.value = resolved.map(tag => ({ ...tag }))
  })

  return {
    paginatedArticles,
    loading,
    error,
    pagination,
    fetchArticles,
    tags,
    selectedTag,
    fetchTags,
    resetPagination: vi.fn(),
  }
}

const flushPromises = async () => {
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
}

const createStub = (tag: string, className: string) =>
  defineComponent({
    name: `${className}Stub`,
    setup(_, { slots, attrs }) {
      return () => {
        const data: Record<string, unknown> = { ...attrs }
        const classes: string[] = [className]

        if (typeof data.class === 'string') {
          classes.push(data.class)
        } else if (Array.isArray(data.class)) {
          classes.push(...data.class.map(String))
        }

        data.class = classes.join(' ')

        return h(tag, data, slots.default?.())
      }
    },
  })

const VContainerStub = createStub('div', 'v-container-stub')
const VSheetStub = createStub('section', 'v-sheet-stub')
const VRowStub = createStub('div', 'v-row-stub')
const VColStub = createStub('div', 'v-col-stub')
const VCardTitleStub = createStub('h2', 'v-card-title-stub')
const VCardTextStub = createStub('div', 'v-card-text-stub')
const VCardActionsStub = createStub('div', 'v-card-actions-stub')
const VProgressCircularStub = createStub('div', 'v-progress-circular')
const VAlertStub = createStub('div', 'v-alert-stub')
const VSpacerStub = createStub('div', 'v-spacer-stub')
const VPaginationStub = createStub('nav', 'v-pagination-stub')

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: { icon: { type: String, default: '' } },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'span',
        {
          ...attrs,
          class: 'v-icon-stub',
          'data-icon': props.icon,
        },
        slots.default?.()
      )
  },
})

const VChipGroupStub = createStub('div', 'v-chip-group-stub')

const VChipStub = defineComponent({
  name: 'VChipStub',
  props: {
    value: { type: [String, Number, Boolean], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: 'v-chip-stub',
          type: 'button',
          'data-value': props.value,
        },
        slots.default?.()
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  emits: ['click'],
  props: {
    type: { type: String, default: 'button' },
  },
  setup(props, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          class: 'v-btn-stub',
          type: props.type ?? 'button',
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      )
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  props: {
    tag: { type: String, default: 'div' },
    to: { type: [String, Object], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        props.tag || 'div',
        {
          ...attrs,
          class: 'v-card-stub',
          'data-test': 'article-card',
          'data-to':
            typeof props.to === 'string'
              ? props.to
              : props.to
                ? JSON.stringify(props.to)
                : undefined,
        },
        slots.default?.()
      )
  },
})

const VImgStub = defineComponent({
  name: 'VImgStub',
  props: {
    src: { type: String, default: '' },
    alt: { type: String, default: '' },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h('figure', { ...attrs, class: 'v-img-stub' }, [
        h('img', {
          src: props.src,
          alt: props.alt,
          'data-test': 'article-image',
        }),
        slots.placeholder?.(),
      ])
  },
})

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => slots.default?.()
  },
})

const NuxtLinkStub = defineComponent({
  name: 'NuxtLinkStub',
  props: {
    to: { type: [String, Object], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'a',
        {
          ...attrs,
          class: 'nuxt-link-stub',
          'data-to':
            typeof props.to === 'string'
              ? props.to
              : props.to
                ? JSON.stringify(props.to)
                : undefined,
        },
        slots.default?.()
      )
  },
})

const globalStubs = {
  VContainer: VContainerStub,
  VSheet: VSheetStub,
  VIcon: VIconStub,
  VChipGroup: VChipGroupStub,
  VChip: VChipStub,
  VProgressCircular: VProgressCircularStub,
  VAlert: VAlertStub,
  VBtn: VBtnStub,
  VRow: VRowStub,
  VCol: VColStub,
  VCard: VCardStub,
  VCardTitle: VCardTitleStub,
  VCardText: VCardTextStub,
  VCardActions: VCardActionsStub,
  VImg: VImgStub,
  VSpacer: VSpacerStub,
  VPagination: VPaginationStub,
  ClientOnly: ClientOnlyStub,
  NuxtLink: NuxtLinkStub,
}

type MountOptions = {
  routeQuery?: Record<string, unknown>
  blogConfig?: MockBlogConfig
  requestUrlOverride?: URL
}

const mountComponent = async (options: MountOptions = {}) => {
  route = createReactiveRoute(options.routeQuery)
  routerPush = vi.fn(async (to: unknown) => {
    if (typeof to === 'string') {
      return
    }

    const location = to as { query?: Record<string, unknown>; path?: string }

    if (location?.query) {
      updateRouteQuery(location.query)
    }

    if (location?.path) {
      ;(route as Record<string, unknown>).path = location.path
    }
  })

  requestUrl = options.requestUrlOverride ?? new URL('https://example.com/blog')

  blogComposable = createMockBlogComposable(options.blogConfig)

  const wrapper = await mountSuspended(TheArticles, {
    global: {
      stubs: globalStubs,
    },
  })

  await flushPromises()

  return { wrapper }
}

describe('TheArticles.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localeRef.value = 'en'
    route = createReactiveRoute()
    routerPush = vi.fn(async () => {})
    requestUrl = new URL('https://example.com/blog')
    blogComposable = createMockBlogComposable()
  })

  it('initializes from the current route and loads tags and articles', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '2', tag: ' Nuxt ' },
      blogConfig: {
        initialTags: [],
        resolveTags: () => defaultTags,
        pagination: { page: 1, size: 6, totalPages: 4, totalElements: 24 },
      },
      requestUrlOverride: new URL('https://example.com/blog?page=2&tag=Nuxt'),
    })

    expect(blogComposable.fetchTags).toHaveBeenCalledTimes(1)
    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(2, 6, 'Nuxt')

    expect(blogComposable.pagination.value.page).toBe(2)
    expect(blogComposable.pagination.value.totalPages).toBe(4)
    expect(blogComposable.selectedTag.value).toBe('Nuxt')

    const tagButtons = wrapper
      .find('.v-chip-group-stub')
      .findAll('.v-chip-stub')
    expect(tagButtons).toHaveLength(3)
    const [, nuxtChip, vueChip] = tagButtons
    expect(nuxtChip).toBeDefined()
    expect(vueChip).toBeDefined()
    expect(nuxtChip!.text()).toBe('Nuxt (3)')
    expect(vueChip!.text()).toBe('Vue (5)')

    const exposed = wrapper.vm as TheArticlesPublicInstance
    expect(exposed.canonicalUrl).toContain('/blog?page=2&tag=Nuxt')
  })

  it('renders article cards with metadata and accessible attributes', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '2', tag: 'Nuxt' },
      blogConfig: {
        articles: defaultArticles,
        initialTags: defaultTags,
        pagination: { page: 2, size: 6, totalPages: 3, totalElements: 12 },
        selectedTag: 'Nuxt',
      },
      requestUrlOverride: new URL('https://example.com/blog?page=2&tag=Nuxt'),
    })

    const cards = wrapper.findAll('[data-test="article-card"]')
    expect(cards).toHaveLength(defaultArticles.length)
    const [firstCard, secondCard] = cards
    expect(firstCard).toBeDefined()
    expect(secondCard).toBeDefined()
    expect(firstCard!.attributes('data-to')).toBe('/blog/hello-world')
    expect(secondCard!.attributes('data-to')).toBe('/blog/vue-insights')

    const titles = wrapper.findAll('.v-card-title-stub')
    const [firstTitle, secondTitle] = titles
    expect(firstTitle).toBeDefined()
    expect(secondTitle).toBeDefined()
    expect(firstTitle!.text()).toContain('Hello Nuxt World')
    expect(secondTitle!.text().trim()).toBe('')

    const summaries = wrapper.findAll('.v-card-text-stub')
    const [firstSummary] = summaries
    expect(firstSummary).toBeDefined()
    expect(firstSummary!.text()).toContain('A welcome post for the blog.')

    const timeElement = wrapper.find('time')
    const firstArticle = defaultArticles[0]!
    expect(timeElement.attributes('datetime')).toBe(
      new Date(firstArticle.createdMs ?? 0).toISOString()
    )

    const images = wrapper.findAll('[data-test="article-image"]')
    const [firstImage, secondImage] = images
    expect(firstImage).toBeDefined()
    expect(secondImage).toBeDefined()
    expect(firstImage!.attributes('alt')).toBe('Hello Nuxt World')
    expect(secondImage!.attributes('alt')).toBe('Blog article illustration')

    expect(wrapper.find('.v-pagination-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('Page 2 of 3 (12)')
  })

  it('shows a loading indicator while articles are being fetched', async () => {
    const { wrapper } = await mountComponent({
      blogConfig: {
        loading: true,
        articles: [],
        pagination: { page: 1, size: 6, totalPages: 1, totalElements: 0 },
      },
    })

    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    expect(wrapper.text()).toContain('Loading articles…')
  })

  it('displays an error state and retries fetching when requested', async () => {
    const { wrapper } = await mountComponent({
      blogConfig: {
        error: 'Network error',
        loading: false,
        articles: [],
        pagination: { page: 1, size: 6, totalPages: 1, totalElements: 0 },
      },
    })

    expect(wrapper.find('.v-alert-stub').text()).toContain('Network error')

    blogComposable.fetchArticles.mockClear()

    await wrapper.find('.v-btn-stub').trigger('click')
    await flushPromises()

    expect(blogComposable.fetchArticles).toHaveBeenCalledTimes(1)
  })

  it('updates the router and refetches when changing page', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '2', tag: 'Nuxt' },
      blogConfig: {
        pagination: { page: 2, size: 6, totalPages: 4, totalElements: 24 },
        selectedTag: 'Nuxt',
      },
    })

    routerPush.mockClear()
    blogComposable.fetchArticles.mockClear()

    await (wrapper.vm as unknown as TheArticlesPublicInstance).handlePageChange(
      3
    )
    await flushPromises()

    expect(routerPush).toHaveBeenCalledWith({
      query: { page: '3', tag: 'Nuxt' },
    })
    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(3, 6, 'Nuxt')
    expect(blogComposable.pagination.value.page).toBe(3)
  })

  it('removes the page query when navigating back to the first page', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '2' },
      blogConfig: {
        pagination: { page: 2, size: 6, totalPages: 4, totalElements: 24 },
      },
    })

    routerPush.mockClear()
    blogComposable.fetchArticles.mockClear()

    await (wrapper.vm as unknown as TheArticlesPublicInstance).handlePageChange(
      1
    )
    await flushPromises()

    expect(routerPush).toHaveBeenCalledWith({ query: {} })
    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(1, 6, null)
    expect(blogComposable.pagination.value.page).toBe(1)
    expect((route.query as Record<string, unknown>).page).toBeUndefined()
  })

  it('navigates and refetches when selecting a specific tag', async () => {
    const { wrapper } = await mountComponent({
      blogConfig: {
        initialTags: defaultTags,
        pagination: { page: 1, size: 6, totalPages: 3, totalElements: 18 },
      },
    })

    routerPush.mockClear()
    blogComposable.fetchArticles.mockClear()

    await (
      wrapper.vm as unknown as TheArticlesPublicInstance
    ).handleTagSelection('Vue')
    await flushPromises()

    expect(routerPush).toHaveBeenCalledWith({
      path: '/blog',
      query: { tag: 'Vue' },
    })
    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(1, 6, 'Vue')
    expect(blogComposable.pagination.value.page).toBe(1)
    expect(blogComposable.selectedTag.value).toBe('Vue')
  })

  it('clears filters and resets pagination when selecting the "all" option', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '3', tag: 'Nuxt' },
      blogConfig: {
        initialTags: defaultTags,
        pagination: { page: 3, size: 6, totalPages: 5, totalElements: 42 },
        selectedTag: 'Nuxt',
      },
    })

    routerPush.mockClear()
    blogComposable.fetchArticles.mockClear()

    await (
      wrapper.vm as unknown as TheArticlesPublicInstance
    ).handleTagSelection(null)
    await flushPromises()

    expect(routerPush).toHaveBeenCalledWith({ path: '/blog', query: {} })
    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(1, 6, null)
    expect(blogComposable.pagination.value.page).toBe(1)
    expect(blogComposable.selectedTag.value).toBeNull()
    expect((route.query as Record<string, unknown>).tag).toBeUndefined()
    expect((route.query as Record<string, unknown>).page).toBeUndefined()
  })

  it('computes consistent SEO metadata and structured data', async () => {
    const { wrapper } = await mountComponent({
      routeQuery: { page: '2', tag: 'Nuxt' },
      blogConfig: {
        articles: defaultArticles,
        initialTags: defaultTags,
        pagination: { page: 2, size: 6, totalPages: 4, totalElements: 24 },
        selectedTag: 'Nuxt',
      },
      requestUrlOverride: new URL('https://example.com/blog?page=2&tag=Nuxt'),
    })

    const vm = wrapper.vm as TheArticlesPublicInstance

    expect(vm.pageSeoTitle).toBe('Open4Goods blog – Nuxt – Page 2')
    expect(vm.seoDescription).toBe(
      'Articles about Nuxt. A welcome post for the blog.'
    )
    expect(vm.primaryArticleImage).toBe(
      'https://cdn.example.com/images/hello.jpg'
    )
    expect(vm.canonicalUrl).toContain('/blog?page=2&tag=Nuxt')

    const schema = vm.structuredData as Record<string, unknown>
    expect(schema).toMatchObject({
      '@context': 'https://schema.org',
    })

    const graph = (schema['@graph'] ?? []) as Array<Record<string, unknown>>
    const collectionPage = graph.find(
      entry => entry['@type'] === 'CollectionPage'
    )
    const breadcrumbList = graph.find(
      entry => entry['@type'] === 'BreadcrumbList'
    )
    const blogPosting = graph.find(
      entry => entry['@type'] === 'BlogPosting'
    )

    expect(collectionPage).toMatchObject({
      name: 'Open4Goods blog – Nuxt – Page 2',
      description: 'Articles about Nuxt. A welcome post for the blog.',
      url: expect.stringContaining('/blog?page=2&tag=Nuxt'),
      inLanguage: 'en',
      about: 'Nuxt',
    })
    expect(breadcrumbList).toBeTruthy()
    expect(blogPosting).toMatchObject({
      headline: 'Hello Nuxt World',
      description: 'A welcome post for the blog.',
      url: expect.stringContaining('/blog/hello-world'),
    })
  })

  it('reacts to route query updates triggered outside the component', async () => {
    await mountComponent({
      routeQuery: { page: '1', tag: 'Nuxt' },
      blogConfig: {
        initialTags: defaultTags,
        pagination: { page: 1, size: 6, totalPages: 5, totalElements: 42 },
        selectedTag: 'Nuxt',
      },
    })

    blogComposable.fetchArticles.mockClear()
    ;(route.query as Record<string, unknown>).page = '4'
    ;(route.query as Record<string, unknown>).tag = 'Vue'
    await flushPromises()

    expect(blogComposable.fetchArticles).toHaveBeenCalledWith(4, 6, 'Vue')
  })
})

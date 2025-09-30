import { mountSuspended } from '@nuxt/test-utils/runtime'
import { createPinia, setActivePinia } from 'pinia'
import { computed, nextTick, reactive, ref } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import TheArticles from './TheArticles.vue'

type MockArticle = {
  id: string
  title: string
  summary: string
  author: string
  createdMs: number
  image: string | null
  url: string
}

const mockArticles: MockArticle[] = [
  {
    id: '1',
    title: 'Test Article 1',
    summary: 'This is a test summary for article 1',
    author: 'Test Author',
    createdMs: 1640995200000, // 2022-01-01
    image: 'https://example.com/image1.jpg',
    url: 'test-article-1',
  },
  {
    id: '2',
    title: 'Test Article 2',
    summary: 'This is a test summary for article 2',
    author: 'Test Author 2',
    createdMs: 1641081600000, // 2022-01-02
    image: null,
    url: 'test-article-2',
  },
]

const cloneArticleWithoutImage = (article: MockArticle): MockArticle => ({
  ...article,
  image: null,
})

const articlesRef = ref(mockArticles)
const paginatedArticlesRef = ref(mockArticles)
const loadingRef = ref(false)
const errorRef = ref<string | null>(null)
const paginationRef = ref({
  page: 1,
  size: 12,
  totalElements: mockArticles.length,
  totalPages: 1,
})

const tagsRef = ref([
  { name: 'Energy', count: 3 },
  { name: 'Kitchen', count: 1 },
])
const selectedTagRef = ref<string | null>(null)

const fetchArticlesMock = vi.fn(
  async (page: number = 1, size: number = 12, tag: string | null = null) => {
    paginationRef.value = {
      ...paginationRef.value,
      page,
      size,
    }
    selectedTagRef.value = tag ?? null
  }
)
const fetchTagsMock = vi.fn(async () => {})
const changePageMock = vi.fn()
const selectTagMock = vi.fn(async (tag: string | null) => {
  selectedTagRef.value = tag
  await fetchArticlesMock(1, paginationRef.value.size, tag)
})
const routeQuery = reactive<Record<string, string | undefined>>({})
const mockRouterPush = vi.fn(
  async ({
    query,
  }: {
    query?: Record<string, string | undefined>
    path?: string
  } = {}) => {
    const nextQuery = query ?? {}

    Object.keys(routeQuery).forEach(key => {
      if (!(key in nextQuery) || nextQuery[key] === undefined) {
        Reflect.deleteProperty(routeQuery, key)
      }
    })

    Object.entries(nextQuery).forEach(([key, value]) => {
      if (value === undefined) {
        Reflect.deleteProperty(routeQuery, key)
      } else {
        routeQuery[key] = value
      }
    })
  }
)

const mockUseBlog = {
  articles: articlesRef,
  paginatedArticles: computed(() => paginatedArticlesRef.value),
  loading: loadingRef,
  error: errorRef,
  pagination: paginationRef,
  fetchArticles: fetchArticlesMock,
  changePage: changePageMock,
  tags: computed(() => tagsRef.value),
  selectedTag: computed(() => selectedTagRef.value),
  fetchTags: fetchTagsMock,
  selectTag: selectTagMock,
}

// Mock the useBlog composable
vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => mockUseBlog,
}))

// Mock the useAppStore to enable debug mode by default
vi.mock('@/stores/useAppStore', () => ({
  useAppStore: () => ({
    debugMode: true,
  }),
}))

const localeRef = ref('fr-FR')

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: {
      apiUrl: 'https://test-api.example.com',
    },
  }),
  useRoute: () => ({
    query: routeQuery,
  }),
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

vi.mock('#imports', () => ({
  useRoute: () => ({
    query: routeQuery,
  }),
  useRouter: () => ({
    push: mockRouterPush,
  }),
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    locale: localeRef,
    t: (key: string) => key,
  }),
}))

describe('TheArticles Component', () => {
  beforeEach(() => {
    // Reset mocks
    vi.clearAllMocks()
    setActivePinia(createPinia())
    localeRef.value = 'fr-FR'
    articlesRef.value = mockArticles
    paginatedArticlesRef.value = mockArticles
    loadingRef.value = false
    errorRef.value = null
    paginationRef.value = {
      page: 1,
      size: 12,
      totalElements: mockArticles.length,
      totalPages: 1,
    }
    fetchArticlesMock.mockReset()
    fetchTagsMock.mockReset()
    changePageMock.mockReset()
    selectTagMock.mockReset()
    mockRouterPush.mockReset()
    tagsRef.value = [
      { name: 'Energy', count: 3 },
      { name: 'Kitchen', count: 1 },
    ]
    selectedTagRef.value = null
    Object.keys(routeQuery).forEach(key => {
      Reflect.deleteProperty(routeQuery, key)
    })
  })

  test('should render loading state', async () => {
    // Override loading state
    loadingRef.value = true
    articlesRef.value = []
    paginatedArticlesRef.value = []

    const wrapper = await mountSuspended(TheArticles)

    expect(wrapper.find('.loading').exists()).toBe(true)
    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    expect(wrapper.text()).toContain('blog.list.loading')
  })

  test('should render articles in cards', async () => {
    // Override normal state
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles
    paginatedArticlesRef.value = mockArticles

    const wrapper = await mountSuspended(TheArticles)

    // Check if articles are rendered
    const articleCards = wrapper.findAll('.article-card')
    expect(articleCards).toHaveLength(2)

    // Check first article content
    const firstCard = articleCards[0]
    if (!firstCard) {
      throw new Error('Expected at least one article card')
    }
    expect(firstCard.find('.article-title').text()).toBe('Test Article 1')
    expect(firstCard.find('.article-summary').text()).toContain(
      'This is a test summary for article 1'
    )
    expect(firstCard.find('.author-name').text()).toBe('Test Author')
  })

  test('should handle articles without images', async () => {
    loadingRef.value = false
    errorRef.value = null
    const [firstArticle] = mockArticles
    if (!firstArticle) {
      throw new Error('Expected at least one article to clone')
    }

    const imagelessArticle = cloneArticleWithoutImage(firstArticle)

    articlesRef.value = [imagelessArticle]
    paginatedArticlesRef.value = [imagelessArticle]

    const wrapper = await mountSuspended(TheArticles)

    const articleCard = wrapper.find('.article-card')
    expect(articleCard.exists()).toBe(true)

    // Should not have v-card-media when no image
    expect(articleCard.find('.v-card-media').exists()).toBe(false)
  })

  test('should format date correctly', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles
    paginatedArticlesRef.value = mockArticles

    const wrapper = await mountSuspended(TheArticles)

    const dateText = wrapper.find('.date-text')
    expect(dateText.exists()).toBe(true)
    // The date should be formatted (exact format depends on locale)
    expect(dateText.text()).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/)
  })

  test('should toggle debug mode', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles
    paginatedArticlesRef.value = mockArticles

    const wrapper = await mountSuspended(TheArticles)

    // Debug info should not be visible initially
    expect(wrapper.find('.debug-info').exists()).toBe(false)

    // Find and click debug toggle button using specific selector
    const debugButton = wrapper.find('.debug-toggle')
    expect(debugButton.exists()).toBe(true)
    expect(debugButton.text()).toBe('Show Debug Info')

    await debugButton.trigger('click')

    // Debug info should be visible after click
    expect(wrapper.find('.debug-info').exists()).toBe(true)
    expect(wrapper.find('.debug-toggle').text()).toBe('Hide Debug Info')
  })

  test('should fetch articles on mount', async () => {
    paginatedArticlesRef.value = []

    await mountSuspended(TheArticles)
    await nextTick()

    expect(fetchArticlesMock).toHaveBeenCalledTimes(1)
    expect(fetchArticlesMock).toHaveBeenCalledWith(1, 12, null)
  })

  test('should handle empty articles array', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = []
    paginatedArticlesRef.value = []

    const wrapper = await mountSuspended(TheArticles)

    // Should not render any article cards
    const articleCards = wrapper.findAll('.article-card')
    expect(articleCards).toHaveLength(0)
  })

  test('read more button links to canonical blog route', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles.map((article, index) => ({
      ...article,
      url: index === 0 ? '/blog/test-article-1' : article.url,
    }))

    const wrapper = await mountSuspended(TheArticles)

    const readMoreButton = wrapper.find('[data-test="article-read-more"]')
    expect(readMoreButton.exists()).toBe(true)
    const readMoreHref =
      readMoreButton.attributes('href') ?? readMoreButton.attributes('to') ?? ''
    expect(readMoreHref).toBe('/blog/test-article-1')
  })

  test('image links to canonical blog route when available', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles
    paginatedArticlesRef.value = mockArticles

    const wrapper = await mountSuspended(TheArticles)

    const imageLink = wrapper.find('[data-test="article-image-link"]')
    expect(imageLink.exists()).toBe(true)
    expect(imageLink.attributes('href')).toBe('/blog/test-article-1')
  })

  test('pushes query parameters when selecting a tag', async () => {
    const wrapper = await mountSuspended(TheArticles)
    await nextTick()

    const tagChips = wrapper.findAll('.tag-filter__chip')
    expect(tagChips.length).toBeGreaterThan(1)

    const energyChip = tagChips[1]
    if (!energyChip) {
      throw new Error('Expected a tag chip to test selection')
    }

    await energyChip.trigger('click')

    expect(mockRouterPush).toHaveBeenCalledWith({
      path: '/blog',
      query: { tag: 'Energy' },
    })
  })

  test('fetches articles again when tag query changes', async () => {
    await mountSuspended(TheArticles)
    await nextTick()

    fetchArticlesMock.mockClear()
    routeQuery.tag = 'Energy'
    await nextTick()
    await nextTick()

    expect(fetchArticlesMock).toHaveBeenCalledWith(1, 12, 'Energy')
  })

  test('loads tags from the API when none are cached', async () => {
    tagsRef.value = []

    await mountSuspended(TheArticles)
    await nextTick()

    expect(fetchTagsMock).toHaveBeenCalledTimes(1)
  })

  test('highlights the selected tag from the route', async () => {
    routeQuery.tag = 'Energy'

    const wrapper = await mountSuspended(TheArticles)
    await nextTick()

    const tagChips = wrapper.findAll('.tag-filter__chip')
    const energyChip = tagChips[1]
    if (!energyChip) {
      throw new Error('Expected Energy tag chip to exist')
    }

    expect(energyChip.classes()).toContain('tag-filter__chip--active')
  })

  test('buildArticleLink normalizes different url formats', async () => {
    const wrapper = await mountSuspended(TheArticles)

    const instance = wrapper.vm as unknown as {
      extractArticleSlug: (slug: string | null | undefined) => string | null
      buildArticleLink: (slug: string | null | undefined) => string | undefined
    }

    expect(instance.extractArticleSlug('test-article-1')).toBe('test-article-1')
    expect(instance.extractArticleSlug('/blog/test-article-2')).toBe(
      'test-article-2'
    )
    expect(
      instance.extractArticleSlug('https://example.com/blog/test-article-3')
    ).toBe('test-article-3')
    expect(instance.extractArticleSlug('  ')).toBeNull()
    expect(instance.extractArticleSlug(null)).toBeNull()

    expect(instance.buildArticleLink('test-article-1')).toBe(
      '/blog/test-article-1'
    )
    expect(instance.buildArticleLink('/blog/test-article-2')).toBe(
      '/blog/test-article-2'
    )
    expect(
      instance.buildArticleLink('https://example.com/blog/test-article-3')
    ).toBe('/blog/test-article-3')
    expect(instance.buildArticleLink('  ')).toBeUndefined()
    expect(instance.buildArticleLink(null)).toBeUndefined()
  })

  test('should display pagination controls when multiple pages exist', async () => {
    paginationRef.value = {
      page: 1,
      size: 12,
      totalElements: 24,
      totalPages: 2,
    }

    const wrapper = await mountSuspended(TheArticles)

    expect(wrapper.find('.pagination-container').exists()).toBe(true)
  })

  test('should update the route when page changes', async () => {
    paginationRef.value = {
      page: 1,
      size: 12,
      totalElements: 24,
      totalPages: 3,
    }

    const wrapper = await mountSuspended(TheArticles)

    const instance = wrapper.vm as unknown as {
      handlePageChange: (page: number) => Promise<void>
    }

    await instance.handlePageChange(2)
    await nextTick()

    expect(mockRouterPush).toHaveBeenCalledWith({ query: { page: '2' } })
  })
})

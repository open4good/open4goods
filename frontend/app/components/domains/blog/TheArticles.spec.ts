import { mountSuspended } from '@nuxt/test-utils/runtime'
import { createPinia, setActivePinia } from 'pinia'
import { computed, nextTick, reactive, ref } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import * as localizedRoutes from '~~/shared/utils/localized-routes'
import TheArticles from './TheArticles.vue'

const resolveLocalizedRoutePathSpy = vi.spyOn(localizedRoutes, 'resolveLocalizedRoutePath')

// Mock the useBlog composable
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

const fetchArticlesMock = vi.fn()
const changePageMock = vi.fn()
const routeQuery = reactive<Record<string, string | undefined>>({})
const mockRouterPush = vi.fn(
  async ({ query }: { query?: Record<string, string | undefined> } = {}) => {
    const nextQuery = query ?? {}

    Object.keys(routeQuery).forEach((key) => {
      if (!(key in nextQuery) || nextQuery[key] === undefined) {
        delete routeQuery[key]
      }
    })

    Object.entries(nextQuery).forEach(([key, value]) => {
      if (value === undefined) {
        delete routeQuery[key]
      } else {
        routeQuery[key] = value
      }
    })
  },
)

const mockUseBlog = {
  articles: articlesRef,
  paginatedArticles: computed(() => paginatedArticlesRef.value),
  loading: loadingRef,
  error: errorRef,
  pagination: paginationRef,
  fetchArticles: fetchArticlesMock,
  changePage: changePageMock,
}

// Mock the useBlog composable
vi.mock('~/composables/blog/useBlog', () => ({
  useBlog: () => mockUseBlog,
}))

const mockNavigateTo = vi.fn()
const localeRef = ref('fr-FR')

vi.mock('#app', () => ({
  useRuntimeConfig: () => ({
    public: {
      apiUrl: 'https://test-api.example.com',
    },
  }),
  navigateTo: mockNavigateTo,
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
    resolveLocalizedRoutePathSpy.mockClear()
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
    changePageMock.mockReset()
    mockRouterPush.mockReset()
    Object.keys(routeQuery).forEach((key) => {
      delete routeQuery[key]
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
    expect(wrapper.text()).toContain('Chargement des articles')
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

    // Find and click debug toggle button
    const debugButton = wrapper.find('button')
    expect(debugButton.text()).toBe('Show Debug Info')

    await debugButton.trigger('click')

    // Debug info should be visible after click
    expect(wrapper.find('.debug-info').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('Hide Debug Info')
  })

  test('should request initial page on mount', async () => {
    paginatedArticlesRef.value = []

    await mountSuspended(TheArticles)
    await nextTick()

    expect(changePageMock).toHaveBeenCalledTimes(1)
    expect(changePageMock).toHaveBeenCalledWith(1)
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

  test('computes localized article path when clicking read more', async () => {
    loadingRef.value = false
    errorRef.value = null
    articlesRef.value = mockArticles
    localeRef.value = 'en-US'

    const wrapper = await mountSuspended(TheArticles)

    const readMoreButton = wrapper.find('.article-actions button')
    expect(readMoreButton.exists()).toBe(true)

    await readMoreButton.trigger('click')

    expect(resolveLocalizedRoutePathSpy).toHaveBeenCalledWith('blog-slug', 'en-US', {
      slug: 'test-article-1',
    })
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

  test('should request page change through composable', async () => {
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

    expect(changePageMock).toHaveBeenCalledWith(2)
  })
})

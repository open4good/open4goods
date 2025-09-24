import { mountSuspended } from '@nuxt/test-utils/runtime'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { beforeEach, describe, expect, test, vi } from 'vitest'

import * as localizedRoutes from '~~/shared/utils/localized-routes'
import TheArticles from './TheArticles.vue'

const resolveLocalizedRoutePathSpy = vi.spyOn(localizedRoutes, 'resolveLocalizedRoutePath')

// Mock du composable useBlog
const mockArticles = [
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

const mockUseBlog = {
  articles: mockArticles,
  loading: false,
  error: null,
  fetchArticles: vi.fn(),
}

// Mock du composable useBlog
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
  })

  test('should render loading state', async () => {
    // Override loading state
    mockUseBlog.loading = true
    mockUseBlog.articles = []

    const wrapper = await mountSuspended(TheArticles)

    expect(wrapper.find('.loading').exists()).toBe(true)
    expect(wrapper.find('.v-progress-circular').exists()).toBe(true)
    expect(wrapper.text()).toContain('Chargement des articles')
  })

  test('should render articles in cards', async () => {
    // Override normal state
    mockUseBlog.loading = false
    mockUseBlog.error = null
    mockUseBlog.articles = mockArticles

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
    mockUseBlog.loading = false
    mockUseBlog.error = null
    const imagelessArticle = mockArticles.find((article) => !article.image)
    if (!imagelessArticle) {
      throw new Error('Expected at least one article without an image')
    }
    mockUseBlog.articles = [imagelessArticle]

    const wrapper = await mountSuspended(TheArticles)

    const articleCard = wrapper.find('.article-card')
    expect(articleCard.exists()).toBe(true)

    // Should not have v-card-media when no image
    expect(articleCard.find('.v-card-media').exists()).toBe(false)
  })

  test('should format date correctly', async () => {
    mockUseBlog.loading = false
    mockUseBlog.error = null
    mockUseBlog.articles = mockArticles

    const wrapper = await mountSuspended(TheArticles)

    const dateText = wrapper.find('.date-text')
    expect(dateText.exists()).toBe(true)
    // The date should be formatted (exact format depends on locale)
    expect(dateText.text()).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/)
  })

  test('should toggle debug mode', async () => {
    mockUseBlog.loading = false
    mockUseBlog.error = null
    mockUseBlog.articles = mockArticles

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

  test('should call fetchArticles on mount', async () => {
    await mountSuspended(TheArticles)

    expect(mockUseBlog.fetchArticles).toHaveBeenCalledTimes(1)
  })

  test('should handle empty articles array', async () => {
    mockUseBlog.loading = false
    mockUseBlog.error = null
    mockUseBlog.articles = []

    const wrapper = await mountSuspended(TheArticles)

    // Should not render any article cards
    const articleCards = wrapper.findAll('.article-card')
    expect(articleCards).toHaveLength(0)
  })

  test('computes localized article path when clicking read more', async () => {
    mockUseBlog.loading = false
    mockUseBlog.error = null
    mockUseBlog.articles = mockArticles
    localeRef.value = 'en-US'

    const wrapper = await mountSuspended(TheArticles)

    const readMoreButton = wrapper.find('.article-actions button')
    expect(readMoreButton.exists()).toBe(true)

    await readMoreButton.trigger('click')

    expect(resolveLocalizedRoutePathSpy).toHaveBeenCalledWith('blog-slug', 'en-US', {
      slug: 'test-article-1',
    })
  })
})

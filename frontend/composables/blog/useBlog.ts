import {
  BlogApi,
  Configuration,
  type BlogPostDto,
  type PageDto,
} from '~/src/api'

/**
 * Composable for blog-related functionality
 */
export const useBlog = () => {
  // Runtime config for API base URL
  const config = useRuntimeConfig()
  const api = new BlogApi(new Configuration({ basePath: config.public.blogUrl }))

  // Reactive state
  const articles = ref<BlogPostDto[]>([])
  const currentArticle = ref<BlogPostDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  })

  /**
   * Fetch all blog articles
   */
  const fetchArticles = async () => {
    loading.value = true
    error.value = null

    try {
      const response: PageDto = await api.posts()
      articles.value = (response.data ?? []) as BlogPostDto[]
      pagination.value = {
        page: response.page?.number ?? 0,
        size: response.page?.size ?? 10,
        totalElements: response.page?.totalElements ?? 0,
        totalPages: response.page?.totalPages ?? 0,
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch articles'
      console.error('Error in fetchArticles:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single article by ID
   * @param id - Article ID
   */
  const fetchArticleById = async (id: string) => {
    loading.value = true
    error.value = null

    try {
      const article = await api.post({ slug: id })
      currentArticle.value = article
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch article'
      console.error('Error in fetchArticleById:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Clear current article
   */
  const clearCurrentArticle = () => {
    currentArticle.value = null
  }

  /**
   * Clear error state
   */
  const clearError = () => {
    error.value = null
  }

  return {
    // State
    articles: readonly(articles),
    currentArticle: readonly(currentArticle),
    loading: readonly(loading),
    error: readonly(error),
    pagination: readonly(pagination),

    // Actions
    fetchArticles,
    fetchArticleById,
    clearCurrentArticle,
    clearError,
  }
}

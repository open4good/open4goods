import type { BlogPostDto, PageDto } from '~/src/api'

/**
 * Composable for blog-related functionality
 */
export const useBlog = () => {
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
      // Use our server API as proxy instead of calling external API directly
      const response = await $fetch<PageDto>('/api/blog/articles')

      articles.value = response.data || []
      pagination.value = {
        page: response.page?.number || 0,
        size: response.page?.size || 10,
        totalElements: response.page?.totalElements || 0,
        totalPages: response.page?.totalPages || 0,
      }
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch articles'
      console.error('Error in fetchArticles:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch a single article by slug
   * @param slug - Article slug
   * @returns The fetched article or null if not found
   */
  const fetchArticle = async (slug: string) => {
    loading.value = true
    error.value = null

    try {
      const article = await $fetch<BlogPostDto>(`/api/blog/articles/${slug}`)
      currentArticle.value = article
      return article
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch article'
      console.error('Error in fetchArticle:', err)
      return null
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
    fetchArticle,
    clearCurrentArticle,
    clearError,
  }
}

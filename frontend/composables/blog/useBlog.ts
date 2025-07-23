import type { BlogArticleData } from '~/server/api/blog/types/blog.models'
import { blogService } from '~/services/blog.services'

/**
 * Composable for blog-related functionality
 */
export const useBlog = () => {
  // Reactive state
  const articles = ref<BlogArticleData[]>([])
  const currentArticle = ref<BlogArticleData | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  })
  const currentPage = ref(0)
  const hasMore = ref(true)

  /**
   * Fetch all blog articles
   */
  const fetchArticles = async (page = 0) => {
    loading.value = true
    error.value = null

    try {
      const response = await blogService.getArticles(page, pagination.value.size)

      if (page === 0) {
        articles.value = response.data || []
      } else {
        articles.value = [...articles.value, ...(response.data || [])]
      }

      pagination.value = {
        page: response.page?.number || 0,
        size: response.page?.size || pagination.value.size,
        totalElements: response.page?.totalElements || 0,
        totalPages: response.page?.totalPages || 0,
      }

      currentPage.value = pagination.value.page
      hasMore.value = currentPage.value + 1 < pagination.value.totalPages
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch articles'
      console.error('Error in fetchArticles:', err)
    } finally {
      loading.value = false
    }
  }

  const loadMoreArticles = async () => {
    if (loading.value || !hasMore.value) {
      return
    }
    await fetchArticles(currentPage.value + 1)
  }

  /**
   * Fetch a single article by ID
   * @param id - Article ID
   */
  const fetchArticleById = async (id: string) => {
    loading.value = true
    error.value = null

    try {
      // Use our server API as proxy
      const article = await $fetch<BlogArticleData>(`/api/blog/articles/${id}`)
      currentArticle.value = article
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch article'
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
    currentPage: readonly(currentPage),
    hasMore: readonly(hasMore),

    // Actions
    fetchArticles,
    loadMoreArticles,
    fetchArticleById,
    clearCurrentArticle,
    clearError,
  }
}

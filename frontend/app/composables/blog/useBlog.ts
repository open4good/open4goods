import type { BlogPostDto, PageDto } from '~~/shared/api-client'

/**
 * Composable for blog-related functionality
 */
const DEFAULT_PAGE_SIZE = 12
const MAX_PAGINATION_PAGES = 100

export const useBlog = () => {
  // Reactive state
  const articles = ref<BlogPostDto[]>([])
  const currentArticle = ref<BlogPostDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = ref({
    page: 1,
    size: DEFAULT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  })

  /**
   * Fetch all blog articles
   */
  const fetchArticles = async (
    page: number = pagination.value.page,
    size: number = pagination.value.size,
  ) => {
    loading.value = true
    error.value = null

    try {
      const aggregatedArticles: BlogPostDto[] = []
      let pageSize = Math.max(size, 1)
      let nextPageToFetch = 0
      let totalPagesFromMeta: number | undefined
      let totalElementsFromMeta: number | undefined
      let safetyCounter = 0

      while (true) {
        // Use our server API as proxy instead of calling external API directly
        const response = await $fetch<PageDto>('/api/blog/articles', {
          params: {
            pageNumber: nextPageToFetch,
            pageSize,
          },
        })

        const pageArticles = response.data ?? []
        aggregatedArticles.push(...pageArticles)

        const pageMeta = response.page
        if (pageMeta?.size) {
          pageSize = pageMeta.size
        }
        if (pageMeta?.totalPages !== undefined) {
          totalPagesFromMeta = pageMeta.totalPages
        }
        if (pageMeta?.totalElements !== undefined) {
          totalElementsFromMeta = pageMeta.totalElements
        }

        const hasMorePagesFromMeta =
          totalPagesFromMeta !== undefined && nextPageToFetch + 1 < totalPagesFromMeta
        const hasMorePagesByCount =
          totalPagesFromMeta === undefined &&
          pageArticles.length > 0 &&
          pageArticles.length >= pageSize

        if (!hasMorePagesFromMeta && !hasMorePagesByCount) {
          break
        }

        nextPageToFetch += 1
        safetyCounter += 1

        if (safetyCounter >= MAX_PAGINATION_PAGES) {
          console.warn(
            `Reached safety limit of ${MAX_PAGINATION_PAGES} pages when fetching blog articles. ` +
              'Stopping to avoid an infinite loop.',
          )
          break
        }
      }

      articles.value = aggregatedArticles

      const resolvedTotalElements = totalElementsFromMeta ?? aggregatedArticles.length
      const computedTotalPages =
        totalPagesFromMeta ??
        (pageSize > 0 ? Math.ceil(resolvedTotalElements / pageSize) : 1) ??
        1
      const safeTotalPages = Math.max(computedTotalPages, 1)
      const requestedPage = Math.max(page, 1)
      const boundedPage = Math.min(requestedPage, safeTotalPages)

      pagination.value = {
        page: boundedPage,
        size: pageSize,
        totalElements: resolvedTotalElements,
        totalPages: safeTotalPages,
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
   * Change the current page and load data from the API
   * @param page - 1-based page number
   */
  const changePage = async (page: number) => {
    if (page < 1 || page === pagination.value.page) {
      return
    }

    const maxPage = pagination.value.totalPages || 1
    pagination.value = {
      ...pagination.value,
      page: Math.min(page, maxPage),
    }
  }

  const paginatedArticles = computed(() => {
    const page = Math.max(pagination.value.page, 1)
    const size = Math.max(pagination.value.size, 1)
    const start = (page - 1) * size
    const end = start + size

    return articles.value.slice(start, end)
  })

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
    paginatedArticles: readonly(paginatedArticles),
    currentArticle: readonly(currentArticle),
    loading: readonly(loading),
    error: readonly(error),
    pagination: readonly(pagination),

    // Actions
    fetchArticles,
    changePage,
    fetchArticle,
    clearCurrentArticle,
    clearError,
  }
}

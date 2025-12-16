import type { BlogPostDto, BlogTagDto, PageDto } from '~~/shared/api-client'

/**
 * Composable for blog-related functionality
 */
const DEFAULT_PAGE_SIZE = 12

export const useBlog = () => {
  // Reactive state
  const articles = useState<BlogPostDto[]>('blog-articles', () => [])
  const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
  const currentArticle = useState<BlogPostDto | null>(
    'blog-current-article',
    () => null
  )
  const tags = useState<BlogTagDto[]>('blog-tags', () => [])
  const selectedTag = useState<string | null>('blog-selected-tag', () => null)
  const loading = useState('blog-loading', () => false)
  const error = useState<string | null>('blog-error', () => null)
  const pagination = useState('blog-pagination', () => ({
    page: 1,
    size: DEFAULT_PAGE_SIZE,
    totalElements: 0,
    totalPages: 0,
  }))

  /**
   * Fetch blog articles for a specific page from the backend proxy
   */
  const fetchArticles = async (
    page: number = pagination.value.page,
    size: number = pagination.value.size,
    tag: string | null = selectedTag.value
  ) => {
    loading.value = true
    error.value = null

    try {
      const sanitizedPage = Number.isFinite(page) ? Math.max(page, 1) : 1
      const sanitizedSize = Number.isFinite(size)
        ? Math.max(size, 1)
        : DEFAULT_PAGE_SIZE
      const sanitizedTag = tag?.trim() ?? null

      // Use our server API as proxy instead of calling external API directly
      const response = await $fetch<PageDto>('/api/blog/articles', {
        headers: requestHeaders,
        params: {
          pageNumber: sanitizedPage - 1,
          pageSize: sanitizedSize,
          tag: sanitizedTag || undefined,
        },
      })

      const currentPageArticles = response.data ?? []
      articles.value = currentPageArticles
      selectedTag.value = sanitizedTag

      const pageMeta = response.page
      const resolvedPageSize = pageMeta?.size ?? sanitizedSize
      const resolvedTotalElements =
        pageMeta?.totalElements ?? currentPageArticles.length
      const computedTotalPages =
        pageMeta?.totalPages ??
        (resolvedPageSize > 0
          ? Math.ceil(resolvedTotalElements / resolvedPageSize)
          : 1)
      const safeTotalPages = Math.max(computedTotalPages ?? 1, 1)
      const zeroBasedPage = pageMeta?.number ?? sanitizedPage - 1
      const safePage = Math.min(zeroBasedPage + 1, safeTotalPages)

      pagination.value = {
        page: safePage,
        size: resolvedPageSize,
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
    if (page < 1) {
      return
    }

    const maxPage = pagination.value.totalPages || 1
    const boundedPage = Math.min(page, maxPage)

    const shouldFetchCurrentPage =
      boundedPage !== pagination.value.page || articles.value.length === 0

    if (!shouldFetchCurrentPage) {
      return
    }

    await fetchArticles(boundedPage, pagination.value.size, selectedTag.value)
  }

  const paginatedArticles = computed(() => {
    return articles.value
  })

  const fetchTags = async () => {
    try {
      const response = await $fetch<BlogTagDto[]>('/api/blog/tags', {
        headers: requestHeaders,
      })
      tags.value = response ?? []
    } catch (err) {
      console.error('Error in fetchTags:', err)
      tags.value = []
    }
  }

  const selectTag = async (tag: string | null) => {
    selectedTag.value = tag
    await fetchArticles(1, pagination.value.size, tag)
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
      const article = await $fetch<BlogPostDto>(`/api/blog/articles/${slug}`, {
        headers: requestHeaders,
      })
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
    tags: readonly(tags),
    selectedTag: readonly(selectedTag),
    loading: readonly(loading),
    error: readonly(error),
    pagination: readonly(pagination),

    // Actions
    fetchArticles,
    changePage,
    fetchTags,
    selectTag,
    fetchArticle,
    clearCurrentArticle,
    clearError,
  }
}

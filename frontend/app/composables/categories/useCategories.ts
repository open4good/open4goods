import type {
  VerticalConfigDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

const TWO_HOURS_MS = 2 * 60 * 60 * 1000

type CacheEntry<T> = {
  data: T
  timestamp: number
}

/**
 * Module-level caches, kept out of `useState` on purpose: `useState` content is
 * serialized in `__NUXT_DATA__` of every page, and the full vertical configs
 * weigh hundreds of kilobytes. On the server they are shared by the requests of
 * the process (public data), keyed by host since the language follows the host.
 */
const categoriesListCache = new Map<string, CacheEntry<VerticalConfigDto[]>>()
const categoryDetailCache = new Map<string, CacheEntry<VerticalConfigFullDto>>()

const readCache = <T>(
  cache: Map<string, CacheEntry<T>>,
  key: string
): T | undefined => {
  const entry = cache.get(key)
  return entry && Date.now() - entry.timestamp < TWO_HOURS_MS
    ? entry.data
    : undefined
}

/**
 * Composable for categories-related functionality
 */
export const useCategories = () => {
  // Reactive state
  const categories = useState<VerticalConfigDto[]>('categories-list', () => [])
  const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

  const buildRequestHeaders = () => {
    if (!requestHeaders) {
      return undefined
    }

    const normalizedEntries = Object.entries(requestHeaders).reduce(
      (accumulator, [key, value]) => {
        if (typeof value === 'string' && value.length > 0) {
          accumulator[key] = value
          return accumulator
        }

        if (Array.isArray(value) && value[0]) {
          accumulator[key] = value[0]
        }

        return accumulator
      },
      {} as Record<string, string>
    )

    return Object.keys(normalizedEntries).length ? normalizedEntries : undefined
  }
  const cacheScope = () =>
    requestHeaders?.['x-forwarded-host'] ?? requestHeaders?.host ?? 'client'
  const loading = useState('categories-loading', () => false)
  const error = useState<string | null>('categories-error', () => null)
  const activeCategoryId = useState<string | null>(
    'categories-active-id',
    () => null
  )
  const currentCategory = useState<VerticalConfigFullDto | null>(
    'categories-current',
    () => null
  )

  /**
   * Fetch categories from the backend proxy
   */
  const fetchCategories = async (options?: {
    forceRefresh?: boolean
  }): Promise<VerticalConfigDto[]> => {
    const forceRefresh = options?.forceRefresh ?? false
    const cacheKey = `${cacheScope()}:list-all`
    const cachedCategories = forceRefresh
      ? undefined
      : readCache(categoriesListCache, cacheKey)

    if (cachedCategories) {
      categories.value = cachedCategories
      return categories.value
    }

    loading.value = true
    error.value = null

    try {
      const headers = buildRequestHeaders()
      const response = await $fetch<VerticalConfigDto[]>('/api/categories', {
        ...(headers ? { headers } : {}),
      })

      categories.value = response ?? []
      categoriesListCache.set(cacheKey, {
        data: categories.value,
        timestamp: Date.now(),
      })
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch categories'
      console.error('Error in fetchCategories:', err)
    } finally {
      loading.value = false
    }

    return categories.value
  }

  const findCategoryBySlug = (slugToFind: string) => {
    return categories.value.find(category => {
      const verticalSlug = category.verticalHomeUrl?.replace(/^\//, '') ?? ''
      return verticalSlug === slugToFind
    })
  }

  const fetchCategoryById = async (
    id: string,
    options?: { forceRefresh?: boolean }
  ): Promise<VerticalConfigFullDto> => {
    const forceRefresh = options?.forceRefresh ?? false
    const detailKey = `${cacheScope()}:${id}`
    const cachedDetail = forceRefresh
      ? undefined
      : readCache(categoryDetailCache, detailKey)

    if (cachedDetail) {
      return cachedDetail
    }

    loading.value = true

    try {
      const detailHeaders = buildRequestHeaders()
      const detail = await $fetch<VerticalConfigFullDto>(
        `/api/categories/${encodeURIComponent(id)}`,
        detailHeaders ? { headers: detailHeaders } : undefined
      )

      categoryDetailCache.set(detailKey, {
        data: detail,
        timestamp: Date.now(),
      })
      return detail
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to resolve category detail'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Select a category based on its slug and load its details
   * @param slug - Category slug to match against verticalHomeUrl
   */
  const selectCategoryBySlug = async (
    slug: string
  ): Promise<VerticalConfigFullDto> => {
    error.value = null

    if (categories.value.length === 0) {
      await fetchCategories()
    }

    let matchingCategory = findCategoryBySlug(slug)

    if (!matchingCategory) {
      await fetchCategories({ forceRefresh: true })
      matchingCategory = findCategoryBySlug(slug)
    }

    if (!matchingCategory?.id) {
      if (error.value) {
        const fetchError = new Error(error.value)
        fetchError.name = 'CategoryResolutionError'
        throw fetchError
      }

      activeCategoryId.value = null
      currentCategory.value = null
      const notFoundError = new Error('Category not found')
      notFoundError.name = 'CategoryNotFoundError'
      error.value = notFoundError.message
      throw notFoundError
    }

    const detail = await fetchCategoryById(matchingCategory.id)
    activeCategoryId.value = matchingCategory.id
    currentCategory.value = detail
    return detail
  }

  /**
   * Clear error state
   */
  const clearError = () => {
    error.value = null
  }

  /**
   * Reset the active category selection
   */
  const resetCategorySelection = () => {
    activeCategoryId.value = null
    currentCategory.value = null
  }

  return {
    // State (readonly)
    categories: readonly(categories),
    loading: readonly(loading),
    error: readonly(error),
    activeCategoryId: readonly(activeCategoryId),
    currentCategory: readonly(currentCategory),

    // Actions
    fetchCategories,
    fetchCategoryById,
    selectCategoryBySlug,
    clearError,
    resetCategorySelection,
  }
}

import type {
  VerticalConfigDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

/**
 * Composable for categories-related functionality
 */
export const useCategories = () => {
  const TWO_HOURS_MS = 2 * 60 * 60 * 1000

  type CacheEntry<T> = {
    data: T
    timestamp: number
  }

  // Reactive state
  const categories = useState<VerticalConfigDto[]>(
    'categories-list',
    () => [],
  )
  const categoriesListCache = useState<
    Record<string, CacheEntry<VerticalConfigDto[]>>
  >('categories-list-cache', () => ({}))
  const categoryDetailCache = useState<
    Record<string, CacheEntry<VerticalConfigFullDto>>
  >('category-detail-cache', () => ({}))
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
      {} as Record<string, string>,
    )

    return Object.keys(normalizedEntries).length ? normalizedEntries : undefined
  }
  const loading = useState(
    'categories-loading',
    () => false,
  )
  const error = useState<string | null>(
    'categories-error',
    () => null,
  )
  const activeCategoryId = useState<string | null>(
    'categories-active-id',
    () => null,
  )
  const currentCategory = useState<VerticalConfigFullDto | null>(
    'categories-current',
    () => null,
  )

  /**
   * Fetch categories from the backend proxy
   * @param onlyEnabled - Filter only enabled categories
   */
  const fetchCategories = async (
    onlyEnabled: boolean = true,
  ): Promise<VerticalConfigDto[]> => {
    const cacheKey = `list-${onlyEnabled}`
    const cachedCategories = categoriesListCache.value[cacheKey]

    if (cachedCategories && Date.now() - cachedCategories.timestamp < TWO_HOURS_MS) {
      categories.value = cachedCategories.data
      return categories.value
    }

    loading.value = true
    error.value = null

    try {
      const headers = buildRequestHeaders()
      const response = await $fetch<VerticalConfigDto[]>('/api/categories', {
        ...(headers ? { headers } : {}),
        params: { onlyEnabled },
      })

      categories.value = response ?? []
      categoriesListCache.value[cacheKey] = {
        data: categories.value,
        timestamp: Date.now(),
      }
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to fetch categories'
      console.error('Error in fetchCategories:', err)
    } finally {
      loading.value = false
    }

    return categories.value
  }

  /**
   * Select a category based on its slug and load its details
   * @param slug - Category slug to match against verticalHomeUrl
   */
  const selectCategoryBySlug = async (
    slug: string,
  ): Promise<VerticalConfigFullDto> => {
    error.value = null

    if (categories.value.length === 0) {
      await fetchCategories(true)

      if (!categories.value.length) {
        if (error.value) {
          const fetchError = new Error(error.value)
          fetchError.name = 'CategoryResolutionError'
          throw fetchError
        }

        const notFoundError = new Error('Category not found')
        notFoundError.name = 'CategoryNotFoundError'
        error.value = notFoundError.message
        activeCategoryId.value = null
        currentCategory.value = null
        throw notFoundError
      }
    }

    loading.value = true

    try {
      const matchingCategory = categories.value.find((category) => {
        const verticalSlug = category.verticalHomeUrl?.replace(/^\//, '') ?? ''
        return verticalSlug === slug
      })

      if (!matchingCategory?.id) {
        activeCategoryId.value = null
        currentCategory.value = null
        const notFoundError = new Error('Category not found')
        notFoundError.name = 'CategoryNotFoundError'
        throw notFoundError
      }

      activeCategoryId.value = matchingCategory.id

      const cachedDetail =
        categoryDetailCache.value[matchingCategory.id] ??
        categoryDetailCache.value[slug]

      if (cachedDetail && Date.now() - cachedDetail.timestamp < TWO_HOURS_MS) {
        currentCategory.value = cachedDetail.data
        return cachedDetail.data
      }

      const detailHeaders = buildRequestHeaders()
      const detail = await $fetch<VerticalConfigFullDto>(
        `/api/categories/${encodeURIComponent(matchingCategory.id)}`,
        detailHeaders ? { headers: detailHeaders } : undefined,
      )

      currentCategory.value = detail
      categoryDetailCache.value[matchingCategory.id] = {
        data: detail,
        timestamp: Date.now(),
      }
      categoryDetailCache.value[slug] = categoryDetailCache.value[matchingCategory.id]
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
    selectCategoryBySlug,
    clearError,
    resetCategorySelection,
  }
}

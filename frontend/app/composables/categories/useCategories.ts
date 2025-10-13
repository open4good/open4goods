import type { VerticalConfigDto, VerticalConfigFullDto } from '~~/shared/api-client'

/**
 * Composable for categories-related functionality
 */
export const useCategories = () => {
  // Reactive state
  const categories = useState<VerticalConfigDto[]>(
    'categories-list',
    () => [],
  )
  const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
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
    loading.value = true
    error.value = null

    try {
      const response = await $fetch<VerticalConfigDto[]>('/api/categories', {
        headers: requestHeaders,
        params: { onlyEnabled },
      })

      categories.value = response ?? []
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
        const fetchErrorMessage =
          error.value ?? 'No categories available to resolve the provided slug'
        const fetchError = new Error(fetchErrorMessage)
        fetchError.name = 'CategoryResolutionError'
        throw fetchError
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

      const detail = await $fetch<VerticalConfigFullDto>(
        `/api/categories/${encodeURIComponent(matchingCategory.id)}`,
        {
          headers: requestHeaders,
        },
      )

      currentCategory.value = detail
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

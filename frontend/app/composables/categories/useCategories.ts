import type { VerticalConfigDto } from '~~/shared/api-client'

/**
 * Composable for categories-related functionality
 */
export const useCategories = () => {
  // Reactive state
  const categories = useState<VerticalConfigDto[]>(
    'categories-list',
    () => [],
  )
  const loading = useState(
    'categories-loading',
    () => false,
  )
  const error = useState<string | null>(
    'categories-error',
    () => null,
  )

  /**
   * Fetch categories from the backend proxy
   * @param onlyEnabled - Filter only enabled categories
   */
  const fetchCategories = async (onlyEnabled: boolean = true) => {
    loading.value = true
    error.value = null

    try {
      const response = await $fetch<VerticalConfigDto[]>('/api/categories', {
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
  }

  /**
   * Clear error state
   */
  const clearError = () => {
    error.value = null
  }

  return {
    // State (readonly)
    categories: readonly(categories),
    loading: readonly(loading),
    error: readonly(error),

    // Actions
    fetchCategories,
    clearError,
  }
}

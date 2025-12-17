import { readonly } from 'vue'
import type { CategoryNavigationDto } from '~~/shared/api-client'

export const useCategoryNavigation = () => {
  const navigation = useState<CategoryNavigationDto | null>(
    'category-navigation',
    () => null
  )
  const loading = useState('category-navigation-loading', () => false)
  const error = useState<string | null>('category-navigation-error', () => null)
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

  const fetchNavigation = async (params?: {
    googleCategoryId?: number
    path?: string
  }): Promise<CategoryNavigationDto | null> => {
    loading.value = true
    error.value = null

    try {
      const headers = buildRequestHeaders()
      const response = await $fetch<CategoryNavigationDto>(
        '/api/categories/navigation',
        {
          ...(headers ? { headers } : {}),
          params,
        }
      )

      navigation.value = response ?? null
    } catch (err) {
      error.value =
        err instanceof Error
          ? err.message
          : 'Failed to fetch category navigation'
      navigation.value = null
      console.error('Error in fetchCategoryNavigation:', err)
    } finally {
      loading.value = false
    }

    return navigation.value
  }

  return {
    navigation: readonly(navigation),
    loading: readonly(loading),
    error: readonly(error),
    fetchNavigation,
  }
}

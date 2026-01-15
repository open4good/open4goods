import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  CategorySuggestionItem,
  ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import { useCategories } from '~/composables/categories/useCategories'
import {
  normalizeLocale,
  resolveLocalizedRoutePath,
} from '~~/shared/utils/localized-routes'

export interface UseMenuSearchControlsOptions {
  /**
   * Optional callback triggered whenever navigation occurs as a result of search interactions.
   * Useful for closing drawers or menus after search submits or suggestion clicks.
   */
  onNavigate?: () => void
  /**
   * When false the composable will not close the search input automatically after navigation.
   * Defaults to true to preserve the desktop menu behaviour.
   */
  closeSearchOnNavigate?: boolean
}

export const MIN_SEARCH_QUERY_LENGTH = 2

export const useMenuSearchControls = (
  options: UseMenuSearchControlsOptions = {}
) => {
  const route = useRoute()
  const router = useRouter()
  const { locale } = useI18n()
  const { categories, fetchCategories } = useCategories()

  const localePath = useLocalePath()

  const searchQuery = ref('')
  const isSearchOpen = ref(false)

  const currentLocale = computed(() => normalizeLocale(locale.value))
  const homeRoutePath = computed(() =>
    resolveLocalizedRoutePath('index', currentLocale.value)
  )
  const searchRoutePath = computed(() =>
    resolveLocalizedRoutePath('search', currentLocale.value)
  )
  const showMenuSearch = computed(() => route.path !== homeRoutePath.value)

  const openSearch = () => {
    isSearchOpen.value = true
  }

  const closeSearch = () => {
    isSearchOpen.value = false
  }

  const handleSearchClear = () => {
    searchQuery.value = ''
  }

  watch(
    () => route.path,
    path => {
      if (path === homeRoutePath.value) {
        closeSearch()
        handleSearchClear()
      }
    }
  )

  watch(homeRoutePath, path => {
    if (route.path === path) {
      closeSearch()
      handleSearchClear()
    }
  })

  watch(showMenuSearch, visible => {
    if (!visible) {
      closeSearch()
      handleSearchClear()
    }
  })

  const finalizeNavigation = () => {
    if (options.closeSearchOnNavigate !== false) {
      closeSearch()
    }

    options.onNavigate?.()
  }

  const navigateToSearch = (query?: string) => {
    const normalizedQuery = query?.trim() ?? ''

    router.push({
      path: searchRoutePath.value,
      query: normalizedQuery ? { q: normalizedQuery } : undefined,
    })

    finalizeNavigation()
  }

  const normalizeSuggestionUrl = (
    raw: string | null | undefined
  ): string | null => {
    if (!raw) {
      return null
    }

    const trimmed = raw.trim()

    if (!trimmed) {
      return null
    }

    if (/^https?:\/\//iu.test(trimmed)) {
      return trimmed
    }

    return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  }

  const resolveCategoryUrl = async (
    suggestion: CategorySuggestionItem
  ): Promise<string | null> => {
    const normalizedUrl = normalizeSuggestionUrl(suggestion.url)

    if (normalizedUrl) {
      return normalizedUrl
    }

    const verticalId = suggestion.verticalId?.trim()

    if (!verticalId) {
      return null
    }

    if (!categories.value.length) {
      await fetchCategories()
    }

    const matchedCategory = categories.value.find(
      category => category.id === verticalId
    )

    return normalizeSuggestionUrl(matchedCategory?.verticalHomeUrl)
  }

  const handleCategorySuggestion = async (
    suggestion: CategorySuggestionItem
  ) => {
    searchQuery.value = suggestion.title

    const normalizedUrl = await resolveCategoryUrl(suggestion)

    if (normalizedUrl) {
      if (
        /^https?:\/\//iu.test(normalizedUrl) &&
        typeof window !== 'undefined'
      ) {
        window.open(normalizedUrl, '_blank', 'noopener,noreferrer')
        finalizeNavigation()
        return
      }

      router.push(normalizedUrl)
      finalizeNavigation()
      return
    }

    navigateToSearch(suggestion.title)
  }

  const handleProductSuggestion = (suggestion: ProductSuggestionItem) => {
    searchQuery.value = suggestion.title

    const gtin = suggestion.gtin?.trim()

    if (gtin) {
      router.push(localePath(`/${gtin}`))
      finalizeNavigation()
      return
    }

    navigateToSearch(suggestion.title)
  }

  const handleSearchSubmit = () => {
    const trimmedQuery = searchQuery.value.trim()

    if (
      trimmedQuery.length > 0 &&
      trimmedQuery.length < MIN_SEARCH_QUERY_LENGTH
    ) {
      return
    }

    navigateToSearch(trimmedQuery)
  }

  return {
    searchQuery,
    isSearchOpen,
    showMenuSearch,
    homeRoutePath,
    searchRoutePath,
    openSearch,
    closeSearch,
    handleSearchClear,
    handleSearchSubmit,
    handleCategorySuggestion,
    handleProductSuggestion,
    navigateToSearch,
  }
}

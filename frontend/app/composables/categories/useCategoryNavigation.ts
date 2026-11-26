import { readonly } from 'vue'
import type {
  CategoryNavigationDto,
  GoogleCategoryDto,
  VerticalConfigDto,
} from '~~/shared/api-client'

const MENU_VERTICAL_FIELDS = [
  'id',
  'order',
  'verticalHomeTitle',
  'verticalHomeUrl',
  'imageSmall',
  'imageMedium',
  'imageLarge',
  'mdiIcon',
] as const satisfies readonly (keyof VerticalConfigDto)[]

const slimVertical = (vertical?: VerticalConfigDto): VerticalConfigDto =>
  Object.fromEntries(
    MENU_VERTICAL_FIELDS.filter(key => vertical?.[key] !== undefined).map(
      key => [key, vertical?.[key]]
    )
  ) as VerticalConfigDto

const slimCategory = (category: GoogleCategoryDto): GoogleCategoryDto => ({
  ...category,
  ...(category.vertical ? { vertical: slimVertical(category.vertical) } : {}),
  ...(category.children
    ? { children: category.children.map(slimCategory) }
    : {}),
})

/**
 * Keep only what the menu renders. The state is serialized in `__NUXT_DATA__`
 * of every page: full vertical configs and the product lists weighed 160 KB.
 */
const toMenuNavigation = (
  navigation: CategoryNavigationDto
): CategoryNavigationDto => {
  const {
    topNewProducts: _topNewProducts,
    topOccasionProducts: _topOccasionProducts,
    ...menu
  } = navigation
  return {
    ...menu,
    ...(menu.childCategories
      ? { childCategories: menu.childCategories.map(slimCategory) }
      : {}),
    ...(menu.descendantVerticals
      ? { descendantVerticals: menu.descendantVerticals.map(slimCategory) }
      : {}),
    ...(menu.popularCategories
      ? { popularCategories: menu.popularCategories.map(slimCategory) }
      : {}),
  }
}

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

      navigation.value = response ? toMenuNavigation(response) : null
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

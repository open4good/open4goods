<template>
  <div class="search-page">
    <PageHeader
      :eyebrow="t('search.hero.eyebrow')"
      :title="t('search.hero.title')"
      :description-html="t('search.hero.subtitle')"
      layout="single-column"
      container="lg"
      background="image"
      background-image-asset-key="searchBackground"
      class="mb-8"
    >
      <form class="search-hero__form mt-6" @submit.prevent="handleSearchSubmit">
        <SearchSuggestField
          v-model="searchInput"
          class="search-hero__field"
          :label="t('search.form.label')"
          :placeholder="t('search.form.placeholder')"
          :aria-label="t('search.form.ariaLabel')"
          :min-chars="MIN_QUERY_LENGTH"
          :enable-suggest="false"
          :enable-voice="true"
          :voice-mobile="true"
          :voice-desktop="true"
          @clear="handleClear"
          @select-category="handleCategorySuggestion"
          @select-product="handleProductSuggestion"
          @submit="handleSearchSubmit"
        >
          <template #append-inner>
            <v-btn
              class="search-hero__submit-icon"
              icon="mdi-arrow-right"
              variant="flat"
              color="primary"
              size="small"
              type="submit"
              :aria-label="t('search.form.submit')"
            />
          </template>
        </SearchSuggestField>
      </form>

      <p
        v-if="showInitialState"
        class="search-hero__helper mt-4 text-medium-emphasis"
      >
        {{ t('search.states.initial') }}
      </p>
      <p
        v-else-if="showMinimumNotice"
        class="search-hero__helper search-hero__helper--warning mt-4 text-warning"
      >
        {{ t('search.states.minimum', { min: MIN_QUERY_LENGTH }) }}
      </p>
    </PageHeader>



    <v-navigation-drawer
      v-model="filtersOpen"
      location="right"
      width="320"
      temporary
      class="search-page__filters-drawer"
    >
      <div class="pa-4 d-flex align-center justify-space-between">
        <h3 class="text-h6 font-weight-bold mb-0">
          {{ t('category.filters.title') }}
        </h3>
        <v-btn
          icon="mdi-close"
          variant="text"
          density="comfortable"
          @click="filtersOpen = false"
        />
      </div>

      <v-divider />

      <div class="pa-4">
        <CategoryFilterList
          :fields="filterFields"
          :aggregations="productAggregations"
          :active-filters="activeFilters"
          @update-range="updateRangeFilter"
          @update-terms="updateTermsFilter"
        />
      </div>
    </v-navigation-drawer>

    <v-progress-linear
      v-if="pending || productsPending"
      class="search-page__loader"
      indeterminate
      color="primary"
      :aria-label="t('search.states.loadingAria')"
      role="progressbar"
    />

    <v-container
      v-if="hasMinimumLength"
      class="search-page__results py-10 px-4 mx-auto"
      max-width="xl"
    >
      <div class="d-flex justify-end align-center mb-4 gap-4">
        <v-select
          v-if="isFiltered"
          v-model="sortOption"
          :items="sortOptions"
          item-title="label"
          item-value="value"
          density="compact"
          variant="outlined"
          hide-details
          class="search-page__sort-select"
          style="max-width: 200px"
          prepend-inner-icon="mdi-sort"
        />
        <v-btn
          v-if="!showInitialState && !error"
          prepend-icon="mdi-filter-variant"
          variant="tonal"
          color="primary"
          @click="filtersOpen = true"
        >
          {{ t('category.filters.title') }}
          <v-badge
            v-if="activeFilters.length"
            :content="activeFilters.length"
            color="primary"
            inline
            class="ms-2"
          />
        </v-btn>
      </div>

      <v-alert
        v-if="error || productsError"
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-6"
        role="alert"
      >
        <div class="search-page__alert-content">
          <p class="search-page__alert-title">
            {{ t('search.states.error.title') }}
          </p>
          <p class="search-page__alert-description">
            {{ t('search.states.error.description') }}
          </p>
        </div>
        <template #append>
          <v-btn
            color="primary"
            variant="text"
            @click="isFiltered ? refreshProducts() : refresh()"
          >
            {{ t('common.actions.retry') }}
          </v-btn>
        </template>
      </v-alert>

      <template v-else>
        <!-- Filtered Results (Product Search) -->
        <div v-if="isFiltered" class="search-page__filtered-results">
           <div
            v-if="!productResults.length && !productsPending"
            class="search-page__empty"
          >
             <h2 class="search-page__empty-title">
              {{ t('search.states.empty.title', { query: normalizedQuery }) }}
            </h2>
            <p class="search-page__empty-description">
              {{ t('search.states.empty.description') }}
            </p>
             <v-btn
              variant="text"
              color="primary"
              class="mt-4"
              @click="clearFilters"
            >
              {{ t('category.filters.reset') }}
            </v-btn>
          </div>

          <CategoryProductCardGrid
            v-else
            :products="productResults"
            size="medium"
          />
        </div>

        <!-- Default Global Search Results (Groups) -->
        <template v-else>
          <div
            v-if="!displayGroups.length && !pending"
            class="search-page__empty"
          >
            <h2 class="search-page__empty-title">
              {{ t('search.states.empty.title', { query: normalizedQuery }) }}
            </h2>
            <p class="search-page__empty-description">
              {{ t('search.states.empty.description') }}
            </p>
          </div>

          <div v-else class="search-page__group-wrapper">
            <p class="search-page__summary">
              {{ resultsSummaryLabel }}
            </p>
            <p v-if="usingFallback" class="search-page__fallback">
              {{ t('search.notice.fallback') }}
            </p>
            <div v-if="activeSearchModeLabel" class="search-page__mode-row">
              <p class="search-page__mode">
                {{
                  t('search.results.modeLabel', {
                    mode: activeSearchModeLabel,
                  })
                }}
              </p>
              <v-btn
                v-if="nextSearchModeLabel"
                variant="tonal"
                color="primary"
                size="small"
                @click="handleNextSearchMode"
              >
                {{
                  t('search.results.nextMode', {
                    mode: nextSearchModeLabel,
                  })
                }}
              </v-btn>
            </div>

            <SearchResultGroup
              v-for="group in displayGroups"
              :key="group.key"
              :title="group.title"
              :count-label="group.countLabel"
              :products="group.products"
              :popular-attributes="group.popularAttributes"
              :vertical-home-url="group.verticalHomeUrl"
              :search-mode-label="group.searchModeLabel"
              :category-link-label="t('search.groups.viewCategory')"
              :category-link-aria="
                t('search.groups.viewCategoryAria', { title: group.title })
              "
            />
          </div>
        </template>
      </template>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AttributeConfigDto,
  GlobalSearchResponseDto,
  ProductDto,
  SearchMode,
  VerticalConfigDto,
  ProductSearchResponseDto,
  ProductSearchRequestDto,
  FilterRequestDto,
  FieldMetadataDto,
  AggregationResponseDto,
  SortDto,
} from '~~/shared/api-client'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import PageHeader from '~/components/shared/header/PageHeader.vue'
import SearchResultGroup from '~/components/search/SearchResultGroup.vue'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import CategoryFilterList from '~/components/category/filters/CategoryFilterList.vue'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'
import { useAnalytics } from '~/composables/useAnalytics'

const MIN_QUERY_LENGTH = 2

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const { trackSearch } = useAnalytics()
const route = useRoute()
const router = useRouter()
const localePath = useLocalePath()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const routeQuery = computed(() =>
  typeof route.query.q === 'string' ? route.query.q : ''
)
const searchInput = ref(routeQuery.value)
const requestedSearchType = ref<
  'auto' | 'exact_vertical' | 'global' | 'semantic'
>('auto')

const filtersOpen = ref(false)
const filterRequest = ref<FilterRequestDto>({ filters: [], filterGroups: [] })
const sortOption = ref<string>('impact')

const sortOptions = computed(() => [
  { label: t('category.sorting.score'), value: 'impact' },
  { label: t('category.sorting.priceAsc'), value: 'price_asc' },
  { label: t('category.sorting.priceDesc'), value: 'price_desc' },
])

watch(
  routeQuery,
  value => {
    searchInput.value = value
    requestedSearchType.value = 'auto'
    // Do NOT reset filters automatically to allow refining search with filters kept?
    // User requirement: "taillés sur les resultats retournés". Usually implies new search -> new context.
    // But if I type "iPhone" and filter price, then change to "Samsung", I might want to keep price filter.
    // For now, let's keep filters.
  },
  { immediate: true }
)

const normalizedQuery = computed(() => routeQuery.value.trim())
const trimmedInput = computed(() => searchInput.value.trim())
const hasMinimumLength = computed(
  () => normalizedQuery.value.length >= MIN_QUERY_LENGTH
)
const showInitialState = computed(
  () => trimmedInput.value.length === 0 && normalizedQuery.value.length === 0
)
const showMinimumNotice = computed(
  () =>
    trimmedInput.value.length > 0 &&
    trimmedInput.value.length < MIN_QUERY_LENGTH
)

// Global Search Data
const { data, pending, error, refresh } =
  await useAsyncData<GlobalSearchResponseDto | null>(
    'global-search',
    async () => {
      // If filtered, we don't need global search, but we might want to keep it cached?
      // Actually, if we switch mode, we strictly use the other endpoint.
      if (!hasMinimumLength.value || isFiltered.value) {
        return null
      }

      return await $fetch<GlobalSearchResponseDto>('/api/products/search', {
        method: 'POST',
        headers: requestHeaders,
        body: {
          query: normalizedQuery.value,
          searchType: requestedSearchType.value,
        },
      })
    },
    {
      watch: [() => normalizedQuery.value, () => requestedSearchType.value, () => isFiltered.value],
      immediate: hasMinimumLength.value,
    }
  )

// Filtered Product Search Data
const activeFilters = computed(() => filterRequest.value.filters ?? [])
const isFiltered = computed(() => activeFilters.value.length > 0)

const manualFields: FieldMetadataDto[] = [
  {
    mapping: 'price.minPrice.price',
    title: 'Price', // Will be localized by resolveFilterFieldTitle if keys match
    valueType: 'numeric',
  },
  {
    mapping: 'scores.ECOSCORE.value',
    title: 'Eco-score',
    valueType: 'numeric',
  },
  {
    mapping: 'price.conditions', // backend mapping for condition
    title: 'Condition',
    valueType: 'keyword',
  },
]

const filterFields = computed(() => manualFields)

const currentSort = computed<SortDto[]>(() => {
    switch(sortOption.value) {
        case 'price_asc':
            return [{ field: 'price.minPrice.price', order: 'asc' }]
        case 'price_desc':
            return [{ field: 'price.minPrice.price', order: 'desc' }]
        case 'impact':
        default:
            return [{ field: 'scores.ECOSCORE.value', order: 'desc' }] // Default Sort: Impact Score DESC
    }
})

const requestBody = computed<ProductSearchRequestDto>(() => ({
  filters: filterRequest.value,
  sort: {
      sorts: currentSort.value
  },
  aggs: {
      aggs: [
          { name: 'price', field: 'price.minPrice.price', type: 'range' },
          { name: 'ecoscore', field: 'scores.ECOSCORE.value', type: 'range' },
          { name: 'condition', field: 'price.conditions', type: 'terms' }
      ]
  }
}))

const {
  data: productSearchData,
  pending: productsPending,
  error: productsError,
  refresh: refreshProducts,
} = await useAsyncData<ProductSearchResponseDto | null>(
  'product-search-filtered',
  async () => {
    // Only fetch if has query AND (filters are open/active OR we want to prefetch? No, lazy load)
    // To support "taillés sur les resultats", we need to fetch aggregations EVEN IF no filter is applied
    // IF the user opens the filter drawer.
    // OR if filters are active.
    if (!hasMinimumLength.value) return null

    // If global search is active and no filters, skip unless drawer is open?
    // Let's simplified: If isFiltered is true, we fetch.
    // If IS NOT filtered, we rely on Global Search.
    // BUT we need aggregations to show in the drawer.
    // So if drawer is OPEN, we need to fetch aggregations.
    if (!isFiltered.value && !filtersOpen.value) return null

    return await $fetch<ProductSearchResponseDto>('/api/products', {
      method: 'POST',
      headers: requestHeaders,
      params: {
        query: normalizedQuery.value,
        domainLanguage: locale.value,
      },
      body: requestBody.value,
    })
  },
  {
    watch: [
        () => normalizedQuery.value,
        () => filterRequest.value,
        () => filtersOpen.value // Fetch when drawer opens to get aggs
    ],
    immediate: false, // Wait for interaction
  }
)

const productResults = computed(() => productSearchData.value?.products?.content ?? [])
const productAggregations = computed<Record<string, AggregationResponseDto>>(() => {
    const aggs = productSearchData.value?.aggregations ?? []
    return aggs.reduce((acc, curr) => {
        if(curr.field) acc[curr.field] = curr
        return acc
    }, {} as Record<string, AggregationResponseDto>)
})

const updateRangeFilter = (field: string, payload: { min?: number; max?: number }) => {
  const current = activeFilters.value.filter(f => f.field !== field)
  if (payload.min == null && payload.max == null) {
      filterRequest.value = { ...filterRequest.value, filters: current }
      return
  }
  filterRequest.value = {
      ...filterRequest.value,
      filters: [...current, { field, operator: 'range', min: payload.min, max: payload.max }]
  }
}

const updateTermsFilter = (field: string, terms: string[]) => {
  const current = activeFilters.value.filter(f => f.field !== field)
  if (!terms.length) {
      filterRequest.value = { ...filterRequest.value, filters: current }
      return
  }
  filterRequest.value = {
      ...filterRequest.value,
      filters: [...current, { field, operator: 'term', terms }]
  }
}

const clearFilters = () => {
    filterRequest.value = { filters: [], filterGroups: [] }
    filtersOpen.value = false
}

// ... Rest of existing code ...

const { data: verticalsData } = await useAsyncData<VerticalConfigDto[]>(
  'search-verticals',
  () =>
    $fetch<VerticalConfigDto[]>('/api/categories', {
      headers: requestHeaders,
      params: { onlyEnabled: true },
    })
)

const verticals = computed(() => verticalsData.value ?? [])
const verticalById = computed(() => {
  const entries = new Map<string, VerticalConfigDto>()

  verticals.value.forEach(vertical => {
    if (vertical.id) {
      entries.set(vertical.id, vertical)
    }
  })

  return entries
})

interface SearchGroup {
  key: string
  title: string
  countLabel: string | null
  products: ProductDto[]
  popularAttributes: AttributeConfigDto[]
  verticalHomeUrl: string | null
  searchModeLabel: string | null
}

const normalizeVerticalHomeUrl = (
  raw: string | null | undefined
): string | null => {
  if (!raw) {
    return null
  }

  const trimmed = raw.trim()

  if (!trimmed) {
    return null
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

const buildGroupCountLabel = (count: number) =>
  translatePlural('search.groups.count', count, { count })

const extractProducts = (
  results: { product?: ProductDto | null }[] | undefined
): ProductDto[] =>
  (results ?? [])
    .map(entry => entry.product)
    .filter((product): product is ProductDto => Boolean(product))

const primaryGroups = computed(() => {
  const groups = data.value?.verticalGroups ?? []

  return groups
    .map((group, index) => {
      const products = extractProducts(group.results)
      const searchModeLabel = formatSearchModeLabel(group.searchMode)
      const verticalId = group.verticalId ?? null
      const vertical = verticalId
        ? (verticalById.value.get(verticalId) ?? null)
        : null

      if (!products.length) {
        return null
      }

      const title =
        vertical?.verticalHomeTitle ??
        (verticalId
          ? formatFallbackVerticalTitle(verticalId)
          : t('search.groups.unknownTitle'))

      const countLabel = buildGroupCountLabel(products.length)
      const verticalHomeUrl = normalizeVerticalHomeUrl(
        vertical?.verticalHomeUrl
      )

      return {
        key: `primary-${verticalId ?? index}`,
        title,
        countLabel,
        products,
        popularAttributes: vertical?.popularAttributes ?? [],
        verticalHomeUrl,
        searchModeLabel,
      } satisfies SearchGroup | null
    })
    .filter((group): group is SearchGroup => Boolean(group))
})

const fallbackGroups = computed(() => {
  const grouped = new Map<
    string | null,
    { products: ProductDto[]; searchModeLabel: string | null }
  >()

  for (const entry of data.value?.fallbackResults ?? []) {
    if (!entry?.product) {
      continue
    }

    const verticalId = entry.product.base?.vertical ?? null

    if (!grouped.has(verticalId)) {
      grouped.set(verticalId, {
        products: [],
        searchModeLabel: formatSearchModeLabel(entry.searchMode),
      })
    }

    grouped.get(verticalId)?.products.push(entry.product)
  }

  return Array.from(grouped.entries())
    .map(([verticalId, groupData], index) => {
      if (!groupData.products.length) {
        return null
      }

      const vertical = verticalId
        ? (verticalById.value.get(verticalId) ?? null)
        : null
      const title =
        vertical?.verticalHomeTitle ??
        (verticalId
          ? formatFallbackVerticalTitle(verticalId)
          : t('search.groups.unknownTitle'))
      const countLabel = buildGroupCountLabel(groupData.products.length)
      const verticalHomeUrl = normalizeVerticalHomeUrl(
        vertical?.verticalHomeUrl
      )

      return {
        key: `fallback-${verticalId ?? index}`,
        title,
        countLabel,
        products: groupData.products,
        popularAttributes: vertical?.popularAttributes ?? [],
        verticalHomeUrl,
        searchModeLabel: groupData.searchModeLabel,
      } satisfies SearchGroup | null
    })
    .filter((group): group is SearchGroup => Boolean(group))
    .sort((a, b) => b.products.length - a.products.length)
})

const usingFallback = computed(
  () => !primaryGroups.value.length && fallbackGroups.value.length > 0
)
const displayGroups = computed(() =>
  primaryGroups.value.length ? primaryGroups.value : fallbackGroups.value
)

const totalResults = computed(() =>
  displayGroups.value.reduce((sum, group) => sum + group.products.length, 0)
)

const resultsCountLabel = computed(() =>
  translatePlural('search.results.count', totalResults.value, {
    count: totalResults.value,
  })
)

const resultsSummaryLabel = computed(() =>
  t('search.results.summary', {
    countLabel: resultsCountLabel.value,
    query: normalizedQuery.value,
  })
)

const activeSearchMode = computed<SearchMode | null>(() => {
  const primaryMode = data.value?.verticalGroups?.[0]?.searchMode ?? null
  const fallbackMode = data.value?.fallbackResults?.[0]?.searchMode ?? null

  return primaryMode ?? fallbackMode
})

const activeSearchModeLabel = computed(() =>
  formatSearchModeLabel(activeSearchMode.value)
)

const nextSearchMode = computed(() => {
  switch (activeSearchMode.value) {
    case 'exact_vertical':
      return 'global'
    case 'global':
      return 'semantic'
    default:
      return null
  }
})

const nextSearchModeLabel = computed(() =>
  formatSearchModeLabel(nextSearchMode.value)
)

const handleSearchSubmit = () => {
  const value = searchInput.value.trim()

  if (value.length > 0 && value.length < MIN_QUERY_LENGTH) {
    return
  }

  trackSearch({ query: value, source: 'form' })

  // Clear filters on new search?
  clearFilters()

  router.push({
    path: route.path,
    query: value ? { q: value } : {},
  })
}

const handleNextSearchMode = () => {
  if (!nextSearchMode.value) {
    return
  }

  requestedSearchType.value = nextSearchMode.value
}

const handleClear = () => {
  searchInput.value = ''
  router.replace({
    path: route.path,
    query: {},
  })
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  const verticalUrl = normalizeVerticalHomeUrl(
    suggestion.url ??
      (suggestion.verticalId
        ? verticalById.value.get(suggestion.verticalId)?.verticalHomeUrl
        : null)
  )

  if (!verticalUrl) {
    return
  }

  trackSearch({ query: suggestion.title, source: 'suggestion' })

  router.push(verticalUrl)
}

const handleProductSuggestion = (suggestion: ProductSuggestionItem) => {
  const gtin = suggestion.gtin?.trim()

  if (!gtin) {
    return
  }

  trackSearch({ query: suggestion.title ?? gtin, source: 'suggestion' })

  router.push(
    localePath({
      name: 'gtin',
      params: { gtin },
    })
  )
}

const canonicalUrl = computed(() =>
  new URL(localePath({ name: 'search' }), requestURL.origin).toString()
)
const siteName = computed(() => String(t('siteIdentity.siteName')))
const ogLocale = computed(() => locale.value.replace('-', '_'))
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)
const ogImageAlt = computed(() => String(t('search.seo.imageAlt')))
const alternateLinks = computed(() =>
  availableLocales.map(availableLocale => ({
    rel: 'alternate' as const,
    hreflang: availableLocale,
    href: new URL(
      localePath({ name: 'search' }, availableLocale),
      requestURL.origin
    ).toString(),
  }))
)

useSeoMeta({
  title: () => String(t('search.seo.title')),
  description: () => String(t('search.seo.description')),
  ogTitle: () => String(t('search.seo.title')),
  ogDescription: () => String(t('search.seo.description')),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => ogImageAlt.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...alternateLinks.value,
  ],
}))

function formatFallbackVerticalTitle(verticalId: string): string {
  return verticalId
    .split(/[-_]/u)
    .filter(Boolean)
    .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}

function formatSearchModeLabel(mode: SearchMode | string | null | undefined) {
  if (!mode) {
    return null
  }

  return t(`search.modes.${mode}`)
}
</script>

<style scoped lang="sass">
.search-page
  display: flex
  flex-direction: column
  gap: 0

  &__loader
    margin-top: -1px

  &__results
    max-width: 1200px
    margin: 0 auto

  &__filters-drawer
     border-left: 1px solid rgba(var(--v-theme-border-primary), 0.12)
     background-color: rgb(var(--v-theme-surface-default))

     :deep(.v-navigation-drawer__content)
        background-color: rgb(var(--v-theme-surface-default))

  &__filtered-results
     display: flex
     flex-direction: column
     gap: 1.5rem

  &__alert-content
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__alert-title
    margin: 0
    font-weight: 600
    font-size: 1.1rem

  &__alert-description
    margin: 0

  &__empty
    text-align: center
    padding: 3rem 1rem
    background: rgba(var(--v-theme-surface-muted), 0.6)
    border-radius: 1.5rem

  &__empty-title
    margin: 0
    font-size: clamp(1.5rem, 1.2rem + 0.8vw, 2rem)
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__empty-description
    margin: 1rem auto 0
    max-width: 28rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__group-wrapper
    display: flex
    flex-direction: column
    gap: 2rem

  &__summary
    margin: 0
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__fallback
    margin: 0
    color: rgb(var(--v-theme-accent-supporting))
    font-weight: 500

  &__mode-row
    display: flex
    flex-direction: column
    gap: 0.75rem

    @media (min-width: 640px)
      flex-direction: row
      align-items: center
      justify-content: space-between

  &__mode
    margin: 0
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__group-wrapper :deep(.search-result-group:first-of-type)
    padding-top: 0

  &__group-wrapper :deep(.search-result-group:last-of-type)
    padding-bottom: 0
    border-bottom: none

.search-hero
  position: relative
  width: 100%
  background: linear-gradient(
    135deg,
    rgb(var(--v-theme-hero-gradient-start)) 0%,
    rgba(var(--v-theme-hero-gradient-mid), 0.92) 55%,
    rgb(var(--v-theme-hero-gradient-end)) 100%
  )
  color: rgb(255, 255, 255)
  padding-block: clamp(1.75rem, 4vw, 3rem)
  margin-bottom: clamp(1.5rem, 4vw, 2.75rem)
  overflow: hidden
  box-shadow: 0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.45)

  &::after
    content: ''
    position: absolute
    inset: 0
    background: radial-gradient(circle at 20% 20%, rgba(var(--v-theme-hero-overlay-strong), 0.2), transparent 60%)
    pointer-events: none

  &__container
    position: relative
    z-index: 1
    padding-block: clamp(1rem, 3vw, 1.75rem)

  &__content
    display: flex
    flex-direction: column
    gap: 0.85rem
    max-width: min(52rem, 100%)

  &__eyebrow
    display: inline-flex
    align-items: center
    align-self: flex-start
    padding: 0.4rem 0.9rem
    border-radius: 999px
    background-color: rgba(var(--v-theme-hero-pill-on-dark), 0.16)
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.75rem
    line-height: 1.1

  &__copy
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__title
    font-weight: 700
    font-size: clamp(1.95rem, 4.5vw, 2.65rem)
    line-height: 1.2
    margin: 0

  &__subtitle
    margin: 0
    --subtitle-size: clamp(1rem, 2.2vw, 1.2rem)
    --subtitle-color: rgba(var(--v-theme-hero-overlay-soft), 0.85)
    line-height: 1.55

  &__form
    display: flex
    flex-direction: column
    gap: 0.75rem
    margin-top: 1rem
    width: 100%
    max-width: 56rem
    margin-inline: auto

    @media (min-width: 640px)
      flex-direction: row
      align-items: center
      gap: 1rem

  &__field
    flex: 1 1 auto

  &__submit
    align-self: stretch
    text-transform: none
    font-weight: 600
    letter-spacing: 0
    border-radius: 999px

    @media (min-width: 640px)
      align-self: center
      min-width: 8rem

    :deep(.v-btn)
      background-color: rgba(var(--v-theme-hero-overlay-soft), 0.18)
      color: rgb(var(--v-theme-hero-overlay-soft))
      border-radius: 999px
      padding-inline: 1.75rem
      backdrop-filter: blur(6px)
      transition: background-color 0.2s ease, transform 0.2s ease

      &:hover
        background-color: rgba(var(--v-theme-hero-overlay-soft), 0.28)
        transform: translateY(-1px)

      &:focus-visible
        box-shadow: 0 0 0 3px rgba(var(--v-theme-hero-overlay-soft), 0.35)

  &__submit-icon
    border-radius: 50% !important

  &__helper
    margin: 0
    margin-top: 0.75rem
    font-size: 0.95rem
    color: rgba(var(--v-theme-hero-overlay-soft), 0.82)

    &--warning
      color: rgba(var(--v-theme-hero-overlay-soft), 0.95)
</style>

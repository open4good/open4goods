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

    <v-container
      v-if="shouldShowResults"
      class="search-page__actions py-2 px-4 mx-auto d-flex align-center justify-center"
      max-width="xl"
    >
      <v-btn
        v-if="!mdAndUp"
        variant="tonal"
        color="primary"
        prepend-icon="mdi-filter-variant"
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

      <CategoryFilterList
        v-else
        :fields="filterFields"
        :aggregations="productAggregations"
        :baseline-aggregations="baselineAggregations"
        :active-filters="activeFilters"
        mode="row"
        @update-range="updateRangeFilter"
        @update-terms="updateTermsFilter"
      />
    </v-container>

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
          :baseline-aggregations="baselineAggregations"
          :active-filters="activeFilters"
          @update-range="updateRangeFilter"
          @update-terms="updateTermsFilter"
        />
      </div>
    </v-navigation-drawer>

    <v-progress-linear
      v-if="pending || productsPending || baselinePending"
      class="search-page__loader"
      indeterminate
      color="primary"
      :aria-label="t('search.states.loadingAria')"
      role="progressbar"
    />

    <v-container
      v-if="shouldShowResults"
      class="search-page__results py-10 px-4 mx-auto"
      max-width="xl"
    >
      <v-alert
        v-if="error || productsError || baselineError"
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
            @click="
              showLatestProducts
                ? refreshBaseline()
                : isFiltered
                  ? refreshProducts()
                  : refresh()
            "
          >
            {{ t('common.actions.retry') }}
          </v-btn>
        </template>
      </v-alert>

      <template v-else>
        <v-row class="search-page__layout" align="start" justify="center">
          <v-col cols="12" lg="5">
            <section class="search-page__column">
              <div class="search-page__column-header">
                <h2 class="text-h5 font-weight-bold mb-0">
                  {{ t('search.columns.verticals.title') }}
                </h2>
              </div>

              <div
                v-if="!limitedGroups.length && !pending"
                class="search-page__empty"
              >
                <h3 class="search-page__empty-title">
                  {{
                    t('search.columns.verticals.emptyTitle', {
                      query: normalizedQuery,
                    })
                  }}
                </h3>
                <p class="search-page__empty-description">
                  {{ t('search.columns.verticals.emptyDescription') }}
                </p>
              </div>

              <v-expansion-panels
                v-else
                v-model="openPanels"
                multiple
                class="search-page__panels"
              >
                <v-expansion-panel
                  v-for="group in limitedGroups"
                  :key="group.key"
                >
                  <v-expansion-panel-title>
                    <div class="search-page__panel-title">
                      <span>{{ group.title }}</span>
                      <span
                        v-if="group.countLabel"
                        class="search-page__panel-count"
                      >
                        {{ group.countLabel }}
                      </span>
                    </div>
                  </v-expansion-panel-title>
                  <v-expansion-panel-text>
                    <div class="search-page__panel-meta">
                      <v-btn
                        v-if="group.verticalHomeUrl"
                        :to="group.verticalHomeUrl"
                        variant="text"
                        color="primary"
                        class="px-0"
                        :aria-label="
                          t('search.groups.viewCategoryAria', {
                            title: group.title,
                          })
                        "
                      >
                        {{ t('search.groups.viewCategory') }}
                        <v-icon
                          icon="mdi-arrow-right"
                          size="small"
                          aria-hidden="true"
                        />
                      </v-btn>
                    </div>
                    <CategoryProductListView
                      :products="group.products"
                      :popular-attributes="group.popularAttributes"
                    />
                  </v-expansion-panel-text>
                </v-expansion-panel>
              </v-expansion-panels>
            </section>
          </v-col>

          <v-col cols="12" lg="7">
            <section class="search-page__column">
              <div
                class="search-page__column-header search-page__column-header--with-actions"
              >
                <div class="search-page__column-title">
                  <h2 class="text-h5 font-weight-bold mb-0">
                    {{ t('search.columns.products.title') }}
                  </h2>
                  <p class="search-page__column-subtitle">
                    {{ t('search.columns.products.subtitle') }}
                  </p>
                </div>
              </div>

              <div
                v-if="!rightColumnProducts.length && !rightColumnPending"
                class="search-page__empty"
              >
                <h3 class="search-page__empty-title">
                  {{
                    t('search.columns.products.emptyTitle', {
                      query: normalizedQuery,
                    })
                  }}
                </h3>
                <p class="search-page__empty-description">
                  {{ t('search.columns.products.emptyDescription') }}
                </p>
                <v-btn
                  v-if="isFiltered"
                  variant="text"
                  color="primary"
                  class="mt-4"
                  @click="clearFilters"
                >
                  {{ t('category.filters.reset') }}
                </v-btn>
              </div>

              <CategoryProductListView v-else :products="rightColumnProducts" />
            </section>
          </v-col>
        </v-row>
      </template>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import type {
  AttributeConfigDto,
  GlobalSearchResponseDto,
  ProductDto,
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
import CategoryProductListView from '~/components/category/products/CategoryProductListView.vue'
import CategoryFilterList from '~/components/category/filters/CategoryFilterList.vue'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'
import { useAnalytics } from '~/composables/useAnalytics'

const MIN_QUERY_LENGTH = 3
const VERTICAL_RESULTS_LIMIT = 4

definePageMeta({
  ssr: true,
})

const { t, locale, availableLocales } = useI18n()
const { mdAndUp } = useDisplay()
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
const filtersOpen = ref(false)
const filterRequest = ref<FilterRequestDto>({ filters: [], filterGroups: [] })
const openPanels = ref<number[]>([0])

watch(
  routeQuery,
  value => {
    searchInput.value = value
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
const showLatestProducts = computed(() => normalizedQuery.value.length === 0)
const shouldShowResults = computed(
  () => hasMinimumLength.value || showLatestProducts.value
)
const showInitialState = computed(
  () => !shouldShowResults.value && trimmedInput.value.length === 0
)
const showMinimumNotice = computed(
  () =>
    trimmedInput.value.length > 0 &&
    trimmedInput.value.length < MIN_QUERY_LENGTH
)

const activeFilters = computed(() => filterRequest.value.filters ?? [])
const isFiltered = computed(() => activeFilters.value.length > 0)
// Global Search Data
const { data, pending, error, refresh } =
  await useAsyncData<GlobalSearchResponseDto | null>(
    'global-search',
    async () => {
      if (!hasMinimumLength.value) {
        return null
      }

      return await $fetch<GlobalSearchResponseDto>('/api/products/search', {
        method: 'POST',
        headers: requestHeaders,
        body: {
          query: normalizedQuery.value,
        },
      })
    },
    {
      watch: [() => normalizedQuery.value],
      immediate: hasMinimumLength.value,
    }
  )

const semanticDiagnostics = computed(() => data.value?.semanticDiagnostics)

watch(
  semanticDiagnostics,
  diagnostics => {
    if (!diagnostics) {
      return
    }
    if (import.meta.client) {
      console.info('[search] Semantic diagnostics', diagnostics)
    }
  },
  { immediate: true }
)

// Filtered Product Search Data
const manualFields: FieldMetadataDto[] = [
  {
    mapping: 'price.minPrice.price',
    title: '',
    valueType: 'numeric',
  },
  {
    mapping: 'price.conditions', // backend mapping for condition
    title: '',
    valueType: 'keyword',
  },
]

const filterFields = computed(() => manualFields)

const aggregationDefinition = computed(() => ({
  aggs: [
    { name: 'price', field: 'price.minPrice.price', type: 'range' },
    { name: 'condition', field: 'price.conditions', type: 'terms' },
  ],
}))

const requestBody = computed<ProductSearchRequestDto>(() => ({
  filters: filterRequest.value,
  aggs: aggregationDefinition.value,
  semanticSearch: hasMinimumLength.value ? true : undefined,
}))

const latestProductsSort = computed<SortDto[]>(() => [
  { field: 'creationDate', order: 'desc' },
])

const baselinePayload = computed(() => ({
  query: hasMinimumLength.value ? normalizedQuery.value : undefined,
  aggs: aggregationDefinition.value,
  sort: showLatestProducts.value
    ? { sorts: latestProductsSort.value }
    : undefined,
  semanticSearch: hasMinimumLength.value ? true : undefined,
}))

const {
  data: baselineSearchData,
  pending: baselinePending,
  error: baselineError,
  refresh: refreshBaseline,
} = await useAsyncData<ProductSearchResponseDto | null>(
  'search-baseline',
  async () => {
    if (!shouldShowResults.value) {
      return null
    }

    return await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      headers: requestHeaders,
      body: baselinePayload.value,
    })
  },
  {
    watch: [() => normalizedQuery.value, () => showLatestProducts.value],
    immediate: true,
  }
)

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
    if (!hasMinimumLength.value && !showLatestProducts.value) return null

    if (!isFiltered.value) return null

    return await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      headers: requestHeaders,
      body: {
        query: hasMinimumLength.value ? normalizedQuery.value : undefined,
        ...requestBody.value,
      },
    })
  },
  {
    watch: [() => normalizedQuery.value, () => filterRequest.value],
    immediate: false, // Wait for interaction
  }
)

const productResults = computed(
  () => productSearchData.value?.products?.content ?? []
)
const baselineResults = computed(
  () => baselineSearchData.value?.products?.content ?? []
)
const baselineAggregations = computed<Record<string, AggregationResponseDto>>(
  () => {
    const aggs = baselineSearchData.value?.aggregations ?? []
    return aggs.reduce(
      (acc, curr) => {
        if (curr.field) acc[curr.field] = curr
        return acc
      },
      {} as Record<string, AggregationResponseDto>
    )
  }
)
const productAggregations = computed<Record<string, AggregationResponseDto>>(
  () => {
    const activeSource = isFiltered.value
      ? productSearchData.value
      : baselineSearchData.value
    const aggs = activeSource?.aggregations ?? []
    return aggs.reduce(
      (acc, curr) => {
        if (curr.field) acc[curr.field] = curr
        return acc
      },
      {} as Record<string, AggregationResponseDto>
    )
  }
)

const updateRangeFilter = (
  field: string,
  payload: { min?: number; max?: number }
) => {
  const current = activeFilters.value.filter(f => f.field !== field)
  if (payload.min == null && payload.max == null) {
    filterRequest.value = { ...filterRequest.value, filters: current }
    return
  }
  filterRequest.value = {
    ...filterRequest.value,
    filters: [
      ...current,
      { field, operator: 'range', min: payload.min, max: payload.max },
    ],
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
    filters: [...current, { field, operator: 'term', terms }],
  }
}

const clearFilters = () => {
  filterRequest.value = { filters: [], filterGroups: [] }
  if (!mdAndUp.value) {
    filtersOpen.value = false
  }
}

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
          : t('search.columns.verticals.unknownTitle'))

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
      } satisfies SearchGroup | null
    })
    .filter((group): group is SearchGroup => Boolean(group))
})

const limitedGroups = computed(() => {
  return primaryGroups.value
    .map(group => {
      const limitedProducts = group.products.slice(0, VERTICAL_RESULTS_LIMIT)
      if (!limitedProducts.length) {
        return null
      }

      return {
        ...group,
        products: limitedProducts,
        countLabel: buildGroupCountLabel(limitedProducts.length),
      }
    })
    .filter((group): group is SearchGroup => Boolean(group))
})

watch(
  limitedGroups,
  groups => {
    openPanels.value = groups.length ? [0] : []
  },
  { immediate: true }
)

const missingVerticalProducts = computed(() =>
  extractProducts(data.value?.missingVerticalResults ?? [])
)

const rightColumnProducts = computed(() => {
  if (isFiltered.value) {
    return productResults.value
  }

  if (showLatestProducts.value) {
    return baselineResults.value
  }

  return missingVerticalProducts.value
})

const rightColumnPending = computed(() => {
  if (isFiltered.value) {
    return productsPending.value
  }

  if (showLatestProducts.value) {
    return baselinePending.value
  }

  return pending.value
})

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

const handleClear = () => {
  searchInput.value = ''
  router.replace({
    path: route.path,
    query: {},
  })
}

const resolveCategorySuggestionUrl = (
  suggestion: CategorySuggestionItem
): string | null => {
  const normalizedFromSuggestion = normalizeVerticalHomeUrl(suggestion.url)

  if (normalizedFromSuggestion) {
    return normalizedFromSuggestion
  }

  const verticalId = suggestion.verticalId?.trim()

  if (!verticalId) {
    return null
  }

  return normalizeVerticalHomeUrl(
    verticalById.value.get(verticalId)?.verticalHomeUrl
  )
}

const handleCategorySuggestion = (suggestion: CategorySuggestionItem) => {
  const verticalUrl = resolveCategorySuggestionUrl(suggestion)

  if (!verticalUrl) {
    trackSearch({ query: suggestion.title, source: 'suggestion' })
    handleSearchSubmit()
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

  router.push(localePath(`/${gtin}`))
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

  &__layout
    row-gap: 2.5rem

  &__column
    display: flex
    flex-direction: column
    gap: 1.5rem

  &__column-header
    display: flex
    flex-direction: column
    gap: 0.75rem

    &--with-actions
      gap: 1rem

      @media (min-width: 960px)
        flex-direction: row
        align-items: flex-start
        justify-content: space-between

  &__column-title
    display: flex
    flex-direction: column
    gap: 0.35rem

  &__column-subtitle
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-size: 0.95rem

  &__column-actions
    display: flex
    flex-wrap: wrap
    gap: 0.75rem
    align-items: center

  &__panels
    border-radius: 1rem
    overflow: hidden
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

    :deep(.v-expansion-panel)
      background: rgb(var(--v-theme-surface-default))

  &__panel-title
    display: flex
    align-items: center
    justify-content: space-between
    width: 100%
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__panel-count
    font-size: 0.9rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__panel-meta
    display: flex
    justify-content: flex-end
    margin-bottom: 0.5rem

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

  &__filters
    margin-top: 1rem
    padding: 1.25rem
    border-radius: 1.25rem
    background: rgba(var(--v-theme-hero-overlay-soft), 0.12)
    border: 1px solid rgba(var(--v-theme-hero-overlay-soft), 0.25)
    backdrop-filter: blur(6px)
    display: flex
    flex-direction: column
    gap: 1rem

  &__filters-header
    display: flex
    align-items: center
    justify-content: space-between
    flex-wrap: wrap
    gap: 0.75rem

  &__filters-title
    margin: 0
    font-weight: 600
    font-size: 1rem
    color: rgba(var(--v-theme-hero-overlay-soft), 0.9)

  &__filters-content
    :deep(.category-filter-list)
      grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr))
</style>

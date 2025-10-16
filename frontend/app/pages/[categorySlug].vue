<template>
  <div class="category-page">
    <CategoryHero
      v-if="category"
      :title="category.verticalHomeTitle ?? siteName"
      :description="category.verticalHomeDescription"
      :image="heroImage"
      :breadcrumbs="category.breadCrumb ?? []"
      :eyebrow="category.verticalMetaTitle"
    />

    <v-container v-if="category" fluid class="py-6 category-page__container">
      <CategoryFastFilters
        :subsets="category.subsets ?? []"
        :active-subset-ids="activeSubsetIds"
        class="mb-6"
        @toggle-subset="onToggleSubset"
        @remove-clause="onRemoveSubsetClause"
        @reset="onResetSubsets"
      />

      <div class="category-page__toolbar">
        <div class="category-page__toolbar-left">
          <v-btn
            v-if="!isDesktop"
            color="primary"
            variant="flat"
            prepend-icon="mdi-filter-variant"
            @click="filtersDrawer = true"
          >
            {{ $t('category.products.openFilters') }}
          </v-btn>

          <p class="category-page__results-count" aria-live="polite">
            {{ resultsCountLabel }}
          </p>
        </div>

        <div class="category-page__toolbar-actions">
          <v-text-field
            v-model="searchTerm"
            :label="$t('category.products.searchPlaceholder')"
            prepend-inner-icon="mdi-magnify"
            clearable
            hide-details
            density="comfortable"
            class="category-page__search"
          />

          <div class="category-page__sort">
            <v-select
              v-model="sortField"
              :items="sortItems"
              :label="$t('category.products.sortLabel')"
              item-title="title"
              item-value="value"
              clearable
              hide-details
              density="comfortable"
              class="me-2"
            />
            <v-btn-toggle v-model="sortOrder" class="category-page__sort-order" density="comfortable">
              <v-btn value="asc" :aria-label="$t('category.products.sortOrderAsc')">
                <v-icon icon="mdi-sort-ascending" />
              </v-btn>
              <v-btn value="desc" :aria-label="$t('category.products.sortOrderDesc')">
                <v-icon icon="mdi-sort-descending" />
              </v-btn>
            </v-btn-toggle>
          </div>

          <v-btn-toggle v-model="viewMode" mandatory class="category-page__view-toggle">
            <v-btn value="cards" :aria-label="$t('category.products.viewCards')">
              <v-icon icon="mdi-view-grid" />
            </v-btn>
            <v-btn value="list" :aria-label="$t('category.products.viewList')">
              <v-icon icon="mdi-view-list" />
            </v-btn>
            <v-btn value="table" :aria-label="$t('category.products.viewTable')">
              <v-icon icon="mdi-table" />
            </v-btn>
          </v-btn-toggle>
        </div>
      </div>

      <div class="category-page__layout">
        <v-navigation-drawer
          v-model="filtersDrawer"
          :permanent="isDesktop"
          :width="isDesktop ? 320 : 360"
          location="start"
          class="category-page__filters"
          :temporary="!isDesktop"
        >
          <div class="category-page__filters-content">
            <CategoryFiltersPanel
              :filter-options="filterOptions"
              :aggregations="currentAggregations"
              :filters="manualFilters"
              :impact-expanded="impactExpanded"
              :technical-expanded="technicalExpanded"
              @update:filters="onFiltersChange"
              @update:impact-expanded="(value: boolean) => (impactExpanded = value)"
              @update:technical-expanded="(value: boolean) => (technicalExpanded = value)"
            />

            <div v-if="!isDesktop" class="category-page__filters-actions">
              <v-btn block color="primary" class="mb-2" @click="applyMobileFilters">
                {{ $t('category.filters.mobileApply') }}
              </v-btn>
              <v-btn block variant="text" @click="clearAllFilters">
                {{ $t('category.filters.mobileClear') }}
              </v-btn>
            </div>

            <template v-if="hasDocumentation">
              <v-divider class="my-4" />
              <CategoryDocumentationRail
                class="category-page__documentation-block"
                :wiki-pages="category.wikiPages ?? []"
                :related-posts="category.relatedPosts ?? []"
                :vertical-home-url="category.verticalHomeUrl"
              />
            </template>
          </div>
        </v-navigation-drawer>

        <section class="category-page__results">
          <v-alert
            v-if="productError"
            type="error"
            variant="tonal"
            border="start"
            class="mb-4"
          >
            {{ productError }}
          </v-alert>

          <template v-if="loadingProducts">
            <template v-if="viewMode === 'cards'">
              <v-skeleton-loader
                v-for="index in 3"
                :key="`card-skeleton-${index}`"
                type="image, article"
                class="mb-4"
              />
            </template>
            <template v-else-if="viewMode === 'list'">
              <v-skeleton-loader
                v-for="index in 5"
                :key="`list-skeleton-${index}`"
                type="list-item-two-line"
                class="mb-2"
              />
            </template>
            <template v-else>
              <v-skeleton-loader type="table" class="mb-4" />
            </template>
          </template>

          <template v-else>
            <component
              :is="viewComponent"
              :products="currentProducts"
              :fields="tableFields"
              :items-per-page="pageSize"
              class="mb-6"
            />

            <p v-if="!currentProducts.length" class="category-page__no-results">
              {{ $t('category.products.noResults') }}
            </p>

            <v-pagination
              v-if="pageCount > 1"
              :length="pageCount"
              :model-value="pageNumber + 1"
              class="category-page__pagination"
              density="comfortable"
              @update:model-value="onPageChange"
            />
          </template>
        </section>

      </div>
    </v-container>

    <v-container v-else fluid class="py-10">
      <v-skeleton-loader type="image, article" />
    </v-container>

    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      border="start"
      class="mx-auto my-6"
      max-width="640"
    >
      {{ errorMessage }}
    </v-alert>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, toRaw, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import type {
  AggregationRequestDto,
  AggregationResponseDto,
  Agg,
  FilterRequestDto,
  ProductFieldOptionsResponse,
  ProductSearchResponseDto,
  SortRequestDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'
import { AggTypeEnum } from '~~/shared/api-client'

import CategoryDocumentationRail from '~/components/category/CategoryDocumentationRail.vue'
import CategoryFastFilters from '~/components/category/CategoryFastFilters.vue'
import CategoryFiltersPanel from '~/components/category/CategoryFiltersPanel.vue'
import CategoryHero from '~/components/category/CategoryHero.vue'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import CategoryProductListView from '~/components/category/products/CategoryProductListView.vue'
import CategoryProductTable from '~/components/category/products/CategoryProductTable.vue'
import { CATEGORY_DEFAULT_VIEW_MODE, CATEGORY_PAGE_SIZES } from '~/constants/category'
import { useCategories } from '~/composables/categories/useCategories'
import {
  buildCategoryHash,
  deserializeCategoryHashState,
  type CategoryHashState,
  type CategoryViewMode,
} from '~/utils/_category-filter-state'
import { buildFilterRequestFromSubsets } from '~/utils/_subset-to-filters'

const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const requestURL = useRequestURL()

const rawParam = route.params.categorySlug
const slug = Array.isArray(rawParam) ? rawParam[0] ?? '' : rawParam ?? ''
const slugPattern = /^[a-z-]+$/

if (!slugPattern.test(slug)) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { currentCategory, error: categoriesError, selectCategoryBySlug } = useCategories()

const { data: categoryData } = await useAsyncData(
  `category-detail-${slug}`,
  async () => {
    try {
      return await selectCategoryBySlug(slug)
    } catch (err) {
      if (err instanceof Error && err.name === 'CategoryNotFoundError') {
        throw createError({ statusCode: 404, statusMessage: err.message, cause: err })
      }

      throw err
    }
  },
  { server: true, immediate: true },
)

const category = computed<VerticalConfigFullDto | null>(() => {
  if (categoryData.value) {
    return categoryData.value
  }

  const fallback = currentCategory.value
  return fallback ? (toRaw(fallback) as VerticalConfigFullDto) : null
})
const errorMessage = computed(() => categoriesError.value)

const heroImage = computed(() => {
  if (!category.value) {
    return null
  }

  return (
    category.value.imageMedium ??
    category.value.imageLarge ??
    category.value.imageSmall ??
    null
  )
})

const siteName = computed(() => String(t('siteIdentity.siteName')))
const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const seoTitle = computed(
  () => category.value?.verticalMetaTitle ?? category.value?.verticalHomeTitle ?? siteName.value,
)
const seoDescription = computed(
  () => category.value?.verticalMetaDescription ?? category.value?.verticalHomeDescription ?? '',
)
const ogTitle = computed(
  () => category.value?.verticalMetaOpenGraphTitle ?? seoTitle.value,
)
const ogDescription = computed(
  () => category.value?.verticalMetaOpenGraphDescription ?? seoDescription.value,
)
const ogImage = computed(() => {
  if (!heroImage.value) {
    return undefined
  }

  try {
    return new URL(heroImage.value, requestURL.origin).toString()
  } catch (error) {
    console.error('Invalid hero image URL', error)
    return undefined
  }
})
const ogLocale = computed(() => locale.value.replace('-', '_'))

useSeoMeta({
  title: () => seoTitle.value,
  description: () => seoDescription.value,
  ogTitle: () => ogTitle.value,
  ogDescription: () => ogDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'website',
  ogImage: () => ogImage.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => category.value?.verticalHomeTitle ?? siteName.value,
})

const breadcrumbJsonLd = computed(() => {
  if (!category.value?.breadCrumb?.length) {
    return null
  }

  const elements = category.value.breadCrumb.map((item, index) => {
    const href = item.link
      ? new URL(item.link, requestURL.origin).toString()
      : canonicalUrl.value

    return {
      '@type': 'ListItem',
      position: index + 1,
      name: item.title ?? siteName.value,
      item: href,
    }
  })

  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: elements,
  }
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
  script: breadcrumbJsonLd.value
    ? [
        {
          type: 'application/ld+json',
          children: JSON.stringify(breadcrumbJsonLd.value),
        },
      ]
    : [],
}))

const verticalId = computed(() => category.value?.id ?? null)

const { data: initialProductsData } = await useAsyncData(
  `category-products-${slug}`,
  async () => {
    if (!verticalId.value) {
      return null
    }

    return await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      body: {
        verticalId: verticalId.value,
        pageNumber: 0,
        pageSize: CATEGORY_PAGE_SIZES[CATEGORY_DEFAULT_VIEW_MODE],
      },
    })
  },
  { server: true, immediate: true },
)

const productsData = ref<ProductSearchResponseDto | null>(initialProductsData.value ?? null)
watch(initialProductsData, (value) => {
  if (value) {
    productsData.value = value
  }
})

const { data: filterOptionsData, execute: loadFilterOptions } = useLazyAsyncData(
  `category-filter-options-${slug}`,
  async () => {
    if (!verticalId.value) {
      return null
    }

    return await $fetch<ProductFieldOptionsResponse>(
      `/api/products/fields/filters/${encodeURIComponent(verticalId.value)}`,
    )
  },
  { server: false, immediate: false },
)

const { data: sortOptionsData, execute: loadSortOptions } = useLazyAsyncData(
  `category-sort-options-${slug}`,
  async () => {
    if (!verticalId.value) {
      return null
    }

    return await $fetch<ProductFieldOptionsResponse>(
      `/api/products/fields/sortable/${encodeURIComponent(verticalId.value)}`,
    )
  },
  { server: false, immediate: false },
)

const filterOptions = computed(() => filterOptionsData.value ?? null)
const sortOptions = computed(() => sortOptionsData.value ?? null)

const display = useDisplay()
const isDesktop = computed(() => display.lgAndUp.value)
const filtersDrawer = ref(false)

watch(
  isDesktop,
  (value) => {
    filtersDrawer.value = value
  },
  { immediate: true },
)

const viewMode = ref<CategoryViewMode>(CATEGORY_DEFAULT_VIEW_MODE)
const pageNumber = ref(0)
const searchTerm = ref('')
const sortField = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc'>('desc')
const activeSubsetIds = ref<string[]>([])
const manualFilters = ref<FilterRequestDto>({})
const impactExpanded = ref(false)
const technicalExpanded = ref(false)
const lastAppliedDefaultSort = ref<string | null>(null)

const subsetFilters = computed(() =>
  buildFilterRequestFromSubsets(category.value?.subsets ?? [], activeSubsetIds.value),
)

const combinedFilters = computed<FilterRequestDto | undefined>(() => {
  const subsetClauses = subsetFilters.value.filters ?? []
  const manualClauses = manualFilters.value.filters ?? []
  const filters = [...subsetClauses, ...manualClauses]

  return filters.length ? { filters } : undefined
})

const pageSize = computed(() => CATEGORY_PAGE_SIZES[viewMode.value])

const defaultSortField = computed<string | null>(() => {
  const impactFields = sortOptions.value?.impact ?? []
  const candidate = impactFields.find((field) => typeof field.mapping === 'string')

  return candidate?.mapping ? String(candidate.mapping) : null
})

const applyDefaultSort = () => {
  if (!defaultSortField.value) {
    sortField.value = null
    sortOrder.value = 'desc'
    lastAppliedDefaultSort.value = null
    return false
  }

  sortField.value = defaultSortField.value
  sortOrder.value = 'desc'
  lastAppliedDefaultSort.value = defaultSortField.value
  return true
}

const sortItems = computed(() => {
  const fields = [
    ...(sortOptions.value?.global ?? []),
    ...(sortOptions.value?.impact ?? []),
    ...(sortOptions.value?.technical ?? []),
  ]

  const seen = new Set<string>()

  return fields
    .filter((field) => field.mapping && !seen.has(field.mapping))
    .map((field) => {
      seen.add(field.mapping as string)
      return {
        value: field.mapping as string,
        title: field.title ?? field.mapping,
      }
    })
})

watch(
  defaultSortField,
  (value, previous) => {
    if (!value) {
      if (!sortField.value) {
        sortOrder.value = 'desc'
      }
      lastAppliedDefaultSort.value = null
      return
    }

    if (
      !sortField.value ||
      sortField.value === previous ||
      sortField.value === lastAppliedDefaultSort.value
    ) {
      applyDefaultSort()
    }
  },
)

const sortRequest = computed<SortRequestDto | undefined>(() => {
  if (!sortField.value) {
    return undefined
  }

  return {
    sorts: [
      {
        field: sortField.value,
        order: sortOrder.value,
      },
    ],
  }
})

const currentProducts = computed(() => productsData.value?.products?.data ?? [])
const resultsCount = computed(() => productsData.value?.products?.page?.totalElements ?? 0)
const resultsCountLabel = computed(() =>
  translatePlural('category.products.resultsCount', resultsCount.value),
)
const pageCount = computed(() => productsData.value?.products?.page?.totalPages ?? 1)
const currentAggregations = computed<AggregationResponseDto[]>(
  () => productsData.value?.aggregations ?? [],
)

const hasDocumentation = computed(() => {
  const wikiCount = category.value?.wikiPages?.length ?? 0
  const postCount = category.value?.relatedPosts?.length ?? 0

  return wikiCount + postCount > 0
})

const tableFields = computed(() => {
  if (!filterOptions.value) {
    return []
  }

  return [
    ...(filterOptions.value.global ?? []),
    ...(filterOptions.value.impact ?? []),
    ...(filterOptions.value.technical ?? []),
  ]
})

const viewComponent = computed(() => {
  if (viewMode.value === 'list') {
    return CategoryProductListView
  }

  if (viewMode.value === 'table') {
    return CategoryProductTable
  }

  return CategoryProductCardGrid
})

const loadingProducts = ref(false)
const productError = ref<string | null>(null)
const hasHydrated = ref(false)

const buildAggregationRequest = (
  options: ProductFieldOptionsResponse | null,
): AggregationRequestDto | undefined => {
  if (!options) {
    return undefined
  }

  const fields = [
    ...(options.global ?? []),
    ...(options.impact ?? []),
    ...(options.technical ?? []),
  ]

  const seen = new Set<string>()
  const aggs: Agg[] = []

  fields.forEach((field) => {
    if (!field.mapping || seen.has(field.mapping)) {
      return
    }

    seen.add(field.mapping)

    const agg: Agg = {
      name: field.mapping,
      field: field.mapping,
      type: field.valueType === 'numeric' ? AggTypeEnum.Range : AggTypeEnum.Terms,
    }

    if (field.aggregationConfiguration?.buckets != null) {
      agg.buckets = field.aggregationConfiguration.buckets
    }

    if (field.aggregationConfiguration?.interval != null) {
      agg.step = field.aggregationConfiguration.interval
    }

    aggs.push(agg)
  })

  return aggs.length ? { aggs } : undefined
}

const fetchProducts = async () => {
  if (!verticalId.value || !hasHydrated.value) {
    return
  }

  loadingProducts.value = true
  productError.value = null

  try {
    const response = await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      body: {
        verticalId: verticalId.value,
        pageNumber: pageNumber.value,
        pageSize: pageSize.value,
        query: searchTerm.value || undefined,
        sort: sortRequest.value,
        filters: combinedFilters.value,
        aggs: buildAggregationRequest(filterOptions.value),
      },
    })

    productsData.value = response
  } catch (error) {
    productError.value =
      error instanceof Error ? error.message : t('category.products.fetchError')
  } finally {
    loadingProducts.value = false
  }
}

const debouncedFetch = useDebounceFn(() => {
  if (import.meta.client) {
    fetchProducts()
  }
}, 250)

watch(
  () => [viewMode.value, sortField.value, sortOrder.value],
  () => {
    if (!hasHydrated.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
)

watch(
  () => manualFilters.value,
  () => {
    if (!hasHydrated.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
  { deep: true },
)

watch(
  () => activeSubsetIds.value,
  () => {
    if (!hasHydrated.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
  { deep: true },
)

watch(
  () => searchTerm.value,
  () => {
    if (!hasHydrated.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
)

watch(
  () => pageNumber.value,
  () => {
    if (!hasHydrated.value) {
      return
    }

    debouncedFetch()
  },
)

watch(
  () => filterOptions.value,
  (value) => {
    if (value && hasHydrated.value) {
      fetchProducts()
    }
  },
)

const hashState = computed<CategoryHashState>(() => ({
  filters: manualFilters.value.filters?.length ? manualFilters.value : {},
  activeSubsets: activeSubsetIds.value,
  view: viewMode.value,
  pageNumber: pageNumber.value,
  search: searchTerm.value || undefined,
  sort: sortRequest.value,
  impactExpanded: impactExpanded.value || undefined,
  technicalExpanded: technicalExpanded.value || undefined,
}))

watch(
  hashState,
  (state) => {
    if (!import.meta.client || !hasHydrated.value) {
      return
    }

    const hash = buildCategoryHash(state)
    const currentHash = window.location.hash ?? ''

    if (currentHash !== hash) {
      router.replace({ hash })
    }
  },
  { deep: true },
)

onMounted(async () => {
  const hashPayload = deserializeCategoryHashState(window.location.hash.slice(1))

  if (hashPayload) {
    if (hashPayload.view) {
      viewMode.value = hashPayload.view
    }

    if (hashPayload.pageNumber != null) {
      pageNumber.value = hashPayload.pageNumber
    }

    if (hashPayload.search) {
      searchTerm.value = hashPayload.search
    }

    const sortEntry = hashPayload.sort?.sorts?.[0]
    if (sortEntry) {
      sortField.value = sortEntry.field ?? null
      sortOrder.value = sortEntry.order ?? 'desc'
    }

    if (hashPayload.filters?.filters?.length) {
      manualFilters.value = hashPayload.filters
    }

    if (hashPayload.activeSubsets?.length) {
      activeSubsetIds.value = [...hashPayload.activeSubsets]
    }

    if (typeof hashPayload.impactExpanded === 'boolean') {
      impactExpanded.value = hashPayload.impactExpanded
    }

    if (typeof hashPayload.technicalExpanded === 'boolean') {
      technicalExpanded.value = hashPayload.technicalExpanded
    }
  }

  if (verticalId.value) {
    await Promise.all([loadFilterOptions(), loadSortOptions()])

    if (!sortField.value) {
      applyDefaultSort()
    }
  }

  hasHydrated.value = true

  if (verticalId.value) {
    await fetchProducts()
  }
})

watch(
  verticalId,
  (id, previousId) => {
    if (import.meta.client && id) {
      loadFilterOptions()
      loadSortOptions()
    }

    if (id && id !== previousId) {
      sortField.value = null
      sortOrder.value = 'desc'
      lastAppliedDefaultSort.value = null
    }
  },
)

const onToggleSubset = (subsetId: string, active: boolean) => {
  const next = new Set(activeSubsetIds.value)

  if (active) {
    next.add(subsetId)
  } else {
    next.delete(subsetId)
  }

  activeSubsetIds.value = Array.from(next)
}

const onRemoveSubsetClause = (clause: { subsetId: string }) => {
  activeSubsetIds.value = activeSubsetIds.value.filter((id) => id !== clause.subsetId)
}

const onResetSubsets = () => {
  activeSubsetIds.value = []
}

const onFiltersChange = (filters: FilterRequestDto) => {
  manualFilters.value = filters
}

const onPageChange = (page: number) => {
  pageNumber.value = page - 1
}

const applyMobileFilters = () => {
  filtersDrawer.value = false
}

const clearAllFilters = () => {
  manualFilters.value = {}
  activeSubsetIds.value = []
  searchTerm.value = ''
  impactExpanded.value = false
  technicalExpanded.value = false
  pageNumber.value = 0
  applyDefaultSort()
}
</script>

<style scoped lang="sass">
.category-page
  display: flex
  flex-direction: column

  &__container
    max-width: 1400px

  &__toolbar
    display: flex
    flex-direction: column
    gap: 1rem
    margin-bottom: 1.5rem

  &__toolbar-left
    display: flex
    align-items: center
    gap: 1rem
    flex-wrap: wrap

  &__toolbar-actions
    display: flex
    align-items: center
    flex-wrap: wrap
    gap: 1rem

  &__search
    min-width: 240px

  &__sort
    display: flex
    align-items: center
    gap: 0.5rem

  &__sort-order
    border-radius: 999px

  &__view-toggle
    border-radius: 999px

  &__results-count
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__layout
    display: grid
    gap: 1.75rem
    grid-template-columns: minmax(0, 1fr)

  &__filters
    position: sticky
    top: 96px
    height: calc(100vh - 120px)
    border-radius: 1rem

  &__filters-content
    display: flex
    flex-direction: column
    gap: 1.5rem
    height: 100%
    padding: 1.25rem 1rem 1.5rem
    overflow-y: auto

  &__filters-actions
    padding: 1rem

  &__results
    min-height: 420px

  &__no-results
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__pagination
    justify-content: center

  &__documentation-block
    width: 100%
    display: flex
    flex-direction: column
    gap: 1.5rem

@media (min-width: 1280px)
  .category-page__layout
    grid-template-columns: 320px minmax(0, 1fr)

  .category-page__filters
    background: transparent
    box-shadow: none

@media (max-width: 959px)
  .category-page__toolbar
    align-items: stretch

  .category-page__toolbar-actions
    justify-content: space-between

  .category-page__layout
    grid-template-columns: minmax(0, 1fr)

  .category-page__filters
    position: static
    height: auto

  .category-page__filters-content
    overflow-y: visible
</style>

<template>
  <div class="category-page">
    <CategoryHero
      v-if="category"
      :title="category.verticalHomeTitle ?? siteName"
      :description="category.verticalHomeDescription"
      :image="heroImage"
      :breadcrumbs="category.breadCrumb ?? []"
      :eyebrow="category.verticalMetaTitle"
      :show-image="isDesktop"
    />

    <v-dialog
      v-model="isNudgeWizardOpen"
      scrollable
      max-width="980"
      transition="dialog-bottom-transition"
    >
      <NudgeToolWizard
        v-if="category"
        :initial-category-id="category.id"
        :initial-filters="manualFilters"
        :initial-subsets="activeSubsetIds"
        :verticals="[category]"
        @navigate="handleNudgeNavigate"
      />
    </v-dialog>

    <v-container v-if="category" fluid class="py-6 category-page__container">
      <div
        v-if="hasFastFilters || isDesktop"
        class="category-page__fast-filters mb-6"
      >
        <div class="category-page__fast-filters-row">
          <v-tooltip v-if="isDesktop" :text="filtersToggleLabel">
            <template #activator="{ props: tooltipProps }">
              <v-btn
                class="category-page__fast-filters-toggle"
                :class="{
                  'category-page__fast-filters-toggle--collapsed':
                    filtersCollapsed,
                  'category-page__fast-filters-toggle--expanded':
                    !filtersCollapsed,
                }"
                icon
                variant="text"
                v-bind="tooltipProps"
                :aria-label="filtersToggleLabel"
                :aria-pressed="(!filtersCollapsed).toString()"
                @click="onToggleFiltersVisibility"
              >
                <v-icon
                  icon="mdi-filter-variant"
                  size="40"
                  class="category-page__fast-filters-toggle-icon"
                  :class="{
                    'category-page__fast-filters-toggle-icon--collapsed':
                      filtersCollapsed,
                    'category-page__fast-filters-toggle-icon--expanded':
                      !filtersCollapsed,
                  }"
                />
              </v-btn>
            </template>
          </v-tooltip>

          <CategoryFastFilters
            class="category-page__fast-filters-groups"
            :subsets="category.subsets ?? []"
            :active-subset-ids="activeSubsetIds"
            @toggle-subset="onToggleSubset"
            @reset="onResetSubsets"
          />
        </div>

        <CategoryActiveFilters
          v-if="hasActiveFilters"
          class="category-page__active-filters"
          :filters="manualFilters"
          :subset-clauses="activeSubsetClauses"
          :field-metadata="filterFieldMap"
          @remove-filter="onRemoveManualFilter"
          @remove-subset-clause="onRemoveSubsetClause"
          @clear-all="clearAllFilters"
        />
      </div>

      <div ref="layoutRef" class="category-page__layout" :style="layoutStyle">
        <template v-if="!isDesktop">
          <ClientOnly>
            <v-navigation-drawer
              v-model="filtersDrawer"
              :width="360"
              location="start"
              class="category-page__filters-drawer"
              temporary
            >
            <CategoryFiltersSidebar
              :filter-options="filterOptions"
              :aggregations="currentAggregations"
              :baseline-aggregations="baselineAggregations"
              :filters="manualFilters"
              :impact-expanded="impactExpanded"
              :technical-expanded="technicalExpanded"
              :show-mobile-actions="true"
              :has-documentation="hasDocumentation"
              :wiki-pages="category?.wikiPages ?? []"
              :related-posts="category?.relatedPosts ?? []"
              :show-admin-panel="showAdminFilters"
              :admin-filter-fields="adminFilterFields"
              @update:filters="onFiltersChange"
              @update:impact-expanded="
                (value: boolean) => (impactExpanded = value)
              "
              @update:technical-expanded="
                (value: boolean) => (technicalExpanded = value)
              "
              @apply-mobile="applyMobileFilters"
              @clear-mobile="clearAllFilters"
            >
              <template #extra>
                <div class="category-page__filters-cta">
                  <CategoryEcoscoreCard
                    :vertical-home-url="category?.verticalHomeUrl"
                    :category-name="categoryDisplayName"
                  />
                  <v-btn
                    block
                    color="primary"
                    variant="flat"
                    prepend-icon="mdi-robot-love"
                    class="category-page__nudge-cta"
                    @click="isNudgeWizardOpen = true"
                  >
                    {{ $t('category.hero.nudge.cta') }}
                  </v-btn>
                </div>
              </template>
            </CategoryFiltersSidebar>
            </v-navigation-drawer>
          </ClientOnly>
        </template>
        <template v-else>
          <aside
            ref="filtersSidebarRef"
            class="category-page__filters-surface"
            :class="{
              'category-page__filters-surface--collapsed':
                !isFiltersColumnVisible,
            }"
            role="complementary"
            :aria-label="$t('category.products.openFilters')"
            :aria-hidden="(!isFiltersColumnVisible).toString()"
            :inert="!isFiltersColumnVisible"
            :style="filtersStyle"
          >
            <CategoryFiltersSidebar
              :filter-options="filterOptions"
              :aggregations="currentAggregations"
              :baseline-aggregations="baselineAggregations"
              :filters="manualFilters"
              :impact-expanded="impactExpanded"
              :technical-expanded="technicalExpanded"
              :show-mobile-actions="false"
              :has-documentation="hasDocumentation"
              :wiki-pages="category?.wikiPages ?? []"
              :related-posts="category?.relatedPosts ?? []"
              :show-admin-panel="showAdminFilters"
              :admin-filter-fields="adminFilterFields"
              @update:filters="onFiltersChange"
              @update:impact-expanded="
                (value: boolean) => (impactExpanded = value)
              "
              @update:technical-expanded="
                (value: boolean) => (technicalExpanded = value)
              "
            >
              <template #extra>
                <div class="category-page__filters-cta">
                  <CategoryEcoscoreCard
                    :vertical-home-url="category?.verticalHomeUrl"
                    :category-name="categoryDisplayName"
                  />
                  <v-btn
                    block
                    color="primary"
                    variant="flat"
                    prepend-icon="mdi-robot-love"
                    class="category-page__nudge-cta"
                    @click="isNudgeWizardOpen = true"
                  >
                    {{ $t('category.hero.nudge.cta') }}
                  </v-btn>
                </div>
              </template>
            </CategoryFiltersSidebar>
          </aside>

          <div
            ref="filtersResizerRef"
            class="category-page__filters-resizer"
            :class="{
              'category-page__filters-resizer--collapsed':
                !isFiltersColumnVisible,
            }"
            role="separator"
            aria-orientation="vertical"
            :tabindex="isFiltersColumnVisible ? 0 : -1"
            @pointerdown="onResizeHandlePointerDown"
          />
        </template>

        <section
          class="category-page__results"
          itemscope
          itemprop="mainEntity"
          itemtype="https://schema.org/ItemList"
        >
          <meta itemprop="name" :content="seoTitle" />
          <meta itemprop="numberOfItems" :content="String(resultsCount)" />

          <div class="category-page__toolbar">
            <div
              class="category-page__toolbar-section category-page__toolbar-section--left"
            >
              <v-btn
                v-if="!isDesktop"
                color="primary"
                variant="flat"
                prepend-icon="mdi-filter-variant"
                @click="filtersDrawer = true"
              >
                {{ $t('category.products.openFilters') }}
              </v-btn>

              <CategoryResultsCount :count="resultsCount" />

              <div class="category-page__sort">
                <div class="category-page__sort-select">
                  <v-tooltip
                    location="bottom"
                    :text="$t('category.products.tooltips.sortField')"
                  >
                    <template #activator="{ props: sortSelectProps }">
                      <v-select
                        v-bind="sortSelectProps"
                        v-model="sortField"
                        :items="sortItems"
                        :label="$t('category.products.sortLabel')"
                        item-title="title"
                        item-value="value"
                        clearable
                        hide-details
                        density="comfortable"
                      />
                    </template>
                  </v-tooltip>
                </div>
                <v-btn-toggle
                  v-model="sortOrder"
                  class="category-page__sort-order"
                  density="comfortable"
                >
                  <v-btn
                    value="asc"
                    :aria-label="$t('category.products.sortOrderAsc')"
                  >
                    <v-icon icon="mdi-sort-ascending" />
                    <v-tooltip
                      activator="parent"
                      location="bottom"
                      :text="$t('category.products.tooltips.sortAscending')"
                    />
                  </v-btn>
                  <v-btn
                    value="desc"
                    :aria-label="$t('category.products.sortOrderDesc')"
                  >
                    <v-icon icon="mdi-sort-descending" />
                    <v-tooltip
                      activator="parent"
                      location="bottom"
                      :text="$t('category.products.tooltips.sortDescending')"
                    />
                  </v-btn>
                </v-btn-toggle>
              </div>
            </div>

            <div
              class="category-page__toolbar-section category-page__toolbar-section--center"
            >
              <div class="category-page__search">
                <v-tooltip
                  location="bottom"
                  :text="$t('category.products.tooltips.search')"
                >
                  <template #activator="{ props: searchProps }">
                    <v-text-field
                      v-bind="searchProps"
                      v-model="searchTerm"
                      :label="$t('category.products.searchPlaceholder')"
                      prepend-inner-icon="mdi-magnify"
                      clearable
                      hide-details
                      density="comfortable"
                      class="category-page__search-input"
                    />
                  </template>
                </v-tooltip>
              </div>
            </div>

            <div
              class="category-page__toolbar-section category-page__toolbar-section--right"
            >
              <v-btn-toggle
                v-model="viewMode"
                mandatory
                class="category-page__view-toggle"
              >
                <v-btn
                  value="cards"
                  :aria-label="$t('category.products.viewCards')"
                >
                  <v-icon icon="mdi-view-grid" />
                  <v-tooltip
                    activator="parent"
                    location="bottom"
                    :text="$t('category.products.tooltips.viewCards')"
                  />
                </v-btn>
                <v-btn
                  value="list"
                  :aria-label="$t('category.products.viewList')"
                >
                  <v-icon icon="mdi-view-list" />
                  <v-tooltip
                    activator="parent"
                    location="bottom"
                    :text="$t('category.products.tooltips.viewList')"
                  />
                </v-btn>
                <v-btn
                  value="table"
                  :aria-label="$t('category.products.viewTable')"
                >
                  <v-icon icon="mdi-table" />
                  <v-tooltip
                    activator="parent"
                    location="bottom"
                    :text="$t('category.products.tooltips.viewTable')"
                  />
                </v-btn>
              </v-btn-toggle>
            </div>
          </div>

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
              v-bind="viewComponentProps"
              class="mb-6"
              @update:sort-field="onTableSortFieldUpdate"
              @update:sort-order="onTableSortOrderUpdate"
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
import { computed, onBeforeUnmount, onMounted, ref, toRaw, watch } from 'vue'
import type { ActiveHeadEntry, UseHeadInput } from '@unhead/vue'
import {
  StorageSerializers,
  useDebounceFn,
  useEventListener,
  useStorage,
} from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { isNavigationFailure } from 'vue-router'
import type {
  AggregationRequestDto,
  AggregationResponseDto,
  Agg,
  AttributeConfigDto,
  FieldMetadataDto,
  Filter,
  FilterRequestDto,
  ProductFieldOptionsResponse,
  ProductSearchResponseDto,
  SortRequestDto,
  VerticalConfigFullDto,
  VerticalSubsetDto,
} from '~~/shared/api-client'
import { AggTypeEnum } from '~~/shared/api-client'

import CategoryActiveFilters from '~/components/category/CategoryActiveFilters.vue'
import CategoryFastFilters from '~/components/category/CategoryFastFilters.vue'
import CategoryHero from '~/components/category/CategoryHero.vue'
import CategoryEcoscoreCard from '~/components/category/CategoryEcoscoreCard.vue'
import CategoryFiltersSidebar from '~/components/category/CategoryFiltersSidebar.vue'
import CategoryResultsCount from '~/components/category/CategoryResultsCount.vue'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import CategoryProductListView from '~/components/category/products/CategoryProductListView.vue'
import CategoryProductTable from '~/components/category/products/CategoryProductTable.vue'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import {
  CATEGORY_DEFAULT_VIEW_MODE,
  CATEGORY_PAGE_SIZES,
} from '~/constants/category'
import { useCategories } from '~/composables/categories/useCategories'
import { useAuth } from '~/composables/useAuth'
import {
  buildCategoryHash,
  deserializeCategoryHashState,
  type CategoryHashState,
  type CategoryViewMode,
} from '~/utils/_category-filter-state'
import { mergeFilterRequests } from '~/utils/_merge-filter-requests'
import {
  buildFilterRequestFromSubsets,
  convertSubsetCriteriaToFilters,
  getRemainingSubsetFilters,
  mergeFiltersWithoutDuplicates,
} from '~/utils/_subset-to-filters'
import type { CategorySubsetClause } from '~/types/category-subset'
import {
  resolveFilterFieldTitle,
  resolveSortFieldTitle,
} from '~/utils/_field-localization'
import { hasAdminAccess } from '~~/shared/utils/_roles'

const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()
const requestURL = useRequestURL()
const { isLoggedIn, roles } = useAuth()
const listComponents = [
  'base',
  'identity',
  'names',
  'attributes',
  'resources',
  'scores',
  'offers',
]
const LISTING_COMPONENTS = listComponents.join(',')

const isAdmin = computed(() => isLoggedIn.value && hasAdminAccess(roles.value))
const ADMIN_EXCLUDED_FIELD = 'excludedCauses'

const adminFilterFields = computed<FieldMetadataDto[]>(() => {
  if (!isAdmin.value) {
    return []
  }

  return [
    {
      mapping: ADMIN_EXCLUDED_FIELD,
      title: t('category.admin.filters.excludedCauses.title'),
      description: t('category.admin.filters.excludedCauses.helper'),
      valueType: 'text',
    },
  ]
})

const showAdminFilters = computed(() => adminFilterFields.value.length > 0)

const MOBILE_USER_AGENT_PATTERN =
  /(Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Kindle|Silk|Opera Mini)/i

const resolveUserAgentFromHeader = (
  headerValue: string | string[] | undefined
): string | null => {
  if (!headerValue) {
    return null
  }

  if (Array.isArray(headerValue)) {
    return headerValue[0] ?? null
  }

  return headerValue
}

const display = useDisplay()

const initialIsDesktop = useState<boolean>(
  'category-page-initial-is-desktop',
  () => {
    if (import.meta.server) {
      const userAgentHeader = useRequestHeaders(['user-agent'])['user-agent']
      const userAgent = resolveUserAgentFromHeader(userAgentHeader)

      if (!userAgent) {
        return true
      }

      return !MOBILE_USER_AGENT_PATTERN.test(userAgent)
    }

    return display.lgAndUp.value
  }
)

const isHydrated = ref(false)

onMounted(() => {
  isHydrated.value = true
})

const isDesktop = computed(() =>
  isHydrated.value ? display.lgAndUp.value : initialIsDesktop.value
)
const filtersDrawer = ref(false)

const props = defineProps<{ slug: string }>()
const slug = props.slug
const slugPattern = /^[a-z-]+$/

if (!slugPattern.test(slug)) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const {
  currentCategory,
  error: categoriesError,
  selectCategoryBySlug,
} = useCategories()

const { data: categoryData, error: categoryError } = await useAsyncData(
  `category-detail-${slug}`,
  async () => {
    try {
      return await selectCategoryBySlug(slug)
    } catch (err) {
      console.error('Error resolving category detail for slug:', slug, err)
      if (
        err instanceof Error &&
        (err.name === 'CategoryNotFoundError' ||
          err.name === 'CategoryResolutionError')
      ) {
        throw createError({
          statusCode: 404,
          statusMessage: err.message,
          cause: err,
        })
      }

      throw err
    }
  },
  { server: true, immediate: true }
)

if (categoryError.value && import.meta.server) {
  const resolvedError = categoryError.value as {
    statusCode?: number
    statusMessage?: string
  }

  throw createError({
    statusCode: resolvedError.statusCode ?? 500,
    statusMessage: resolvedError.statusMessage ?? 'Failed to load category',
    cause: categoryError.value,
  })
}

const category = computed<VerticalConfigFullDto | null>(() => {
  if (categoryData.value) {
    return categoryData.value
  }

  const fallback = currentCategory.value
  return fallback ? (toRaw(fallback) as VerticalConfigFullDto) : null
})
const errorMessage = computed(() => categoriesError.value)
const hasFastFilters = computed(
  () => (category.value?.subsets?.length ?? 0) > 0
)

const heroImage = computed(() => {
  if (!category.value) {
    return null
  }

  return (
    category.value.imageMedium ??
    category.value.imageSmall ??
    category.value.imageLarge ??
    null
  )
})

const siteName = computed(() => String(t('siteIdentity.siteName')))
const categoryDisplayName = computed(
  () =>
    category.value?.verticalHomeTitle ??
    category.value?.verticalMetaTitle ??
    category.value?.verticalHomeDescription ??
    siteName.value
)
const shouldRestrictCategoryProducts = computed(
  () => category.value?.enabled === false && !isLoggedIn.value
)
const canonicalUrl = computed(() =>
  new URL(route.path, requestURL.origin).toString()
)
const seoTitle = computed(
  () =>
    category.value?.verticalMetaTitle ??
    category.value?.verticalHomeTitle ??
    siteName.value
)
const seoDescription = computed(
  () =>
    category.value?.verticalMetaDescription ??
    category.value?.verticalHomeDescription ??
    ''
)
const robotsContent = computed(() =>
  shouldRestrictCategoryProducts.value ? 'noindex, nofollow' : undefined
)
const ogTitle = computed(
  () => category.value?.verticalMetaOpenGraphTitle ?? seoTitle.value
)
const ogDescription = computed(
  () => category.value?.verticalMetaOpenGraphDescription ?? seoDescription.value
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
  ogImage: () => ogImage.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => category.value?.verticalHomeTitle ?? siteName.value,
})

useHead(() => ({
  meta: robotsContent.value
    ? [{ name: 'robots', content: robotsContent.value }]
    : [],
}))

useHead(() => ({
  meta: [
    {
      key: 'og:type',
      property: 'og:type',
      content: 'product.group',
    },
  ],
}))

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

const toAbsoluteUrl = (value?: string | null) => {
  if (!value) {
    return undefined
  }

  try {
    return new URL(value, requestURL.origin).toString()
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to build absolute URL for product entry.', error)
    }

    return undefined
  }
}

type StructuredDataScript = {
  key: string
  type: string
  children: string
}

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
    ...(heroImage.value && isDesktop.value
      ? [
          {
            rel: 'preload',
            as: 'image',
            href: heroImage.value,
          },
        ]
      : []),
  ],
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
      query: { include: LISTING_COMPONENTS },
      body: {
        verticalId: verticalId.value,
        pageNumber: 0,
        pageSize: CATEGORY_PAGE_SIZES[CATEGORY_DEFAULT_VIEW_MODE],
      },
    })
  },
  { server: true, immediate: true }
)

const productsData = ref<ProductSearchResponseDto | null>(
  initialProductsData.value ?? null
)
watch(initialProductsData, value => {
  if (value) {
    productsData.value = value
  }
})

const baselineAggregationMap = ref<Record<string, AggregationResponseDto>>({})

const baselineAggregations = computed(() =>
  Object.values(baselineAggregationMap.value)
)

const { data: filterOptionsData, execute: loadFilterOptions } =
  useLazyAsyncData(
    `category-filter-options-${slug}`,
    async () => {
      if (!verticalId.value) {
        return null
      }

      return await $fetch<ProductFieldOptionsResponse>(
        `/api/products/fields/filters/${encodeURIComponent(verticalId.value)}`
      )
    },
    { server: false, immediate: false }
  )

const { data: sortOptionsData, execute: loadSortOptions } = useLazyAsyncData(
  `category-sort-options-${slug}`,
  async () => {
    if (!verticalId.value) {
      return null
    }

    return await $fetch<ProductFieldOptionsResponse>(
      `/api/products/fields/sortable/${encodeURIComponent(verticalId.value)}`
    )
  },
  { server: false, immediate: false }
)

const filterOptions = computed(() => filterOptionsData.value ?? null)
const sortOptions = computed(() => sortOptionsData.value ?? null)

const FILTERS_VISIBILITY_STORAGE_KEY = 'category-page-filters-collapsed'
const DEFAULT_FILTERS_COLLAPSED_STATE = false

const filtersVisibilityCookie = useCookie<string | null>(
  FILTERS_VISIBILITY_STORAGE_KEY,
  {
    sameSite: 'lax',
    path: '/',
    watch: false,
  }
)

const resolveCollapsedPreference = (
  value: string | boolean | null | undefined,
  fallback: boolean
): boolean => {
  if (typeof value === 'boolean') {
    return value
  }

  if (typeof value === 'string') {
    if (value === 'true') {
      return true
    }

    if (value === 'false') {
      return false
    }
  }

  return fallback
}

const filtersCollapsedState = useStorage<boolean>(
  FILTERS_VISIBILITY_STORAGE_KEY,
  resolveCollapsedPreference(
    filtersVisibilityCookie.value,
    DEFAULT_FILTERS_COLLAPSED_STATE
  ),
  undefined,
  {
    serializer: StorageSerializers.boolean,
  }
)

const syncFiltersCollapsedCookie = (value: boolean) => {
  const cookieValue = value ? 'true' : 'false'

  if (filtersVisibilityCookie.value !== cookieValue) {
    filtersVisibilityCookie.value = cookieValue
  }
}

syncFiltersCollapsedCookie(filtersCollapsedState.value)

watch(filtersCollapsedState, value => {
  syncFiltersCollapsedCookie(value)
})

const filtersCollapsed = computed<boolean>({
  get: () => filtersCollapsedState.value,
  set: value => {
    filtersCollapsedState.value = value
  },
})
const filtersToggleLabel = computed(() =>
  filtersCollapsed.value
    ? t('category.filters.toggle.show')
    : t('category.filters.toggle.hide')
)
const isFiltersColumnVisible = computed(
  () => isDesktop.value && !filtersCollapsed.value
)

const FILTERS_PANEL_STORAGE_KEY = 'category-page-filters-width'
const DEFAULT_FILTERS_PANEL_WIDTH = 300
const MIN_FILTERS_PANEL_WIDTH = 260
const MAX_FILTERS_PANEL_WIDTH = 480
const RESIZER_COLUMN_WIDTH = 12

const clampFiltersPanelWidth = (value: number | null | undefined): number => {
  const numericValue =
    typeof value === 'number' && !Number.isNaN(value)
      ? value
      : DEFAULT_FILTERS_PANEL_WIDTH

  return Math.min(
    MAX_FILTERS_PANEL_WIDTH,
    Math.max(MIN_FILTERS_PANEL_WIDTH, numericValue)
  )
}

const filtersPanelWidth = useStorage<number>(
  FILTERS_PANEL_STORAGE_KEY,
  DEFAULT_FILTERS_PANEL_WIDTH
)

filtersPanelWidth.value = clampFiltersPanelWidth(filtersPanelWidth.value)

const layoutRef = ref<HTMLElement | null>(null)
const filtersSidebarRef = ref<HTMLElement | null>(null)
const filtersResizerRef = ref<HTMLElement | null>(null)
const isResizing = ref(false)

const layoutStyle = computed(() => {
  if (!isDesktop.value) {
    return {}
  }

  const width = clampFiltersPanelWidth(filtersPanelWidth.value)
  const activeWidth = isFiltersColumnVisible.value ? width : 0
  const resizerHitbox = isFiltersColumnVisible.value ? RESIZER_COLUMN_WIDTH : 0

  return {
    gridTemplateColumns: `${activeWidth}px minmax(0, 1fr)`,
    columnGap: isFiltersColumnVisible.value ? '1.75rem' : '0',
    '--filters-panel-width': `${width}px`,
    '--filters-active-width': `${activeWidth}px`,
    '--filters-resizer-hitbox': `${resizerHitbox}px`,
  }
})

const filtersStyle = computed(() => {
  if (!isDesktop.value) {
    return {}
  }

  const width = clampFiltersPanelWidth(filtersPanelWidth.value)
  const activeWidth = isFiltersColumnVisible.value ? width : 0

  return {
    width: `${activeWidth}px`,
    maxWidth: `${width}px`,
  }
})

let activePointerId: number | null = null
let initialPointerX = 0
let initialPanelWidth = clampFiltersPanelWidth(filtersPanelWidth.value)

let stopPointerMove: (() => void) | undefined
let stopPointerUp: (() => void) | undefined
let stopPointerCancel: (() => void) | undefined

const removePointerListeners = () => {
  stopPointerMove?.()
  stopPointerMove = undefined
  stopPointerUp?.()
  stopPointerUp = undefined
  stopPointerCancel?.()
  stopPointerCancel = undefined

  if (activePointerId !== null) {
    filtersResizerRef.value?.releasePointerCapture?.(activePointerId)
  }
}

const onPointerMove = (event: PointerEvent) => {
  if (
    !isResizing.value ||
    (activePointerId !== null && event.pointerId !== activePointerId)
  ) {
    return
  }

  event.preventDefault()

  const deltaX = event.clientX - initialPointerX
  const nextWidth = clampFiltersPanelWidth(initialPanelWidth + deltaX)
  filtersPanelWidth.value = nextWidth
}

const onPointerEnd = (event: PointerEvent) => {
  if (
    !isResizing.value ||
    (activePointerId !== null && event.pointerId !== activePointerId)
  ) {
    return
  }

  isResizing.value = false
  activePointerId = null
  initialPointerX = 0
  initialPanelWidth = clampFiltersPanelWidth(filtersPanelWidth.value)

  if (event.pointerId !== undefined) {
    filtersResizerRef.value?.releasePointerCapture?.(event.pointerId)
  }

  removePointerListeners()
}

const onResizeHandlePointerDown = (event: PointerEvent) => {
  if (!isDesktop.value || !isFiltersColumnVisible.value) {
    return
  }

  event.preventDefault()

  removePointerListeners()

  isResizing.value = true
  activePointerId = event.pointerId
  initialPointerX = event.clientX
  const sidebarRect = filtersSidebarRef.value?.getBoundingClientRect()
  initialPanelWidth = sidebarRect
    ? clampFiltersPanelWidth(sidebarRect.width)
    : clampFiltersPanelWidth(filtersPanelWidth.value)

  filtersPanelWidth.value = initialPanelWidth

  filtersResizerRef.value?.setPointerCapture?.(event.pointerId)

  if (!stopPointerMove) {
    stopPointerMove = useEventListener(window, 'pointermove', onPointerMove)
  }

  if (!stopPointerUp) {
    stopPointerUp = useEventListener(window, 'pointerup', onPointerEnd)
  }

  if (!stopPointerCancel) {
    stopPointerCancel = useEventListener(window, 'pointercancel', onPointerEnd)
  }
}

watch(
  isDesktop,
  (value, previous) => {
    if (value) {
      filtersDrawer.value = false

      if (previous === false) {
        initialPanelWidth = clampFiltersPanelWidth(filtersPanelWidth.value)
      }

      return
    }

    filtersDrawer.value = false
    removePointerListeners()
    isResizing.value = false
    activePointerId = null
    initialPointerX = 0
    initialPanelWidth = clampFiltersPanelWidth(filtersPanelWidth.value)
  },
  { immediate: true }
)

watch(filtersCollapsed, collapsed => {
  if (collapsed) {
    removePointerListeners()
    isResizing.value = false
    activePointerId = null
    initialPointerX = 0
  }
})

const onToggleFiltersVisibility = () => {
  filtersCollapsed.value = !filtersCollapsed.value
}

const viewMode = ref<CategoryViewMode>(CATEGORY_DEFAULT_VIEW_MODE)
const pageNumber = ref(0)
const searchTerm = ref('')
const shouldUseSemanticSearch = computed(
  () => searchTerm.value.trim().length > 0
)
const sortField = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc'>('desc')
const activeSubsetIds = ref<string[]>([])
const manualFilters = ref<FilterRequestDto>({})
const isNudgeWizardOpen = ref(false)
const impactExpanded = ref(false)
const technicalExpanded = ref(false)
const lastAppliedDefaultSort = ref<string | null>(null)

const areFiltersEqual = (
  left: FilterRequestDto,
  right: FilterRequestDto
): boolean => {
  const leftFilters = left.filters ?? []
  const rightFilters = right.filters ?? []

  if (leftFilters.length !== rightFilters.length) {
    return false
  }

  return leftFilters.every((filter, index) => {
    const other = rightFilters[index]
    return JSON.stringify(filter) === JSON.stringify(other)
  })
}

function arraysEqual<T>(left: T[], right: T[]): boolean {
  if (left.length !== right.length) {
    return false
  }

  return left.every((value, index) => value === right[index])
}

const subsetMap = computed(() => {
  const map = new Map<string, VerticalSubsetDto>()
  const availableSubsets = category.value?.subsets ?? []

  availableSubsets
    .filter((subset): subset is VerticalSubsetDto =>
      Boolean(subset && subset.id)
    )
    .forEach(subset => {
      if (subset.id) {
        map.set(subset.id, subset)
      }
    })

  return map
})

const DEFAULT_SUBSET_GROUP_KEY = 'ungrouped'

const getSubsetGroupKey = (
  subset: VerticalSubsetDto | undefined | null
): string => {
  return subset?.group ?? DEFAULT_SUBSET_GROUP_KEY
}

const normalizeActiveSubsetIds = (subsetIds: string[]): string[] => {
  const seenGroups = new Set<string>()
  const normalized: string[] = []

  subsetIds.forEach(subsetId => {
    const subset = subsetMap.value.get(subsetId)
    const groupKey = subset ? getSubsetGroupKey(subset) : subsetId
    if (seenGroups.has(groupKey)) {
      return
    }

    seenGroups.add(groupKey)
    normalized.push(subsetId)
  })

  return normalized
}

const applyHashPayload = (payload: CategoryHashState | null) => {
  const nextViewMode = payload?.view ?? CATEGORY_DEFAULT_VIEW_MODE
  if (viewMode.value !== nextViewMode) {
    viewMode.value = nextViewMode
  }

  const nextPageNumber = payload?.pageNumber ?? 0
  if (pageNumber.value !== nextPageNumber) {
    pageNumber.value = nextPageNumber
  }

  const nextSearchTerm = payload?.search ?? ''
  if (searchTerm.value !== nextSearchTerm) {
    searchTerm.value = nextSearchTerm
  }

  const sortEntry = payload?.sort?.sorts?.[0]
  const nextSortField = sortEntry?.field ?? null
  const nextSortOrder = sortEntry?.order ?? 'desc'

  if (sortField.value !== nextSortField) {
    sortField.value = nextSortField
  }

  if (sortOrder.value !== nextSortOrder) {
    sortOrder.value = nextSortOrder
  }

  const nextFilters: FilterRequestDto = payload?.filters?.filters?.length
    ? payload.filters
    : {}

  const normalizedFilters = nextFilters.filters?.length
    ? nextFilters.filters
        .map(filter => ({ ...filter }))
        .filter(
          filter => isAdmin.value || filter.field !== ADMIN_EXCLUDED_FIELD
        )
    : []

  const filtersPayload: FilterRequestDto = normalizedFilters.length
    ? { filters: normalizedFilters }
    : {}

  if (!areFiltersEqual(manualFilters.value, filtersPayload)) {
    manualFilters.value = filtersPayload
  }

  const nextActiveSubsets = normalizeActiveSubsetIds(
    payload?.activeSubsets ?? []
  )
  if (!arraysEqual(activeSubsetIds.value, nextActiveSubsets)) {
    activeSubsetIds.value = [...nextActiveSubsets]
  }

  const nextImpactExpanded = payload?.impactExpanded ?? false
  if (impactExpanded.value !== nextImpactExpanded) {
    impactExpanded.value = nextImpactExpanded
  }

  const nextTechnicalExpanded = payload?.technicalExpanded ?? false
  if (technicalExpanded.value !== nextTechnicalExpanded) {
    technicalExpanded.value = nextTechnicalExpanded
  }
}

const handleHashChange = () => {
  if (!import.meta.client) {
    return
  }

  const payload = deserializeCategoryHashState(window.location.hash.slice(1))
  applyHashPayload(payload)
}

const subsetFilters = computed(() =>
  buildFilterRequestFromSubsets(
    category.value?.subsets ?? [],
    activeSubsetIds.value
  )
)

const buildSubsetClauseLabel = (filter: Filter): string => {
  const mapping = filter.field ?? ''
  const fallbackField = mapping || 'field'
  const metadata = mapping ? filterFieldMap.value[mapping] : undefined
  const fieldLabel =
    resolveFilterFieldTitle(metadata, t, fallbackField) || fallbackField

  if (filter.operator === 'term') {
    const term = filter.terms?.[0] ?? ''
    return term ? `${fieldLabel}: ${term}` : fieldLabel
  }

  const bounds: string[] = []

  if (typeof filter.min === 'number') {
    bounds.push(
      t('category.fastFilters.operator.greaterThan', { value: filter.min })
    )
  }

  if (typeof filter.max === 'number') {
    bounds.push(
      t('category.fastFilters.operator.lowerThan', { value: filter.max })
    )
  }

  if (!bounds.length) {
    return fieldLabel
  }

  return `${fieldLabel}: ${bounds.join(' · ')}`
}

const activeSubsetClauses = computed<CategorySubsetClause[]>(() => {
  return activeSubsetIds.value.flatMap(subsetId => {
    const subset = subsetMap.value.get(subsetId)
    if (!subset) {
      return []
    }

    const filters = convertSubsetCriteriaToFilters(subset)

    return filters.map((filter, index) => ({
      id: `${subsetId}-${index}`,
      subsetId,
      filter,
      index,
      label: buildSubsetClauseLabel(filter),
    }))
  })
})

const hasActiveFilters = computed(() => {
  const manualCount = manualFilters.value.filters?.length ?? 0
  return manualCount > 0 || activeSubsetClauses.value.length > 0
})

const combinedFilters = computed<FilterRequestDto | undefined>(() =>
  mergeFilterRequests(subsetFilters.value, manualFilters.value)
)

const pageSize = computed(() => CATEGORY_PAGE_SIZES[viewMode.value])

const defaultSortField = computed<string | null>(() => {
  const impactFields = sortOptions.value?.impact ?? []
  const candidate = impactFields.find(
    field => typeof field.mapping === 'string'
  )

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
    .filter(field => field.mapping && !seen.has(field.mapping))
    .map(field => {
      seen.add(field.mapping as string)
      return {
        value: field.mapping as string,
        title: resolveSortFieldTitle(field, t),
      }
    })
})

watch(defaultSortField, (value, previous) => {
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
})

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
const resultsCount = computed(
  () => productsData.value?.products?.page?.totalElements ?? 0
)

const productListJsonLd = computed(() => {
  const products = currentProducts.value

  if (!products.length) {
    return null
  }

  const items = products.slice(0, 20).map((product, index) => {
    const name =
      product.identity?.bestName ??
      product.identity?.model ??
      product.identity?.brand ??
      (product.gtin != null ? `GTIN ${product.gtin}` : undefined)

    if (!name) {
      return null
    }

    const productUrl = toAbsoluteUrl(product.fullSlug ?? product.slug)
    const imageUrl =
      toAbsoluteUrl(product.resources?.coverImagePath) ??
      toAbsoluteUrl(
        product.resources?.externalCover ?? product.resources?.images?.[0]?.url
      )

    const offersCount = product.offers?.offersCount ?? 0
    const bestPrice = product.offers?.bestPrice
    const offer =
      bestPrice?.price != null
        ? {
            '@type': 'Offer',
            price: Number(bestPrice.price),
            priceCurrency: bestPrice.currency ?? undefined,
            url: productUrl ?? canonicalUrl.value,
            availability:
              offersCount > 0
                ? 'https://schema.org/InStock'
                : 'https://schema.org/OutOfStock',
          }
        : undefined

    const productSchema: Record<string, unknown> = {
      '@type': 'Product',
      name,
      url: productUrl,
      image: imageUrl,
      brand: product.identity?.brand
        ? {
            '@type': 'Brand',
            name: product.identity.brand,
          }
        : undefined,
      sku: product.identity?.model,
      offers: offer,
    }

    if (product.gtin != null) {
      const gtin = String(product.gtin)
      productSchema.gtin = gtin
      if (gtin.length === 13) {
        productSchema.gtin13 = gtin
      }
    }

    return {
      '@type': 'ListItem',
      position: index + 1,
      url: productUrl,
      item: Object.fromEntries(
        Object.entries(productSchema).filter(([, value]) => value != null)
      ),
    }
  })

  const filteredItems = items.filter(
    (item): item is Exclude<(typeof items)[number], null> => item !== null
  )

  if (!filteredItems.length) {
    return null
  }

  return {
    '@context': 'https://schema.org',
    '@type': 'ItemList',
    name: seoTitle.value,
    numberOfItems: resultsCount.value,
    itemListOrder: 'https://schema.org/ItemListUnordered',
    url: canonicalUrl.value,
    itemListElement: filteredItems,
  }
})

const structuredDataScripts = computed(() => {
  const scripts: StructuredDataScript[] = []

  if (breadcrumbJsonLd.value) {
    scripts.push({
      key: 'category-breadcrumb-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(breadcrumbJsonLd.value),
    })
  }

  if (productListJsonLd.value) {
    scripts.push({
      key: 'category-product-list-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(productListJsonLd.value),
    })
  }

  return scripts
})

let structuredDataHeadEntry: ActiveHeadEntry<UseHeadInput> | null = null

watch(
  structuredDataScripts,
  scripts => {
    structuredDataHeadEntry?.dispose?.()

    if (!scripts.length) {
      structuredDataHeadEntry = null
      return
    }

    structuredDataHeadEntry = useHead({
      script: scripts,
    })
  },
  { immediate: true }
)

const pageCount = computed(
  () => productsData.value?.products?.page?.totalPages ?? 1
)
const currentAggregations = computed<AggregationResponseDto[]>(
  () => productsData.value?.aggregations ?? []
)

const captureBaselineAggregations = (
  aggregations: AggregationResponseDto[] | null | undefined
) => {
  if (!aggregations || !aggregations.length) {
    baselineAggregationMap.value = {}
    return
  }

  baselineAggregationMap.value = aggregations.reduce<
    Record<string, AggregationResponseDto>
  >((accumulator, aggregation) => {
    if (aggregation.field) {
      accumulator[aggregation.field] = aggregation
    }

    return accumulator
  }, {})
}

watch(
  () => ({
    aggregations: productsData.value?.aggregations ?? null,
    manualFiltersCount: manualFilters.value.filters?.length ?? 0,
  }),
  (state, previous) => {
    if (state.manualFiltersCount !== 0) {
      return
    }

    if (!state.aggregations || !state.aggregations.length) {
      baselineAggregationMap.value = {}
      return
    }

    if (state.aggregations === previous?.aggregations) {
      return
    }

    captureBaselineAggregations(state.aggregations)
  },
  { immediate: true }
)

watch(
  () => verticalId.value,
  () => {
    baselineAggregationMap.value = {}
  }
)

const hasDocumentation = computed(() => {
  const wikiCount = category.value?.wikiPages?.length ?? 0
  const postCount = category.value?.relatedPosts?.length ?? 0

  return wikiCount + postCount > 0
})

const tableFields = computed<FieldMetadataDto[]>(() => {
  const fields: FieldMetadataDto[] = [
    ...(filterOptions.value?.global ?? []),
    ...(filterOptions.value?.impact ?? []),
    ...(filterOptions.value?.technical ?? []),
  ]

  if (adminFilterFields.value.length) {
    fields.push(...adminFilterFields.value)
  }

  return fields
})

const filterFieldMap = computed<Record<string, FieldMetadataDto>>(() => {
  return tableFields.value.reduce<Record<string, FieldMetadataDto>>(
    (accumulator, field) => {
      if (field.mapping) {
        accumulator[String(field.mapping)] = field
      }

      return accumulator
    },
    {}
  )
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

const popularAttributes = computed(
  () => category.value?.popularAttributes ?? []
)

const attributeKeys = computed(() => {
  const technical = category.value?.technicalFilters ?? []
  const eco = category.value?.ecoFilters ?? []

  return Array.from(
    new Set(
      [...technical, ...eco].filter((entry): entry is string => Boolean(entry))
    )
  )
})

const attributeConfigMap = computed<Record<string, AttributeConfigDto>>(() => {
  const configs = category.value?.attributesConfig?.configs ?? []

  return configs.reduce<Record<string, AttributeConfigDto>>(
    (accumulator, config) => {
      if (config?.key) {
        accumulator[config.key] = config
      }

      return accumulator
    },
    {}
  )
})

const viewComponentProps = computed(() => {
  const base = {
    products: currentProducts.value,
    popularAttributes: popularAttributes.value,
  }

  if (viewMode.value === 'table') {
    return {
      ...base,
      itemsPerPage: pageSize.value,
      sortField: sortField.value,
      sortOrder: sortOrder.value,
      attributeKeys: attributeKeys.value,
      attributeConfigs: attributeConfigMap.value,
      fieldMetadata: filterFieldMap.value,
    }
  }

  if (viewMode.value === 'list') {
    return base
  }

  return {
    ...base,
    isCategoryDisabled: shouldRestrictCategoryProducts.value,
    nofollowLinks: shouldRestrictCategoryProducts.value,
  }
})

const loadingProducts = ref(false)
const productError = ref<string | null>(null)
const hasHydrated = ref(false)

const isDefaultQueryState = computed(() => {
  const hasDefaultFilters = !manualFilters.value.filters?.length
  const hasDefaultSubsets = activeSubsetIds.value.length === 0
  const hasDefaultSearch = searchTerm.value === ''
  const hasDefaultPaging =
    pageNumber.value === 0 && viewMode.value === CATEGORY_DEFAULT_VIEW_MODE
  const hasDefaultSort = !sortField.value && sortOrder.value === 'desc'

  return (
    hasDefaultFilters &&
    hasDefaultSubsets &&
    hasDefaultSearch &&
    hasDefaultPaging &&
    hasDefaultSort
  )
})

const isUsingInitialProductsData = computed(() => {
  if (!initialProductsData.value) {
    return false
  }

  return (
    productsData.value === initialProductsData.value &&
    isDefaultQueryState.value
  )
})

const shouldFetchFreshProducts = computed(
  () => hasHydrated.value && !isUsingInitialProductsData.value
)

const buildAggregationRequest = (
  options: ProductFieldOptionsResponse | null,
  extraFields: FieldMetadataDto[] = []
): AggregationRequestDto | undefined => {
  const fields = [
    ...(options?.global ?? []),
    ...(options?.impact ?? []),
    ...(options?.technical ?? []),
    ...extraFields,
  ]

  const seen = new Set<string>()
  const aggs: Agg[] = []

  fields.forEach(field => {
    if (!field.mapping || seen.has(field.mapping)) {
      return
    }

    seen.add(field.mapping)

    const agg: Agg = {
      name: field.mapping,
      field: field.mapping,
      type:
        field.valueType === 'numeric' ? AggTypeEnum.Range : AggTypeEnum.Terms,
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
    const response = await $fetch<ProductSearchResponseDto>(
      '/api/products/search',
      {
        method: 'POST',
        query: { include: LISTING_COMPONENTS },
        body: {
          verticalId: verticalId.value,
          pageNumber: pageNumber.value,
          pageSize: pageSize.value,
          query: searchTerm.value || undefined,
          semanticSearch: shouldUseSemanticSearch.value ? true : undefined,
          sort: sortRequest.value,
          filters: combinedFilters.value,
          aggs: buildAggregationRequest(
            filterOptions.value,
            adminFilterFields.value
          ),
        },
      }
    )

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
    if (!shouldFetchFreshProducts.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  }
)

watch(
  () => manualFilters.value,
  () => {
    if (!shouldFetchFreshProducts.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
  { deep: true }
)

watch(
  () => activeSubsetIds.value,
  () => {
    if (!shouldFetchFreshProducts.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  },
  { deep: true }
)

watch(
  () => searchTerm.value,
  () => {
    if (!shouldFetchFreshProducts.value) {
      return
    }

    pageNumber.value = 0
    debouncedFetch()
  }
)

watch(
  () => pageNumber.value,
  () => {
    if (!shouldFetchFreshProducts.value) {
      return
    }

    debouncedFetch()
  }
)

watch(
  () => filterOptions.value,
  value => {
    if (value && shouldFetchFreshProducts.value) {
      fetchProducts()
    }
  }
)

watch(isAdmin, value => {
  if (!value) {
    const existing = manualFilters.value.filters ?? []
    const filtered = existing.filter(
      filter => filter.field !== ADMIN_EXCLUDED_FIELD
    )

    if (filtered.length !== existing.length) {
      manualFilters.value = filtered.length ? { filters: filtered } : {}
    }

    return
  }

  if (shouldFetchFreshProducts.value) {
    pageNumber.value = 0
    debouncedFetch()
  }
})

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

const onTableSortFieldUpdate = (field: string | null) => {
  if (sortField.value !== field) {
    sortField.value = field
  }
}

const onTableSortOrderUpdate = (order: 'asc' | 'desc') => {
  if (sortOrder.value !== order) {
    sortOrder.value = order
  }
}

watch(
  hashState,
  state => {
    if (!import.meta.client || !hasHydrated.value) {
      return
    }

    const hash = buildCategoryHash(state)
    const currentHash = window.location.hash ?? ''

    if (currentHash !== hash) {
      const baseLocation = {
        path: route.path,
        query: { ...route.query },
      }

      const targetLocation = hash ? { ...baseLocation, hash } : baseLocation

      void router.push(targetLocation).catch(error => {
        if (!isNavigationFailure(error)) {
          console.error('Failed to update category hash fragment.', error)
        }
      })
    }
  },
  { deep: true }
)

onMounted(async () => {
  if (import.meta.client) {
    applyHashPayload(
      deserializeCategoryHashState(window.location.hash.slice(1))
    )
    window.addEventListener('hashchange', handleHashChange)
  }

  if (verticalId.value) {
    try {
      await Promise.all([loadFilterOptions(), loadSortOptions()])
      if (!sortField.value) {
        applyDefaultSort()
      }
    } catch (err) {
      console.error('Failed to load filter/sort options:', err)
    }
  }

  hasHydrated.value = true

  if (verticalId.value && !isUsingInitialProductsData.value) {
    try {
      await fetchProducts()
    } catch (err) {
      console.error('Failed to fetch initial products:', err)
    }
  }
})

onBeforeUnmount(() => {
  if (import.meta.client) {
    window.removeEventListener('hashchange', handleHashChange)
  }

  structuredDataHeadEntry?.dispose?.()
  structuredDataHeadEntry = null
  removePointerListeners()
})

watch(verticalId, (id, previousId) => {
  if (import.meta.client && id) {
    loadFilterOptions()
    loadSortOptions()
  }

  if (id && id !== previousId) {
    sortField.value = null
    sortOrder.value = 'desc'
    lastAppliedDefaultSort.value = null
  }
})

watch(subsetMap, () => {
  const normalized = normalizeActiveSubsetIds(activeSubsetIds.value)
  if (!arraysEqual(activeSubsetIds.value, normalized)) {
    activeSubsetIds.value = normalized
  }
})

const onToggleSubset = (subsetId: string, active: boolean) => {
  const subset = subsetMap.value.get(subsetId)
  const next = new Set(activeSubsetIds.value)

  if (active) {
    if (subset) {
      const targetGroup = getSubsetGroupKey(subset)

      next.forEach(id => {
        if (id === subsetId) {
          return
        }

        const candidate = subsetMap.value.get(id)
        if (candidate && getSubsetGroupKey(candidate) === targetGroup) {
          next.delete(id)
        }
      })
    }

    next.add(subsetId)
  } else {
    next.delete(subsetId)
  }

  activeSubsetIds.value = normalizeActiveSubsetIds(Array.from(next))
}

const onRemoveSubsetClause = (clause: CategorySubsetClause) => {
  activeSubsetIds.value = normalizeActiveSubsetIds(
    activeSubsetIds.value.filter(id => id !== clause.subsetId)
  )

  const subset = subsetMap.value.get(clause.subsetId)
  const remainingFilters = getRemainingSubsetFilters(subset, clause.index)

  if (!remainingFilters.length) {
    return
  }

  const currentManualFilters = manualFilters.value.filters ?? []
  const merged = mergeFiltersWithoutDuplicates(
    currentManualFilters,
    remainingFilters
  )

  manualFilters.value = merged.length ? { filters: merged } : {}
}

const onRemoveManualFilter = (
  field: string,
  type: 'term' | 'range',
  term: string | null
) => {
  const next = (manualFilters.value.filters ?? []).filter(filter => {
    if (filter.field !== field) {
      return true
    }

    if (type === 'term') {
      return !(filter.operator === 'term' && filter.terms?.includes(term ?? ''))
    }

    return filter.operator !== 'range'
  })

  manualFilters.value = next.length ? { filters: next } : {}
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

const handleNudgeNavigate = () => {
  isNudgeWizardOpen.value = false
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
    max-width: 1560px

  &__toolbar
    display: flex
    flex-direction: column
    gap: 1rem
    margin-bottom: 1.5rem
    width: 100%

  &__toolbar-section
    display: flex
    align-items: center
    gap: 1rem
    width: 100%
    flex-wrap: wrap

  &__toolbar-section--left
    justify-content: flex-start

  &__toolbar-section--center
    justify-content: center

  &__toolbar-section--right
    justify-content: flex-end

  &__fast-filters
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__fast-filters-row
    display: flex
    align-items: center
    gap: 0.75rem

  &__fast-filters-toggle
    flex: 0 0 auto
    align-self: center
    width: 48px
    height: 48px
    min-width: 48px
    border-radius: 999px
    background: rgb(var(--v-theme-surface-glass))
    box-shadow: 0 14px 28px -24px rgba(var(--v-theme-shadow-primary-600), 0.4)
    color: rgb(var(--v-theme-text-neutral-secondary))
    display: inline-flex
    align-items: center
    justify-content: center
    transition: background-color 0.24s ease, box-shadow 0.24s ease, transform 0.2s ease

    :deep(.v-btn__overlay)
      opacity: 0

    &:hover
      transform: translateY(-1px)

    &:active
      transform: translateY(0)

    &:focus-visible
      outline: 2px solid rgb(var(--v-theme-accent-primary-highlight))
      outline-offset: 3px

  &__fast-filters-toggle--collapsed
    background: rgba(var(--v-theme-surface-primary-080), 0.85)
    color: rgb(var(--v-theme-primary))

  &__fast-filters-toggle--expanded
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__fast-filters-toggle-icon
    color: inherit
    transform-origin: center
    transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1)
    will-change: transform

  &__fast-filters-toggle-icon--collapsed
    transform: rotate(90deg)

  &__fast-filters-toggle-icon--expanded
    transform: rotate(-90deg)

  @media (prefers-reduced-motion: reduce)
    &__fast-filters-toggle-icon
      transition: none

  &__fast-filters-groups
    flex: 1 1 auto
    min-width: 0

  &__active-filters
    width: 100%

  &__search
    position: relative
    min-width: min(260px, 100%)
    width: 100%
    max-width: 420px
    flex: 1 1 100%

  &__search-input
    width: 100%

  &__sort
    display: flex
    align-items: center
    gap: 0.5rem
    flex-wrap: wrap

  &__sort-select
    position: relative
    min-width: 200px

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
    position: relative
    row-gap: 1.75rem
    column-gap: 1.75rem
    grid-template-columns: minmax(0, 1fr)
    align-items: start
    transition: grid-template-columns 0.3s cubic-bezier(0.4, 0, 0.2, 1), column-gap 0.3s cubic-bezier(0.4, 0, 0.2, 1)

  &__filters-drawer
    :deep(.v-navigation-drawer__content)
      padding: 0

  &__filters-surface
    position: sticky
    top: 96px
    align-self: start
    max-height: calc(100vh - 136px)
    display: flex
    flex-direction: column
    border-radius: 1rem
    background: rgb(var(--v-theme-surface-glass))
    box-shadow: 0 22px 46px -28px rgba(var(--v-theme-shadow-primary-600), 0.28)
    overflow: hidden
    overscroll-behavior: contain
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.24s ease, box-shadow 0.24s ease
    opacity: 1
    visibility: visible

  &__filters-surface--collapsed
    opacity: 0
    visibility: hidden
    pointer-events: none
    box-shadow: none

  &__filters-resizer
    display: none
    position: absolute
    top: 0
    bottom: 0
    left: var(--filters-active-width, 0px)
    transform: translateX(-50%)
    width: var(--filters-resizer-hitbox, 12px)
    cursor: col-resize
    border-radius: 999px
    background: transparent
    justify-content: center
    align-items: center
    outline: none
    touch-action: none
    z-index: 1
    transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.24s ease, width 0.3s cubic-bezier(0.4, 0, 0.2, 1)
    opacity: 1

    &::before
      content: ''
      position: absolute
      left: 50%
      top: 20%
      bottom: 20%
      width: 2px
      border-radius: 999px
      background: rgba(var(--v-theme-border-primary-strong), 0.6)
      transform: translateX(-50%)
      transition: background-color 0.2s ease

    &:hover::before
      background: rgb(var(--v-theme-border-primary-strong))

    &:focus-visible
      outline: 2px solid rgb(var(--v-theme-accent-primary-highlight))
      outline-offset: 2px

  &__filters-resizer--collapsed
    opacity: 0
    pointer-events: none
    width: 0

  &__filters-content
    display: flex
    flex-direction: column
    gap: 1.5rem
    flex: 1 1 auto
    padding: 1.5rem 1.25rem 1.75rem
    overflow-y: auto

  &__filters-actions
    padding: 0.5rem 0.75rem 0

  &__filters-extra
    display: flex
    flex-direction: column
    gap: 1rem

  &__filters-cta
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__nudge-cta
    box-shadow: 0 14px 28px -22px rgba(var(--v-theme-shadow-primary-600), 0.45)

  &__results
    min-height: 420px
    min-width: 0

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

@media (min-width: 960px)
  .category-page__toolbar
    display: grid
    grid-template-columns: auto minmax(260px, 1fr) auto
    align-items: center
    gap: 1.5rem

  .category-page__toolbar-section
    flex-wrap: nowrap

  .category-page__toolbar-section--left
    justify-content: flex-start

  .category-page__toolbar-section--center
    justify-content: center

  .category-page__toolbar-section--right
    justify-content: flex-end

  .category-page__search
    flex: 0 1 420px

@media (min-width: 1280px)
  .category-page__layout
    grid-template-columns: minmax(260px, 300px) minmax(0, 1fr)
    column-gap: 1.5rem

  .category-page__filters-surface
    max-height: calc(100vh - 152px)

  .category-page__filters-resizer
    display: flex

@media (max-width: 959px)
  .category-page__toolbar
    align-items: stretch

  .category-page__fast-filters
    flex-direction: column
    align-items: stretch

  .category-page__search
    flex: 1 1 100%
    min-width: 0
    max-width: 100%

  .category-page__toolbar-section--center
    justify-content: center

  .category-page__toolbar-section--right
    justify-content: flex-end

  .category-page__sort
    width: 100%

  .category-page__sort-select
    flex: 1 1 100%

  .category-page__layout
    grid-template-columns: minmax(0, 1fr)

  .category-page__filters-content
    padding: 1.25rem 1rem 1.5rem
    overflow-y: visible

  .category-page__filters-actions
    padding: 0.5rem 0
</style>

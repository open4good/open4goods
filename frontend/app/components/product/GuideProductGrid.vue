<template>
  <div
    v-if="!isUnknownSubcategory"
    class="guide-product-grid"
    @click="onWidgetClick"
  >
    <v-row v-if="pending" class="guide-product-grid__skeletons">
      <v-col
        v-for="index in topCount"
        :key="index"
        cols="12"
        sm="6"
        :md="topCount >= 4 ? 3 : 4"
      >
        <v-skeleton-loader type="image, article" class="rounded-xl" />
      </v-col>
    </v-row>

    <template v-else-if="products.length">
      <slot name="header" :products="products" />
      <CategoryProductCardGrid :products="products" size="small" />
      <slot name="footer" :products="products" />
      <div v-if="seeAllUrl" class="guide-product-grid__see-all">
        <NuxtLink
          :to="seeAllUrl"
          class="guide-product-grid__see-all-link"
          data-widget-target="see-all"
        >
          {{ t('buyingGuide.widget.seeAll') }}
        </NuxtLink>
      </div>
    </template>

    <p v-else-if="!hideWhenEmpty" class="guide-product-grid__empty">
      {{ t('buyingGuide.widget.empty') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  Filter,
  ProductDto,
  ProductCardSearchResponseDto,
  SortRequestDto,
} from '~~/shared/api-client'
import CategoryProductCardGrid from '~/components/category/products/CategoryProductCardGrid.vue'
import { useGuideContext } from '~/composables/useGuideContext'
import { useAnalytics } from '~/composables/useAnalytics'
import { useCategories } from '~/composables/categories/useCategories'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import { buildFilterRequestFromCriteria } from '~/utils/_subset-to-filters'

const props = withDefaults(
  defineProps<{
    vertical?: string
    /** Sub-category id (ex. `tv_55_inches`): same products as its category page. */
    subcategory?: string
    top?: string | number
    sort?: string
    /** Extra filters combined (AND) with the vertical. */
    filters?: Filter[]
    /** Only keep products with at least one active offer. */
    requireOffers?: boolean
    /** Render nothing (no empty message) when there is no product. */
    hideWhenEmpty?: boolean
    /** Without `subcategory`, link "Voir tout" to the vertical category page. */
    seeAllCategory?: boolean
  }>(),
  {
    vertical: undefined,
    subcategory: undefined,
    top: 3,
    sort: 'ecoscore',
    filters: undefined,
    requireOffers: false,
    hideWhenEmpty: false,
    seeAllCategory: false,
  }
)

const { $fetch: csrfFetch } = useNuxtApp()

const { t } = useI18n()
const guideContext = useGuideContext()
const route = useRoute()
const { trackEvent } = useAnalytics()
const { fetchCategoryById } = useCategories()

const normalizedVertical = computed(() =>
  `${props.vertical ?? guideContext?.verticalId ?? ''}`.trim()
)
const normalizedSubcategory = computed(() =>
  `${props.subcategory ?? ''}`.trim()
)
const topCount = computed(() => {
  const parsed = Number.parseInt(`${props.top}`, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 3
})

// `sort` accepts a known keyword (ecoscore) or a raw field mapping. Order can be
// suffixed with ":asc" / ":desc"; impact score defaults to descending.
const sortRequest = computed<SortRequestDto>(() => {
  const raw = (props.sort ?? 'ecoscore').trim()
  const [keyword, order] = raw.split(':')
  const field =
    !keyword || keyword === 'ecoscore' || keyword === 'impact'
      ? ECOSCORE_RELATIVE_FIELD
      : keyword

  return {
    sorts: [
      {
        field,
        order: order === 'asc' ? 'asc' : 'desc',
      },
    ],
  }
})

// Mirrors the competition filter of the category page (`offersCount` range).
const requestFilters = computed<Filter[]>(() => [
  ...(props.filters ?? []),
  ...(props.requireOffers || normalizedSubcategory.value
    ? [{ field: 'offersCount', operator: 'range' as const, min: 1 }]
    : []),
])

type SubcategoryResolution = {
  filters: Filter[]
  url: string
} | null

const categoryHome = (verticalHomeUrl?: string) =>
  verticalHomeUrl?.replace(/^\/+|\/+$/g, '') ?? ''

// Same source as CategoryPage.vue: the category detail served by front-api.
const resolveSubcategory = async (): Promise<SubcategoryResolution> => {
  try {
    const category = await fetchCategoryById(normalizedVertical.value)
    const subCategory = category.subCategories?.find(
      candidate => candidate.id === normalizedSubcategory.value
    )
    const slug = subCategory?.slug?.trim()
    if (!subCategory || !slug) {
      return null
    }

    const home = categoryHome(category.verticalHomeUrl)
    return {
      filters:
        buildFilterRequestFromCriteria(subCategory.activatedFilters ?? [])
          .filters ?? [],
      url: `/${home ? `${home}/` : ''}${slug.replace(/^\/+/, '')}`,
    }
  } catch (error) {
    console.error('Failed to load guide product grid subcategory', error)
    return null
  }
}

const resolveCategoryUrl = async (): Promise<string | null> => {
  try {
    const category = await fetchCategoryById(normalizedVertical.value)
    const home = categoryHome(category.verticalHomeUrl)
    return home ? `/${home}` : null
  } catch (error) {
    console.error('Failed to load guide product grid category', error)
    return null
  }
}

type GridData = {
  products: ProductDto[]
  seeAllUrl: string | null
  unknownSubcategory: boolean
}

const { data, pending } = await useAsyncData<GridData>(
  () =>
    `guide-product-grid:${normalizedVertical.value}:${normalizedSubcategory.value}:${topCount.value}:${props.sort}:${props.seeAllCategory}:${JSON.stringify(requestFilters.value)}`,
  async () => {
    const empty: GridData = {
      products: [],
      seeAllUrl: null,
      unknownSubcategory: false,
    }
    if (!normalizedVertical.value) {
      return empty
    }

    let subFilters: Filter[] = []
    let seeAllUrl: string | null = null
    if (normalizedSubcategory.value) {
      const resolved = await resolveSubcategory()
      if (!resolved) {
        console.warn(
          `guide-product-grid: unknown subcategory "${normalizedSubcategory.value}" for vertical "${normalizedVertical.value}"`
        )
        return { ...empty, unknownSubcategory: true }
      }
      subFilters = resolved.filters
      seeAllUrl = resolved.url
    } else if (props.seeAllCategory) {
      seeAllUrl = await resolveCategoryUrl()
    }
    const searchFilters = [...subFilters, ...requestFilters.value]

    try {
      const response = await csrfFetch<ProductCardSearchResponseDto>(
        '/api/products/cards',
        {
          method: 'POST',
          body: {
            verticalId: normalizedVertical.value,
            pageNumber: 0,
            pageSize: topCount.value,
            sort: sortRequest.value,
            ...(searchFilters.length
              ? { filters: { filters: searchFilters } }
              : {}),
          },
        }
      )

      return { ...empty, products: response.products?.data ?? [], seeAllUrl }
    } catch (error) {
      console.error('Failed to load guide product grid', error)
      return { ...empty, seeAllUrl }
    }
  },
  {
    watch: [
      normalizedVertical,
      normalizedSubcategory,
      topCount,
      () => props.sort,
      () => props.seeAllCategory,
      requestFilters,
    ],
  }
)

const products = computed(() => data.value?.products ?? [])
const seeAllUrl = computed(() => data.value?.seeAllUrl ?? null)
const isUnknownSubcategory = computed(
  () => data.value?.unknownSubcategory === true
)

const onWidgetClick = (event: MouseEvent) => {
  const target = (event.target as HTMLElement | null)?.closest?.('a[href]')
  if (!target) {
    return
  }

  trackEvent('product-widget-click', {
    props: {
      page: route.path,
      vertical: normalizedVertical.value,
      subcategory: normalizedSubcategory.value,
      target: target.closest('[data-widget-target="see-all"]')
        ? 'see-all'
        : 'product',
    },
  })
}
</script>

<style scoped lang="sass">
.guide-product-grid
  margin: 1.5rem 0

  &__see-all
    margin-top: 1rem
    text-align: center

  &__empty
    font-size: 0.85rem
    font-style: italic
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

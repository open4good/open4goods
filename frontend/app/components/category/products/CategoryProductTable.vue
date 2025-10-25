<template>
  <v-data-table
    :headers="headers"
    :items="rows"
    :items-per-page="itemsPerPage"
    class="category-product-table"
    density="comfortable"
    :fixed-header="true"
    height="600"
    :sort-by="internalSortBy"
    :item-class="() => 'category-product-table__row'"
    @update:sort-by="onSortByUpdate"
    @click:row="onRowClick"
  >
    <template #[`item.compare`]="{ item }">
      <CategoryProductCompareToggle :product="item.product" class="category-product-table__compare" />
    </template>

    <template #[`item.brand`]="{ value }">
      <span class="category-product-table__brand">{{ value ?? $t('category.products.unknownBrand') }}</span>
    </template>

    <template #[`item.model`]="{ value, item }">
      <div class="category-product-table__model">
        <div class="category-product-table__model-name">
          {{ value ?? item.product.identity?.bestName ?? '#' + (item.product.gtin ?? '') }}
        </div>
      </div>
    </template>

    <template #[`item.impactScore`]="{ value }">
      <ImpactScore v-if="value != null" :score="value" :max="5" size="small" />
      <span v-else>{{ $t('category.products.notRated') }}</span>
    </template>

    <template #[`item.bestPrice`]="{ value }">
      {{ value ?? $t('category.products.priceUnavailable') }}
    </template>

    <template #[`item.offersCount`]="{ item }">
      {{ offersCountLabel(item.product) }}
    </template>

    <template #[`item.popularAttributes`]="{ value }">
      <ul v-if="value.length" class="category-product-table__attributes" role="list">
        <li v-for="attribute in value" :key="attribute.key" class="category-product-table__attribute" role="listitem">
          <span class="category-product-table__attribute-label">{{ attribute.label }}</span>
          <span class="category-product-table__attribute-value">{{ attribute.value }}</span>
        </li>
      </ul>
      <span v-else class="category-product-table__empty">—</span>
    </template>

    <template #[`item.remainingAttributes`]="{ value }">
      <ul v-if="value.length" class="category-product-table__attributes" role="list">
        <li v-for="attribute in value" :key="attribute.key" class="category-product-table__attribute" role="listitem">
          <span class="category-product-table__attribute-label">{{ attribute.label }}</span>
          <span class="category-product-table__attribute-value">{{ attribute.value }}</span>
        </li>
      </ul>
      <span v-else class="category-product-table__empty">—</span>
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
  resolveRemainingAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'

interface TableAttribute {
  key: string
  label: string
  value: string
}

interface CategoryProductTableRow {
  product: ProductDto
  compare: null
  brand: string | null | undefined
  model: string | null | undefined
  impactScore: number | null
  bestPrice: string | null
  offersCount: number
  popularAttributes: TableAttribute[]
  remainingAttributes: TableAttribute[]
}

const props = defineProps<{
  products: ProductDto[]
  itemsPerPage: number
  popularAttributes?: AttributeConfigDto[]
  sortField?: string | null
  sortOrder?: 'asc' | 'desc'
}>()

const emit = defineEmits<{
  (event: 'update:sort-field', field: string | null): void
  (event: 'update:sort-order', order: 'asc' | 'desc'): void
}>()

const router = useRouter()
const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const headers = computed(() => [
  { key: 'compare', title: t('category.products.headers.compare'), sortable: false, width: 48 },
  { key: 'brand', title: t('category.products.headers.brand'), sortable: false, width: 140 },
  { key: 'model', title: t('category.products.headers.model'), sortable: false, minWidth: 200 },
  { key: 'impactScore', title: t('category.products.headers.impactScore'), sortable: false, width: 140 },
  {
    key: 'bestPrice',
    title: t('category.products.headers.bestPrice'),
    sortable: true,
    width: 140,
  },
  {
    key: 'offersCount',
    title: t('category.products.headers.offers'),
    sortable: true,
    width: 140,
  },
  { key: 'popularAttributes', title: t('category.products.headers.popularAttributes'), sortable: false, minWidth: 220 },
  {
    key: 'remainingAttributes',
    title: t('category.products.headers.remainingAttributes'),
    sortable: false,
    minWidth: 220,
  },
])

const sortHeaderToFieldMapping: Record<string, string> = {
  bestPrice: 'price.minPrice.price',
  offersCount: 'offersCount',
}

const fieldToSortHeader = Object.fromEntries(Object.entries(sortHeaderToFieldMapping).map(([key, value]) => [value, key]))

const internalSortBy = computed(() => {
  const field = props.sortField ?? null
  const headerKey = field ? (fieldToSortHeader[field] ?? null) : null

  if (!headerKey) {
    return []
  }

  return [
    {
      key: headerKey,
      order: props.sortOrder ?? 'desc',
    },
  ]
})

const bestPriceLabel = (product: ProductDto) => formatBestPrice(product, t, n)
const offersCountLabel = (product: ProductDto) => formatOffersCount(product, translatePlural)

const formatAttributes = (attributes: ReturnType<typeof resolvePopularAttributes>) => {
  return attributes
    .map((attribute) => {
      const value = formatAttributeValue(attribute, t, n)

      if (!value) {
        return null
      }

      return {
        key: attribute.key,
        label: attribute.label,
        value,
      }
    })
    .filter((attribute): attribute is TableAttribute => attribute != null)
}

const formatRemainingAttributes = (attributes: ReturnType<typeof resolveRemainingAttributes>) => {
  return attributes
    .map((attribute) => {
      const value = formatAttributeValue(attribute, t, n)

      if (!value) {
        return null
      }

      return {
        key: attribute.key,
        label: attribute.label,
        value,
      }
    })
    .filter((attribute): attribute is TableAttribute => attribute != null)
}

const rows = computed<CategoryProductTableRow[]>(() => {
  return props.products.map((product) => {
    const popular = formatAttributes(resolvePopularAttributes(product, popularAttributeConfigs.value))
    const remaining = formatRemainingAttributes(
      resolveRemainingAttributes(
        product,
        popularAttributeConfigs.value.map((attribute) => attribute.key ?? '').filter(Boolean),
      ),
    )

    return {
      product,
      compare: null,
      brand: product.identity?.brand,
      model: product.identity?.model ?? product.identity?.bestName ?? product.identity?.brand,
      impactScore: resolvePrimaryImpactScore(product),
      bestPrice: bestPriceLabel(product),
      offersCount: product.offers?.offersCount ?? 0,
      popularAttributes: popular,
      remainingAttributes: remaining,
    }
  })
})

const onSortByUpdate = (sortBy: { key: string; order?: 'asc' | 'desc' }[]) => {
  const entry = sortBy[0]

  if (!entry) {
    emit('update:sort-field', null)
    emit('update:sort-order', 'desc')
    return
  }

  const field = sortHeaderToFieldMapping[entry.key]

  if (!field) {
    return
  }

  emit('update:sort-field', field)
  emit('update:sort-order', entry.order ?? 'asc')
}

const onRowClick = (_event: MouseEvent, item: { item: CategoryProductTableRow }) => {
  const product = item.item.product
  const target = product.fullSlug ?? product.slug

  if (target) {
    router.push(target)
  }
}
</script>

<style scoped lang="sass">
.category-product-table
  width: 100%

  :deep(.v-data-table__tr)
    cursor: pointer

    &:nth-child(even)
      background-color: rgba(var(--v-theme-surface-muted), 0.6)

    &:hover
      background-color: rgba(var(--v-theme-surface-muted), 0.9)

  &__brand
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__model
    display: flex
    flex-direction: column
    gap: 0.25rem

  &__model-name
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attributes
    display: grid
    gap: 0.4rem
    padding: 0
    margin: 0
    list-style: none

  &__attribute
    display: flex
    gap: 0.4rem
    align-items: baseline

  &__attribute-label
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attribute-value
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__empty
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__compare :deep(.v-btn)
    margin-inline: auto
</style>

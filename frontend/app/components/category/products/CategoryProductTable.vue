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
      <CategoryProductCompareToggle
        :product="item.product"
        size="compact"
        class="category-product-table__compare"
      />
    </template>

    <template #[`item.brand`]="{ value }">
      <span class="category-product-table__brand">{{
        value ?? $t('category.products.unknownBrand')
      }}</span>
    </template>

    <template #[`item.model`]="{ value, item }">
      <div class="category-product-table__model">
        <div class="category-product-table__model-name">
          {{
            value ??
            item.product.identity?.bestName ??
            '#' + (item.product.gtin ?? '')
          }}
        </div>
      </div>
    </template>

    <template #[`item.impactScore`]="{ value }">
      <ImpactScore
        v-if="value != null"
        :score="value"
        :max="5"
        size="small"
        flat
      />
      <span v-else>{{ $t('category.products.notRated') }}</span>
    </template>

    <template #[`item.bestPrice`]="{ value }">
      {{ value ?? $t('category.products.priceUnavailable') }}
    </template>

    <template #[`item.offersCount`]="{ item }">
      {{ offersCountLabel(item.product) }}
    </template>

    <template
      v-for="column in attributeColumns"
      :key="column.headerKey"
      #[`item.${column.headerKey}`]="{ value }"
    >
      <span v-if="value">{{ value }}</span>
      <span v-else class="category-product-table__empty">—</span>
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  AttributeConfigDto,
  FieldMetadataDto,
  ProductDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import {
  formatAttributeValue,
  resolveAttributeRawValueByKey,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

type AttributeCellValue = string | null

interface CategoryProductTableRowBase {
  product: ProductDto
  compare: null
  brand: string | null | undefined
  model: string | null | undefined
  impactScore: number | null
  bestPrice: string | null
  offersCount: number
}

interface CategoryProductTableRow extends CategoryProductTableRowBase {
  [key: string]:
    | AttributeCellValue
    | ProductDto
    | string
    | number
    | null
    | undefined
}

interface AttributeColumn {
  key: string
  headerKey: string
  title: string
  mapping: string
  config?: AttributeConfigDto
}

const ATTRIBUTE_COLUMN_PREFIX = 'attribute:'
const attributeHeaderKey = (key: string) => `${ATTRIBUTE_COLUMN_PREFIX}${key}`

const props = defineProps<{
  products: ProductDto[]
  itemsPerPage: number
  popularAttributes?: AttributeConfigDto[]
  sortField?: string | null
  sortOrder?: 'asc' | 'desc'
  attributeKeys?: string[]
  attributeConfigs?: Record<string, AttributeConfigDto>
  fieldMetadata?: Record<string, FieldMetadataDto>
}>()

const emit = defineEmits<{
  (event: 'update:sort-field', field: string | null): void
  (event: 'update:sort-order', order: 'asc' | 'desc'): void
}>()

const router = useRouter()
const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const attributeConfigMap = computed<Record<string, AttributeConfigDto>>(
  () => props.attributeConfigs ?? {}
)
const attributeKeys = computed(() => props.attributeKeys ?? [])
const fieldMetadataMap = computed<Record<string, FieldMetadataDto>>(
  () => props.fieldMetadata ?? {}
)

const extractAttributeKeyFromMapping = (
  mapping: string | undefined
): string | null => {
  if (!mapping) {
    return null
  }

  const patterns = [
    /attributes\.(?:indexed(?:Attributes)?|referential(?:Attributes)?)\.([^.]+)/i,
    /attributes\.([^.]+)/i,
  ]

  for (const pattern of patterns) {
    const match = mapping.match(pattern)
    if (match?.[1]) {
      return match[1]
    }
  }

  return null
}

const attributeFieldMappingByKey = computed<Record<string, string>>(() => {
  const entries = Object.entries(fieldMetadataMap.value ?? {})
  return entries.reduce<Record<string, string>>((accumulator, [mapping]) => {
    const key = extractAttributeKeyFromMapping(mapping)

    if (key && !(key in accumulator)) {
      accumulator[key] = mapping
    }

    return accumulator
  }, {})
})

const guessAttributeMapping = (
  key: string,
  config: AttributeConfigDto | undefined
): string => {
  const base = `attributes.indexed.${key}`

  switch (config?.filteringType) {
    case 'NUMERIC':
      return `${base}.numericValue`
    case 'BOOLEAN':
      return `${base}.booleanValue`
    default:
      return `${base}.value`
  }
}

const attributeColumns = computed<AttributeColumn[]>(() => {
  const seen = new Set<string>()

  return attributeKeys.value.reduce<AttributeColumn[]>((accumulator, key) => {
    const normalizedKey = String(key)

    if (!normalizedKey || seen.has(normalizedKey)) {
      return accumulator
    }

    seen.add(normalizedKey)

    const config = attributeConfigMap.value?.[normalizedKey]
    const mapping =
      attributeFieldMappingByKey.value[normalizedKey] ??
      guessAttributeMapping(normalizedKey, config)
    const field = fieldMetadataMap.value?.[mapping]
    const title =
      config?.name ??
      resolveFilterFieldTitle(field, t, mapping) ??
      normalizedKey

    accumulator.push({
      key: normalizedKey,
      headerKey: attributeHeaderKey(normalizedKey),
      title,
      mapping,
      config,
    })

    return accumulator
  }, [])
})

const headers = computed(() => [
  {
    key: 'compare',
    title: t('category.products.headers.compare'),
    sortable: false,
    width: 48,
  },
  {
    key: 'brand',
    title: t('category.products.headers.brand'),
    sortable: true,
    width: 140,
  },
  {
    key: 'model',
    title: t('category.products.headers.model'),
    sortable: true,
    minWidth: 200,
  },
  {
    key: 'impactScore',
    title: t('category.products.headers.impactScore'),
    sortable: true,
    width: 140,
  },
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
  ...attributeColumns.value.map(column => ({
    key: column.headerKey,
    title: column.title,
    sortable: true,
    minWidth: 160,
  })),
])

const staticSortHeaderToFieldMapping: Record<string, string> = {
  brand: 'attributes.referentielAttributes.BRAND',
  model: 'attributes.referentielAttributes.MODEL',
  impactScore: ECOSCORE_RELATIVE_FIELD,
  bestPrice: 'price.minPrice.price',
  offersCount: 'offersCount',
}

const sortHeaderToFieldMapping = computed<Record<string, string>>(() => {
  const mapping: Record<string, string> = { ...staticSortHeaderToFieldMapping }

  attributeColumns.value.forEach(column => {
    if (column.mapping) {
      mapping[column.headerKey] = column.mapping
    }
  })

  return mapping
})

const fieldToSortHeader = computed<Record<string, string>>(() =>
  Object.fromEntries(
    Object.entries(sortHeaderToFieldMapping.value).map(([header, field]) => [
      field,
      header,
    ])
  )
)

const internalSortBy = computed(() => {
  const field = props.sortField ?? null
  const headerKey = field ? (fieldToSortHeader.value[field] ?? null) : null

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
const offersCountLabel = (product: ProductDto) =>
  formatOffersCount(product, translatePlural)

const resolveAttributeCellValue = (
  product: ProductDto,
  column: AttributeColumn
): string | null => {
  const rawValue = resolveAttributeRawValueByKey(product, column.key)

  return formatAttributeValue(
    {
      key: column.key,
      label: column.config?.name ?? column.key,
      rawValue,
      unit: column.config?.unit,
      icon: column.config?.icon,
      suffix: column.config?.suffix ?? null,
    },
    t,
    n
  )
}

const rows = computed<CategoryProductTableRow[]>(() => {
  return props.products.map(product => {
    const baseRow: CategoryProductTableRow = {
      product,
      compare: null,
      brand: product.identity?.brand,
      model:
        product.identity?.model ??
        product.identity?.bestName ??
        product.identity?.brand,
      impactScore: resolvePrimaryImpactScore(product),
      bestPrice: bestPriceLabel(product),
      offersCount: product.offers?.offersCount ?? 0,
    }

    attributeColumns.value.forEach(column => {
      baseRow[column.headerKey] = resolveAttributeCellValue(product, column)
    })

    return baseRow
  })
})

const onSortByUpdate = (sortBy: { key: string; order?: 'asc' | 'desc' }[]) => {
  const entry = sortBy[0]

  if (!entry) {
    emit('update:sort-field', null)
    emit('update:sort-order', 'desc')
    return
  }

  const field = sortHeaderToFieldMapping.value[entry.key]

  if (!field) {
    return
  }

  emit('update:sort-field', field)
  emit('update:sort-order', entry.order ?? 'asc')
}

const onRowClick = (
  _event: MouseEvent,
  item: { item: CategoryProductTableRow }
) => {
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

  &__empty
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__compare :deep(.v-btn)
    margin-inline: auto
</style>

<template>
  <v-data-table
    :headers="headers"
    :items="rows"
    :items-per-page="itemsPerPage"
    class="category-product-table"
    density="comfortable"
    :fixed-header="true"
    height="600"
  >
    <template #item.brand="{ value }">
      <span class="category-product-table__brand">{{ value ?? $t('category.products.unknownBrand') }}</span>
    </template>

    <template #item.model="{ value, item }">
      <div class="category-product-table__model">
        <div class="category-product-table__model-name">
          {{ value ?? item.identity?.bestName ?? '#' + (item.gtin ?? '') }}
        </div>
        <div class="category-product-table__model-meta">
          <v-icon icon="mdi-cash" size="16" class="me-1" />
          {{ bestPriceLabel(item) }}
        </div>
      </div>
    </template>

    <template #item.ecoscore="{ value }">
      <v-chip v-if="value" size="small" color="success" variant="flat">
        {{ value }}
      </v-chip>
      <span v-else>{{ $t('category.products.notRated') }}</span>
    </template>

    <template #item.bestPrice="{ value }">
      {{ value ?? $t('category.products.priceUnavailable') }}
    </template>

    <template #item.offersCount="{ value }">
      {{ $t('category.products.offerCount', value ?? 0, { count: value ?? 0 }) }}
    </template>

  </v-data-table>
</template>

<script setup lang="ts">
import type { FieldMetadataDto, ProductDto } from '~~/shared/api-client'

const props = defineProps<{
  products: ProductDto[]
  fields: FieldMetadataDto[]
  itemsPerPage: number
}>()

const { t } = useI18n()

const baseHeaders = computed(() => [
  { key: 'brand', title: t('category.products.headers.brand'), sortable: false },
  { key: 'model', title: t('category.products.headers.model'), sortable: false },
  { key: 'ecoscore', title: t('category.products.headers.ecoscore'), sortable: false },
  { key: 'bestPrice', title: t('category.products.headers.bestPrice'), sortable: false },
  { key: 'offersCount', title: t('category.products.headers.offers'), sortable: false },
])

const dynamicHeaders = computed(() => {
  const seen = new Set<string>()

  return props.fields
    .filter((field) => field.mapping && !seen.has(field.mapping))
    .map((field) => {
      seen.add(field.mapping as string)
      return {
        key: field.mapping,
        title: field.title ?? field.mapping,
        sortable: false,
      }
    })
})

const headers = computed(() => [...baseHeaders.value, ...dynamicHeaders.value])

const getValueByPath = (product: ProductDto, path: string | undefined) => {
  if (!path) {
    return undefined
  }

  return path.split('.').reduce<any>((accumulator, segment) => {
    if (accumulator == null) {
      return undefined
    }

    if (segment in accumulator) {
      return accumulator[segment as keyof typeof accumulator]
    }

    return undefined
  }, product)
}

const ecoscoreLabel = (product: ProductDto) => {
  const letter =
    product.scores?.ecoscore?.letter ??
    product.scores?.scores?.['scores.ECOSCORE.value']?.letter ??
    product.scores?.scores?.['ECOSCORE']?.letter

  const value =
    product.scores?.ecoscore?.percent ??
    product.scores?.scores?.['scores.ECOSCORE.value']?.percent ??
    product.scores?.scores?.['ECOSCORE']?.percent

  if (!letter) {
    return null
  }

  return value != null ? `${letter} (${Math.round(value)}%)` : letter
}

const bestPriceLabel = (product: ProductDto) => {
  return (
    product.offers?.bestPrice?.shortPrice ??
    (product.offers?.bestPrice?.price != null
      ? `${product.offers?.bestPrice?.price} ${product.offers?.bestPrice?.currency ?? ''}`
      : t('category.products.priceUnavailable'))
  )
}

const rows = computed(() => {
  return props.products.map((product) => {
    const row: Record<string, unknown> = {
      ...product,
      brand: product.identity?.brand,
      model: product.identity?.model ?? product.identity?.bestName ?? product.identity?.brand,
      ecoscore: ecoscoreLabel(product),
      bestPrice: bestPriceLabel(product),
      offersCount: product.offers?.offersCount ?? 0,
    }

    dynamicHeaders.value.forEach((header) => {
      const value = getValueByPath(product, header.key)
      row[header.key] = typeof value === 'number' ? value : value ?? null
    })

    return row
  })
})
</script>

<style scoped lang="sass">
.category-product-table
  width: 100%

  :deep(.v-data-table__tr)
    &:nth-child(even)
      background-color: rgba(var(--v-theme-surface-muted), 0.6)

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

  &__model-meta
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

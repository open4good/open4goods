<template>
  <v-list class="category-product-list" lines="three" density="comfortable">
    <v-list-item
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
      class="category-product-list__item"
    >
      <template #prepend>
        <v-avatar size="88" rounded="lg">
          <v-img
            :src="resolveImage(product)"
            :alt="product.identity?.bestName ?? product.identity?.model ?? $t('category.products.untitledProduct')"
            cover
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>
        </v-avatar>
      </template>

      <div class="category-product-list__content">
        <div class="category-product-list__header">
          <span class="category-product-list__brand">
            {{ product.identity?.brand ?? $t('category.products.unknownBrand') }}
          </span>
          <v-chip v-if="ecoscoreLetter(product)" size="small" color="success" variant="flat">
            {{ $t('category.products.ecoscoreLabel', { letter: ecoscoreLetter(product) }) }}
          </v-chip>
        </div>
        <h3 class="category-product-list__title">
          {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
        </h3>
        <div class="category-product-list__meta">
          <span>
            <v-icon icon="mdi-cash" size="18" class="me-1" />
            {{ bestPriceLabel(product) }}
          </span>
          <span>
            <v-icon icon="mdi-store" size="18" class="me-1" />
            {{ offersCountLabel(product) }}
          </span>
        </div>
      </div>
    </v-list-item>
  </v-list>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'

defineProps<{ products: ProductDto[] }>()

const { t } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const ecoscoreLetter = (product: ProductDto) => {
  return (
    product.scores?.ecoscore?.letter ??
    product.scores?.scores?.['scores.ECOSCORE.value']?.letter ??
    product.scores?.scores?.['ECOSCORE']?.letter ??
    null
  )
}

const bestPriceLabel = (product: ProductDto) => {
  return (
    product.offers?.bestPrice?.shortPrice ??
    (product.offers?.bestPrice?.price != null
      ? `${product.offers?.bestPrice?.price} ${product.offers?.bestPrice?.currency ?? ''}`
      : t('category.products.priceUnavailable'))
  )
}

const offersCountLabel = (product: ProductDto) => {
  const count = product.offers?.offersCount ?? 0
  return translatePlural('category.products.offerCount', count)
}
</script>

<style scoped lang="sass">
.category-product-list
  background: transparent

  &__item
    background: rgb(var(--v-theme-surface-default))
    border-radius: 1rem
    margin-bottom: 1rem
    padding-inline: 1rem

  &__content
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__header
    display: flex
    gap: 0.75rem
    align-items: center
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__meta
    display: flex
    gap: 1rem
    flex-wrap: wrap
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

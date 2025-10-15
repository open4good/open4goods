<template>
  <v-row class="category-product-card-grid" dense>
    <v-col
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
      cols="12"
      sm="6"
      lg="4"
    >
      <v-card class="category-product-card-grid__card" rounded="xl" elevation="2">
        <v-img
          :src="resolveImage(product)"
          :alt="product.identity?.bestName ?? product.identity?.model ?? $t('category.products.untitledProduct')"
          height="200"
          cover
          class="category-product-card-grid__image"
        >
          <template #placeholder>
            <v-skeleton-loader type="image" class="h-100" />
          </template>
        </v-img>

        <v-card-item class="category-product-card-grid__body">
          <div class="category-product-card-grid__eyebrow">
            <span>{{ product.identity?.brand ?? $t('category.products.unknownBrand') }}</span>
            <v-chip
              v-if="ecoscoreLetter(product)"
              size="small"
              color="success"
              variant="flat"
              class="ms-auto"
            >
              {{ $t('category.products.ecoscoreLabel', { letter: ecoscoreLetter(product) }) }}
            </v-chip>
          </div>

          <h3 class="category-product-card-grid__title">
            {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
          </h3>

          <div class="category-product-card-grid__meta">
            <v-chip
              color="primary"
              variant="tonal"
              size="small"
              class="me-2"
            >
              <v-icon icon="mdi-cash" size="18" class="me-1" />
              {{ bestPriceLabel(product) }}
            </v-chip>
            <span class="category-product-card-grid__offers">
              <v-icon icon="mdi-store" size="18" class="me-1" />
              {{ offersCountLabel(product) }}
            </span>
          </div>
        </v-card-item>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'

const props = defineProps<{ products: ProductDto[] }>()

const { t } = useI18n()

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
  return t('category.products.offerCount', count, { count })
}
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &__card
    height: 100%
    display: flex
    flex-direction: column

  &__image
    border-top-left-radius: inherit
    border-top-right-radius: inherit

  &__body
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__eyebrow
    display: flex
    align-items: center
    gap: 0.5rem
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__title
    font-size: 1.125rem
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__meta
    display: flex
    align-items: center
    gap: 0.5rem
    flex-wrap: wrap

  &__offers
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

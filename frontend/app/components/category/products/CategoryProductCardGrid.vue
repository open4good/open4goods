<template>
  <v-row class="category-product-card-grid" dense>
    <v-col
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
      cols="12"
      sm="6"
      lg="4"
    >
      <v-card
        class="category-product-card-grid__card"
        rounded="xl"
        elevation="2"
        :to="productLink(product)"
        link
      >
        <div class="category-product-card-grid__media">
          <v-img
            :src="resolveImage(product)"
            :alt="product.identity?.bestName ?? product.identity?.model ?? $t('category.products.untitledProduct')"
            :aspect-ratio="4 / 3"
            contain
            class="category-product-card-grid__image"
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>
        </div>

        <v-card-item class="category-product-card-grid__body">
          <div class="category-product-card-grid__eyebrow">
            <span class="category-product-card-grid__brand">
              {{ product.identity?.brand ?? $t('category.products.unknownBrand') }}
            </span>
            <ImpactScore
              v-if="impactScoreValue(product) !== null"
              :score="impactScoreValue(product) ?? 0"
              :max="5"
              size="small"
              class="ms-auto"
            />
            <span
              v-else-if="ecoscoreLetter(product)"
              class="category-product-card-grid__score-letter ms-auto"
            >
              {{ $t('category.products.ecoscoreLabel', { letter: ecoscoreLetter(product) }) }}
            </span>
          </div>

          <h3 class="category-product-card-grid__title">
            {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
          </h3>

          <div class="category-product-card-grid__meta">
            <div class="category-product-card-grid__price">
              <v-icon icon="mdi-cash" size="20" class="category-product-card-grid__price-icon" />
              <span class="category-product-card-grid__price-value">{{ bestPriceLabel(product) }}</span>
            </div>
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
import { defineAsyncComponent } from 'vue'
import type { ProductDto } from '~~/shared/api-client'

const ImpactScore = defineAsyncComponent(() => import('~/components/shared/ui/ImpactScore.vue'))

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

const impactScoreValue = (product: ProductDto) => {
  const ecoscore =
    product.scores?.ecoscore ??
    product.scores?.scores?.['scores.ECOSCORE.value'] ??
    product.scores?.scores?.['ECOSCORE'] ??
    null

  if (!ecoscore) {
    return null
  }

  if (typeof ecoscore.percent === 'number' && Number.isFinite(ecoscore.percent)) {
    return (ecoscore.percent / 100) * 5
  }

  if (typeof ecoscore.on20 === 'number' && Number.isFinite(ecoscore.on20)) {
    return (ecoscore.on20 / 20) * 5
  }

  if (typeof ecoscore.value === 'number' && Number.isFinite(ecoscore.value)) {
    return ecoscore.value
  }

  return null
}

const bestPriceLabel = (product: ProductDto) => {
  const price = product.offers?.bestPrice?.price
  const currency = product.offers?.bestPrice?.currency
  const shortPrice = product.offers?.bestPrice?.shortPrice

  if (price != null && currency) {
    try {
      return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(price)
    }
    catch {
      return `${price} ${currency}`
    }
  }

  if (shortPrice) {
    return shortPrice
  }

  return t('category.products.priceUnavailable')
}

const offersCountLabel = (product: ProductDto) => {
  const count = product.offers?.offersCount ?? 0
  return translatePlural('category.products.offerCount', count)
}

const productLink = (product: ProductDto) => {
  const slug = product.fullSlug ?? product.slug

  if (!slug) {
    return undefined
  }

  return slug.startsWith('/') ? slug : `/${slug}`
}
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &__card
    height: 100%
    display: flex
    flex-direction: column
    text-decoration: none

    &:hover,
    &:focus-visible
      box-shadow: 0 16px 24px -12px rgba(var(--v-theme-shadow-primary-600), 0.3)

  &__media
    background: rgb(var(--v-theme-surface-default))
    padding: clamp(0.75rem, 1.5vw, 1.25rem)
    border-top-left-radius: inherit
    border-top-right-radius: inherit
    display: flex
    align-items: center
    justify-content: center

  &__image
    border-radius: 0
    background: #fff
    width: 100%

    :deep(img)
      object-fit: contain
      padding: clamp(0.25rem, 1vw, 0.75rem)
      width: 100%
      height: 100%

  &__body
    display: flex
    flex-direction: column
    gap: 0.75rem
    background: rgb(var(--v-theme-surface-muted))
    border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
    border-bottom-left-radius: inherit
    border-bottom-right-radius: inherit
    padding: 1rem 1.25rem

  &__brand
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))
    text-transform: uppercase
    letter-spacing: 0.05em
    font-size: 0.75rem

  &__eyebrow
    display: flex
    align-items: center
    gap: 0.5rem
    min-height: 1.5rem

  &__title
    font-size: 1.125rem
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis

  &__meta
    display: flex
    align-items: center
    gap: 0.5rem
    flex-wrap: wrap
    justify-content: space-between

  &__price
    display: flex
    align-items: center
    gap: 0.4rem
    font-size: 1.125rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__price-icon
    color: rgba(var(--v-theme-text-neutral-soft), 0.8)

  &__offers
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__score-letter
    font-size: 0.75rem
    font-weight: 600
    padding: 0.25rem 0.5rem
    border-radius: 999px
    background: rgba(var(--v-theme-accent-supporting), 0.12)
    color: rgb(var(--v-theme-accent-supporting))
</style>

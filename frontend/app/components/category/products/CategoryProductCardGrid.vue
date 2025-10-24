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
        hover
        :to="productLink(product)"
      >
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

        <v-card-item class="category-product-card-grid__body">
          <h3 class="category-product-card-grid__title">
            {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
          </h3>

          <div class="category-product-card-grid__score" role="presentation">
            <ImpactScore
              v-if="impactScoreValue(product) != null"
              :score="impactScoreValue(product) ?? 0"
              :max="5"
              size="small"
            />
            <span v-else class="category-product-card-grid__score-fallback">
              {{ $t('category.products.notRated') }}
            </span>
          </div>

          <div class="category-product-card-grid__meta">
            <span class="category-product-card-grid__price">
              {{ bestPriceLabel(product) }}
            </span>
            <span class="category-product-card-grid__offers">
              {{ offersCountLabel(product) }}
            </span>
          </div>
        </v-card-item>

        <v-card-actions class="category-product-card-grid__actions">
          <v-btn
            class="category-product-card-grid__compare"
            :class="{ 'category-product-card-grid__compare--active': isProductSelected(product) }"
            :variant="isProductSelected(product) ? 'flat' : 'outlined'"
            color="primary"
            size="small"
            :title="compareTooltip(product)"
            :aria-pressed="isProductSelected(product)"
            :aria-label="compareAriaLabel(product)"
            :disabled="isCompareDisabled(product)"
            data-test="category-product-compare"
            @click.stop.prevent="toggleCompare(product)"
          >
            {{ compareLabel(product) }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import {
  MAX_COMPARE_ITEMS,
  useProductCompareStore,
  type CompareListBlockReason,
} from '~/stores/useProductCompareStore'

defineProps<{ products: ProductDto[] }>()

const { t, n, te } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const compareStore = useProductCompareStore()

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const bestPriceLabel = (product: ProductDto) => {
  const price = product.offers?.bestPrice?.price
  const currency = product.offers?.bestPrice?.currency

  if (price == null) {
    return t('category.products.priceUnavailable')
  }

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const offersCountLabel = (product: ProductDto) => {
  const count = product.offers?.offersCount ?? 0
  return translatePlural('category.products.offerCount', count)
}

const productLink = (product: ProductDto) => {
  return product.fullSlug ?? product.slug ?? undefined
}

const impactScoreValue = (product: ProductDto) => {
  const scores = product.scores?.scores

  if (!scores) {
    return null
  }

  const preferredKeys = [
    'ECOSCORE',
  ]

  const impactEntry =
    preferredKeys.map((key) => scores[key]).find((entry) => entry != null) ??
    Object.values(scores).find((entry) => entry?.id?.toLowerCase()?.includes('impact')) ??
    null

  if (!impactEntry) {
    return null
  }

  const normalize = (score: number) => Math.max(0, Math.min(score, 5))

  if (impactEntry.on20 != null && Number.isFinite(impactEntry.on20)) {
    return normalize((impactEntry.on20 / 20) * 5)
  }

  if (impactEntry.percent != null && Number.isFinite(impactEntry.percent)) {
    return normalize((impactEntry.percent / 100) * 5)
  }

  if (impactEntry.value != null && impactEntry.absolute?.max) {
    const max = impactEntry.absolute.max

    if (Number.isFinite(max) && max > 0) {
      return normalize((impactEntry.value / max) * 5)
    }
  }

  if (impactEntry.value != null && Number.isFinite(impactEntry.value)) {
    return normalize(impactEntry.value)
  }

  return null
}

const productDisplayName = (product: ProductDto) => {
  return (
    product.identity?.bestName ??
    product.base?.bestName ??
    product.identity?.model ??
    product.identity?.brand ??
    t('category.products.untitledProduct')
  )
}

const isProductSelected = (product: ProductDto) => compareStore.hasProduct(product)

const compareLabel = (product: ProductDto) =>
  isProductSelected(product)
    ? t('category.products.compare.removeFromList')
    : t('category.products.compare.addToList')

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', { count: MAX_COMPARE_ITEMS })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return null
  }
}

const isCompareDisabled = (product: ProductDto) => {
  if (isProductSelected(product)) {
    return false
  }

  const eligibility = compareStore.canAddProduct(product)
  return !eligibility.success
}

const compareTooltip = (product: ProductDto) => {
  if (isProductSelected(product)) {
    return t('category.products.compare.removeFromList')
  }

  const eligibility = compareStore.canAddProduct(product)

  if (!eligibility.success) {
    return reasonMessage(eligibility.reason) ?? t('category.products.compare.addToList')
  }

  return t('category.products.compare.addToList')
}

const compareAriaLabel = (product: ProductDto) => {
  if (isProductSelected(product)) {
    if (te('category.products.compare.removeSingle')) {
      return t('category.products.compare.removeSingle', { name: productDisplayName(product) })
    }

    return t('category.products.compare.removeFromList')
  }

  const eligibility = compareStore.canAddProduct(product)
  return reasonMessage(eligibility.reason) ?? t('category.products.compare.addToList')
}

const toggleCompare = (product: ProductDto) => {
  if (isCompareDisabled(product) && !isProductSelected(product)) {
    return
  }

  compareStore.toggleProduct(product)
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
    transition: transform 0.2s ease, box-shadow 0.2s ease

    &:hover
      transform: translateY(-4px)
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)

  &__image
    border-top-left-radius: inherit
    border-top-right-radius: inherit
    background: #fff
    display: flex
    align-items: center
    justify-content: center

    :deep(img)
      object-fit: contain
      mix-blend-mode: multiply
      background: #fff

  &__body
    display: flex
    flex-direction: column
    gap: 0.75rem
    align-items: center
    text-align: center
    padding: 1.25rem
    background: rgb(var(--v-theme-surface-glass))
    border-bottom-left-radius: inherit
    border-bottom-right-radius: inherit

  &__actions
    padding: 0 1.25rem 1.25rem
    justify-content: center

  &__compare
    text-transform: none
    font-weight: 600

    &--active
      color: rgb(var(--v-theme-text-on-accent))

  &__title
    font-size: 1.125rem
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    width: 100%
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis

  &__meta
    display: flex
    flex-direction: column
    align-items: center
    gap: 0.25rem

  &__offers
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__price
    font-size: 1.35rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__score
    display: flex
    align-items: center
    justify-content: center
    min-height: 1.75rem

  &__score-fallback
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

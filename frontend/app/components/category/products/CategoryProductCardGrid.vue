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
          :transition="false"
          contain
          class="category-product-card-grid__image"
        />

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
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'

defineProps<{ products: ProductDto[] }>()

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

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
    position: relative
    overflow: hidden

    &.v-img--booting
      background: linear-gradient(
        90deg,
        rgba(var(--v-theme-surface-glass), 0.35),
        rgba(var(--v-theme-surface-glass), 0.75),
        rgba(var(--v-theme-surface-glass), 0.35)
      )
      background-size: 200% 100%
      animation: category-product-card-grid-image-skeleton 1.2s ease-in-out infinite

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

@keyframes category-product-card-grid-image-skeleton
  0%
    background-position: 200% 0
  100%
    background-position: -200% 0
</style>

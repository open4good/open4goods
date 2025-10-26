<template>
  <v-row
    class="category-product-card-grid"
    :class="[`category-product-card-grid--size-${cardSize}`]"
    dense
  >
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
        <div class="category-product-card-grid__compare">
          <CategoryProductCompareToggle :product="product" size="compact" />
        </div>

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
          <div class="category-product-card-grid__header">
            <h3 class="category-product-card-grid__title">
              {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
            </h3>
          </div>

          <div
            v-if="popularAttributesByProduct(product).length"
            class="category-product-card-grid__attributes"
            role="list"
          >
            <v-chip
              v-for="attribute in popularAttributesByProduct(product)"
              :key="attribute.key"
              size="small"
              class="category-product-card-grid__attribute"
              variant="flat"
              color="surface-primary-080"
              role="listitem"
            >
              <v-icon
                v-if="attribute.icon"
                :icon="attribute.icon"
                size="16"
                class="me-1 category-product-card-grid__attribute-icon"
              />
              <span class="category-product-card-grid__attribute-value">
                {{ attribute.value }}
              </span>
            </v-chip>
          </div>

          <div class="category-product-card-grid__score" role="presentation">
            <ImpactScore
              v-if="impactScoreValue(product) != null"
              :score="impactScoreValue(product) ?? 0"
              :max="5"
              size="medium"
            />
            <span v-else class="category-product-card-grid__score-fallback">
              {{ $t('category.products.notRated') }}
            </span>
          </div>

          <div
            v-if="offerBadges(product).length"
            class="category-product-card-grid__pricing"
            role="list"
          >
            <div
              v-for="badge in offerBadges(product)"
              :key="badge.key"
              class="category-product-card-grid__price-badge"
              :class="`category-product-card-grid__price-badge--${badge.appearance}`"
              role="listitem"
            >
              <span class="category-product-card-grid__price-badge-label">{{ badge.label }}</span>
              <span class="category-product-card-grid__price-badge-amount">{{ badge.price }}</span>
              <span class="category-product-card-grid__price-badge-condition">{{ badge.conditionText }}</span>
            </div>
          </div>

          <div class="category-product-card-grid__meta">
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
import { computed } from 'vue'
import type {
  AttributeConfigDto,
  ProductAggregatedPriceDto,
  ProductDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import { formatAttributeValue, resolvePopularAttributes } from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  size?: 'compact' | 'comfortable'
}>()

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])
const cardSize = computed(() => props.size ?? 'comfortable')

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const productLink = (product: ProductDto) => {
  return product.fullSlug ?? product.slug ?? undefined
}

const impactScoreValue = (product: ProductDto) => resolvePrimaryImpactScore(product)

const bestPriceLabel = (product: ProductDto) => formatBestPrice(product, t, n)

const offersCountLabel = (product: ProductDto) => formatOffersCount(product, translatePlural)

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = (product: ProductDto): DisplayedAttribute[] => {
  const attributes = resolvePopularAttributes(product, popularAttributeConfigs.value)
  const entries: DisplayedAttribute[] = []

  attributes.forEach((attribute) => {
    const value = formatAttributeValue(attribute, t, n)

    if (!value) {
      return
    }

    entries.push({
      key: attribute.key,
      label: attribute.label,
      value,
      icon: attribute.icon ?? null,
    })
  })

  return entries
}

const conditionLabel = (condition?: string | null) => {
  if (!condition) {
    return t('category.products.conditions.unknown')
  }

  const normalized = condition.toUpperCase()

  if (normalized === 'NEW') {
    return t('category.products.conditions.new')
  }

  if (normalized === 'OCCASION' || normalized === 'SECOND_HAND') {
    return t('category.products.conditions.occasion')
  }

  return t('category.products.conditions.other', { condition })
}

const formatOfferPrice = (
  offer: ProductAggregatedPriceDto | undefined,
  fallback: ProductDto,
): string | null => {
  if (!offer) {
    return null
  }

  if (offer.shortPrice) {
    return offer.shortPrice
  }

  const price = offer.price

  if (price == null) {
    return null
  }

  const currency = offer.currency ?? fallback.offers?.bestPrice?.currency

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

type OfferBadge = {
  key: string
  label: string
  price: string
  conditionText: string
  appearance: 'new' | 'occasion' | 'default'
}

const offerBadges = (product: ProductDto): OfferBadge[] => {
  const entries: OfferBadge[] = []
  const newOffer = product.offers?.bestNewOffer
  const occasionOffer = product.offers?.bestOccasionOffer

  if (newOffer) {
    const formatted = formatOfferPrice(newOffer, product)

    if (formatted) {
      entries.push({
        key: 'new',
        label: t('category.products.pricing.newOfferLabel'),
        price: formatted,
        conditionText: t('category.products.pricing.conditionLabel', {
          condition: conditionLabel(newOffer.condition),
        }),
        appearance: 'new',
      })
    }
  }

  if (occasionOffer) {
    const formatted = formatOfferPrice(occasionOffer, product)

    if (formatted) {
      entries.push({
        key: 'occasion',
        label: t('category.products.pricing.occasionOfferLabel'),
        price: formatted,
        conditionText: t('category.products.pricing.conditionLabel', {
          condition: conditionLabel(occasionOffer.condition),
        }),
        appearance: 'occasion',
      })
    }
  }

  if (!entries.length) {
    const fallbackOffer = product.offers?.bestPrice
    const formatted = formatOfferPrice(fallbackOffer, product) ?? bestPriceLabel(product)

    entries.push({
      key: 'best',
      label: t('category.products.pricing.bestOfferLabel'),
      price: formatted,
      conditionText: t('category.products.pricing.conditionLabel', {
        condition: conditionLabel(fallbackOffer?.condition ?? null),
      }),
      appearance: 'default',
    })
  }

  return entries
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
    position: relative

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

    :deep(img)
      object-fit: contain
      mix-blend-mode: multiply
      background: #fff

  &__compare
    position: absolute
    right: 1.25rem
    bottom: 1.25rem
    display: flex
    justify-content: flex-end
    z-index: 2

  &__body
    display: flex
    flex-direction: column
    gap: 1rem
    align-items: center
    text-align: center
    padding: 1.25rem
    background: rgb(var(--v-theme-surface-glass))
    border-bottom-left-radius: inherit
    border-bottom-right-radius: inherit
    padding-bottom: 3.5rem

  &__header
    display: flex
    align-items: center
    justify-content: center

  &__title
    font-size: 1.125rem
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    text-align: center

  &__attributes
    display: flex
    flex-wrap: wrap
    justify-content: center
    gap: 0.5rem
    margin: 0

  &__attribute
    background: rgba(var(--v-theme-surface-primary-080), 0.8)
    color: rgb(var(--v-theme-text-neutral-strong))
    font-weight: 500

  &__attribute-icon
    color: rgba(var(--v-theme-text-neutral-strong), 0.75)

  &__attribute-value
    white-space: nowrap

  &__meta
    display: flex
    flex-direction: column
    align-items: center
    gap: 0.25rem

  &__offers
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__score
    display: flex
    align-items: center
    justify-content: center
    min-height: 1.75rem

  &__score-fallback
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__pricing
    display: grid
    gap: 0.75rem
    width: 100%

  &__price-badge
    background: rgba(var(--v-theme-surface-primary-080), 0.8)
    border-radius: 1rem
    padding: 0.75rem 1rem
    display: flex
    flex-direction: column
    gap: 0.25rem
    align-items: center
    text-align: center

    &--new
      background: rgba(var(--v-theme-primary), 0.12)
      color: rgb(var(--v-theme-primary))

    &--occasion
      background: rgba(var(--v-theme-accent-supporting), 0.12)
      color: rgb(var(--v-theme-accent-supporting))

    &--default
      color: rgb(var(--v-theme-text-neutral-strong))

  &__price-badge-label
    font-size: 0.75rem
    text-transform: uppercase
    letter-spacing: 0.08em
    font-weight: 600

  &__price-badge-amount
    font-size: 1.25rem
    font-weight: 700

  &__price-badge-condition
    font-size: 0.875rem
    color: rgba(var(--v-theme-text-neutral-strong), 0.72)

  &--size-compact
    .category-product-card-grid__body
      gap: 0.75rem
      padding: 1rem
      padding-bottom: 3rem

    .category-product-card-grid__title
      font-size: 1rem

    .category-product-card-grid__price-badge
      padding: 0.6rem 0.85rem

    .category-product-card-grid__price-badge-amount
      font-size: 1.1rem

    .category-product-card-grid__price-badge-label
      font-size: 0.7rem

    .category-product-card-grid__price-badge-condition
      font-size: 0.8rem
</style>

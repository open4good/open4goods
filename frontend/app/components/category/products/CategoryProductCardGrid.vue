<template>
  <v-row
    class="category-product-card-grid"
    :class="[
      `category-product-card-grid--size-${normalizedSize}`,
      `category-product-card-grid--variant-${variant}`,
      { 'category-product-card-grid--disabled': isDisabledCategory },
    ]"
    dense
  >
    <v-col
      v-for="product in products"
      :key="resolveProductTitle(product) ?? Math.random()"
      cols="12"
      :sm="normalizedSize === 'small' ? 6 : 6"
      :md="normalizedSize === 'small' ? 4 : normalizedSize === 'big' ? 4 : 6"
      :lg="normalizedSize === 'small' ? 3 : normalizedSize === 'big' ? 4 : 4"
      :xl="normalizedSize === 'small' ? 2 : normalizedSize === 'big' ? 4 : 3"
    >
      <ProductTileCard
        v-if="variant === 'compact-tile'"
        :product="product"
        :product-link="productLink(product)"
        :image-src="resolveImage(product)"
        :attributes="popularAttributesByProduct(product)"
        :impact-score="impactScoreValue(product)"
        :score-max="5"
        :offer-badges="offerBadges(product)"
        :offers-count-label="offersCountLabel(product)"
        :untitled-label="$t('category.products.untitledProduct')"
        :not-rated-label="$t('category.products.notRated')"
        :disabled="isDisabledCategory"
        layout="vertical"
        :link-rel="linkRel"
      />
      <v-card
        v-else
        class="category-product-card-grid__card"
        :class="{
          'category-product-card-grid__card--disabled': isDisabledCategory,
        }"
        :rounded="normalizedSize === 'small' ? 'lg' : 'xl'"
        elevation="2"
        hover
        :to="productLink(product)"
        :rel="linkRel"
      >
        <div class="category-product-card-grid__compare">
          <CategoryProductCompareToggle :product="product" size="compact" />
        </div>

        <div class="category-product-card-grid__media-wrapper">
          <div class="category-product-card-grid__media">
            <v-img
              :src="resolveImage(product)"
              :alt="
                product.identity?.bestName ??
                product.identity?.model ??
                $t('category.products.untitledProduct')
              "
              contain
              class="category-product-card-grid__image"
            >
              <template #placeholder>
                <v-skeleton-loader type="image" class="h-100" />
              </template>
            </v-img>

            <div class="category-product-card-grid__title-overlay">
              <component
                :is="normalizedSize === 'big' ? 'h2' : 'h3'"
                class="category-product-card-grid__title"
              >
                {{ resolveProductTitle(product) }}
              </component>
            </div>

            <div class="category-product-card-grid__corner" role="presentation">
              <ImpactScore
                v-if="impactScoreValue(product) != null"
                :score="impactScoreValue(product) ?? 0"
                :max="5"
                :size="normalizedSize === 'big' ? 'default' : 'small'"
                mode="badge"
                badge-layout="stacked"
                badge-variant="corner"
              />
              <span v-else class="category-product-card-grid__corner-fallback">
                {{ $t('category.products.notRated') }}
              </span>
            </div>
          </div>
        </div>

        <v-card-item class="category-product-card-grid__body">
          <div
            v-if="popularAttributesByProduct(product).length"
            class="category-product-card-grid__attributes"
            role="list"
          >
            <v-chip
              v-for="attribute in popularAttributesByProduct(product)"
              :key="attribute.key"
              size="x-small"
              class="category-product-card-grid__attribute"
              variant="flat"
              color="surface-primary-080"
              role="listitem"
            >
              <v-icon
                v-if="attribute.icon"
                :icon="attribute.icon"
                size="14"
                class="me-1 category-product-card-grid__attribute-icon"
              />
              <span class="category-product-card-grid__attribute-value">
                {{ attribute.value }}
              </span>
            </v-chip>
          </div>

          <!-- Microtable Pricing Layout -->
          <div class="category-product-card-grid__pricing-table">
            <template v-for="badge in offerBadges(product)" :key="badge.key">
              <div
                class="category-product-card-grid__pricing-cell"
                :class="`category-product-card-grid__pricing-cell--${badge.appearance}`"
              >
                <div class="category-product-card-grid__pricing-label-group">
                  <span class="category-product-card-grid__pricing-label">
                    {{ badge.label }}
                  </span>

                  <v-tooltip
                    v-if="badge.trendIcon"
                    location="top"
                    open-on-click
                  >
                    <template #activator="{ props: tooltipProps }">
                      <v-icon
                        v-bind="tooltipProps"
                        :icon="badge.trendIcon"
                        :color="badge.trendColor"
                        size="14"
                        class="category-product-card-grid__pricing-trend"
                      />
                    </template>
                    <span>
                      {{ getTrendDescription(badge.trend, badge.label) }}
                    </span>
                  </v-tooltip>
                </div>

                <span class="category-product-card-grid__pricing-amount">
                  {{ badge.price }}
                </span>

                <div
                  v-if="badge.countLabel"
                  class="category-product-card-grid__pricing-count"
                >
                  {{ badge.countLabel }}
                </div>
              </div>
            </template>
          </div>

          <!-- Global offers count removed (moved to cells) -->
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
import { ProductPriceTrendDtoTrendEnum } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductTileCard from '~/components/category/products/ProductTileCard.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'
import { resolveProductTitle } from '~/utils/_product-title-resolver'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  size?: 'compact' | 'comfortable' | 'small' | 'medium' | 'big'
  variant?: 'classic' | 'compact-tile'
  maxAttributes?: number
  showAttributeIcons?: boolean
  isCategoryDisabled?: boolean
  nofollowLinks?: boolean
}>()

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const currencySymbolCache = new Map<string, string>()
const NBSP = '\u00A0'

const resolveCurrencySymbol = (currency?: string | null): string | null => {
  if (!currency) {
    return null
  }

  const upperCaseCurrency = currency.toUpperCase()

  if (currencySymbolCache.has(upperCaseCurrency)) {
    return currencySymbolCache.get(upperCaseCurrency) ?? null
  }

  try {
    const formatter = new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: upperCaseCurrency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
      currencyDisplay: 'symbol',
    })

    const symbol =
      formatter.formatToParts(0).find(part => part.type === 'currency')
        ?.value ?? upperCaseCurrency

    currencySymbolCache.set(upperCaseCurrency, symbol)

    return symbol
  } catch {
    currencySymbolCache.set(upperCaseCurrency, upperCaseCurrency)

    return upperCaseCurrency
  }
}

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

// Normalize size prop to small/medium/big
const normalizedSize = computed(() => {
  if (props.size === 'compact') return 'small'
  if (props.size === 'comfortable') return 'medium'
  return props.size ?? 'medium'
})

const variant = computed(() => props.variant ?? 'classic')
const maxAttributes = computed(() => props.maxAttributes)
const showAttributeIcons = computed(() => props.showAttributeIcons ?? true)
const isDisabledCategory = computed(() => props.isCategoryDisabled ?? false)
const linkRel = computed(() => (props.nofollowLinks ? 'nofollow' : undefined))

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

const impactScoreValue = (product: ProductDto) =>
  resolvePrimaryImpactScore(product)

const bestPriceLabel = (product: ProductDto) => formatBestPrice(product, t, n)

const offersCountLabel = (product: ProductDto) =>
  formatOffersCount(product, translatePlural)

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = (
  product: ProductDto
): DisplayedAttribute[] => {
  const attributes = resolvePopularAttributes(
    product,
    popularAttributeConfigs.value
  )
  const entries: DisplayedAttribute[] = []

  attributes.forEach(attribute => {
    const value = formatAttributeValue(attribute, t, n)

    if (!value) {
      return
    }

    entries.push({
      key: attribute.key,
      label: attribute.label,
      value,
      icon: showAttributeIcons.value ? (attribute.icon ?? null) : null,
    })
  })

  // In small mode, limit attributes even more strictly if not specified
  const effectiveMax =
    maxAttributes.value ?? (normalizedSize.value === 'small' ? 2 : 3)

  return entries.slice(0, effectiveMax)
}

const formatOfferPrice = (
  offer: ProductAggregatedPriceDto | undefined,
  fallback: ProductDto
): string | null => {
  if (!offer) {
    return null
  }

  const currency = offer.currency ?? fallback.offers?.bestPrice?.currency
  const shortPrice = offer.shortPrice?.trim()

  if (shortPrice) {
    if (!currency) {
      return shortPrice
    }

    const symbol = resolveCurrencySymbol(currency)

    if (!symbol) {
      return shortPrice
    }

    const normalisedShortPrice = shortPrice.replace(/\s+/g, ' ').trim()
    const containsSymbol =
      normalisedShortPrice.includes(symbol) ||
      normalisedShortPrice.toUpperCase().includes(currency.toUpperCase())

    return containsSymbol
      ? normalisedShortPrice
      : `${normalisedShortPrice}${NBSP}${symbol}`
  }

  const price = offer.price

  if (price == null) {
    return null
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

type OfferBadge = {
  key: string
  label: string
  price: string
  appearance: 'new' | 'occasion' | 'default'
  trendIcon?: string
  trendColor?: string
  trend?: ProductPriceTrendDtoTrendEnum
  countLabel?: string
}

const resolveTrendIcon = (
  trend?: ProductPriceTrendDtoTrendEnum
): string | undefined => {
  switch (trend) {
    case ProductPriceTrendDtoTrendEnum.PriceDecrease:
      return 'mdi-trending-down'
    case ProductPriceTrendDtoTrendEnum.PriceIncrease:
      return 'mdi-trending-up'
    case ProductPriceTrendDtoTrendEnum.PriceStable:
      return 'mdi-trending-neutral'
    default:
      return undefined
  }
}

const resolveTrendColor = (
  trend?: ProductPriceTrendDtoTrendEnum
): string | undefined => {
  switch (trend) {
    case ProductPriceTrendDtoTrendEnum.PriceDecrease:
      return 'success'
    case ProductPriceTrendDtoTrendEnum.PriceIncrease:
      return 'error'
    case ProductPriceTrendDtoTrendEnum.PriceStable:
      return 'neutral'
    default:
      return undefined
  }
}

const getTrendDescription = (
  trend: ProductPriceTrendDtoTrendEnum | undefined,
  label: string
) => {
  switch (trend) {
    case ProductPriceTrendDtoTrendEnum.PriceDecrease:
      return t('category.products.pricing.trends.decrease', { label })
    case ProductPriceTrendDtoTrendEnum.PriceIncrease:
      return t('category.products.pricing.trends.increase', { label })
    case ProductPriceTrendDtoTrendEnum.PriceStable:
      return t('category.products.pricing.trends.stable', { label })
    default:
      return ''
  }
}

const offerBadges = (product: ProductDto): OfferBadge[] => {
  const entries: OfferBadge[] = []
  const newOffer = product.offers?.bestNewOffer
  const occasionOffer = product.offers?.bestOccasionOffer
  const newTrend = product.offers?.newTrend?.trend
  const occasionTrend = product.offers?.occasionTrend?.trend

  if (newOffer) {
    const formatted = formatOfferPrice(newOffer, product)

    if (formatted) {
      entries.push({
        key: 'new',
        label: t('category.products.pricing.newOfferLabel'),
        price: formatted,
        appearance: 'new',
        trendIcon: resolveTrendIcon(newTrend),
        trendColor: resolveTrendColor(newTrend),
        trend: newTrend,
        countLabel: getConditionCountLabel(product, 'new'),
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
        appearance: 'occasion',
        trendIcon: resolveTrendIcon(occasionTrend),
        trendColor: resolveTrendColor(occasionTrend),
        trend: occasionTrend,
        countLabel: getConditionCountLabel(product, 'occasion'),
      })
    }
  }

  // If no specific offer types, show generic best
  if (!entries.length) {
    const fallbackOffer = product.offers?.bestPrice
    const formatted =
      formatOfferPrice(fallbackOffer, product) ?? bestPriceLabel(product)

    entries.push({
      key: 'best',
      label: t('category.products.pricing.bestOfferLabel'),
      price: formatted,
      appearance: 'default',
    })
  }

  return entries
}

const getConditionCountLabel = (
  product: ProductDto,
  type: 'new' | 'occasion'
): string | undefined => {
  const offersByCondition = product.offers?.offersByCondition
  if (!offersByCondition) return undefined

  let count = 0
  if (type === 'new') {
    // Sum up "new" like keys. Typically 'NEW'
    count = offersByCondition['NEW']?.length ?? 0
  } else {
    // Sum up anything not NEW? or specific keys like 'USED', 'REFURBISHED'
    // For simplicity, we assume we want to show count for the 'bestOccasion' context.
    // We can iterate keys.
    Object.keys(offersByCondition).forEach(key => {
      if (key !== 'NEW') {
        count += offersByCondition[key]?.length ?? 0
      }
    })
  }

  if (count <= 0) return undefined
  return translatePlural('category.products.offerCount', count, { count })
}
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &--disabled
    .category-product-card-grid__card,
    .product-tile-card
      filter: grayscale(1)
      opacity: 0.6


  &__card
    height: 100%
    display: flex
    flex-direction: column
    text-decoration: none
    transition: transform 0.2s ease, box-shadow 0.2s ease
    position: relative
    background: rgb(var(--v-theme-surface-glass))
    overflow: hidden
    border: 1px solid rgba(var(--v-theme-border-primary), 0.1)

    &:hover
      transform: translateY(-4px)
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)
      border-color: rgba(var(--v-theme-primary), 0.3)

    &--disabled
      filter: grayscale(1)
      opacity: 0.6

  &__media-wrapper
    position: relative
    overflow: hidden
    background: #fff
    /* Responsive sizing logic */
    aspect-ratio: 4/3
    min-height: 140px /* preventing too small images on mobile */
    max-height: 240px
    display: flex
    align-items: center
    justify-content: center

  &__media
    width: 100%
    height: 100%
    position: relative

  &__image
    width: 100%
    height: 100%
    transition: transform 0.3s ease

    :deep(img)
      object-fit: contain
      padding: 1rem /* Add some breathing room */
      mix-blend-mode: multiply
      background: #fff

  /* Hover effect on image */
  &__card:hover &__image
    transform: scale(1.05)

  &__corner
    position: absolute
    top: 0
    left: 0
    width: 56px
    height: 56px
    display: inline-flex
    align-items: center
    justify-content: center
    border-radius: 0 0 54% 0
    background: rgba(var(--v-theme-surface-glass-strong), 0.95)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45)
    color: rgb(var(--v-theme-text-neutral-strong))
    box-shadow: 0 4px 12px rgba(0,0,0, 0.08)
    backdrop-filter: blur(8px)
    z-index: 2
    pointer-events: none

  &__corner-fallback
    font-size: 0.65rem
    font-weight: 700
    letter-spacing: 0.05em
    text-transform: uppercase
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
    text-align: center
    line-height: 1.1
    transform: rotate(-12deg)

  &__compare
    position: absolute
    right: 0.75rem
    bottom: 0.75rem /* Moved to bottom */
    z-index: 5

  &__body
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: 1rem
    flex-grow: 1

  &__header
    display: flex
    flex-direction: column
    align-items: center
    text-align: center
    gap: 0.25rem

  &__title-overlay
    position: absolute
    top: 0
    left: 56px /* Width of corner badge */
    right: 0
    padding: 0.5rem 2.5rem 0.5rem 0.75rem /* Right padding for compare toggle */
    z-index: 2
    display: flex
    align-items: center
    min-height: 56px /* Match corner height */

  &__title
    font-size: 0.85rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))
    margin: 0
    line-height: 1.2
    display: -webkit-box
    -webkit-line-clamp: 2
    -webkit-box-orient: vertical
    overflow: hidden
    background: rgba(var(--v-theme-surface-default), 0.7)
    backdrop-filter: blur(4px)
    padding: 0.35rem 0.75rem
    border-radius: 1rem
    box-shadow: 0 2px 8px rgba(0,0,0,0.05)

  &__attributes
    display: flex
    flex-wrap: wrap
    justify-content: center
    gap: 0.35rem
    margin: 0.25rem 0

  &__attribute
    background: rgba(var(--v-theme-surface-primary-080), 0.5) !important
    color: rgb(var(--v-theme-text-neutral-secondary)) !important
    font-weight: 500
    border: 1px solid rgba(var(--v-theme-border-primary), 0.1)

  &__pricing-table
    display: grid
    grid-template-columns: 1fr 1fr
    gap: 0px
    border: 1px solid rgba(var(--v-theme-border-primary), 0.15)
    border-radius: 8px
    overflow: hidden
    margin-top: auto /* Push to bottom */
    margin-top: auto /* Push to bottom */
    background: rgba(var(--v-theme-surface-default), 0.6) /* More transparent */
    backdrop-filter: blur(4px)

    /* Only one item? Span full width */
    > *:only-child
      grid-column: span 2

  &__pricing-cell
    display: flex
    flex-direction: column
    align-items: center
    justify-content: center
    padding: 0.65rem 0.5rem
    gap: 0.15rem
    transition: background 0.2s

    &:not(:last-child)
      border-right: 1px solid rgba(var(--v-theme-border-primary), 0.15)

    &--new
      background: transparent
      .category-product-card-grid__pricing-label
        color: rgb(var(--v-theme-primary))

    &--occasion
      background: transparent
      .category-product-card-grid__pricing-label
        color: rgb(var(--v-theme-accent-supporting))

    &--default
      background: transparent

  &__pricing-label
    font-size: 0.65rem
    text-transform: uppercase
    font-weight: 700
    letter-spacing: 0.05em
    opacity: 0.85

  &__pricing-label-group
    display: flex
    align-items: center
    gap: 0.35rem

  &__pricing-value-group
    display: flex
    align-items: center
    gap: 0.25rem

  &__pricing-amount
    font-weight: 700
    font-size: 0.95rem
    color: rgb(var(--v-theme-text-neutral-strong))

  .category-product-card-grid__pricing-count
    font-size: 0.7rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-weight: 500

  &__pricing-trend
    opacity: 0.9

  &__meta
    display: flex
    align-items: center
    justify-content: center
    margin-top: 0.25rem

  &__offers
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  /* --- Size Variants --- */
  &--size-small
    .category-product-card-grid__card
        border-radius: 12px

    .category-product-card-grid__media-wrapper
        min-height: 120px
        max-height: 180px

    .category-product-card-grid__corner
        width: 48px
        height: 48px

    .category-product-card-grid__body
        padding: 0.75rem
        gap: 0.5rem

    .category-product-card-grid__title
        font-size: 0.9rem

  &--size-big
    .category-product-card-grid__media-wrapper
        min-height: 200px
        max-height: 320px

    .category-product-card-grid__title
        font-size: 1rem

    .category-product-card-grid__pricing-amount
        font-size: 1.25rem
</style>

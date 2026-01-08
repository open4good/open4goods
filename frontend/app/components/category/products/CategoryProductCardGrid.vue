<template>
  <v-row
    class="category-product-card-grid"
    :class="[
      `category-product-card-grid--size-${cardSize}`,
      `category-product-card-grid--variant-${variant}`,
      { 'category-product-card-grid--disabled': isDisabledCategory },
    ]"
    dense
  >
    <v-col
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
      cols="12"
      sm="6"
      lg="4"
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
        rounded="xl"
        elevation="2"
        hover
        :to="productLink(product)"
        :rel="linkRel"
      >
        <div class="category-product-card-grid__compare">
          <CategoryProductCompareToggle :product="product" size="compact" />
        </div>

        <div class="category-product-card-grid__media">
          <v-img
            :src="resolveImage(product)"
            :alt="
              product.identity?.bestName ??
              product.identity?.model ??
              $t('category.products.untitledProduct')
            "
            :aspect-ratio="4 / 3"
            contain
            class="category-product-card-grid__image"
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>
          <div class="category-product-card-grid__corner" role="presentation">
            <ImpactScore
              v-if="impactScoreValue(product) != null"
              :score="impactScoreValue(product) ?? 0"
              :max="5"
              size="small"
              mode="badge"
              badge-layout="stacked"
              badge-variant="corner"
            />
            <span v-else class="category-product-card-grid__corner-fallback">
              {{ $t('category.products.notRated') }}
            </span>
          </div>
        </div>

        <v-card-item class="category-product-card-grid__body">
          <div class="category-product-card-grid__header">
            <h3 class="category-product-card-grid__title">
              {{
                product.identity?.bestName ??
                product.identity?.model ??
                product.identity?.brand ??
                '#' + product.gtin
              }}
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

          <template
            v-for="(badges, index) in [offerBadges(product)]"
            :key="`pricing-${index}-${
              product.gtin ??
              product.identity?.bestName ??
              product.identity?.model ??
              product.slug ??
              product.fullSlug ??
              'fallback'
            }`"
          >
            <div
              v-if="badges.length"
              class="category-product-card-grid__pricing"
              :class="{
                'category-product-card-grid__pricing--split': badges.length > 1,
              }"
              role="list"
            >
              <v-chip
                v-for="badge in badges"
                :key="badge.key"
                class="category-product-card-grid__price-badge"
                :color="
                  badge.appearance === 'new'
                    ? 'primary'
                    : badge.appearance === 'occasion'
                      ? 'accent-supporting'
                      : 'surface-primary-080'
                "
                :variant="badge.appearance === 'default' ? 'flat' : 'tonal'"
                size="default"
                role="listitem"
              >
                <template v-if="badge.trendIcon" #prepend>
                  <v-icon
                    :icon="badge.trendIcon"
                    :color="badge.trendColor"
                    size="16"
                    class="me-1"
                  />
                </template>
                <div class="d-flex flex-column align-center">
                  <span class="category-product-card-grid__price-badge-label">{{
                    badge.label
                  }}</span>
                  <span
                    class="category-product-card-grid__price-badge-amount"
                    >{{ badge.price }}</span
                  >
                </div>
              </v-chip>
            </div>
          </template>

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

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  size?: 'compact' | 'comfortable'
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
const cardSize = computed(() => props.size ?? 'comfortable')
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

  if (maxAttributes.value != null) {
    return entries.slice(0, maxAttributes.value)
  }

  return entries
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
      })
    }
  }

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

    &:hover
      transform: translateY(-4px)
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)

    &--disabled
      filter: grayscale(1)
      opacity: 0.6

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

  &__media
    position: relative
    overflow: hidden
    border-top-left-radius: inherit
    border-top-right-radius: inherit

  &__corner
    position: absolute
    top: 0
    left: 0
    width: 64px
    height: 64px
    display: inline-flex
    align-items: center
    justify-content: center
    border-radius: 0 0 54% 0
    background: rgba(var(--v-theme-surface-glass-strong), 0.92)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45)
    color: rgb(var(--v-theme-text-neutral-strong))
    box-shadow: 0 14px 26px rgba(15, 23, 42, 0.14)
    backdrop-filter: blur(6px)
    z-index: 2
    pointer-events: none

  &__corner-fallback
    font-size: 0.72rem
    font-weight: 700
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
    text-align: center
    line-height: 1.1
    transform: rotate(-12deg)

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

  &__pricing
    display: grid
    gap: 0.75rem
    width: 100%
    grid-template-columns: minmax(0, 1fr)

  &__pricing--split
    grid-template-columns: repeat(2, minmax(0, 1fr))

    @media (max-width: 600px)
      grid-template-columns: minmax(0, 1fr)

  &__price-badge
    height: 100%
    justify-content: center
    padding: 1.5rem 1rem

    .d-flex
        gap: 0.1rem

  &__price-badge-label
    font-size: 0.65rem
    text-transform: uppercase
    letter-spacing: 0.08em
    font-weight: 700
    opacity: 0.9

  &__price-badge-amount
    font-size: 1rem
    font-weight: 700

  &--size-compact
    .category-product-card-grid__body
      gap: 0.75rem
      padding: 1rem
      padding-bottom: 3rem

    .category-product-card-grid__title
      font-size: 1rem

    .category-product-card-grid__price-badge
      padding: 0.65rem 0.85rem

    .category-product-card-grid__price-badge-amount
      font-size: 1.1rem

    .category-product-card-grid__price-badge-label
      font-size: 0.7rem
</style>

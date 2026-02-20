<template>
  <div
    class="product-price-rows"
    :class="{ 'product-price-rows--compact': variant === 'compact' }"
  >
    <template v-for="badge in offerBadges" :key="badge.key">
      <div
        class="product-price-rows__row"
        :class="`product-price-rows__row--${badge.appearance}`"
      >
        <!-- Label (Neuf / Occasion) -->
        <span class="product-price-rows__label">
          {{ badge.label }}
        </span>

        <!-- Price Area (Clickable) -->
        <a
          :href="badge.url"
          target="_blank"
          rel="nofollow noopener noreferrer"
          class="product-price-rows__content"
          @click.stop
        >
          <!-- Merchant Favicon -->
          <img
            v-if="badge.favicon"
            :src="badge.favicon"
            :alt="badge.merchantName"
            class="product-price-rows__favicon"
            width="16"
            height="16"
          />
          <v-icon
            v-else
            :icon="
              badge.appearance === 'new' ? 'mdi-tag-outline' : 'mdi-recycle'
            "
            size="16"
            class="product-price-rows__fallback-icon"
          />

          <!-- Price -->
          <span class="product-price-rows__amount">
            {{ badge.price }}
          </span>
        </a>

        <!-- Trend -->
        <v-tooltip
          v-if="badge.trendIcon && badge.trendDescription"
          location="bottom"
          :text="badge.trendDescription"
        >
          <template #activator="{ props: tooltipProps }">
            <v-icon
              v-bind="tooltipProps"
              :icon="badge.trendIcon"
              :color="badge.trendColor"
              size="small"
              class="product-price-rows__trend"
            />
          </template>
        </v-tooltip>
        <v-icon
          v-else-if="badge.trendIcon"
          :icon="badge.trendIcon"
          :color="badge.trendColor"
          size="small"
          class="product-price-rows__trend"
        />

        <!-- Alternatives Dropdown -->
        <v-menu
          v-if="badge.hasAlternatives"
          location="bottom end"
          :close-on-content-click="true"
        >
          <template #activator="{ props: menuProps }">
            <v-btn
              v-bind="menuProps"
              icon="mdi-chevron-down"
              variant="text"
              density="compact"
              size="20"
              class="product-price-rows__more-btn"
              @click.stop.prevent
            />
          </template>
          <v-list density="compact" nav class="product-price-rows__menu-list">
            <v-list-item
              v-for="alt in badge.alternatives"
              :key="alt.id"
              :href="alt.url"
              target="_blank"
              rel="nofollow noopener noreferrer"
              class="product-price-rows__menu-item"
            >
              <template #prepend>
                <img
                  v-if="alt.favicon"
                  :src="alt.favicon"
                  :alt="alt.merchantName"
                  width="16"
                  height="16"
                  class="mr-2"
                />
                <v-icon v-else icon="mdi-store" size="16" class="mr-2" />
              </template>

              <div
                class="d-flex justify-space-between align-center w-100 gap-4"
              >
                <span
                  class="text-caption font-weight-medium text-truncate"
                  style="max-width: 100px"
                >
                  {{ alt.merchantName }}
                </span>
                <span class="text-caption font-weight-bold text-primary">
                  {{ alt.priceLabel }}
                </span>
              </div>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useProductPriceTrend } from '~/composables/useProductPriceTrend'
import type {
  ProductDto,
  ProductAggregatedPriceDto,
} from '~~/shared/api-client'
import { formatBestPrice } from '~/utils/_product-pricing'

const props = defineProps<{
  product: ProductDto
  variant?: 'default' | 'compact'
}>()

const { t, n, locale } = useI18n()

// Reusing logic from ProductHeroPricing for contrib links
// Since we don't have direct access to 'useAnalytics' inside helpers in the same way, we'll inline some logic or use the composable if available.
// Actually, let's adapt the logic straightforwardly.

const getAffiliationLink = (offer: ProductAggregatedPriceDto | undefined) => {
  if (!offer) return null
  // Simplified check: if we have a token, use it.
  // The original generic component had a check 'isSingleOffer', but here we are listing best offers per condition.
  // If there is an affiliation token on the offer, we should probably use it.

  const token = offer.affiliationToken
  return token ? `/contrib/${token}` : null
}

const resolveUrl = (offer: ProductAggregatedPriceDto | undefined) => {
  if (!offer) return undefined
  const affLink = getAffiliationLink(offer)
  return affLink ?? offer.url ?? undefined
}

const NBSP = '\u00A0'
const currencySymbolCache = new Map<string, string>()

const resolveCurrencySymbol = (currency?: string | null): string | null => {
  if (!currency) return null
  const upperCaseCurrency = currency.toUpperCase()
  if (currencySymbolCache.has(upperCaseCurrency))
    return currencySymbolCache.get(upperCaseCurrency) ?? null

  try {
    const formatter = new Intl.NumberFormat(locale.value, {
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

const formatOfferPrice = (
  offer: ProductAggregatedPriceDto | undefined
): string | null => {
  if (!offer) return null
  const currency = offer.currency
  const shortPrice = offer.shortPrice?.trim()

  if (shortPrice) {
    if (!currency) return shortPrice
    const symbol = resolveCurrencySymbol(currency)
    if (!symbol) return shortPrice

    const normalised = shortPrice.replace(/\s+/g, ' ').trim()
    const containsSymbol =
      normalised.includes(symbol) ||
      normalised.toUpperCase().includes(currency.toUpperCase())
    return containsSymbol ? normalised : `${normalised}${NBSP}${symbol}`
  }

  const price = offer.price
  if (price == null) return null

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }
  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const { resolvePriceTrendTone, resolveTrendIcon, formatTrendTooltip } =
  useProductPriceTrend()

const resolveTrendColor = (tone: string): string | undefined => {
  switch (tone) {
    case 'decrease':
      return 'success' // Price down is good
    case 'increase':
      return 'error' // Price up is bad
    case 'stable':
      return 'neutral'
    default:
      return undefined
  }
}

// Generate deterministic ID from offer URL to avoid SSR hydration mismatches
const deterministicId = (url?: string, index: number = 0) =>
  url ? `row-${url.slice(-12).replace(/[^a-z0-9]/gi, '-')}` : `row-${index}`

type AlternativeOffer = {
  id: string
  merchantName: string
  priceLabel: string
  favicon?: string
  url?: string
}

const getAlternatives = (
  conditionKey: string,
  bestOfferUrl?: string | null
): AlternativeOffer[] => {
  const offers = props.product.offers?.offersByCondition?.[conditionKey] || []
  if (!offers.length) return []

  return offers
    .filter(o => o.url !== bestOfferUrl)
    .map((o, index) => ({
      id: deterministicId(o.url, index),
      merchantName: o.datasourceName ?? o.merchantName ?? 'Unknown',
      priceLabel: formatOfferPrice(o) || 'N/A',
      favicon: o.favicon ?? o.merchantFavicon,
      url: resolveUrl(o), // Apply same URL resolution logic
    }))
}

type Badge = {
  key: string
  label: string
  price: string
  appearance: 'new' | 'occasion' | 'default'
  trendIcon?: string
  trendColor?: string
  trendDescription?: string
  favicon?: string
  merchantName?: string
  url?: string
  hasAlternatives: boolean
  alternatives: AlternativeOffer[]
}

const offerBadges = computed<Badge[]>(() => {
  const entries: Badge[] = []
  const newOffer = props.product.offers?.bestNewOffer
  const occasionOffer = props.product.offers?.bestOccasionOffer
  const newTrend = props.product.offers?.newTrend
  const occasionTrend = props.product.offers?.occasionTrend

  if (newOffer) {
    const formatted = formatOfferPrice(newOffer)
    const url = resolveUrl(newOffer)
    const tone = resolvePriceTrendTone(newTrend)
    if (formatted) {
      entries.push({
        key: 'new',
        label: t('category.products.pricing.newOfferLabel'),
        price: formatted,
        appearance: 'new',
        trendIcon: resolveTrendIcon(tone),
        trendColor: resolveTrendColor(tone),
        trendDescription: formatTrendTooltip(
          newTrend,
          newOffer.currency ?? 'EUR'
        ),
        favicon: newOffer.favicon ?? newOffer.merchantFavicon,
        merchantName: newOffer.merchantName,
        url,
        hasAlternatives:
          (props.product.offers?.offersByCondition?.['NEW']?.length ?? 0) > 1,
        alternatives: getAlternatives('NEW', newOffer.url),
      })
    }
  }

  if (occasionOffer) {
    const formatted = formatOfferPrice(occasionOffer)
    const url = resolveUrl(occasionOffer)
    const tone = resolvePriceTrendTone(occasionTrend)
    if (formatted) {
      entries.push({
        key: 'occasion',
        label: t('category.products.pricing.occasionOfferLabel'),
        price: formatted,
        appearance: 'occasion',
        trendIcon: resolveTrendIcon(tone),
        trendColor: resolveTrendColor(tone),
        trendDescription: formatTrendTooltip(
          occasionTrend,
          occasionOffer.currency ?? 'EUR'
        ),
        favicon: occasionOffer.favicon ?? occasionOffer.merchantFavicon,
        merchantName: occasionOffer.merchantName,
        url,
        hasAlternatives:
          (props.product.offers?.offersByCondition?.['USED']?.length ?? 0) +
            (props.product.offers?.offersByCondition?.['REFURBISHED']?.length ??
              0) >
          1,
        // For occasion, we might need to aggreage USED and REFURBISHED or just check what 'offersByCondition' keys exist.
        // Simplified: just fetch what we can. Usually API gives 'NEW' and 'OCCASION' (or similar?).
        // Based on ProductMicroPrice it used 'occasion'.
        alternatives: getAlternatives('OCCASION', occasionOffer.url),
      })
    }
  }

  // NOTE: If we want to fallback to "Best Price" generic if no specific new/used logic matches?
  // The request implies "condition row", so we usually expect New/Occasion.
  // But if both missing but there is a price?
  if (!entries.length) {
    const fallbackOffer = props.product.offers?.bestPrice
    const formatted =
      formatOfferPrice(fallbackOffer) ?? formatBestPrice(props.product, t, n)
    if (formatted && formatted !== '—') {
      // Only show if valid price
      const url = resolveUrl(fallbackOffer)
      entries.push({
        key: 'best',
        label: t('category.products.pricing.bestOfferLabel'),
        price: formatted,
        appearance: 'default',
        favicon: fallbackOffer?.favicon ?? fallbackOffer?.merchantFavicon,
        merchantName: fallbackOffer?.merchantName,
        url,
        hasAlternatives: false,
        alternatives: [],
      })
    }
  }

  return entries
})
</script>

<style scoped lang="sass">
.product-price-rows
  display: flex
  flex-direction: column
  border: 1px solid rgba(var(--v-theme-border-primary), 0.15)
  border-radius: 8px
  overflow: hidden
  background: rgba(var(--v-theme-surface-default), 0.6)
  backdrop-filter: blur(4px)

  &__row
    display: flex
    align-items: center
    padding: 0.25rem 0.5rem
    gap: 0.25rem
    min-height: 32px /* Consistent height */

    &:not(:last-child)
      border-bottom: 1px solid rgba(var(--v-theme-border-primary), 0.15)

    &--new
      .product-price-rows__label
         color: rgb(var(--v-theme-primary))
      .product-price-rows__amount
         font-weight: 700
         color: rgb(var(--v-theme-text-neutral-strong))

    &--occasion
      .product-price-rows__label
         color: rgb(var(--v-theme-accent-supporting))
      .product-price-rows__amount
         font-weight: 700
         color: rgb(var(--v-theme-text-neutral-strong))

  &__label
    font-size: 0.7rem
    text-transform: uppercase
    font-weight: 700
    letter-spacing: 0.05em
    opacity: 0.85
    width: 4rem /* Fixed width for alignment */
    flex-shrink: 0

  &__content
    display: flex
    align-items: center
    flex: 1
    gap: 0.35rem
    text-decoration: none
    color: inherit
    padding: 2px 4px
    border-radius: 4px
    transition: background-color 0.2s

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-080), 0.3)

  &__favicon
    border-radius: 2px
    object-fit: contain
    flex-shrink: 0

  &__fallback-icon
    opacity: 0.6

  &__amount
    font-size: 1.4rem
    font-weight: 700
    white-space: nowrap

  &__trend
    opacity: 0.8
    flex-shrink: 0

  &__more-btn
    opacity: 0.6
    flex-shrink: 0
    margin-left: -2px /* Tighten layout */
    &:hover
      opacity: 1
      background: rgba(0,0,0,0.05)

  &__menu-list
     min-width: 200px

.product-price-rows--compact
  border-radius: 6px

  .product-price-rows__row
    min-height: 20px
    padding: 0.1rem 0.35rem
    gap: 0.3rem

  .product-price-rows__label
    font-size: 0.55rem
    width: 3.25rem

  .product-price-rows__amount
    font-size: 0.9rem

  .product-price-rows__favicon
    width: 14px
    height: 14px
</style>

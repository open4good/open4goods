<template>
  <div
    class="product-hero__pricing-card"
    itemprop="offers"
    itemscope
    itemtype="https://schema.org/AggregateOffer"
    v-bind="$attrs"
  >
    <meta
      itemprop="offerCount"
      :content="String(product.offers?.offersCount ?? 0)"
    />
    <meta itemprop="priceCurrency" :content="defaultCurrencyCode" />

    <div class="product-hero__pricing-stack">
      <ProductHeroPricingPanel
        v-for="panel in conditionPanels"
        :key="panel.condition"
        :condition="panel.condition"
        :condition-label="panel.conditionLabel"
        :offers-count-label="panel.offersCountLabel"
        :price-title="panel.priceTitle"
        :price-label="panel.priceLabel"
        :price-currency="panel.priceCurrency"
        :empty-state-label="panel.emptyStateLabel"
        :has-offer="panel.hasOffer"
        :merchant="panel.merchant"
        :offer-name="panel.offerName"
        :show-merchant-name="true"
        :trend-label="panel.trendLabel"
        :trend-tooltip="panel.trendTooltip"
        :trend-tone-class="panel.trendToneClass"
        :trend-icon="panel.trendIcon"
        :view-offers-label="panel.viewOffersLabel"
        @merchant-click="handleMerchantClick"
        @trend-click="scrollToSelector('#price-history')"
        @view-offers="scrollToSelector('#prix', 136)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAnalytics } from '~/composables/useAnalytics'
import type { ProductDto } from '~~/shared/api-client'
import ProductHeroPricingPanel from '~/components/product/ProductHeroPricingPanel.vue'

type OfferCondition = 'occasion' | 'new'

type ProductTrend = NonNullable<ProductDto['offers']>['newTrend']

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { n, t } = useI18n()
const {
  trackProductRedirect,
  trackAffiliateClick,
  isClientContribLink,
  extractTokenFromLink,
} = useAnalytics()

type AggregatedOffer = NonNullable<
  NonNullable<ProductDto['offers']>['bestPrice']
>

const aggregatedBestOffer = computed<AggregatedOffer | null>(
  () => props.product.offers?.bestPrice ?? null
)

const bestOffersByCondition = computed<
  Record<OfferCondition, AggregatedOffer | null>
>(() => {
  const offers = props.product.offers
  const aggregated = aggregatedBestOffer.value

  const mapped: Record<OfferCondition, AggregatedOffer | null> = {
    occasion: offers?.bestOccasionOffer ?? null,
    new: offers?.bestNewOffer ?? null,
  }

  if (!mapped.occasion && aggregated?.condition === 'OCCASION') {
    mapped.occasion = aggregated
  }

  if (!mapped.new && aggregated?.condition === 'NEW') {
    mapped.new = aggregated
  }

  if (!mapped.new && !mapped.occasion && aggregated) {
    mapped.new = aggregated
  }

  return mapped
})

const conditionOrder: OfferCondition[] = ['new', 'occasion']

const defaultCurrencyCode = computed(
  () => aggregatedBestOffer.value?.currency ?? 'EUR'
)

const priceRangesByCondition = computed<
  Record<
    OfferCondition,
    {
      min: number
      max: number
      currency: string
    } | null
  >
>(() => {
  const ranges: Record<
    OfferCondition,
    { min: number; max: number; currency: string } | null
  > = {
    occasion: null,
    new: null,
  }

  const offersByCondition = props.product.offers?.offersByCondition ?? {}

  for (const [conditionKey, offers] of Object.entries(offersByCondition)) {
    const normalizedCondition = conditionKey.toLowerCase()
    if (normalizedCondition !== 'new' && normalizedCondition !== 'occasion') {
      continue
    }

    const numericPrices = (offers ?? [])
      .map(offer => ({
        value: typeof offer?.price === 'number' ? offer.price : null,
        currency: offer?.currency ?? defaultCurrencyCode.value,
      }))
      .filter(
        (entry): entry is { value: number; currency: string } =>
          typeof entry.value === 'number'
      )

    if (!numericPrices.length) {
      continue
    }

    const values = numericPrices.map(entry => entry.value)
    ranges[normalizedCondition as OfferCondition] = {
      min: Math.min(...values),
      max: Math.max(...values),
      currency: numericPrices[0]?.currency ?? defaultCurrencyCode.value,
    }
  }

  ;['occasion', 'new'].forEach(condition => {
    const typedCondition = condition as OfferCondition
    if (ranges[typedCondition]) {
      return
    }

    const fallbackPrice = bestOffersByCondition.value[typedCondition]?.price
    if (typeof fallbackPrice === 'number') {
      ranges[typedCondition] = {
        min: fallbackPrice,
        max: fallbackPrice,
        currency:
          bestOffersByCondition.value[typedCondition]?.currency ??
          defaultCurrencyCode.value,
      }
    }
  })

  return ranges
})

const animatedPrices = ref<Record<OfferCondition, number | null>>({
  occasion: null,
  new: null,
})

const offersCount = computed(() => props.product.offers?.offersCount ?? 0)

const offersCountLabel = computed(() =>
  t('product.hero.offersCountLabel', { count: n(offersCount.value) })
)

const viewOffersLabel = computed(() =>
  offersCount.value <= 1
    ? t('product.hero.viewSingleOffer')
    : t('product.hero.viewOffersCount', { count: offersCountLabel.value })
)

const isSingleOffer = computed(() => offersCount.value === 1)

const getAffiliationLink = (offer: AggregatedOffer | null) => {
  if (!isSingleOffer.value) {
    return null
  }

  const token =
    offer?.affiliationToken ?? aggregatedBestOffer.value?.affiliationToken
  return token ? `/contrib/${token}` : null
}

const createMerchant = (offer: AggregatedOffer | null) => {
  if (!offer?.datasourceName) {
    return null
  }

  const affiliationLink = getAffiliationLink(offer)
  const url = isSingleOffer.value
    ? (affiliationLink ?? offer.url ?? null)
    : (offer.url ?? null)

  return {
    name: offer.datasourceName,
    url,
    favicon: offer.favicon ?? null,
    isInternal: typeof url === 'string' && url.startsWith('/'),
    clientOnly: isSingleOffer.value && Boolean(affiliationLink),
  }
}

const handleMerchantClick = (payload: {
  name: string | null
  url: string | null
}) => {
  const link = payload.url

  if (!isClientContribLink(link)) {
    return
  }

  trackProductRedirect({
    token: extractTokenFromLink(link),
    placement: 'product-hero',
    source: payload.name ?? null,
    url: link,
  })

  trackAffiliateClick({
    token: extractTokenFromLink(link),
    url: link,
    partner: payload.name ?? null,
    placement: 'product-hero',
    productId: props.product.id ?? null,
  })
}

const formatPriceLabel = (price: number | null, currency: string) => {
  if (typeof price !== 'number') {
    return '—'
  }

  if (currency !== 'EUR') {
    return n(price, {
      style: 'currency',
      currency,
      maximumFractionDigits: price >= 100 ? 0 : 2,
    })
  }

  return n(price, {
    style: 'decimal',
    minimumFractionDigits: price >= 100 ? 0 : 2,
    maximumFractionDigits: price >= 100 ? 0 : 2,
  })
}

const formatCurrencyDisplay = (priceLabel: string, currency: string) => {
  if (priceLabel === '—') {
    return null
  }

  return currency === 'EUR' ? '€' : currency
}

const resolvePriceTrendLabel = (trend: ProductTrend, currency: string) => {
  if (!trend?.trend) {
    return null
  }

  if (trend.trend === 'PRICE_DECREASE') {
    return t('product.price.trend.decrease', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
      }),
    })
  }

  if (trend.trend === 'PRICE_INCREASE') {
    return t('product.price.trend.increase', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
      }),
    })
  }

  return t('product.price.trend.stable')
}

const resolvePriceTrendTone = (trend: ProductTrend) => {
  if (trend?.trend === 'PRICE_DECREASE') {
    return 'decrease'
  }

  if (trend?.trend === 'PRICE_INCREASE') {
    return 'increase'
  }

  return 'stable'
}

const resolveTrendIcon = (tone: 'decrease' | 'increase' | 'stable') => {
  switch (tone) {
    case 'decrease':
      return 'mdi-trending-down'
    case 'increase':
      return 'mdi-trending-up'
    default:
      return 'mdi-trending-neutral'
  }
}

const formatTrendPeriod = (period?: number) => {
  if (!period || period <= 0) {
    return null
  }

  const minutes = Math.max(1, Math.round(period / 60000))
  const hours = Math.round(period / 3600000)
  const days = Math.round(period / 86400000)

  if (days >= 1) {
    return t('product.hero.trendPeriodDays', { count: days })
  }

  if (hours >= 1) {
    return t('product.hero.trendPeriodHours', { count: hours })
  }

  return t('product.hero.trendPeriodMinutes', { count: minutes })
}

const formatTrendTooltip = (trend: ProductTrend, currency: string) => {
  if (!trend) {
    return null
  }

  const deviation =
    typeof trend.variation === 'number'
      ? n(Math.abs(trend.variation), {
          style: 'currency',
          currency,
          maximumFractionDigits: 2,
        })
      : null
  const periodLabel = formatTrendPeriod(trend.period)

  if (!deviation || !periodLabel) {
    return null
  }

  return t('product.hero.trendTooltip', {
    deviation,
    period: periodLabel,
  })
}

const scrollToSelector = (selector: string, offset = 120) => {
  if (!import.meta.client) {
    return
  }

  const target = document.querySelector<HTMLElement>(selector)
  if (!target) {
    return
  }

  const top =
    target.getBoundingClientRect().top +
    (window.scrollY || window.pageYOffset || 0) -
    offset
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}

const animatedConditions = ref<Record<OfferCondition, boolean>>({
  occasion: false,
  new: false,
})
const animationFrameIds = ref<Record<OfferCondition, number | null>>({
  occasion: null,
  new: null,
})

const startPriceCountdown = (condition: OfferCondition) => {
  if (!import.meta.client) {
    return
  }

  if (animatedConditions.value[condition]) {
    animatedPrices.value[condition] = null
    return
  }

  const range = priceRangesByCondition.value[condition]
  const targetPrice = bestOffersByCondition.value[condition]?.price

  if (!range || typeof targetPrice !== 'number') {
    animatedPrices.value[condition] = null
    return
  }

  const startValue = Number.isFinite(range.max) ? range.max : targetPrice
  const endValue = Number.isFinite(range.min) ? range.min : targetPrice

  if (
    !Number.isFinite(startValue) ||
    !Number.isFinite(endValue) ||
    startValue === endValue
  ) {
    animatedPrices.value[condition] = null
    animatedConditions.value[condition] = true
    return
  }

  const duration = 4000
  const easing = (progress: number) => 1 - Math.pow(1 - progress, 2.6)
  const startedAt = performance.now()

  const runningFrame = animationFrameIds.value[condition]
  if (runningFrame != null) {
    cancelAnimationFrame(runningFrame)
  }

  const tick = (timestamp: number) => {
    const elapsed = timestamp - startedAt
    const progress = Math.min(Math.max(elapsed / duration, 0), 1)
    const eased = easing(progress)
    animatedPrices.value[condition] =
      startValue + (endValue - startValue) * eased

    if (progress < 1) {
      animationFrameIds.value[condition] = requestAnimationFrame(tick)
      return
    }

    animatedPrices.value[condition] = null
    animationFrameIds.value[condition] = null
    animatedConditions.value[condition] = true
  }

  animatedPrices.value[condition] = startValue
  animationFrameIds.value[condition] = requestAnimationFrame(tick)
}

onMounted(() => {
  conditionOrder.forEach(condition => {
    startPriceCountdown(condition)
  })
})

onBeforeUnmount(() => {
  Object.values(animationFrameIds.value).forEach(frameId => {
    if (frameId != null) {
      cancelAnimationFrame(frameId)
    }
  })
})

const conditionPanels = computed(() => {
  const offers = props.product.offers
  return conditionOrder.map(condition => {
    const offer = bestOffersByCondition.value[condition]
    const currency = offer?.currency ?? defaultCurrencyCode.value
    const price =
      animatedPrices.value[condition] ??
      (typeof offer?.price === 'number' ? offer.price : null)
    const priceLabel = formatPriceLabel(price, currency)
    const priceCurrency = formatCurrencyDisplay(priceLabel, currency)
    const trend =
      condition === 'occasion' ? offers?.occasionTrend : offers?.newTrend
    const trendLabel = resolvePriceTrendLabel(trend, currency)
    const trendTone = resolvePriceTrendTone(trend)
    const trendIcon = resolveTrendIcon(trendTone)
    const trendTooltip = formatTrendTooltip(trend, currency)

    return {
      condition,
      conditionLabel: t(`product.hero.offerConditions.${condition}`),
      offersCountLabel: offersCount.value ? offersCountLabel.value : null,
      priceTitle: t('product.hero.bestPriceTitle'),
      priceLabel,
      priceCurrency,
      hasOffer: typeof offer?.price === 'number',
      emptyStateLabel: t(`product.hero.noOffers.${condition}`),
      merchant: createMerchant(offer),
      offerName: offer?.offerName ?? null,
      trendLabel,
      trendTooltip,
      trendToneClass: `product-hero__price-trend--${trendTone}`,
      trendIcon,
      viewOffersLabel: viewOffersLabel.value,
    }
  })
})
</script>

<style scoped>
.product-hero__pricing-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
}

.product-hero__pricing-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>

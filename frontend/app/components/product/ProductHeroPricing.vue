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
        :alternative-offers="panel.alternativeOffers"
        :alternative-offers-label="panel.alternativeOffersLabel"
        :alternative-offers-placeholder="panel.alternativeOffersPlaceholder"
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
import { useProductPriceTrend } from '~/composables/useProductPriceTrend'
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

const { n, t, locale } = useI18n()
const {
  trackProductRedirect,
  trackAffiliateClick,
  isClientContribLink,
  extractTokenFromLink,
} = useAnalytics()

type AggregatedOffer = NonNullable<
  NonNullable<ProductDto['offers']>['bestPrice']
>

type AlternativeOfferOption = {
  id: string
  label: string
  offerName: string | null
  priceLabel: string
  favicon: string | null
  url: string | null
}

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

const conditionOrder = computed<OfferCondition[]>(() => {
  const hasOccasion = !!props.product.offers?.bestOccasionOffer
  return hasOccasion ? ['occasion', 'new'] : ['new', 'occasion']
})

const defaultCurrencyCode = computed(
  () => aggregatedBestOffer.value?.currency ?? 'EUR'
)

const offersByCondition = computed(
  () => props.product.offers?.offersByCondition ?? {}
)

const resolveOffersForCondition = (condition: OfferCondition) => {
  const normalized = condition.toLowerCase()
  const entries = Object.entries(offersByCondition.value)
  return entries.flatMap(([key, offers]) => {
    if (key.toLowerCase() !== normalized || !Array.isArray(offers)) {
      return []
    }

    return offers
  })
}

const offersCountByCondition = computed(() => ({
  new: resolveOffersForCondition('new').length,
  occasion: resolveOffersForCondition('occasion').length,
}))

const isSameOffer = (
  offer: AggregatedOffer,
  bestOffer: AggregatedOffer | null
) => {
  if (!bestOffer) {
    return false
  }

  return (
    offer.url === bestOffer.url &&
    offer.price === bestOffer.price &&
    offer.datasourceName === bestOffer.datasourceName
  )
}

const buildAlternativeOffers = (condition: OfferCondition) => {
  const bestOffer = bestOffersByCondition.value[condition]
  return resolveOffersForCondition(condition)
    .filter(offer => !isSameOffer(offer, bestOffer))
    .map((offer, index) => {
      const currency = offer.currency ?? defaultCurrencyCode.value
      const priceLabel = n(offer.price ?? 0, {
        style: 'currency',
        currency,
        maximumFractionDigits: 2,
      })

      return {
        id: `${condition}-${offer.datasourceName ?? 'offer'}-${offer.price ?? index}-${index}`,
        label:
          offer.datasourceName ??
          t('product.hero.alternativeOffers.unknownMerchant'),
        offerName: offer.offerName ?? null,
        priceLabel,
        favicon: offer.favicon ?? null,
        url: offer.url ?? null,
      }
    })
}

const alternativeOffersByCondition = computed<
  Record<OfferCondition, AlternativeOfferOption[]>
>(() => ({
  new: buildAlternativeOffers('new'),
  occasion: buildAlternativeOffers('occasion'),
}))

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

  const loc = import.meta.client ? undefined : locale.value
  const isEuro = currency === 'EUR'

  return new Intl.NumberFormat(loc, {
    style: isEuro ? 'decimal' : 'currency',
    currency: isEuro ? undefined : currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 1,
  }).format(price)
}

const formatCurrencyDisplay = (priceLabel: string, currency: string) => {
  if (priceLabel === '—') {
    return null
  }

  return currency === 'EUR' ? '€' : currency
}

const {
  resolvePriceTrendLabel,
  resolvePriceTrendTone,
  resolveTrendIcon,
  formatTrendTooltip,
} = useProductPriceTrend()

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
  conditionOrder.value.forEach(condition => {
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
  return conditionOrder.value.map(condition => {
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

    const count = offersCountByCondition.value[condition]

    let viewOffersLabel = ''
    if (count <= 1) {
      viewOffersLabel = t('product.hero.viewSingleOffer')
    } else if (condition === 'new') {
      viewOffersLabel = t('product.hero.viewOffersNew', count)
    } else {
      viewOffersLabel = t('product.hero.viewOffersOccasion', count)
    }

    return {
      condition,
      conditionLabel: t(`product.hero.offerConditions.${condition}`),
      offersCountLabel:
        offersCountByCondition.value[condition] > 0
          ? t(
              'product.hero.offersCountLabel',
              offersCountByCondition.value[condition]
            )
          : null,
      priceTitle: t('product.hero.bestPriceTitle'),
      priceLabel,
      priceCurrency,
      hasOffer: typeof offer?.price === 'number',
      emptyStateLabel: t(`product.hero.noOffers.${condition}`),
      merchant: createMerchant(offer),
      offerName: offer?.offerName ?? null,
      alternativeOffers: alternativeOffersByCondition.value[condition],
      alternativeOffersLabel: t('product.hero.alternativeOffers.label'),
      alternativeOffersPlaceholder: t(
        'product.hero.alternativeOffers.placeholder'
      ),
      trendLabel,
      trendTooltip,
      trendToneClass: `product-hero__price-trend--${trendTone}`,
      trendIcon,
      viewOffersLabel,
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

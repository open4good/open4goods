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
    <meta itemprop="priceCurrency" :content="priceCurrencyCode" />

    <!-- Condition Tabs -->
    <v-tabs
      v-if="conditionOptions.length"
      v-model="selectedCondition"
      density="compact"
      color="primary"
      class="mb-4 product-hero__tabs"
      hide-slider
      height="36"
    >
      <v-tab
        v-for="option in conditionOptions"
        :key="option.value"
        :value="option.value"
        class="product-hero__tab"
        :class="{ 'product-hero__tab--active': option.selected }"
        rounded="pill"
        variant="flat"
      >
        {{ option.label }}
      </v-tab>
    </v-tabs>

    <v-row no-gutters class="align-center mt-2">
      <!-- Merchant / Favicon (Left) -->
      <v-col cols="12" md="6" class="d-flex align-center">
        <div v-if="bestPriceMerchant" class="product-hero__price-merchant">
          <template v-if="bestPriceMerchant.url">
            <ClientOnly v-if="bestPriceMerchant.clientOnly">
              <template #default>
                <NuxtLink
                  :to="bestPriceMerchant.url"
                  class="product-hero__price-merchant-link"
                  :target="bestPriceMerchant.isInternal ? undefined : '_blank'"
                  :rel="
                    bestPriceMerchant.isInternal
                      ? undefined
                      : 'nofollow noopener'
                  "
                  :prefetch="false"
                  @click="handleMerchantClick"
                >
                  <img
                    v-if="bestPriceMerchant.favicon"
                    :src="bestPriceMerchant.favicon"
                    :alt="bestPriceMerchant.name"
                    width="48"
                    height="48"
                    class="product-hero__price-merchant-favicon"
                  />
                  <span class="d-md-none ml-2">{{
                    bestPriceMerchant.name
                  }}</span>
                </NuxtLink>
              </template>
              <template #fallback>
                <div class="product-hero__price-merchant-static">
                  <img
                    v-if="bestPriceMerchant.favicon"
                    :src="bestPriceMerchant.favicon"
                    :alt="bestPriceMerchant.name"
                    width="48"
                    height="48"
                    class="product-hero__price-merchant-favicon"
                  />
                  <span class="d-md-none ml-2">{{
                    bestPriceMerchant.name
                  }}</span>
                </div>
              </template>
            </ClientOnly>
            <NuxtLink
              v-else
              :to="bestPriceMerchant.url"
              class="product-hero__price-merchant-link"
              :target="bestPriceMerchant.isInternal ? undefined : '_blank'"
              :rel="
                bestPriceMerchant.isInternal ? undefined : 'nofollow noopener'
              "
              :prefetch="false"
              @click="handleMerchantClick"
            >
              <img
                v-if="bestPriceMerchant.favicon"
                :src="bestPriceMerchant.favicon"
                :alt="bestPriceMerchant.name"
                width="48"
                height="48"
                class="product-hero__price-merchant-favicon"
              />
              <span class="d-md-none ml-2">{{ bestPriceMerchant.name }}</span>
            </NuxtLink>
          </template>
          <div v-else class="product-hero__price-merchant-static">
            <img
              v-if="bestPriceMerchant.favicon"
              :src="bestPriceMerchant.favicon"
              :alt="bestPriceMerchant.name"
              width="48"
              height="48"
              class="product-hero__price-merchant-favicon"
            />
            <span class="d-md-none ml-2">{{ bestPriceMerchant.name }}</span>
          </div>
        </div>
      </v-col>

      <!-- Price (Right) -->
      <v-col
        cols="12"
        md="6"
        class="text-right d-flex flex-column align-end justify-center"
      >
        <p
          class="product-hero__pricing-title mb-1 text-subtitle-2 text-medium-emphasis"
        >
          {{ $t('product.hero.bestPriceTitle') }}
        </p>
        <div class="product-hero__price">
          <span class="product-hero__price-value" itemprop="lowPrice">
            {{ bestPriceLabel }}
          </span>
          <span v-if="displayCurrency" class="product-hero__price-currency">
            {{ displayCurrency }}
          </span>
        </div>
      </v-col>
    </v-row>

    <!-- Trend & Meta info -->
    <div
      class="product-hero__pricing-footer d-flex align-center justify-space-between mt-4"
    >
      <!-- Trend -->
      <button
        v-if="priceTrendLabel"
        type="button"
        class="product-hero__price-trend ma-0"
        :class="priceTrendToneClass"
        @click="scrollToSelector('#price-history')"
      >
        <v-icon
          :icon="priceTrendIcon"
          size="18"
          class="product-hero__price-trend-icon"
        />
        <span>{{ priceTrendLabel }}</span>
      </button>

      <!-- CTA View Offers -->
      <div class="product-hero__price-actions ml-auto">
        <v-btn
          color="primary"
          variant="flat"
          @click="scrollToSelector('#offers-list', 136)"
        >
          {{ viewOffersLabel }}
        </v-btn>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAnalytics } from '~/composables/useAnalytics'
import type { ProductDto } from '~~/shared/api-client'

type OfferCondition = 'occasion' | 'new'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { n, t } = useI18n()
const { trackProductRedirect, isClientContribLink, extractTokenFromLink } =
  useAnalytics()

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

const conditionPriority: OfferCondition[] = ['occasion', 'new']

const availableConditions = computed<OfferCondition[]>(() =>
  conditionPriority.filter(condition =>
    Boolean(bestOffersByCondition.value[condition])
  )
)

const selectedCondition = ref<OfferCondition>(conditionPriority[0])

watch(
  availableConditions,
  conditions => {
    if (!conditions.length) {
      return
    }

    if (!conditions.includes(selectedCondition.value)) {
      selectedCondition.value = conditions[0]
    }
  },
  { immediate: true }
)

const activeCondition = computed<OfferCondition>(() => {
  if (availableConditions.value.includes(selectedCondition.value)) {
    return selectedCondition.value
  }

  return availableConditions.value[0] ?? 'new'
})

const activeOffer = computed<AggregatedOffer | null>(
  () => bestOffersByCondition.value[activeCondition.value]
)

const conditionOptions = computed(() =>
  availableConditions.value.map(condition => ({
    value: condition,
    label: t(`product.hero.offerConditions.${condition}`),
    selected: condition === activeCondition.value,
  }))
)

const priceCurrencyCode = computed(
  () =>
    activeOffer.value?.currency ?? aggregatedBestOffer.value?.currency ?? 'EUR'
)

const bestPriceValue = computed(() =>
  typeof activeOffer.value?.price === 'number' ? activeOffer.value.price : null
)

const animatedPrice = ref<number | null>(null)

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
        currency: offer?.currency ?? priceCurrencyCode.value,
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
      currency: numericPrices[0]?.currency ?? priceCurrencyCode.value,
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
          priceCurrencyCode.value,
      }
    }
  })

  return ranges
})

const displayedPriceValue = computed(
  () => animatedPrice.value ?? bestPriceValue.value
)

const bestPriceLabel = computed(() => {
  const price = displayedPriceValue.value
  if (typeof price !== 'number') {
    return '—'
  }

  const currency = priceCurrencyCode.value

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
})

const displayCurrency = computed(() => {
  if (bestPriceLabel.value === '—') {
    return null
  }

  return priceCurrencyCode.value === 'EUR' ? '€' : priceCurrencyCode.value
})

const offersCount = computed(() => props.product.offers?.offersCount ?? 0)

const offersCountLabel = computed(() => n(offersCount.value))

const viewOffersLabel = computed(() =>
  offersCount.value <= 1
    ? t('product.hero.viewSingleOffer')
    : t('product.hero.viewOffersCount', { count: offersCountLabel.value })
)

const isSingleOffer = computed(() => offersCount.value === 1)

const affiliationLink = computed(() => {
  if (!isSingleOffer.value) {
    return null
  }

  const token =
    activeOffer.value?.affiliationToken ??
    aggregatedBestOffer.value?.affiliationToken
  return token ? `/contrib/${token}` : null
})

const bestPriceMerchant = computed(() => {
  const merchant = activeOffer.value
  if (!merchant?.datasourceName) {
    return null
  }

  const url = isSingleOffer.value
    ? (affiliationLink.value ?? merchant.url ?? null)
    : (merchant.url ?? null)

  return {
    name: merchant.datasourceName,
    url,
    favicon: merchant.favicon ?? null,
    isInternal: typeof url === 'string' && url.startsWith('/'),
    clientOnly: isSingleOffer.value && Boolean(affiliationLink.value),
  }
})

const handleMerchantClick = () => {
  const merchant = bestPriceMerchant.value
  const link = merchant?.url

  if (!isClientContribLink(link)) {
    return
  }

  trackProductRedirect({
    token: extractTokenFromLink(link),
    placement: 'product-hero',
    source: merchant?.name ?? null,
    url: link,
  })
}

const priceTrend = computed(() => {
  const offers = props.product.offers
  if (!offers) {
    return null
  }

  const trend =
    activeCondition.value === 'occasion'
      ? offers.occasionTrend
      : offers.newTrend
  return trend ?? null
})

const priceTrendLabel = computed(() => {
  const trend = priceTrend.value
  if (!trend) {
    return null
  }

  const currency = priceCurrencyCode.value

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
})

const priceTrendTone = computed<'decrease' | 'increase' | 'stable'>(() => {
  const trendType = priceTrend.value?.trend
  if (trendType === 'PRICE_DECREASE') {
    return 'decrease'
  }

  if (trendType === 'PRICE_INCREASE') {
    return 'increase'
  }

  return 'stable'
})

const priceTrendIcon = computed(() => {
  switch (priceTrendTone.value) {
    case 'decrease':
      return 'mdi-trending-down'
    case 'increase':
      return 'mdi-trending-up'
    default:
      return 'mdi-trending-neutral'
  }
})

const priceTrendToneClass = computed(
  () => `product-hero__price-trend--${priceTrendTone.value}`
)

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
const animationFrameId = ref<number | null>(null)

const startPriceCountdown = (condition: OfferCondition) => {
  if (!import.meta.client) {
    return
  }

  if (animatedConditions.value[condition]) {
    animatedPrice.value = null
    return
  }

  const range = priceRangesByCondition.value[condition]
  const targetPrice = bestOffersByCondition.value[condition]?.price

  if (!range || typeof targetPrice !== 'number') {
    animatedPrice.value = null
    return
  }

  const startValue = Number.isFinite(range.max) ? range.max : targetPrice
  const endValue = Number.isFinite(range.min) ? range.min : targetPrice

  if (
    !Number.isFinite(startValue) ||
    !Number.isFinite(endValue) ||
    startValue === endValue
  ) {
    animatedPrice.value = null
    animatedConditions.value[condition] = true
    return
  }

  const duration = 4000
  const easing = (progress: number) => 1 - Math.pow(1 - progress, 2.6)
  const startedAt = performance.now()

  if (animationFrameId.value != null) {
    cancelAnimationFrame(animationFrameId.value)
  }

  const tick = (timestamp: number) => {
    const elapsed = timestamp - startedAt
    const progress = Math.min(Math.max(elapsed / duration, 0), 1)
    const eased = easing(progress)
    animatedPrice.value = startValue + (endValue - startValue) * eased

    if (progress < 1) {
      animationFrameId.value = requestAnimationFrame(tick)
      return
    }

    animatedPrice.value = null
    animationFrameId.value = null
    animatedConditions.value[condition] = true
  }

  animatedPrice.value = startValue
  animationFrameId.value = requestAnimationFrame(tick)
}

watch(
  activeCondition,
  condition => {
    if (!import.meta.client) {
      return
    }

    startPriceCountdown(condition)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (animationFrameId.value != null) {
    cancelAnimationFrame(animationFrameId.value)
  }
})
</script>

<style scoped>
.product-hero__pricing-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
}

/* CSS removed as part of refactoring to v-tabs */

.product-hero__pricing-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.product-hero__pricing-title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 700;
  flex: 1 1 100%;
}

.product-hero__price {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.product-hero__price-value {
  font-size: clamp(2rem, 3.4vw, 2.6rem);
  font-weight: 700;
}

.product-hero__price-currency {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-hero__price-meta {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-start;
}

.product-hero__price-merchant {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
}

.product-hero__price-merchant-link,
.product-hero__price-merchant-static {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.45rem 0.75rem;
  border-radius: 16px;
  font-weight: 600;
  text-decoration: none;
  background: rgba(var(--v-theme-surface-default), 0.92);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__price-merchant-link {
  color: rgb(var(--v-theme-primary));
}

.product-hero__price-merchant-link:hover,
.product-hero__price-merchant-link:focus-visible {
  box-shadow: 0 16px 36px rgba(var(--v-theme-primary), 0.2);
}

.product-hero__price-merchant-favicon {
  border-radius: 4px;
  width: 48px;
  height: 48px;
  object-fit: cover;
}

.product-hero__price-trend {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.6rem;
  border: none;
  border-radius: 999px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
  background: transparent;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__price-trend:hover,
.product-hero__price-trend:focus-visible {
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.14);
}

.product-hero__price-trend-icon {
  transition: color 0.2s ease;
}

.product-hero__price-trend--stable {
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-hero__price-trend--stable .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-accent-primary-highlight));
}

.product-hero__price-trend--stable:hover,
.product-hero__price-trend--stable:focus-visible {
  background: rgba(var(--v-theme-surface-primary-100), 0.85);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-hero__price-trend--decrease {
  background: rgba(var(--v-theme-primary), 0.14);
  color: rgb(var(--v-theme-primary));
}

.product-hero__price-trend--decrease .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-primary));
}

.product-hero__price-trend--decrease:hover,
.product-hero__price-trend--decrease:focus-visible {
  background: rgba(var(--v-theme-primary), 0.22);
}

.product-hero__price-trend--increase {
  background: rgba(var(--v-theme-error), 0.18);
  color: rgb(var(--v-theme-error));
}

.product-hero__price-trend--increase .product-hero__price-trend-icon {
  color: rgb(var(--v-theme-error));
}

.product-hero__price-trend--increase:hover,
.product-hero__price-trend--increase:focus-visible {
  background: rgba(var(--v-theme-error), 0.26);
}

.product-hero__price-actions {
  margin-top: 0.5rem;
}
</style>

<template>
  <div
    ref="pricingWrapper"
    class="product-hero__pricing-wrapper"
    :class="{ 'product-hero__pricing-wrapper--affixed': shouldAffixToViewport }"
  >
    <div
      v-if="shouldAffixToViewport && placeholderHeight > 0"
      class="product-hero__pricing-placeholder"
      aria-hidden="true"
      :style="{ height: `${placeholderHeight}px` }"
    />
    <div
      ref="pricingCard"
      class="product-hero__pricing-card"
      :class="{ 'product-hero__pricing-card--affixed': shouldAffixToViewport }"
      :style="affixedStyles"
      itemprop="offers"
      itemscope
      itemtype="https://schema.org/AggregateOffer"
      v-bind="$attrs"
    >
    <meta itemprop="offerCount" :content="String(product.offers?.offersCount ?? 0)" />
    <meta itemprop="priceCurrency" :content="priceCurrencyCode" />
    <div
      v-if="conditionOptions.length"
      class="product-hero__price-conditions"
      role="group"
      :aria-label="conditionToggleAriaLabel"
    >
      <button
        v-for="option in conditionOptions"
        :key="option.value"
        type="button"
        class="product-hero__price-chip"
        :class="{ 'product-hero__price-chip--active': option.selected }"
        :aria-pressed="option.selected"
        :disabled="!hasConditionToggle && !option.selected"
        @click="selectCondition(option.value)"
      >
        {{ option.label }}
      </button>
    </div>
    <div class="product-hero__pricing-header">
      <button
        v-if="priceTrendLabel"
        type="button"
        class="product-hero__price-trend"
        :class="priceTrendToneClass"
        @click="scrollToSelector('#price-history')"
      >
        <v-icon :icon="priceTrendIcon" size="18" class="product-hero__price-trend-icon" />
        <span>{{ priceTrendLabel }}</span>
      </button>

      <h2 class="product-hero__pricing-title">
        {{ $t('product.hero.bestPriceTitle') }}
      </h2>
    </div>
    <div class="product-hero__price">
      <span class="product-hero__price-value" itemprop="lowPrice">
        {{ bestPriceLabel }}
      </span>
      <span v-if="displayCurrency" class="product-hero__price-currency">
        {{ displayCurrency }}
      </span>
    </div>
    <div class="product-hero__price-meta">
      <div v-if="bestPriceMerchant" class="product-hero__price-merchant">
        <template v-if="bestPriceMerchant.url">
          <ClientOnly v-if="bestPriceMerchant.clientOnly">
            <template #default>
              <NuxtLink
                :to="bestPriceMerchant.url"
                class="product-hero__price-merchant-link"
                :target="bestPriceMerchant.isInternal ? undefined : '_blank'"
                :rel="bestPriceMerchant.isInternal ? undefined : 'nofollow noopener'"
                :prefetch="false"
              >
                <img
                  v-if="bestPriceMerchant.favicon"
                  :src="bestPriceMerchant.favicon"
                  :alt="bestPriceMerchant.name"
                  width="48"
                  height="48"
                  class="product-hero__price-merchant-favicon"
                />
                <span>{{ bestPriceMerchant.name }}</span>
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
                <span>{{ bestPriceMerchant.name }}</span>
              </div>
            </template>
          </ClientOnly>
          <NuxtLink
            v-else
            :to="bestPriceMerchant.url"
            class="product-hero__price-merchant-link"
            :target="bestPriceMerchant.isInternal ? undefined : '_blank'"
            :rel="bestPriceMerchant.isInternal ? undefined : 'nofollow noopener'"
            :prefetch="false"
          >
            <img
              v-if="bestPriceMerchant.favicon"
              :src="bestPriceMerchant.favicon"
              :alt="bestPriceMerchant.name"
              width="48"
              height="48"
              class="product-hero__price-merchant-favicon"
            />
            <span>{{ bestPriceMerchant.name }}</span>
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
          <span>{{ bestPriceMerchant.name }}</span>
        </div>
      </div>

    </div>
    <div class="product-hero__price-actions">
      <v-btn color="primary" variant="flat" @click="scrollToSelector('#offers-list', 136)">
        {{ viewOffersLabel }}
      </v-btn>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useEventListener, useResizeObserver } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import type { ProductDto } from '~~/shared/api-client'
import { STICKY_VIEWPORT_OFFSET } from './productHeroSticky.constants'

type OfferCondition = 'occasion' | 'new'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { n, t } = useI18n()
const display = useDisplay()
const emit = defineEmits<{
  (event: 'affix-change' | 'hero-visibility-change', value: boolean): void
}>()

const pricingWrapper = ref<HTMLElement | null>(null)
const pricingCard = ref<HTMLElement | null>(null)
const heroSection = ref<HTMLElement | null>(null)
const isAffixed = ref(false)
const placeholderHeight = ref(0)
const affixInlineStart = ref(0)
const affixWidth = ref(0)

const isDesktop = computed(() => display.mdAndUp.value)

const heroHasExitedViewport = ref(false)

const shouldAffixToViewport = computed(
  () => isDesktop.value && isAffixed.value && affixWidth.value > 0,
)

const affixedStyles = computed(() => {
  if (!shouldAffixToViewport.value) {
    return undefined
  }

  return {
    insetInlineStart: `${affixInlineStart.value}px`,
    width: `${affixWidth.value}px`,
    maxWidth: `${affixWidth.value}px`,
  }
})

const recalculateAffixMetrics = () => {
  if (!import.meta.client) {
    return
  }

  const wrapperElement = pricingWrapper.value
  if (!wrapperElement) {
    return
  }

  const wrapperRect = wrapperElement.getBoundingClientRect()
  affixInlineStart.value = wrapperRect.left
  affixWidth.value = wrapperRect.width
}

const updateAffixState = () => {
  if (!import.meta.client) {
    return
  }

  const heroElement = heroSection.value
  if (!heroElement) {
    heroHasExitedViewport.value = false
    isAffixed.value = false
    return
  }

  const { bottom } = heroElement.getBoundingClientRect()
  const hasExited = bottom <= STICKY_VIEWPORT_OFFSET + 16
  heroHasExitedViewport.value = hasExited

  if (!isDesktop.value) {
    isAffixed.value = false
    return
  }

  isAffixed.value = hasExited
}

if (import.meta.client) {
  useResizeObserver(pricingCard, (entries) => {
    const [entry] = entries
    if (entry) {
      placeholderHeight.value = entry.contentRect.height
    }
  })

  useResizeObserver(pricingWrapper, () => {
    recalculateAffixMetrics()
  })
}

let stopScrollListener: (() => void) | undefined
let stopResizeListener: (() => void) | undefined

onMounted(async () => {
  if (!import.meta.client) {
    return
  }

  heroSection.value = pricingCard.value?.closest('.product-hero') as HTMLElement | null

  await nextTick()
  recalculateAffixMetrics()
  updateAffixState()

  stopScrollListener = useEventListener(window, 'scroll', updateAffixState, { passive: true })
  stopResizeListener = useEventListener(window, 'resize', () => {
    recalculateAffixMetrics()
    updateAffixState()
  })
})

onBeforeUnmount(() => {
  stopScrollListener?.()
  stopResizeListener?.()
})

watch(isDesktop, (enabled) => {
  if (!import.meta.client) {
    return
  }

  if (!enabled) {
    isAffixed.value = false
    return
  }

  nextTick().then(() => {
    recalculateAffixMetrics()
    updateAffixState()
  })
})

watch(
  shouldAffixToViewport,
  (value) => {
    emit('affix-change', value)
  },
  { immediate: true },
)

watch(
  heroHasExitedViewport,
  (value) => {
    emit('hero-visibility-change', value)
  },
  { immediate: true },
)

type AggregatedOffer = NonNullable<NonNullable<ProductDto['offers']>['bestPrice']>

const aggregatedBestOffer = computed<AggregatedOffer | null>(() => props.product.offers?.bestPrice ?? null)

const bestOffersByCondition = computed<Record<OfferCondition, AggregatedOffer | null>>(() => {
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
  conditionPriority.filter((condition) => Boolean(bestOffersByCondition.value[condition])),
)

const selectedCondition = ref<OfferCondition>(conditionPriority[0])

watch(
  availableConditions,
  (conditions) => {
    if (!conditions.length) {
      return
    }

    if (!conditions.includes(selectedCondition.value)) {
      selectedCondition.value = conditions[0]
    }
  },
  { immediate: true },
)

const activeCondition = computed<OfferCondition>(() => {
  if (availableConditions.value.includes(selectedCondition.value)) {
    return selectedCondition.value
  }

  return availableConditions.value[0] ?? 'new'
})

const selectCondition = (condition: OfferCondition) => {
  if (!availableConditions.value.includes(condition)) {
    return
  }

  selectedCondition.value = condition
}

const activeOffer = computed<AggregatedOffer | null>(() => bestOffersByCondition.value[activeCondition.value])

const conditionOptions = computed(() =>
  availableConditions.value.map((condition) => ({
    value: condition,
    label: t(`product.hero.offerConditions.${condition}`),
    selected: condition === activeCondition.value,
  })),
)

const hasConditionToggle = computed(() => conditionOptions.value.length > 1)

const conditionToggleAriaLabel = computed(() => t('product.hero.offerConditionsToggleAria'))

const priceCurrencyCode = computed(
  () => activeOffer.value?.currency ?? aggregatedBestOffer.value?.currency ?? 'EUR',
)

const bestPriceLabel = computed(() => {
  const price = activeOffer.value?.price
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
    : t('product.hero.viewOffersCount', { count: offersCountLabel.value }),
)

const isSingleOffer = computed(() => offersCount.value === 1)

const affiliationLink = computed(() => {
  if (!isSingleOffer.value) {
    return null
  }

  const token = activeOffer.value?.affiliationToken ?? aggregatedBestOffer.value?.affiliationToken
  return token ? `/contrib/${token}` : null
})

const bestPriceMerchant = computed(() => {
  const merchant = activeOffer.value
  if (!merchant?.datasourceName) {
    return null
  }

  const url = isSingleOffer.value
    ? affiliationLink.value ?? merchant.url ?? null
    : merchant.url ?? null

  return {
    name: merchant.datasourceName,
    url,
    favicon: merchant.favicon ?? null,
    isInternal: typeof url === 'string' && url.startsWith('/'),
    clientOnly: isSingleOffer.value && Boolean(affiliationLink.value),
  }
})

const priceTrend = computed(() => {
  const offers = props.product.offers
  if (!offers) {
    return null
  }

  const trend = activeCondition.value === 'occasion' ? offers.occasionTrend : offers.newTrend
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

const priceTrendToneClass = computed(() => `product-hero__price-trend--${priceTrendTone.value}`)

const scrollToSelector = (selector: string, offset = 120) => {
  if (!import.meta.client) {
    return
  }

  const target = document.querySelector<HTMLElement>(selector)
  if (!target) {
    return
  }

  const top = target.getBoundingClientRect().top + (window.scrollY || window.pageYOffset || 0) - offset
  window.scrollTo({ top: Math.max(0, top), behavior: 'smooth' })
}
</script>

<style scoped>
.product-hero__pricing-card {
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.9);
  padding: 1.75rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__price-conditions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.product-hero__price-chip {
  border: none;
  border-radius: 999px;
  padding: 0.35rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  background: rgba(var(--v-theme-surface-default), 0.85);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.product-hero__price-chip:hover,
.product-hero__price-chip:focus-visible {
  background: rgba(var(--v-theme-surface-default), 1);
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.12);
}

.product-hero__price-chip--active {
  background: rgba(var(--v-theme-primary), 0.18);
  color: rgb(var(--v-theme-primary));
  box-shadow: 0 8px 20px rgba(var(--v-theme-primary), 0.18);
}

.product-hero__price-chip:disabled {
  cursor: default;
  box-shadow: none;
}

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
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
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

.product-hero__pricing-wrapper {
  position: relative;
}

.product-hero__pricing-placeholder {
  display: none;
  width: 100%;
}

.product-hero__pricing-wrapper--affixed .product-hero__pricing-placeholder {
  display: block;
}

@media (min-width: 960px) {
  .product-hero__pricing-card {
    position: sticky;
    top: calc(64px + 1.5rem);
    z-index: 2;
  }

  .product-hero__pricing-card--affixed {
    position: fixed;
    z-index: 24;
  }
}
</style>

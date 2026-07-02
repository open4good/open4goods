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

    <div v-if="visiblePanels.length === 0" class="product-hero__pricing-empty">
      <v-icon icon="mdi-basket-off-outline" size="32" />
      <p class="product-hero__pricing-empty-title">
        {{ t('product.hero.noOffersUnified') }}
      </p>
    </div>

    <v-tabs
      v-else-if="useTabbedLayout"
      v-model="activeTab"
      density="comfortable"
      color="primary"
      align-tabs="center"
      grow
      class="product-hero__pricing-tabs"
      :aria-label="t('product.hero.offerConditionsToggleAria')"
    >
      <v-tab
        v-for="panel in visiblePanels"
        :key="panel.condition"
        :value="panel.condition"
      >
        <v-icon
          :icon="
            panel.condition === 'new'
              ? 'mdi-tag-outline'
              : 'mdi-recycle-variant'
          "
          size="18"
          class="me-1"
        />
        {{ panel.conditionLabel }}
        <v-chip
          v-if="panel.offersCount > 0"
          size="x-small"
          variant="tonal"
          class="ms-2"
        >
          {{ panel.offersCount }}
        </v-chip>
      </v-tab>
    </v-tabs>

    <v-window
      v-if="useTabbedLayout && visiblePanels.length > 0"
      v-model="activeTab"
      class="product-hero__pricing-window"
    >
      <v-window-item
        v-for="panel in visiblePanels"
        :key="panel.condition"
        :value="panel.condition"
      >
        <ProductHeroPricingPanel
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
          :offers-list="panel.offersList"
          :offers-list-label="panel.offersListLabel"
          :more-offers-label="panel.moreOffersLabel"
          :trend-label="panel.trendLabel"
          :trend-tooltip="panel.trendTooltip"
          :trend-tone-class="panel.trendToneClass"
          :trend-icon="panel.trendIcon"
          :view-offers-label="panel.viewOffersLabel"
          :hide-header="true"
          @merchant-click="handleMerchantClick"
          @trend-click="scrollToSelector('#price-history')"
          @view-offers="handleViewOffers(panel)"
        />
      </v-window-item>
    </v-window>

    <div v-else class="product-hero__pricing-stack">
      <ProductHeroPricingPanel
        v-for="panel in visiblePanels"
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
        :offers-list="panel.offersList"
        :offers-list-label="panel.offersListLabel"
        :more-offers-label="panel.moreOffersLabel"
        :trend-label="panel.trendLabel"
        :trend-tooltip="panel.trendTooltip"
        :trend-tone-class="panel.trendToneClass"
        :trend-icon="panel.trendIcon"
        :view-offers-label="panel.viewOffersLabel"
        @merchant-click="handleMerchantClick"
        @trend-click="scrollToSelector('#price-history')"
        @view-offers="handleViewOffers(panel)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, type PropType } from 'vue'
import { useRouter } from '#imports'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import { useAnalytics } from '~/composables/useAnalytics'
import { useProductPriceTrend } from '~/composables/useProductPriceTrend'
import type { ProductDto } from '~~/shared/api-client'
import ProductHeroPricingPanel from '~/components/product/ProductHeroPricingPanel.vue'

type OfferCondition = 'occasion' | 'new'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { n, t, locale } = useI18n()
const router = useRouter()
const display = useDisplay()
const {
  trackProductRedirect,
  trackAffiliateClick,
  isClientContribLink,
  extractTokenFromLink,
} = useAnalytics()

type AggregatedOffer = NonNullable<
  NonNullable<ProductDto['offers']>['bestPrice']
>

type OfferOption = {
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

  // Compare merchant + price only; URLs can differ across affiliation tokens
  // for the same underlying offer.
  return (
    offer.price === bestOffer.price &&
    offer.datasourceName === bestOffer.datasourceName
  )
}

const buildOffersList = (condition: OfferCondition): OfferOption[] => {
  const bestOffer = bestOffersByCondition.value[condition]
  const allOffers = resolveOffersForCondition(condition)

  // Alternatives only: the best offer is already promoted in the hero block.
  const alternatives = allOffers.filter(offer => !isSameOffer(offer, bestOffer))

  return alternatives.map((offer, index) => {
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

const offersListByCondition = computed<Record<OfferCondition, OfferOption[]>>(
  () => ({
    new: buildOffersList('new'),
    occasion: buildOffersList('occasion'),
  })
)

const offersCount = computed(() => props.product.offers?.offersCount ?? 0)

const isSingleOffer = computed(() => offersCount.value === 1)

const productGtin = computed(
  () => props.product.gtin ?? props.product.base?.gtin
)
const productVertical = computed(() => props.product.base?.vertical ?? null)
const productCategorySlug = computed(() => {
  const fullSlug = props.product.fullSlug?.trim()
  if (!fullSlug) {
    return null
  }

  return fullSlug.split('/').filter(Boolean)[0] ?? null
})

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
    merchantName: payload.name ?? null,
    placement: 'product-hero',
    productId: productGtin.value ?? null,
    gtin: productGtin.value ?? null,
    vertical: productVertical.value,
    categorySlug: productCategorySlug.value,
    offerRank: 1,
    price: aggregatedBestOffer.value?.price ?? null,
    currency: aggregatedBestOffer.value?.currency ?? null,
    condition: aggregatedBestOffer.value?.condition ?? null,
  })
}

const formatPriceLabel = (price: number | null, currency: string) => {
  if (typeof price !== 'number') {
    return '—'
  }

  const isEuro = currency === 'EUR'

  return new Intl.NumberFormat(locale.value, {
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

type ConditionPanel = {
  condition: OfferCondition
  conditionLabel: string
  offersCount: number
  offersCountLabel: string | null
  priceTitle: string
  priceLabel: string
  priceCurrency: string | null
  hasOffer: boolean
  emptyStateLabel: string
  merchant: {
    name: string
    url: string | null
    favicon: string | null
    isInternal: boolean
    clientOnly: boolean
  } | null
  offerName: string | null
  offersList: OfferOption[]
  offersListLabel: string
  moreOffersLabel: string | null
  trendLabel: string | null
  trendTooltip: string | null
  trendToneClass: string
  trendIcon: string | null
  viewOffersLabel: string
}

const handleViewOffers = (panel: ConditionPanel) => {
  if (isSingleOffer.value && panel.merchant?.url) {
    handleMerchantClick({
      name: panel.merchant.name,
      url: panel.merchant.url,
    })

    if (panel.merchant.isInternal) {
      router.push(panel.merchant.url)
    } else {
      window.open(panel.merchant.url, '_blank', 'noopener,noreferrer')
    }
  } else {
    scrollToSelector('#prix', 136)
  }
}

const conditionPanels = computed<ConditionPanel[]>(() => {
  const offers = props.product.offers
  return conditionOrder.value.map(condition => {
    const offer = bestOffersByCondition.value[condition]
    const currency = offer?.currency ?? defaultCurrencyCode.value
    const price = typeof offer?.price === 'number' ? offer.price : null
    const priceLabel = formatPriceLabel(price, currency)
    const priceCurrency = formatCurrencyDisplay(priceLabel, currency)
    const trend =
      condition === 'occasion' ? offers?.occasionTrend : offers?.newTrend
    const trendLabel = resolvePriceTrendLabel(trend, currency)
    const trendTone = resolvePriceTrendTone(trend)
    const trendIcon = resolveTrendIcon(trendTone)
    const trendTooltip = formatTrendTooltip(trend, currency)

    const count = offersCountByCondition.value[condition]
    const offersList = offersListByCondition.value[condition]

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
      offersCount: count,
      offersCountLabel:
        count > 0 ? t('product.hero.offersCountLabel', count) : null,
      priceTitle: t('product.hero.bestPriceTitle'),
      priceLabel,
      priceCurrency,
      hasOffer: typeof offer?.price === 'number',
      emptyStateLabel: t(`product.hero.noOffers.${condition}`),
      merchant: createMerchant(offer),
      offerName: offer?.offerName ?? null,
      offersList,
      offersListLabel: t('product.hero.alternativeOffers.label'),
      moreOffersLabel:
        count - offersList.length - 1 > 0
          ? t('product.hero.moreOffersBadge', count - offersList.length - 1)
          : null,
      trendLabel,
      trendTooltip,
      trendToneClass: `product-hero__price-trend--${trendTone}`,
      trendIcon,
      viewOffersLabel,
    }
  })
})

const visiblePanels = computed<ConditionPanel[]>(() => {
  // Filter out fully empty conditions when there's at least one with offers
  const panels = conditionPanels.value
  const anyHasOffer = panels.some(panel => panel.hasOffer)
  if (!anyHasOffer) {
    return []
  }
  return panels.filter(panel => panel.hasOffer)
})

const useTabbedLayout = computed(
  () => display.mdAndDown.value && visiblePanels.value.length > 1
)

const activeTab = ref<OfferCondition>(
  visiblePanels.value[0]?.condition ?? 'new'
)

watch(visiblePanels, panels => {
  if (
    panels.length > 0 &&
    !panels.some(panel => panel.condition === activeTab.value)
  ) {
    activeTab.value = panels[0].condition
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

.product-hero__pricing-stack {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-hero__pricing-tabs {
  border-radius: 16px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.7);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  margin-bottom: 0.5rem;
  overflow: hidden;
}

.product-hero__pricing-window {
  width: 100%;
}

.product-hero__pricing-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem 1.5rem;
  border-radius: 24px;
  background: rgba(var(--v-theme-surface-primary-050), 0.6);
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.25);
  color: rgb(var(--v-theme-text-neutral-secondary));
  text-align: center;
}

.product-hero__pricing-empty-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}
</style>

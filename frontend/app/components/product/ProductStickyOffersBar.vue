<template>
  <div
    v-if="visible && segments.length"
    ref="rootElement"
    class="product-sticky-offers"
    data-testid="sticky-offers-bar"
  >
    <v-container class="product-sticky-offers__container" max-width="xl">
      <div
        class="product-sticky-offers__content"
        :class="{ 'product-sticky-offers__content--mobile': isMobile }"
      >
        <div class="product-sticky-offers__intro">
          <v-icon
            class="product-sticky-offers__icon"
            color="primary"
            icon="mdi-heart-outline"
            size="26"
          />
          <p class="product-sticky-offers__message">
            {{ t('product.stickyOffers.message') }}
          </p>
        </div>

        <div
          class="product-sticky-offers__segments"
          :class="{ 'product-sticky-offers__segments--stacked': isMobile }"
        >
          <div
            v-for="segment in segments"
            :key="segment.key"
            class="product-sticky-offers__segment"
            :data-testid="`sticky-offer-segment-${segment.key}`"
          >
            <div class="product-sticky-offers__segment-header">
              <span class="product-sticky-offers__segment-title">{{ segment.title }}</span>
              <div
                v-if="segment.selectedOffer"
                class="product-sticky-offers__segment-summary"
              >
                <img
                  v-if="segment.selectedOffer.favicon"
                  :src="segment.selectedOffer.favicon"
                  :alt="segment.selectedOffer.merchantName"
                  width="20"
                  height="20"
                >
                <span class="product-sticky-offers__segment-merchant">
                  {{ segment.selectedOffer.merchantName }}
                </span>
                <span class="product-sticky-offers__segment-price">
                  {{ segment.selectedOffer.priceLabel }}
                </span>
              </div>
            </div>

            <v-select
              class="product-sticky-offers__select"
              density="comfortable"
              variant="outlined"
              hide-details
              item-title="label"
              item-value="id"
              :items="segment.options"
              :model-value="segment.selectedId"
              :aria-label="segment.selectAriaLabel"
              :menu-props="{ maxHeight: 340 }"
              @update:model-value="(value) => handleSelect(segment.key, value)"
            >
              <template #selection="{ item }">
                <div class="product-sticky-offers__selection">
                  <img
                    v-if="item.raw?.favicon"
                    :src="item.raw.favicon"
                    :alt="item.raw.merchantName"
                    width="20"
                    height="20"
                  >
                  <span class="product-sticky-offers__selection-merchant">
                    {{ item.raw?.merchantName ?? '' }}
                  </span>
                  <span class="product-sticky-offers__selection-price">
                    {{ item.raw?.priceLabel ?? '' }}
                  </span>
                </div>
              </template>
              <template #item="{ item, props: slotProps }">
                <v-list-item v-bind="slotProps" class="product-sticky-offers__item">
                  <template #prepend>
                    <img
                      v-if="item.raw?.favicon"
                      :src="item.raw.favicon"
                      :alt="item.raw?.merchantName ?? ''"
                      width="20"
                      height="20"
                    >
                  </template>
                  <v-list-item-title>{{ item.raw?.merchantName ?? '' }}</v-list-item-title>
                  <v-list-item-subtitle>{{ item.raw?.priceLabel ?? '' }}</v-list-item-subtitle>
                </v-list-item>
              </template>
            </v-select>

            <v-btn
              :data-testid="`sticky-offer-button-${segment.key}`"
              class="product-sticky-offers__button"
              color="primary"
              variant="flat"
              :disabled="!segment.selectedOffer?.link"
              :to="segment.selectedOffer?.link && segment.selectedOffer.link.startsWith('/') ? segment.selectedOffer.link : undefined"
              :href="segment.selectedOffer?.link && !segment.selectedOffer.link.startsWith('/') ? segment.selectedOffer.link : undefined"
              :rel="segment.selectedOffer?.link && !segment.selectedOffer.link.startsWith('/') ? 'nofollow noopener' : undefined"
              :aria-label="segment.buttonAriaLabel"
            >
              {{ t('product.stickyOffers.buttonLabel') }}
            </v-btn>
          </div>
        </div>
      </div>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, type PropType } from 'vue'
import { useDisplay } from 'vuetify'
import { useI18n } from 'vue-i18n'
import type { ProductAggregatedPriceDto, ProductDto } from '~~/shared/api-client'

type OfferCondition = 'occasion' | 'new'

type OfferOption = {
  id: string
  label: string
  merchantName: string
  priceLabel: string
  favicon: string | null
  link: string | null
}

type OfferSegment = {
  key: OfferCondition
  title: string
  options: OfferOption[]
  selectedId: string | null
  selectedOffer: OfferOption | null
  selectAriaLabel: string
  buttonAriaLabel: string
}

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  visible: {
    type: Boolean,
    default: false,
  },
})

const { t, n } = useI18n()
const display = useDisplay()

const rootElement = ref<HTMLElement | null>(null)

const isMobile = computed(() => display.smAndDown.value)

const formatPriceLabel = (price: number, currency?: string | null) => {
  const resolvedCurrency = currency?.trim().length ? currency.trim() : 'EUR'

  if (resolvedCurrency !== 'EUR') {
    return n(price, {
      style: 'currency',
      currency: resolvedCurrency,
      maximumFractionDigits: price >= 100 ? 0 : 2,
    })
  }

  const formatted = n(price, {
    style: 'decimal',
    minimumFractionDigits: price >= 100 ? 0 : 2,
    maximumFractionDigits: price >= 100 ? 0 : 2,
  })

  return `${formatted} €`
}

const resolveOfferLink = (offer: Partial<ProductAggregatedPriceDto>) => {
  const token = typeof offer.affiliationToken === 'string' && offer.affiliationToken.trim().length
    ? offer.affiliationToken.trim()
    : null

  if (token) {
    return `/contrib/${token}`
  }

  const url = typeof offer.url === 'string' && offer.url.trim().length ? offer.url.trim() : null
  return url
}

const resolveOffersByCondition = (condition: OfferCondition) => {
  const offers = props.product.offers
  if (!offers) {
    return [] as ProductAggregatedPriceDto[]
  }

  const direct = condition === 'occasion' ? offers.occasionOffers : offers.newOffers
  if (Array.isArray(direct) && direct.length) {
    return direct as ProductAggregatedPriceDto[]
  }

  const byCondition = offers.offersByCondition ?? {}
  const normalizedKey = condition === 'occasion' ? 'OCCASION' : 'NEW'

  const entry = Object.entries(byCondition).find(([key]) => key?.toUpperCase?.() === normalizedKey)
  if (entry && Array.isArray(entry[1])) {
    return entry[1] as ProductAggregatedPriceDto[]
  }

  return [] as ProductAggregatedPriceDto[]
}

const unknownSourceLabel = computed(() => t('product.price.metrics.unknownSource'))

const buildOptions = (condition: OfferCondition) => {
  const offers = resolveOffersByCondition(condition)
  const fallbackCurrency = props.product.offers?.bestPrice?.currency ?? 'EUR'

  return offers
    .filter((offer): offer is ProductAggregatedPriceDto & { price: number } => typeof offer?.price === 'number' && Number.isFinite(offer.price))
    .slice()
    .sort((a, b) => (a.price ?? Number.POSITIVE_INFINITY) - (b.price ?? Number.POSITIVE_INFINITY))
    .map((offer, index) => {
      const merchant = typeof offer.datasourceName === 'string' && offer.datasourceName.trim().length
        ? offer.datasourceName.trim()
        : unknownSourceLabel.value

      const priceLabel = formatPriceLabel(offer.price, offer.currency ?? fallbackCurrency)
      const favicon = typeof offer.favicon === 'string' && offer.favicon.trim().length ? offer.favicon : null

      const token = typeof offer.affiliationToken === 'string' && offer.affiliationToken.trim().length
        ? offer.affiliationToken.trim()
        : null

      const url = typeof offer.url === 'string' && offer.url.trim().length ? offer.url.trim() : null

      const id = token ?? url ?? `${condition}-${index}`

      return {
        id,
        label: `${merchant} • ${priceLabel}`,
        merchantName: merchant,
        priceLabel,
        favicon,
        link: resolveOfferLink(offer),
      }
    })
}

const occasionOptions = computed(() => buildOptions('occasion'))
const newOptions = computed(() => buildOptions('new'))

const selectedOccasionId = ref<string | null>(null)
const selectedNewId = ref<string | null>(null)

watch(
  occasionOptions,
  (options) => {
    if (!options.length) {
      selectedOccasionId.value = null
      return
    }

    if (!options.some((option) => option.id === selectedOccasionId.value)) {
      selectedOccasionId.value = options[0]?.id ?? null
    }
  },
  { immediate: true },
)

watch(
  newOptions,
  (options) => {
    if (!options.length) {
      selectedNewId.value = null
      return
    }

    if (!options.some((option) => option.id === selectedNewId.value)) {
      selectedNewId.value = options[0]?.id ?? null
    }
  },
  { immediate: true },
)

const selectedOccasionOffer = computed(() =>
  occasionOptions.value.find((option) => option.id === selectedOccasionId.value) ?? null,
)

const selectedNewOffer = computed(() =>
  newOptions.value.find((option) => option.id === selectedNewId.value) ?? null,
)

const segments = computed<OfferSegment[]>(() => {
  const list: OfferSegment[] = []
  const conditionLabels: Record<OfferCondition, string> = {
    occasion: t('product.hero.offerConditions.occasion'),
    new: t('product.hero.offerConditions.new'),
  }

  if (occasionOptions.value.length) {
    const label = conditionLabels.occasion
    list.push({
      key: 'occasion',
      title: label,
      options: occasionOptions.value,
      selectedId: selectedOccasionId.value,
      selectedOffer: selectedOccasionOffer.value,
      selectAriaLabel: t('product.stickyOffers.selectAria', { condition: label }),
      buttonAriaLabel: t('product.stickyOffers.buttonAria', { condition: label }),
    })
  }

  if (newOptions.value.length) {
    const label = conditionLabels.new
    list.push({
      key: 'new',
      title: label,
      options: newOptions.value,
      selectedId: selectedNewId.value,
      selectedOffer: selectedNewOffer.value,
      selectAriaLabel: t('product.stickyOffers.selectAria', { condition: label }),
      buttonAriaLabel: t('product.stickyOffers.buttonAria', { condition: label }),
    })
  }

  return list
})

const handleSelect = (condition: OfferCondition, value: unknown) => {
  const normalized = typeof value === 'string' ? value : null

  if (condition === 'occasion') {
    selectedOccasionId.value = normalized
    return
  }

  selectedNewId.value = normalized
}

defineExpose({
  segments,
  rootElement,
})
</script>

<style scoped>
.product-sticky-offers {
  position: sticky;
  top: var(--v-layout-top, 0px);
  z-index: 40;
  background: rgb(var(--v-theme-surface-default));
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.12);
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
}

.product-sticky-offers__container {
  padding-block: 0.75rem;
}

.product-sticky-offers__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
}

.product-sticky-offers__content--mobile {
  flex-direction: column;
  align-items: flex-start;
  gap: 1rem;
}

.product-sticky-offers__intro {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 auto;
}

.product-sticky-offers__icon {
  flex-shrink: 0;
}

.product-sticky-offers__message {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offers__segments {
  display: flex;
  gap: 1.25rem;
  flex: 2 1 auto;
}

.product-sticky-offers__segments--stacked {
  flex-direction: column;
  width: 100%;
}

.product-sticky-offers__segment {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-width: 0;
}

.product-sticky-offers__segment-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.product-sticky-offers__segment-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offers__segment-summary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-size: 0.85rem;
}

.product-sticky-offers__segment-summary img {
  border-radius: 4px;
}

.product-sticky-offers__segment-price {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offers__select {
  min-width: 220px;
}

.product-sticky-offers__selection {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.product-sticky-offers__selection img {
  border-radius: 4px;
}

.product-sticky-offers__selection-price {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offers__item {
  gap: 0.75rem;
}

.product-sticky-offers__button {
  align-self: flex-start;
}

@media (max-width: 960px) {
  .product-sticky-offers__container {
    padding-inline: 1rem;
  }

  .product-sticky-offers__segments {
    width: 100%;
  }

  .product-sticky-offers__select {
    width: 100%;
    min-width: 0;
  }

  .product-sticky-offers__button {
    width: 100%;
  }
}
</style>

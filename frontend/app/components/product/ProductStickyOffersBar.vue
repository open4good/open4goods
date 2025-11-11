<template>
  <transition name="product-sticky-offers__fade">
    <div
      v-if="shouldDisplayBar"
      class="product-sticky-offers"
      :style="{ top: `${topOffset}px` }"
      role="region"
      :aria-label="barAriaLabel"
    >
      <div class="product-sticky-offers__surface">
        <v-container class="product-sticky-offers__container" max-width="xl">
          <div class="product-sticky-offers__content">
            <div class="product-sticky-offers__intro">
              <v-icon
                icon="mdi-heart-outline"
                size="24"
                class="product-sticky-offers__icon"
                aria-hidden="true"
              />
              <p class="product-sticky-offers__message">
                {{ supportMessage }}
              </p>
            </div>

            <div v-if="!isMobile" class="product-sticky-offers__segments" :aria-label="segmentsAriaLabel" role="group">
              <div
                v-for="segment in segments"
                :key="segment.key"
                class="product-sticky-offers__segment"
              >
                <p class="product-sticky-offers__segment-title">{{ segment.label }}</p>
                <v-select
                  v-model="selectedOffers[segment.key]"
                  :items="segment.offers"
                  item-value="id"
                  item-title="label"
                  return-object
                  density="comfortable"
                  variant="outlined"
                  color="primary"
                  hide-details="auto"
                  class="product-sticky-offers__select"
                  :aria-label="segment.selectLabel"
                  :menu-props="menuProps"
                >
                  <template #selection="{ item }">
                    <div class="product-sticky-offers__selection">
                      <img
                        v-if="item.raw.favicon"
                        :src="item.raw.favicon"
                        :alt="item.raw.merchant"
                        width="24"
                        height="24"
                        class="product-sticky-offers__selection-favicon"
                      >
                      <span class="product-sticky-offers__selection-merchant">
                        {{ item.raw.merchant }}
                      </span>
                      <span class="product-sticky-offers__selection-price">
                        {{ item.raw.priceLabel }}
                      </span>
                    </div>
                  </template>
                  <template #item="{ item, props: itemProps }">
                    <v-list-item v-bind="itemProps">
                      <template #prepend>
                        <img
                          v-if="item.raw.favicon"
                          :src="item.raw.favicon"
                          :alt="item.raw.merchant"
                          width="28"
                          height="28"
                          class="product-sticky-offers__menu-favicon"
                        >
                        <div v-else class="product-sticky-offers__menu-favicon product-sticky-offers__menu-favicon--placeholder">
                          {{ item.raw.merchant?.charAt(0)?.toUpperCase() ?? '?' }}
                        </div>
                      </template>
                      <v-list-item-title>{{ item.raw.merchant }}</v-list-item-title>
                      <v-list-item-subtitle>{{ item.raw.priceLabel }}</v-list-item-subtitle>
                    </v-list-item>
                  </template>
                </v-select>
                <v-btn
                  class="product-sticky-offers__cta"
                  color="primary"
                  variant="flat"
                  :aria-label="resolveButtonAriaLabel(segment.key)"
                  :disabled="!selectedOffers[segment.key]"
                  @click="handleOfferRedirect(segment.key)"
                >
                  {{ ctaLabel }}
                </v-btn>
              </div>
            </div>

            <v-expansion-panels
              v-else
              class="product-sticky-offers__panels"
              variant="accordion"
              density="compact"
            >
              <v-expansion-panel
                v-for="segment in segments"
                :key="segment.key"
                class="product-sticky-offers__panel"
              >
                <v-expansion-panel-title expand-icon="mdi-chevron-down">
                  <div class="product-sticky-offers__panel-header">
                    <span class="product-sticky-offers__segment-title">{{ segment.label }}</span>
                    <span v-if="selectedOffers[segment.key]" class="product-sticky-offers__panel-price">
                      {{ selectedOffers[segment.key]?.priceLabel }}
                    </span>
                  </div>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <v-select
                    v-model="selectedOffers[segment.key]"
                    :items="segment.offers"
                    item-value="id"
                    item-title="label"
                    return-object
                    density="comfortable"
                    variant="outlined"
                    color="primary"
                    hide-details="auto"
                    class="product-sticky-offers__select"
                    :aria-label="segment.selectLabel"
                    :menu-props="menuProps"
                  >
                    <template #selection="{ item }">
                      <div class="product-sticky-offers__selection">
                        <img
                          v-if="item.raw.favicon"
                          :src="item.raw.favicon"
                          :alt="item.raw.merchant"
                          width="24"
                          height="24"
                          class="product-sticky-offers__selection-favicon"
                        >
                        <span class="product-sticky-offers__selection-merchant">
                          {{ item.raw.merchant }}
                        </span>
                        <span class="product-sticky-offers__selection-price">
                          {{ item.raw.priceLabel }}
                        </span>
                      </div>
                    </template>
                    <template #item="{ item, props: itemProps }">
                      <v-list-item v-bind="itemProps">
                        <template #prepend>
                          <img
                            v-if="item.raw.favicon"
                            :src="item.raw.favicon"
                            :alt="item.raw.merchant"
                            width="28"
                            height="28"
                            class="product-sticky-offers__menu-favicon"
                          >
                          <div v-else class="product-sticky-offers__menu-favicon product-sticky-offers__menu-favicon--placeholder">
                            {{ item.raw.merchant?.charAt(0)?.toUpperCase() ?? '?' }}
                          </div>
                        </template>
                        <v-list-item-title>{{ item.raw.merchant }}</v-list-item-title>
                        <v-list-item-subtitle>{{ item.raw.priceLabel }}</v-list-item-subtitle>
                      </v-list-item>
                    </template>
                  </v-select>
                  <v-btn
                    class="product-sticky-offers__cta"
                    color="primary"
                    variant="flat"
                    block
                    :aria-label="resolveButtonAriaLabel(segment.key)"
                    :disabled="!selectedOffers[segment.key]"
                    @click="handleOfferRedirect(segment.key)"
                  >
                    {{ ctaLabel }}
                  </v-btn>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </div>
        </v-container>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import type { ProductDto, ProductAggregatedPriceDto } from '~~/shared/api-client'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  sentinel: {
    type: Object as PropType<HTMLElement | null>,
    default: null,
  },
})

const { t, n } = useI18n()
const display = useDisplay()

const conditionOrder = ['OCCASION', 'NEW'] as const

type ConditionKey = (typeof conditionOrder)[number]

interface NormalizedOffer {
  id: string
  merchant: string
  price: number
  priceLabel: string
  currency: string
  favicon?: string
  affiliationToken?: string
  url?: string
  raw: ProductAggregatedPriceDto
}

interface ConditionSegment {
  key: ConditionKey
  label: string
  selectLabel: string
  offers: NormalizedOffer[]
}

const conditionLabelKey: Record<ConditionKey, string> = {
  OCCASION: 'product.stickySupportBar.conditions.occasion',
  NEW: 'product.stickySupportBar.conditions.new',
}

const conditionSelectKey: Record<ConditionKey, string> = {
  OCCASION: 'product.stickySupportBar.selectLabels.occasion',
  NEW: 'product.stickySupportBar.selectLabels.new',
}

const conditionButtonAriaKey: Record<ConditionKey, string> = {
  OCCASION: 'product.stickySupportBar.actions.openOccasion',
  NEW: 'product.stickySupportBar.actions.openNew',
}

const unknownMerchantLabel = computed(() => t('product.price.metrics.unknownSource'))

const normalizeOffer = (
  offer: ProductAggregatedPriceDto,
  condition: ConditionKey,
  index: number,
): NormalizedOffer | null => {
  if (typeof offer?.price !== 'number') {
    return null
  }

  const merchantName = offer.datasourceName?.trim().length
    ? offer.datasourceName.trim()
    : unknownMerchantLabel.value

  const currency = offer.currency ?? props.product.offers?.bestPrice?.currency ?? 'EUR'
  const priceLabel = n(offer.price, {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  })

  const idSource = offer.affiliationToken
    ? `aff-${offer.affiliationToken}`
    : offer.url?.trim().length
      ? `url-${offer.url}`
      : `${condition}-${index}`

  return {
    id: idSource,
    merchant: merchantName,
    price: offer.price,
    priceLabel,
    currency,
    favicon: offer.favicon ?? undefined,
    affiliationToken: offer.affiliationToken ?? undefined,
    url: offer.url ?? undefined,
    raw: offer,
  }
}

const segments = computed<ConditionSegment[]>(() => {
  const offersByCondition = props.product.offers?.offersByCondition ?? {}

  return conditionOrder
    .map((condition) => {
      const rawOffers = Array.isArray(offersByCondition[condition])
        ? (offersByCondition[condition] as ProductAggregatedPriceDto[])
        : []

      const normalizedOffers = rawOffers
        .map((offer, index) => normalizeOffer(offer, condition, index))
        .filter((offer): offer is NormalizedOffer => Boolean(offer))
        .sort((a, b) => a.price - b.price)

      return {
        key: condition,
        label: t(conditionLabelKey[condition]),
        selectLabel: t(conditionSelectKey[condition]),
        offers: normalizedOffers,
      }
    })
    .filter((segment) => segment.offers.length > 0)
})

const hasOffers = computed(() => segments.value.length > 0)

const selectedOffers = reactive<Record<ConditionKey, NormalizedOffer | null>>({
  OCCASION: null,
  NEW: null,
})

watch(
  segments,
  (updatedSegments) => {
    updatedSegments.forEach((segment) => {
      const current = selectedOffers[segment.key]
      const fallback = segment.offers[0] ?? null

      if (!current || !segment.offers.some((offer) => offer.id === current.id)) {
        selectedOffers[segment.key] = fallback
      }
    })

    conditionOrder.forEach((condition) => {
      if (!updatedSegments.some((segment) => segment.key === condition)) {
        selectedOffers[condition] = null
      }
    })
  },
  { immediate: true },
)

const isStickyActive = ref(false)

let observer: IntersectionObserver | null = null

const cleanupObserver = () => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

watch(
  () => props.sentinel,
  (element) => {
    if (!import.meta.client) {
      return
    }

    cleanupObserver()

    if (!element) {
      isStickyActive.value = false
      return
    }

    observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries
        if (!entry) {
          return
        }

        isStickyActive.value = !entry.isIntersecting
      },
      {
        threshold: [0, 0.1, 1],
        rootMargin: '0px 0px -1px 0px',
      },
    )

    observer.observe(element)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  cleanupObserver()
})

const shouldDisplayBar = computed(() => hasOffers.value && isStickyActive.value)

const isMobile = computed(() => display.smAndDown.value)

const topOffset = computed(() => (isMobile.value ? 56 : 64))

const menuProps = reactive({
  maxHeight: 360,
  offset: 4,
})

const supportMessage = computed(() => t('product.stickySupportBar.message'))
const barAriaLabel = computed(() => t('product.stickySupportBar.ariaLabel'))
const segmentsAriaLabel = computed(() => t('product.stickySupportBar.ariaLabelOffers'))
const ctaLabel = computed(() => t('product.stickySupportBar.actions.goToOffer'))

const resolveButtonAriaLabel = (condition: ConditionKey) => {
  const offer = selectedOffers[condition]
  if (!offer) {
    return ctaLabel.value
  }

  const ariaKey = conditionButtonAriaKey[condition]
  return t(ariaKey, { merchant: offer.merchant })
}

const handleOfferRedirect = async (condition: ConditionKey) => {
  const offer = selectedOffers[condition]
  if (!offer) {
    return
  }

  if (offer.affiliationToken) {
    await navigateTo(`/contrib/${offer.affiliationToken}`)
    return
  }

  if (offer.url) {
    await navigateTo(offer.url, { external: true })
  }
}
</script>

<style scoped lang="scss">
.product-sticky-offers {
  position: fixed;
  left: 0;
  right: 0;
  z-index: 1200;
  padding: 0.5rem 0;
}

.product-sticky-offers__surface {
  background-color: rgb(var(--v-theme-surface-default));
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.14);
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
}

.product-sticky-offers__container {
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-sticky-offers__content {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.product-sticky-offers__intro {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 auto;
  min-width: 0;
}

.product-sticky-offers__icon {
  color: rgb(var(--v-theme-primary));
}

.product-sticky-offers__message {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offers__segments {
  display: flex;
  gap: 1rem;
  flex: 2 1 auto;
}

.product-sticky-offers__segment {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 0;
  min-width: 0;
}

.product-sticky-offers__segment-title {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
  white-space: nowrap;
}

.product-sticky-offers__select {
  flex: 1 1 auto;
  min-width: 0;
}

.product-sticky-offers__selection {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.product-sticky-offers__selection-favicon,
.product-sticky-offers__menu-favicon {
  border-radius: 999px;
  object-fit: contain;
  background-color: rgb(var(--v-theme-surface-primary-120));
}

.product-sticky-offers__menu-favicon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
}

.product-sticky-offers__menu-favicon--placeholder {
  width: 28px;
  height: 28px;
}

.product-sticky-offers__selection-merchant {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-sticky-offers__selection-price {
  margin-left: auto;
  font-weight: 500;
  color: rgb(var(--v-theme-primary));
}

.product-sticky-offers__cta {
  white-space: nowrap;
}

.product-sticky-offers__panels {
  width: 100%;
}

.product-sticky-offers__panel + .product-sticky-offers__panel {
  margin-top: 0.5rem;
}

.product-sticky-offers__panel-header {
  display: flex;
  justify-content: space-between;
  width: 100%;
  align-items: center;
  gap: 0.75rem;
}

.product-sticky-offers__panel-price {
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
}

.product-sticky-offers__fade-enter-active,
.product-sticky-offers__fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.product-sticky-offers__fade-enter-from,
.product-sticky-offers__fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

@media (max-width: 1280px) {
  .product-sticky-offers__content {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .product-sticky-offers__intro {
    justify-content: center;
    text-align: center;
  }

  .product-sticky-offers__segments {
    width: 100%;
  }
}

@media (max-width: 960px) {
  .product-sticky-offers {
    padding: 0.25rem 0;
  }

  .product-sticky-offers__message {
    font-size: 0.95rem;
  }

  .product-sticky-offers__intro {
    justify-content: flex-start;
  }
}
</style>

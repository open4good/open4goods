<template>
  <Transition name="product-sticky-offer__transition">
    <div
      v-if="shouldRender"
      class="product-sticky-offer"
      role="region"
      :aria-label="ariaLabel"
    >
      <v-container class="product-sticky-offer__container" max-width="xl">
        <div class="product-sticky-offer__content">
          <div class="product-sticky-offer__message">
            <v-icon icon="mdi-heart" size="22" class="product-sticky-offer__message-icon" />
            <span>{{ supportMessage }}</span>
          </div>

          <div class="product-sticky-offer__offers">
            <div
              v-for="block in conditionBlocks"
              :key="block.condition"
              class="product-sticky-offer__condition"
            >
              <p class="product-sticky-offer__condition-title">
                {{ block.title }}
              </p>
              <v-select
                :model-value="block.model"
                :items="block.options"
                :item-props="resolveItemProps"
                :menu-props="{ maxHeight: 320 }"
                variant="solo"
                density="compact"
                class="product-sticky-offer__select"
                hide-details
                :placeholder="selectPlaceholder"
                @update:model-value="(value) => updateCondition(block.condition, value as string | null)"
              >
                <template #selection="{ item }">
                  <div v-if="item?.raw" class="product-sticky-offer__selection">
                    <div class="product-sticky-offer__selection-price">{{ item.raw.priceLabel }}</div>
                    <div class="product-sticky-offer__selection-merchant">{{ item.raw.merchant }}</div>
                  </div>
                </template>
                <template #item="{ item, props: itemProps }">
                  <v-list-item v-bind="itemProps">
                    <template #prepend>
                      <img
                        v-if="item?.raw?.avatar"
                        :src="item.raw.avatar"
                        :alt="item.raw.merchant"
                        class="product-sticky-offer__option-avatar"
                        width="32"
                        height="32"
                      >
                    </template>
                    <v-list-item-title class="product-sticky-offer__option-price">
                      {{ item?.raw?.priceLabel }}
                    </v-list-item-title>
                    <v-list-item-subtitle class="product-sticky-offer__option-merchant">
                      {{ item?.raw?.merchant }}
                    </v-list-item-subtitle>
                  </v-list-item>
                </template>
              </v-select>
              <v-btn
                class="product-sticky-offer__cta"
                color="primary"
                variant="flat"
                :to="block.selected?.isInternal ? block.selected?.link ?? undefined : undefined"
                :href="!block.selected?.isInternal ? block.selected?.link ?? undefined : undefined"
                :target="block.selected?.isInternal ? undefined : '_blank'"
                :rel="block.selected?.isInternal ? undefined : 'nofollow noopener'"
                :disabled="!block.selected?.link"
              >
                {{ goToOfferLabel }}
              </v-btn>
            </div>
          </div>
        </div>
      </v-container>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { computed, reactive, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductAggregatedPriceDto, ProductDto } from '~~/shared/api-client'

type OfferCondition = 'occasion' | 'new'

type OfferOption = {
  value: string
  link: string | null
  isInternal: boolean
  priceLabel: string
  merchant: string
  avatar: string | null
  offer: ProductAggregatedPriceDto
}

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const { t, n } = useI18n()

const supportMessage = computed(() => t('product.stickyOffer.support'))
const ariaLabel = computed(() => t('product.stickyOffer.ariaLabel'))
const selectPlaceholder = computed(() => t('product.stickyOffer.selectPlaceholder'))
const goToOfferLabel = computed(() => t('product.stickyOffer.goToOffer'))

const conditions: OfferCondition[] = ['occasion', 'new']

const offersByCondition = computed<Record<OfferCondition, ProductAggregatedPriceDto[]>>(() => {
  const entries = props.product.offers?.offersByCondition ?? {}
  const mapped: Record<OfferCondition, ProductAggregatedPriceDto[]> = {
    occasion: [],
    new: [],
  }

  Object.entries(entries).forEach(([key, list]) => {
    const normalized = key.trim().toLowerCase()
    if (normalized === 'occasion' && Array.isArray(list)) {
      mapped.occasion = list.filter(Boolean) as ProductAggregatedPriceDto[]
    }
    if (normalized === 'new' && Array.isArray(list)) {
      mapped.new = list.filter(Boolean) as ProductAggregatedPriceDto[]
    }
  })

  return mapped
})

const aggregatedBestOffer = computed<ProductAggregatedPriceDto | null>(() => props.product.offers?.bestPrice ?? null)

const bestOffersByCondition = computed<Record<OfferCondition, ProductAggregatedPriceDto | null>>(() => {
  const offers = props.product.offers
  const aggregated = aggregatedBestOffer.value

  const mapped: Record<OfferCondition, ProductAggregatedPriceDto | null> = {
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

const resolveOfferLink = (offer: ProductAggregatedPriceDto | null | undefined) => {
  if (!offer) {
    return null
  }

  if (offer.affiliationToken) {
    return `/contrib/${offer.affiliationToken}`
  }

  return offer.url ?? null
}

const formatPrice = (offer: ProductAggregatedPriceDto) => {
  if (typeof offer.price !== 'number') {
    return '—'
  }

  const currency = offer.currency ?? aggregatedBestOffer.value?.currency ?? 'EUR'
  return n(offer.price, {
    style: 'currency',
    currency,
    maximumFractionDigits: offer.price >= 100 ? 0 : 2,
  })
}

const buildOption = (offer: ProductAggregatedPriceDto, index: number): OfferOption | null => {
  const link = resolveOfferLink(offer)
  if (!link) {
    return null
  }

  const priceLabel = offer.shortPrice?.trim().length ? offer.shortPrice : formatPrice(offer)
  const merchant = offer.datasourceName?.trim().length ? offer.datasourceName : t('product.price.metrics.unknownSource')
  const value = offer.affiliationToken ?? offer.url ?? `${merchant}-${offer.price ?? 0}-${index}`

  return {
    value,
    link,
    isInternal: link.startsWith('/'),
    priceLabel,
    merchant,
    avatar: offer.favicon ?? null,
    offer,
  }
}

const optionMap = computed<Record<OfferCondition, OfferOption[]>>(() => {
  return conditions.reduce<Record<OfferCondition, OfferOption[]>>((acc, condition) => {
    const offers = offersByCondition.value[condition] ?? []
    acc[condition] = offers
      .map((offer, index) => buildOption(offer, index))
      .filter((option): option is OfferOption => Boolean(option))
    return acc
  }, {
    occasion: [],
    new: [],
  })
})

const selectedValues = reactive<Record<OfferCondition, string | null>>({
  occasion: null,
  new: null,
})

const isSameOffer = (a: ProductAggregatedPriceDto | null | undefined, b: OfferOption | null) => {
  if (!a || !b) {
    return false
  }

  if (a.affiliationToken && b.offer.affiliationToken && a.affiliationToken === b.offer.affiliationToken) {
    return true
  }

  if (a.url && b.offer.url && a.url === b.offer.url) {
    return true
  }

  if (
    a.datasourceName &&
    b.offer.datasourceName &&
    a.datasourceName === b.offer.datasourceName &&
    typeof a.price === 'number' &&
    typeof b.offer.price === 'number' &&
    Math.abs(a.price - b.offer.price) < 0.01
  ) {
    return true
  }

  return false
}

watch(
  [optionMap, bestOffersByCondition],
  ([options, bestByCondition]) => {
    conditions.forEach((condition) => {
      const entries = options[condition]
      if (!entries.length) {
        selectedValues[condition] = null
        return
      }

      const preferred = entries.find((option) => isSameOffer(bestByCondition[condition], option))
      selectedValues[condition] = (preferred ?? entries[0])?.value ?? null
    })
  },
  { immediate: true },
)

const conditionBlocks = computed(() =>
  conditions
    .map((condition) => {
      const options = optionMap.value[condition]
      if (!options.length) {
        return null
      }

      const selected = options.find((option) => option.value === selectedValues[condition]) ?? null

      return {
        condition,
        title: t('product.stickyOffer.conditionTitle', { condition: t(`product.hero.offerConditions.${condition}`) }),
        options,
        selected,
        model: selectedValues[condition],
      }
    })
    .filter((block): block is {
      condition: OfferCondition
      title: string
      options: OfferOption[]
      selected: OfferOption | null
      model: string | null
    } => Boolean(block)),
)

const shouldRender = computed(() => props.active && conditionBlocks.value.length > 0)

const resolveItemProps = (option: OfferOption) => ({
  title: option.priceLabel,
  subtitle: option.merchant,
})

const updateCondition = (condition: OfferCondition, value: string | null) => {
  selectedValues[condition] = typeof value === 'string' ? value : null
}
</script>

<style scoped>
.product-sticky-offer {
  position: sticky;
  top: 64px;
  z-index: 40;
  background: rgba(var(--v-theme-surface-default), 0.96);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16);
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
}

@media (max-width: 959px) {
  .product-sticky-offer {
    top: 56px;
  }
}

.product-sticky-offer__container {
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
}

.product-sticky-offer__content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

@media (min-width: 960px) {
  .product-sticky-offer__content {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 1.5rem;
  }
}

.product-sticky-offer__message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offer__message-icon {
  color: rgb(var(--v-theme-primary));
}

.product-sticky-offer__offers {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  width: 100%;
}

.product-sticky-offer__condition {
  display: flex;
  flex-direction: column;
}

.product-sticky-offer__condition-title {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-sticky-offer__select {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 14px;
}

.product-sticky-offer__selection {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.product-sticky-offer__selection-price {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offer__selection-merchant {
  font-size: 0.75rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-sticky-offer__option-price {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-sticky-offer__option-merchant {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-sticky-offer__option-avatar {
  border-radius: 8px;
  object-fit: cover;
}

.product-sticky-offer__cta {
  margin-top: 0.5rem;
  width: 100%;
  font-weight: 600;
}

.product-sticky-offer__transition-enter-active,
.product-sticky-offer__transition-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.product-sticky-offer__transition-enter-from,
.product-sticky-offer__transition-leave-to {
  transform: translateY(-12px);
  opacity: 0;
}
</style>

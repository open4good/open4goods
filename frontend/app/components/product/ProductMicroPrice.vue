<template>
  <div class="product-micro-price">
    <!-- Main Price Button -->
    <v-btn
      v-if="bestOffer"
      :href="bestOffer.url || undefined"
      target="_blank"
      rel="noopener noreferrer"
      class="product-micro-price__main-btn"
      :color="condition === 'new' ? 'primary' : 'secondary'"
      variant="elevated"
      size="small"
      height="32"
      elevation="0"
      @click="$emit('offer-click', condition)"
    >
      <template #prepend>
        <div class="product-micro-price__icon-wrapper">
          <img
            v-if="bestOffer.favicon"
            :src="bestOffer.favicon"
            :alt="bestOffer.label"
            class="product-micro-price__favicon"
            width="16"
            height="16"
          />
          <v-icon
            v-else
            :icon="condition === 'new' ? 'mdi-tag' : 'mdi-recycle'"
            size="14"
          />
        </div>
      </template>
      <span class="product-micro-price__label">
        {{ bestOffer.priceLabel }}
      </span>
    </v-btn>

    <!-- Alternatives Dropdown -->
    <v-menu
      v-if="hasAlternatives"
      location="bottom end"
      :close-on-content-click="true"
    >
      <template #activator="{ props: activatorProps }">
        <v-btn
          v-bind="activatorProps"
          class="product-micro-price__more-btn"
          :color="condition === 'new' ? 'primary' : 'secondary'"
          variant="tonal"
          size="small"
          height="32"
          width="24"
          icon="mdi-chevron-down"
          density="comfortable"
        />
      </template>
      <v-list density="compact" nav class="product-micro-price__list">
        <v-list-item
          v-for="offer in alternatives"
          :key="offer.id"
          :href="offer.url || undefined"
          target="_blank"
          rel="noopener noreferrer"
          class="product-micro-price__list-item"
          rounded="lg"
        >
          <template #prepend>
            <div class="product-micro-price__list-icon">
              <img
                v-if="offer.favicon"
                :src="offer.favicon"
                :alt="offer.label"
                width="16"
                height="16"
                class="rounded-sm"
              />
              <v-icon v-else icon="mdi-store" size="16" />
            </div>
          </template>

          <div class="d-flex justify-space-between align-center w-100 gap-4">
            <span
              class="text-caption font-weight-medium text-truncate mr-2"
              style="max-width: 100px"
            >
              {{ offer.label }}
            </span>
            <span class="text-caption font-weight-bold text-primary">
              {{ offer.priceLabel }}
            </span>
          </div>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
// Generate deterministic ID from offer URL to avoid SSR hydration mismatches
const deterministicId = (url?: string, fallback: string = 'offer') =>
  url ? `offer-${url.slice(-12).replace(/[^a-z0-9]/gi, '-')}` : fallback

// We need to define a local type or import it if shared, based on ProductHeroPricingPanel it seems ad-hoc there
// But here we rely on the DTOs from props.
// We'll normalize the offers for display.

const props = defineProps<{
  product: ProductDto
  condition: 'new' | 'used'
}>()

defineEmits<{
  'offer-click': [condition: 'new' | 'used']
}>()

// Helpers to extract offer data safely
const resolveBestOffer = () => {
  if (props.condition === 'new') {
    return props.product.offers?.bestNewOffer
  } else {
    return props.product.offers?.bestOccasionOffer
  }
}

// Internal type for display
type MicroPriceOffer = {
  id: string
  label: string
  priceLabel: string
  favicon?: string
  url?: string
}

// Normalized Best Offer
const bestOffer = computed<MicroPriceOffer | null>(() => {
  const raw = resolveBestOffer()
  if (!raw) return null

  return {
    id: deterministicId(raw.url, 'best'),
    label: raw.merchantName || 'Unknown',
    priceLabel: raw.priceLabel || 'N/A',
    favicon: raw.merchantFavicon,
    url: raw.url,
  }
})

// Alternatives
const alternatives = computed<MicroPriceOffer[]>(() => {
  // Note: DTO usually has 'new' and 'used' or 'occasion' as keys. Let's assume 'new' and 'occasion' based on ProductOffersDto
  const dtoConditionKey = props.condition === 'new' ? 'new' : 'occasion'

  const offers =
    props.product.offers?.offersByCondition?.[dtoConditionKey] || []

  if (!bestOffer.value) return []

  // Filter out the best offer from the list to avoid duplication
  // We can compare URLs or some ID if available.
  // Let's assume URL is a good enough proxy for uniqueness here along with price.
  return offers
    .filter(o => o.url !== bestOffer.value?.url)
    .map((o, index) => ({
      id: deterministicId(o.url, `alt-${index}`),
      label: o.merchantName || 'Unknown',
      priceLabel: o.priceLabel || 'N/A',
      favicon: o.merchantFavicon,
      url: o.url,
    }))
})

const hasAlternatives = computed(() => alternatives.value.length > 0)
</script>

<style scoped lang="sass">
.product-micro-price
  display: inline-flex
  align-items: stretch
  gap: 1px // Tiny gap between button and arrow

  &__main-btn
    border-top-right-radius: 0
    border-bottom-right-radius: 0
    padding-inline: 10px !important
    text-transform: none
    letter-spacing: normal
    min-width: 0

  &__icon-wrapper
    display: flex
    align-items: center
    justify-content: center
    margin-right: 6px
    background: rgba(255,255,255,0.9)
    border-radius: 4px
    padding: 2px
    width: 20px
    height: 20px

  &__favicon
    object-fit: contain

  &__more-btn
    border-top-left-radius: 0
    border-bottom-left-radius: 0
    border-left: 1px solid rgba(255,255,255,0.2)

  &__list
    min-width: 180px

  &__list-icon
    margin-right: 8px
    display: flex
    align-items: center
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

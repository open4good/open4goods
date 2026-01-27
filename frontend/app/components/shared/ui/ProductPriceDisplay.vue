<template>
  <div
    class="product-price-display"
    :class="[`product-price-display--${layout}`]"
    role="list"
  >
    <div
      v-for="badge in offerBadges"
      :key="badge.key"
      class="product-price-display__badge"
      :class="`product-price-display__badge--${badge.appearance}`"
      role="listitem"
    >
      <span class="product-price-display__badge-label">{{ badge.label }}</span>
      <span class="product-price-display__badge-price">{{ badge.price }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { formatPrice } from '~/utils/_product-pricing'

const props = withDefaults(
  defineProps<{
    product: ProductDto
    layout?: 'table' | 'inline'
    showTrend?: boolean
  }>(),
  {
    layout: 'table',
    showTrend: true,
  }
)

const { t, n } = useI18n()

type OfferBadge = {
  key: string
  label: string
  price: string
  appearance: 'new' | 'occasion' | 'default'
}

const offerBadges = computed<OfferBadge[]>(() => {
  const result: OfferBadge[] = []
  const newPrice = props.product.price?.minPrice?.price
  const usedPrice = props.product.price?.minPriceOccasion?.price

  // 1. Used/Refurbished Price
  if (typeof usedPrice === 'number') {
    result.push({
      key: 'occasion',
      label: t('category.products.pricing.occasionOfferLabel'),
      price: formatPrice(usedPrice, n),
      appearance: 'occasion',
    })
  }

  // 2. New Price
  if (typeof newPrice === 'number') {
    result.push({
      key: 'new',
      label: t('category.products.pricing.newOfferLabel'),
      price: formatPrice(newPrice, n),
      appearance: 'new',
    })
  }

  return result
})
</script>

<style scoped lang="sass">
.product-price-display
  display: grid
  gap: 0.5rem

  &--table
    grid-template-columns: 1fr

  &--inline
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr))
    align-items: center

  &__badge
    display: flex
    justify-content: space-between
    align-items: center
    padding: 0.35rem 0.6rem
    border-radius: 0.5rem
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3)
    white-space: nowrap

    &--new
      background: rgba(var(--v-theme-primary), 0.08)
      border-color: rgba(var(--v-theme-primary), 0.25)

    &--occasion
      background: rgba(var(--v-theme-accent-supporting), 0.08)
      border-color: rgba(var(--v-theme-accent-supporting), 0.25)

  &__badge-label
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-size: 0.8rem
    font-weight: 600
    margin-right: 0.75rem

  &__badge-price
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))
    font-size: 0.9rem
</style>

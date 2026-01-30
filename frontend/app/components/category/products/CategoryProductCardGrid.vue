<template>
  <v-row
    class="category-product-card-grid"
    :class="[
      `category-product-card-grid--size-${normalizedSize}`,
      `category-product-card-grid--variant-${variant}`,
      { 'category-product-card-grid--disabled': isDisabledCategory },
    ]"
  >
    <v-col
      v-for="product in products"
      :key="product.gtin ?? resolveCardProductName(product) ?? Math.random()"
      cols="12"
      :sm="normalizedSize === 'small' ? 6 : 6"
      :md="normalizedSize === 'small' ? 4 : normalizedSize === 'big' ? 4 : 6"
      :lg="normalizedSize === 'small' ? 3 : normalizedSize === 'big' ? 4 : 4"
      :xl="normalizedSize === 'small' ? 2 : normalizedSize === 'big' ? 4 : 3"
    >
      <ProductTileCard
        v-if="variant === 'compact-tile'"
        :product="product"
        :product-link="productLink(product)"
        :image-src="resolveImage(product)"
        :attributes="popularAttributesByProduct(product)"
        :impact-score="impactScoreValue(product)"
        :score-max="5"
        :offer-badges="offerBadges(product)"
        :offers-count-label="offersCountLabel(product)"
        :untitled-label="$t('category.products.untitledProduct')"
        :not-rated-label="$t('category.products.notRated')"
        :disabled="isDisabledCategory"
        layout="vertical"
        :link-rel="linkRel"
      />
      <v-card
        v-else
        class="category-product-card-grid__card"
        :class="{
          'category-product-card-grid__card--disabled': isDisabledCategory,
        }"
        :rounded="normalizedSize === 'small' ? 'lg' : 'xl'"
        elevation="2"
        hover
        :to="productLink(product)"
        :rel="linkRel"
      >
        <div class="category-product-card-grid__media-wrapper">
          <div class="category-product-card-grid__media">
            <!-- Header Overlay -->
            <div class="category-product-card-grid__media-header">
              <!-- Impact Score (Left) -->
              <div
                class="category-product-card-grid__corner"
                role="presentation"
              >
                <ImpactScore
                  v-if="impactScoreValue(product) != null"
                  :score="(impactScoreValue(product) ?? 0) * 4"
                  :max="20"
                  size="small"
                  flat
                />
                <span
                  v-else
                  class="category-product-card-grid__corner-fallback"
                >
                  {{ $t('category.products.notRated') }}
                </span>
              </div>

              <!-- Actions (Right) -->
              <div class="category-product-card-grid__actions">
                <AiReviewActionButton
                  :is-reviewed="isReviewed(product)"
                  :review-created-at="reviewCreatedAt(product)"
                  size="compact"
                />
                <CompareToggleButton :product="product" size="compact" />
              </div>
            </div>

            <v-img
              :src="resolveImage(product)"
              :alt="
                resolveCardProductName(product) ||
                $t('category.products.untitledProduct')
              "
              contain
              class="category-product-card-grid__image"
            >
              <template #placeholder>
                <v-skeleton-loader type="image" class="h-100" />
              </template>
            </v-img>
          </div>
        </div>

        <div class="category-product-card-grid__title-wrapper">
          <ProductDesignation
            :product="product"
            variant="card"
            :title-tag="normalizedSize === 'big' ? 'h4' : 'h5'"
            title-class="category-product-card-grid__title"
          />
          <div
            v-if="product.aiReview?.review?.baseLine"
            class="category-product-card-grid__baseline text-truncate"
            :title="product.aiReview.review.baseLine"
          >
            {{ product.aiReview.review.baseLine }}
          </div>
        </div>

        <v-card-item class="category-product-card-grid__body">
          <div
            v-if="popularAttributesByProduct(product).length"
            class="category-product-card-grid__attributes"
            role="list"
          >
            <v-chip
              v-for="attribute in popularAttributesByProduct(product)"
              :key="attribute.key"
              size="x-small"
              class="category-product-card-grid__attribute"
              variant="flat"
              color="surface-primary-080"
              role="listitem"
            >
              <v-icon
                v-if="attribute.icon"
                :icon="attribute.icon"
                size="14"
                class="me-1 category-product-card-grid__attribute-icon"
              />
              <span class="category-product-card-grid__attribute-value">
                {{ attribute.value }}
              </span>
            </v-chip>
          </div>

          <!-- Global offers count removed (moved to cells) -->
        </v-card-item>

        <!-- Microtable Pricing Layout -->
        <div class="category-product-card-grid__pricing-table">
          <ProductPriceRows :product="product" />
        </div>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductTileCard from '~/components/category/products/ProductTileCard.vue'
import CompareToggleButton from '~/components/shared/ui/CompareToggleButton.vue'
import AiReviewActionButton from '~/components/shared/ai/AiReviewActionButton.vue'
import ProductDesignation from '~/components/product/ProductDesignation.vue'
import ProductPriceRows from '~/components/product/ProductPriceRows.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatOffersCount } from '~/utils/_product-pricing'
import { resolveProductShortName } from '~/utils/_product-title-resolver'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  size?: 'compact' | 'comfortable' | 'small' | 'medium' | 'big'
  variant?: 'classic' | 'compact-tile'
  maxAttributes?: number
  showAttributeIcons?: boolean
  isCategoryDisabled?: boolean
  nofollowLinks?: boolean
}>()

const { t, n, locale } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const resolveCardProductName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value)

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

// Normalize size prop to small/medium/big
const normalizedSize = computed(() => {
  if (props.size === 'compact') return 'small'
  if (props.size === 'comfortable') return 'medium'
  return props.size ?? 'medium'
})

const variant = computed(() => props.variant ?? 'classic')
const maxAttributes = computed(() => props.maxAttributes)
const showAttributeIcons = computed(() => props.showAttributeIcons ?? true)
const isDisabledCategory = computed(() => props.isCategoryDisabled ?? false)
const linkRel = computed(() => (props.nofollowLinks ? 'nofollow' : undefined))

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const productLink = (product: ProductDto) => {
  return product.fullSlug ?? product.slug ?? undefined
}

const impactScoreValue = (product: ProductDto) =>
  resolvePrimaryImpactScore(product)

const offersCountLabel = (product: ProductDto) =>
  formatOffersCount(product, translatePlural)

const isReviewed = (product: ProductDto) => !!product.aiReview

const reviewCreatedAt = (product: ProductDto) =>
  product.aiReview?.createdMs ?? undefined

type OfferBadge = {
  key: string
  label: string
  price: string
  appearance: string
}

const offerBadges = (product: ProductDto) => {
  const entries: OfferBadge[] = []
  const newOffer = product.offers?.bestNewOffer
  const occasionOffer = product.offers?.bestOccasionOffer

  if (newOffer?.price != null) {
    entries.push({
      key: 'new',
      label: t('category.products.pricing.newOfferLabel'),
      price:
        newOffer.shortPrice ||
        n(newOffer.price, {
          style: 'currency',
          currency: newOffer.currency || 'EUR',
        }),
      appearance: 'new',
    })
  }

  if (occasionOffer?.price != null) {
    entries.push({
      key: 'occasion',
      label: t('category.products.pricing.occasionOfferLabel'),
      price:
        occasionOffer.shortPrice ||
        n(occasionOffer.price, {
          style: 'currency',
          currency: occasionOffer.currency || 'EUR',
        }),
      appearance: 'occasion',
    })
  }

  return entries
}

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = (
  product: ProductDto
): DisplayedAttribute[] => {
  const attributes = resolvePopularAttributes(
    product,
    popularAttributeConfigs.value
  )
  const entries: DisplayedAttribute[] = []

  attributes.forEach(attribute => {
    const value = formatAttributeValue(attribute, t, n)

    if (!value) {
      return
    }

    entries.push({
      key: attribute.key,
      label: attribute.label,
      value,
      icon: showAttributeIcons.value ? (attribute.icon ?? null) : null,
    })
  })

  // In small mode, limit attributes even more strictly if not specified
  const effectiveMax =
    maxAttributes.value ?? (normalizedSize.value === 'small' ? 2 : 3)

  return entries.slice(0, effectiveMax)
}
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &--disabled
    .category-product-card-grid__card,
    .product-tile-card
      filter: grayscale(1)
      opacity: 0.6


  &__card
    height: 100%
    display: flex
    flex-direction: column
    text-decoration: none
    transition: transform 0.2s ease, box-shadow 0.2s ease
    position: relative
    background: rgb(var(--v-theme-surface-glass))
    overflow: hidden
    border: 1px solid rgba(var(--v-theme-border-primary), 0.1)

    &:hover
      transform: translateY(-4px)
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)
      border-color: rgba(var(--v-theme-primary), 0.3)

    &--disabled
      filter: grayscale(1)
      opacity: 0.6

  &__media-wrapper
    position: relative
    overflow: hidden
    background: #fff
    /* Responsive sizing logic */
    aspect-ratio: 4/3
    min-height: 140px /* preventing too small images on mobile */
    max-height: 240px
    display: flex
    align-items: center
    justify-content: center

  &__media
    width: 100%
    height: 100%
    position: relative

  &__image
    width: 100%
    height: 100%
    transition: transform 0.3s ease

    :deep(img)
      object-fit: contain
      padding: 1rem /* Add some breathing room */
      mix-blend-mode: multiply
      background: #fff

  /* Hover effect on image */
  &__card:hover &__image
    transform: scale(1.05)

  &__media-header
    position: absolute
    top: 0
    left: 0
    right: 0
    z-index: 5
    display: flex
    align-items: flex-start
    justify-content: space-between
    pointer-events: none /* let clicks pass through to image link */
    padding: 0
    background: rgba(var(--v-theme-surface-default), 0.5)

  &__score-container
    margin: 0.5rem 0 0 0.5rem
    pointer-events: auto

  &__corner-fallback
    font-size: 0.65rem
    font-weight: 700
    letter-spacing: 0.05em
    text-transform: uppercase
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
    text-align: center
    line-height: 1.1
    transform: rotate(-12deg)
    margin-top: -4px /* visual tweak */
    margin-left: -4px

  &__actions
    pointer-events: auto
    margin: 0.5rem 0.5rem 0 0 /* top right spacing */
    display: inline-flex
    align-items: center
    gap: 0.4rem

  &__body
    display: flex
    flex-direction: column
    gap: 0.25rem
    padding: 0 0.75rem 0.75rem
    flex-grow: 1

  &__header
    display: flex
    flex-direction: column
    align-items: center
    text-align: center
    gap: 0.25rem

  &__title-wrapper
    padding: 0 0.75rem 0.5rem

  &__title
    font-size: 0.85rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))
    margin: 0
    line-height: 1.2
    white-space: nowrap
    overflow: hidden
    text-overflow: ellipsis

  &__baseline
    margin-top: 0.25rem
    font-size: 0.85rem
    line-height: 1.3
    color: rgb(var(--v-theme-text-neutral-secondary))
    opacity: 0.9

  &__attributes
    display: flex
    flex-wrap: wrap
    justify-content: center
    gap: 0.35rem
    margin: 0.25rem 0

  &__attribute
    background: rgba(var(--v-theme-surface-primary-080), 0.5) !important
    color: rgb(var(--v-theme-text-neutral-secondary)) !important
    font-weight: 500
    border: 1px solid rgba(var(--v-theme-border-primary), 0.1)

  &__pricing-table
    display: flex
    flex-direction: column
    gap: 0px
    border-top: 1px solid rgba(var(--v-theme-border-primary), 0.15)
    margin-top: auto /* Push to bottom */
    background: rgba(var(--v-theme-surface-default), 0.5)
    backdrop-filter: blur(4px)
    width: 100%

  &__pricing-cell
    display: flex
    flex-direction: row
    align-items: baseline
    justify-content: flex-start
    padding: 0.5rem 0.75rem
    gap: 0.5rem
    transition: background 0.2s
    cursor: help

    &:not(:last-child)
      border-bottom: 1px solid rgba(var(--v-theme-border-primary), 0.15)

    &--new
      background: transparent
      .category-product-card-grid__pricing-label
        color: rgb(var(--v-theme-primary))

    &--occasion
      background: transparent
      .category-product-card-grid__pricing-label
        color: rgb(var(--v-theme-accent-supporting))

    &--default
      background: transparent

  &__pricing-label
    font-size: 0.7rem
    text-transform: uppercase
    font-weight: 700
    letter-spacing: 0.05em
    opacity: 0.85
    min-width: 4rem /* Align prices if possible */

  &__pricing-amount
    font-weight: 700
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-strong))
    margin-left: auto /* Push price to right? Or keep left? User said flow */


  &__pricing-trend
    opacity: 0.9
    margin-left: 0.25rem

  &__meta
    display: flex
    align-items: center
    justify-content: center
    margin-top: 0.25rem

  &__offers
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  /* --- Size Variants --- */
  &--size-small
    .category-product-card-grid__card
        border-radius: 12px

    .category-product-card-grid__media-wrapper
        min-height: 120px
        max-height: 180px

    .category-product-card-grid__body
        padding: 0.75rem
        gap: 0.5rem

    .category-product-card-grid__title
        font-size: 0.9rem

  &--size-big
    .category-product-card-grid__media-wrapper
        min-height: 200px
        max-height: 320px

    .category-product-card-grid__title
        font-size: 1rem

    .category-product-card-grid__pricing-amount
        font-size: 1.25rem
</style>

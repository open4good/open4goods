<template>
  <v-card
    class="product-card"
    :class="[
      `product-card--size-${size}`,
      { 'product-card--disabled': disabled },
    ]"
    :rounded="rounded"
    elevation="2"
    hover
    :to="resolvedProductLink"
    :rel="linkRel"
  >
    <div class="product-card__media-wrapper">
      <div class="product-card__media">
        <!-- Header Overlay -->
        <div class="product-card__media-header">
          <!-- Impact Score (Left) -->
          <div class="product-card__corner" role="presentation">
            <ImpactScore
              v-if="impactScoreValue != null"
              :score="(impactScoreValue ?? 0) * 4"
              :max="20"
              size="small"
              flat
            />
            <span v-else class="product-card__corner-fallback">
              {{ $t('category.products.notRated') }}
            </span>
          </div>

          <!-- Actions (Right) -->
          <div class="product-card__actions">
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
          class="product-card__image"
        >
          <template #placeholder>
            <v-skeleton-loader type="image" class="h-100" />
          </template>
        </v-img>
      </div>
    </div>

    <div class="product-card__title-wrapper">
      <ProductDesignation
        :product="product"
        variant="card"
        :title-tag="titleTag"
        title-class="product-card__title"
      />
      <div
        v-if="product.aiReview?.review?.baseLine"
        class="product-card__baseline text-truncate"
        :title="product.aiReview.review.baseLine"
      >
        {{ product.aiReview.review.baseLine }}
      </div>
    </div>

    <v-card-item class="product-card__body">
      <div
        v-if="popularAttributesByProduct.length"
        class="product-card__attributes"
        role="list"
      >
        <v-chip
          v-for="attribute in popularAttributesByProduct"
          :key="attribute.key"
          size="x-small"
          class="product-card__attribute"
          variant="flat"
          color="surface-primary-080"
          role="listitem"
        >
          <v-icon
            v-if="attribute.icon"
            :icon="attribute.icon"
            size="14"
            class="me-1 product-card__attribute-icon"
          />
          <span class="product-card__attribute-value">
            {{ attribute.value }}
          </span>
        </v-chip>
      </div>

      <!-- Sorted Attribute (when sorting by custom field) -->
      <div
        v-if="sortedAttributeByProduct"
        class="product-card__sorted-attribute"
      >
        <span class="product-card__sorted-attribute-label">
          {{ sortedAttributeByProduct.label }}:
        </span>
        <span class="product-card__sorted-attribute-value">
          {{ sortedAttributeByProduct.value }}
        </span>
      </div>
    </v-card-item>

    <!-- Microtable Pricing Layout -->
    <div class="product-card__pricing-table">
      <ProductPriceRows :product="product" />
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  AttributeConfigDto,
  FieldMetadataDto,
  ProductDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CompareToggleButton from '~/components/shared/ui/CompareToggleButton.vue'
import ProductDesignation from '~/components/product/ProductDesignation.vue'
import ProductPriceRows from '~/components/product/ProductPriceRows.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { resolveProductShortName } from '~/utils/_product-title-resolver'
import {
  isCustomSortField,
  resolveSortedAttributeValue,
} from '~/utils/_sort-attribute-display'

const props = withDefaults(
  defineProps<{
    product: ProductDto
    popularAttributes?: AttributeConfigDto[]
    sortField?: string | null
    fieldMetadata?: Record<string, FieldMetadataDto>
    size?: 'small' | 'medium' | 'big'
    maxAttributes?: number
    showAttributeIcons?: boolean
    disabled?: boolean
    nofollowLinks?: boolean
  }>(),
  {
    popularAttributes: () => [],
    sortField: null,
    fieldMetadata: () => ({}),
    size: 'medium',
    maxAttributes: undefined,
    showAttributeIcons: true,
    disabled: false,
    nofollowLinks: false,
  }
)

const { t, n, locale } = useI18n()

const resolveCardProductName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value)

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const titleTag = computed(() => (props.size === 'big' ? 'h4' : 'h5'))

const rounded = computed(() => (props.size === 'small' ? 'lg' : 'xl'))

const linkRel = computed(() => (props.nofollowLinks ? 'nofollow' : undefined))

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const resolvedProductLink = computed(
  () => props.product.fullSlug ?? props.product.slug ?? undefined
)

const impactScoreValue = computed(() =>
  resolvePrimaryImpactScore(props.product)
)

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = computed<DisplayedAttribute[]>(() => {
  const attributes = resolvePopularAttributes(
    props.product,
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
      icon: props.showAttributeIcons ? (attribute.icon ?? null) : null,
    })
  })

  // In small mode, limit attributes even more strictly if not specified
  const effectiveMax = props.maxAttributes ?? (props.size === 'small' ? 2 : 3)

  return entries.slice(0, effectiveMax)
})

const sortedAttributeByProduct = computed<DisplayedAttribute | null>(() => {
  if (
    !props.sortField ||
    !isCustomSortField(
      props.sortField,
      popularAttributeConfigs.value.map(c => c.key).filter(Boolean) as string[]
    )
  ) {
    return null
  }

  const result = resolveSortedAttributeValue(
    props.product,
    props.sortField,
    props.fieldMetadata,
    t,
    n
  )

  if (!result) {
    return null
  }

  return {
    key: result.key,
    label: result.label,
    value: result.value,
  }
})
</script>

<style scoped lang="sass">
.product-card
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
    &:hover &__image
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

    &__corner
        /* pointer-events: auto - corner fallback might not need pointer events, but keep if needed */

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
        display: inline-block
        padding: 4px

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

    &__sorted-attribute
        display: flex
        justify-content: center
        align-items: center
        gap: 0.25rem
        margin-top: 0.5rem
        font-size: 1.2em
        line-height: 1.2

    &__sorted-attribute-label
        font-weight: 500
        color: rgb(var(--v-theme-text-neutral-secondary))

    &__sorted-attribute-value
        font-weight: 700
        color: rgb(var(--v-theme-text-neutral-strong))

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

/* Size variants */
.product-card
    &--size-small
        border-radius: 12px

        .product-card__media-wrapper
            min-height: 120px
            max-height: 180px

        .product-card__body
            padding: 0.75rem
            gap: 0.5rem

        .product-card__title
            font-size: 0.9rem

    &--size-big
        .product-card__media-wrapper
            min-height: 200px
            max-height: 320px

        .product-card__title
            font-size: 1rem
</style>

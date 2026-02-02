<template>
  <v-list class="category-product-list" lines="three" density="comfortable">
    <v-list-item
      v-for="product in products"
      :key="product.gtin ?? resolveListProductName(product) ?? Math.random()"
      class="category-product-list__item"
      :to="productLink(product)"
      link
      rounded="lg"
    >
      <template #prepend>
        <v-avatar size="88" rounded="lg">
          <v-img
            :src="resolveImage(product)"
            :alt="
              resolveListProductName(product) ||
              $t('category.products.untitledProduct')
            "
            cover
          >
            <template #placeholder>
              <v-skeleton-loader type="image" class="h-100" />
            </template>
          </v-img>
        </v-avatar>
      </template>

      <div class="category-product-list__layout">
        <div class="category-product-list__content">
          <div class="category-product-list__header">
            <h3 class="category-product-list__title">
              {{
                resolveListProductName(product) ||
                (product.gtin
                  ? '#' + product.gtin
                  : $t('category.products.untitledProduct'))
              }}
            </h3>
          </div>

          <div class="category-product-list__meta">
            <div class="category-product-list__status">
              <template v-if="resolveBaseLine(product)">
                <span class="category-product-list__status-text">
                  {{ resolveBaseLine(product) }}
                </span>
              </template>
            </div>
          </div>

          <ul
            v-if="popularAttributesByProduct(product).length"
            class="category-product-list__attributes"
            role="list"
          >
            <li
              v-for="attribute in popularAttributesByProduct(product)"
              :key="attribute.key"
              class="category-product-list__attribute"
              role="listitem"
            >
              <span class="category-product-list__attribute-label">{{
                attribute.label
              }}</span>
              <span
                class="category-product-list__attribute-separator"
                aria-hidden="true"
                >:</span
              >
              <span class="category-product-list__attribute-value">{{
                attribute.value
              }}</span>
            </li>
          </ul>

          <!-- Sorted Attribute (when sorting by custom field) -->
          <div
            v-if="sortedAttributeByProduct(product)"
            class="category-product-list__sorted-attribute"
          >
            <span class="category-product-list__sorted-attribute-label">
              {{ sortedAttributeByProduct(product)!.label }}:
            </span>
            <span class="category-product-list__sorted-attribute-value">
              {{ sortedAttributeByProduct(product)!.value }}
            </span>
          </div>
        </div>

        <div class="category-product-list__prices-column">
          <ProductPriceRows :product="product" />
        </div>

        <div class="category-product-list__score">
          <ImpactScore
            v-if="impactScoreValue(product) != null"
            :score="(impactScoreValue(product) ?? 0) * 4"
            :max="20"
            size="xs"
            flat
          />
          <span v-else class="category-product-list__score-fallback">
            {{ $t('category.products.notRated') }}
          </span>
        </div>

        <div v-if="hasVertical(product)" class="category-product-list__actions">
          <!-- Removed best price button as it is now in micro prices -->
          <v-btn
            class="category-product-list__compare-button"
            :class="{
              'category-product-list__compare-button--active':
                isCompareSelected(product),
            }"
            variant="flat"
            :aria-pressed="isCompareSelected(product)"
            :aria-label="getCompareButtonAriaLabel(product)"
            :title="getCompareButtonTitle(product)"
            :disabled="isCompareDisabled(product)"
            @click.stop.prevent="toggleCompare(product)"
          >
            <v-icon
              :icon="getCompareButtonIcon(product)"
              size="20"
              class="category-product-list__compare-icon"
            />
            <span class="category-product-list__compare-label">{{
              getCompareButtonText(product)
            }}</span>
          </v-btn>
        </div>
      </div>
    </v-list-item>
  </v-list>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  AttributeConfigDto,
  FieldMetadataDto,
  ProductDto,
} from '~~/shared/api-client'
import {
  MAX_COMPARE_ITEMS,
  useProductCompareStore,
  type CompareListBlockReason,
} from '~/stores/useProductCompareStore'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
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

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  sortField?: string | null
  fieldMetadata?: Record<string, FieldMetadataDto>
}>()

const { t, n, locale } = useI18n()

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

const resolveListProductName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value)

const resolveImage = (product: ProductDto) => {
  return (
    product.resources?.coverImagePath ??
    product.resources?.externalCover ??
    product.resources?.images?.[0]?.url ??
    undefined
  )
}

const productLink = (product: ProductDto) =>
  product.fullSlug ?? product.slug ?? undefined

const impactScoreValue = (product: ProductDto) =>
  resolvePrimaryImpactScore(product)


const resolveBaseLine = (product: ProductDto) =>
  product.aiReview?.baseLine ?? null

type DisplayedAttribute = {
  key: string
  label: string
  value: string
}

const popularAttributesByProduct = (
  product: ProductDto
): DisplayedAttribute[] => {
  return resolvePopularAttributes(product, popularAttributeConfigs.value)
    .map(attribute => {
      const value = formatAttributeValue(attribute, t, n)

      if (!value) {
        return null
      }

      return {
        key: attribute.key,
        label: attribute.label,
        value,
      }
    })
    .filter((attribute): attribute is DisplayedAttribute => attribute != null)
}

const hasVertical = (product: ProductDto) => {
  return !!product.fullSlug?.trim()
}

const sortedAttributeByProduct = (
  product: ProductDto
): DisplayedAttribute | null => {
  if (
    !isCustomSortField(
      props.sortField,
      popularAttributeConfigs.value.map(c => c.key).filter(Boolean) as string[]
    )
  ) {
    return null
  }

  const result = resolveSortedAttributeValue(
    product,
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
}

const compareStore = useProductCompareStore()

const reasonMessage = (reason: CompareListBlockReason | undefined) => {
  switch (reason) {
    case 'limit-reached':
      return t('category.products.compare.limitReached', {
        count: MAX_COMPARE_ITEMS,
      })
    case 'vertical-mismatch':
      return t('category.products.compare.differentCategory')
    case 'missing-identifier':
      return t('category.products.compare.missingIdentifier')
    default:
      return t('product.hero.compare.add')
  }
}

const isCompareSelected = (product: ProductDto) =>
  compareStore.hasProduct(product)

const compareEligibility = (product: ProductDto) =>
  compareStore.canAddProduct(product)

const isCompareDisabled = (product: ProductDto) =>
  !isCompareSelected(product) && !compareEligibility(product).success

const toggleCompare = (product: ProductDto) => {
  if (isCompareDisabled(product)) {
    return
  }
  compareStore.toggleProduct(product)
}

const getCompareButtonText = (product: ProductDto) =>
  isCompareSelected(product)
    ? t('product.hero.compare.remove')
    : t('product.hero.compare.add')

const getCompareButtonIcon = (product: ProductDto) =>
  isCompareSelected(product) ? 'mdi-minus' : 'mdi-plus'

const getCompareButtonTitle = (product: ProductDto) => {
  if (isCompareSelected(product)) {
    return t('product.hero.compare.remove')
  }

  const eligibility = compareEligibility(product)
  if (!eligibility.success) {
    return reasonMessage(eligibility.reason)
  }

  return t('product.hero.compare.add')
}

const getCompareButtonAriaLabel = (product: ProductDto) => {
  const productName = resolveListProductName(product) || ''

  if (isCompareSelected(product)) {
    if (t('product.hero.compare.ariaSelected')) {
      return t('product.hero.compare.ariaSelected', { name: productName })
    }
    return t('product.hero.compare.remove')
  }

  const eligibility = compareEligibility(product)
  if (!eligibility.success) {
    return reasonMessage(eligibility.reason)
  }

  if (t('product.hero.compare.ariaAdd')) {
    return t('product.hero.compare.ariaAdd', { name: productName })
  }

  return t('product.hero.compare.add')
}
</script>

<style scoped lang="sass">
.category-product-list
  background: transparent

  &__item
    background: rgb(var(--v-theme-surface-default))
    border-radius: 1rem
    margin-bottom: 1rem
    padding-inline: 1rem
    transition: box-shadow 0.2s ease, transform 0.2s ease

    &:hover
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)
      transform: translateY(-2px)

  &__layout
    display: flex
    align-items: stretch
    gap: 1.5rem
    flex-wrap: wrap

    @media (min-width: 992px)
      flex-wrap: nowrap

  &__content
    display: flex
    flex-direction: column
    gap: 0.75rem
    flex: 1 1 auto

  &__header
    display: flex
    gap: 0.75rem
    align-items: center

  &__title
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    flex: 1 1 auto
    min-width: 0

  &__meta
    display: flex
    gap: 1rem
    flex-wrap: wrap
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__prices-column
    display: flex
    flex-direction: column
    justify-content: center
    width: 230px
    flex: 0 0 auto

  &__status
    display: inline-flex
    align-items: center
    gap: 0.5rem
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__status-text
    font-weight: 500
    color: rgb(var(--v-theme-primary))

  &__offers
    display: inline-flex
    align-items: center
    gap: 0.35rem

  &__score
    display: flex
    align-items: center
    justify-content: center
    width: 180px
    flex: 0 0 auto

  &__actions
    display: flex
    align-items: center
    justify-content: flex-end
    gap: 0.5rem
    flex: 0 0 auto

  &__score-fallback
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__attributes
    display: flex
    flex-wrap: wrap
    gap: 0.5rem 1.25rem
    margin: 0
    padding: 0
    list-style: none

  &__attribute
    display: flex
    gap: 0.35rem
    align-items: baseline
    font-size: 0.875rem

  &__attribute-label
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attribute-separator
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-weight: 600

  &__attribute-value
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__sorted-attribute
    margin-top: 0.5rem
    display: inline-flex
    gap: 0.35rem
    align-items: baseline
    font-size: 1.1em

  &__sorted-attribute-label
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__sorted-attribute-value
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__compare-button
    background: rgba(var(--v-theme-surface-glass-strong), 0.5)
    backdrop-filter: blur(8px)
    border: 1px solid rgba(var(--v-theme-accent-primary-highlight), 0.2)
    color: rgb(var(--v-theme-accent-primary-highlight))
    padding: 0 1.25rem
    height: 48px
    border-radius: 14px
    text-transform: none
    letter-spacing: 0.01em
    font-weight: 600
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06)
    transition: all 0.25s ease

    &:hover
      background: rgba(var(--v-theme-accent-primary-highlight), 0.1)

    &--active
      background: linear-gradient(120deg, rgba(var(--v-theme-primary), 0.16), rgba(var(--v-theme-primary), 0.2))
      color: rgb(var(--v-theme-primary))
      box-shadow: 0 16px 32px rgba(var(--v-theme-primary), 0.2)

  &__compare-icon
    margin-inline-end: 0.25rem

  &__compare-label
    font-size: 0.98rem
</style>

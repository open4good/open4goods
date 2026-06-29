<template>
  <v-list class="category-product-list" lines="three" density="comfortable">
    <v-list-item
      v-for="(product, index) in products"
      :key="
        product.gtin ?? resolveListProductName(product) ?? `product-${index}`
      "
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
            <h2 class="category-product-list__title">
              {{
                resolveListProductName(product) ||
                (product.gtin
                  ? '#' + product.gtin
                  : $t('category.products.untitledProduct'))
              }}
            </h2>
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
              :class="{
                'category-product-list__attribute--sorted': attribute.isSorted,
              }"
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

          <!-- Sorted field highlight -->
          <div
            v-if="sortedFieldDisplay(product)"
            class="category-product-list__sorted-attribute"
          >
            <span class="category-product-list__sorted-attribute-label">
              {{ sortedFieldDisplay(product)!.label }}:
            </span>
            <span class="category-product-list__sorted-attribute-value">
              {{ sortedFieldDisplay(product)!.value }}
            </span>
          </div>
        </div>

        <div class="category-product-list__prices-column">
          <ProductPriceRows :product="product" />
        </div>

        <div class="category-product-list__score">
          <ImpactScore
            v-if="impactScoreValue(product) != null"
            :score="impactScoreValue(product) ?? 0"
            :max="20"
            size="xs"
            flat
          />
          <span v-else class="category-product-list__score-fallback">
            {{ $t('category.products.notRated') }}
          </span>
        </div>

        <div v-if="hasVertical(product)" class="category-product-list__actions">
          <CompareToggleButton
            :product="product"
            variant="button-icon"
            size="compact"
            appearance="plain"
            class="category-product-list__compare-button"
          />
        </div>
      </div>
    </v-list-item>
  </v-list>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import type {
  AttributeConfigDto,
  FieldMetadataDto,
  ProductDto,
} from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductPriceRows from '~/components/product/ProductPriceRows.vue'
import CompareToggleButton from '~/components/shared/ui/CompareToggleButton.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { resolveProductShortName } from '~/utils/_product-title-resolver'
import { resolveSortedFieldDisplay } from '~/utils/_sort-attribute-display'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  sortField?: string | null
  fieldMetadata?: Record<string, FieldMetadataDto>
}>()

const { t, n, locale } = useI18n()
const { translatePlural } = usePluralizedTranslation()

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

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  isSorted?: boolean
}

const popularAttributesByProduct = (
  product: ProductDto
): DisplayedAttribute[] => {
  const sortedKey =
    resolveSortedFieldDisplay(
      product,
      props.sortField,
      props.fieldMetadata,
      t,
      n,
      translatePlural
    )?.attributeKey ?? null

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
        isSorted: attribute.key === sortedKey,
      }
    })
    .filter((attribute): attribute is DisplayedAttribute => attribute != null)
}

const hasVertical = (product: ProductDto) => {
  return !!product.fullSlug?.trim()
}

const sortedFieldDisplay = (
  product: ProductDto
): { label: string; value: string } | null => {
  const display = resolveSortedFieldDisplay(
    product,
    props.sortField,
    props.fieldMetadata,
    t,
    n,
    translatePlural
  )

  if (!display) {
    return null
  }

  if (
    display.key === ECOSCORE_RELATIVE_FIELD ||
    display.key === 'price.minPrice.price'
  ) {
    return null
  }

  if (display.attributeKey) {
    const isVisible = popularAttributesByProduct(product).some(
      attribute => attribute.key === display.attributeKey
    )

    if (isVisible) {
      return null
    }
  }

  return {
    label: display.label,
    value: display.value,
  }
}
</script>

<style scoped lang="sass">
.category-product-list
  background: transparent
  display: flex
  flex-direction: column
  gap: 0.55rem

  :deep(.v-list-item__prepend)
    align-self: center

  &__item
    background: rgb(var(--v-theme-surface-default))
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.22)
    border-radius: 8px
    margin-bottom: 0
    padding: 0.65rem 0.75rem
    min-height: 128px
    transition: border-color 0.2s ease, box-shadow 0.2s ease

    &:hover
      border-color: rgba(var(--v-theme-border-primary-strong), 0.45)
      box-shadow: 0 10px 24px -20px rgba(21, 46, 73, 0.22)

  &__layout
    display: grid
    grid-template-columns: minmax(260px, 1fr)
    align-items: center
    gap: 0.85rem
    width: 100%

    @media (min-width: 992px)
      grid-template-columns: minmax(300px, 1fr) minmax(190px, 240px) minmax(132px, 160px) minmax(116px, auto)

  &__content
    display: flex
    flex-direction: column
    gap: 0.42rem
    min-width: 0

  &__header
    display: flex
    gap: 0.75rem
    align-items: center

  &__title
    margin: 0
    font-weight: 600
    font-size: 1rem
    line-height: 1.3
    color: rgb(var(--v-theme-text-neutral-strong))
    flex: 1 1 auto
    min-width: 0
    display: -webkit-box
    overflow: hidden
    -webkit-line-clamp: 2
    -webkit-box-orient: vertical

  &__meta
    display: flex
    gap: 1rem
    flex-wrap: wrap
    font-size: 0.82rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__prices-column
    display: flex
    flex-direction: column
    justify-content: center
    width: auto
    min-width: 0

  &__status
    display: inline-flex
    align-items: center
    gap: 0.5rem
    font-size: 0.82rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__status-text
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-strong))

  &__offers
    display: inline-flex
    align-items: center
    gap: 0.35rem

  &__score
    display: flex
    align-items: center
    justify-content: center
    width: auto
    min-width: 0

  &__actions
    display: flex
    align-items: center
    justify-content: flex-end
    gap: 0.5rem

  &__score-fallback
    font-size: 0.82rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attributes
    display: flex
    flex-wrap: wrap
    gap: 0.32rem 0.8rem
    margin: 0
    padding: 0
    list-style: none

  &__attribute
    display: flex
    gap: 0.35rem
    align-items: baseline
    font-size: 0.82rem

  &__attribute-label
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attribute-separator
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-weight: 600

  &__attribute-value
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__attribute--sorted
    .category-product-list__attribute-label,
    .category-product-list__attribute-value
      font-weight: 700
      color: rgb(var(--v-theme-text-neutral-strong))

  &__sorted-attribute
    margin-top: 0.15rem
    display: inline-flex
    gap: 0.35rem
    align-items: baseline
    font-size: 0.88rem

  &__sorted-attribute-label
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__sorted-attribute-value
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__compare-button
    justify-self: end

@media (max-width: 991px)
  .category-product-list
    :deep(.v-list-item__prepend)
      margin-inline-end: 0.75rem

    &__item
      min-height: 0

    &__layout
      align-items: start

    &__prices-column,
    &__score,
    &__actions
      justify-content: flex-start
</style>

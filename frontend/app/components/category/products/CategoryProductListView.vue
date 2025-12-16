<template>
  <v-list class="category-product-list" lines="three" density="comfortable">
    <v-list-item
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
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
              product.identity?.bestName ??
              product.identity?.model ??
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
                product.identity?.bestName ??
                product.identity?.model ??
                product.identity?.brand ??
                '#' + product.gtin
              }}
            </h3>
          </div>

          <div class="category-product-list__meta">
            <span class="category-product-list__price">
              <span aria-hidden="true" class="category-product-list__price-icon"
                >€</span
              >
              {{ listPriceValue(product) }}
            </span>
            <span class="category-product-list__offers">
              <v-icon icon="mdi-store" size="18" class="me-1" />
              {{ offersCountLabel(product) }}
            </span>
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
        </div>

        <div class="category-product-list__score">
          <ImpactScore
            v-if="impactScoreValue(product) != null"
            :score="impactScoreValue(product) ?? 0"
            :max="5"
            size="large"
            :show-value="true"
          />
          <span v-else class="category-product-list__score-fallback">
            {{ $t('category.products.notRated') }}
          </span>
        </div>

        <div class="category-product-list__actions">
          <CategoryProductCompareToggle :product="product" />
        </div>
      </div>
    </v-list-item>
  </v-list>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import {
  formatAttributeValue,
  resolvePopularAttributes,
} from '~/utils/_product-attributes'
import { resolvePrimaryImpactScore } from '~/utils/_product-scores'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
}>()

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const popularAttributeConfigs = computed(() => props.popularAttributes ?? [])

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

const bestPriceLabel = (product: ProductDto) => formatBestPrice(product, t, n)

const listPriceValue = (product: ProductDto) => {
  const currency = product.offers?.bestPrice?.currency
  const price = product.offers?.bestPrice?.price

  if (currency && currency !== 'EUR') {
    return bestPriceLabel(product)
  }

  if (price != null) {
    return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }

  return bestPriceLabel(product)
}

const offersCountLabel = (product: ProductDto) =>
  formatOffersCount(product, translatePlural)

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

  &__price
    display: inline-flex
    align-items: center
    gap: 0.4rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__price-icon
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__offers
    display: inline-flex
    align-items: center
    gap: 0.35rem

  &__score
    display: flex
    align-items: center
    justify-content: center
    min-width: 180px
    flex: 0 0 auto

  &__actions
    display: flex
    align-items: center
    justify-content: flex-end
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
</style>

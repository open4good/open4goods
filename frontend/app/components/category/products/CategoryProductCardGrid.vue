<template>
  <v-row class="category-product-card-grid" dense>
    <v-col
      v-for="product in products"
      :key="product.gtin ?? product.identity?.bestName ?? Math.random()"
      cols="12"
      sm="6"
      lg="4"
    >
      <v-card
        class="category-product-card-grid__card"
        rounded="xl"
        elevation="2"
        hover
        :to="productLink(product)"
      >
        <v-img
          :src="resolveImage(product)"
          :alt="product.identity?.bestName ?? product.identity?.model ?? $t('category.products.untitledProduct')"
          :aspect-ratio="4 / 3"
          contain
          class="category-product-card-grid__image"
        >
          <template #placeholder>
            <v-skeleton-loader type="image" class="h-100" />
          </template>
        </v-img>

        <v-card-item class="category-product-card-grid__body">
          <div class="category-product-card-grid__header">
            <h3 class="category-product-card-grid__title">
              {{ product.identity?.bestName ?? product.identity?.model ?? product.identity?.brand ?? '#' + product.gtin }}
            </h3>
            <CategoryProductCompareToggle :product="product" />
          </div>

          <div class="category-product-card-grid__score" role="presentation">
            <ImpactScore
              v-if="impactScoreValue(product) != null"
              :score="impactScoreValue(product) ?? 0"
              :max="5"
              size="small"
            />
            <span v-else class="category-product-card-grid__score-fallback">
              {{ $t('category.products.notRated') }}
            </span>
          </div>

          <div class="category-product-card-grid__meta">
            <span class="category-product-card-grid__price">
              {{ bestPriceLabel(product) }}
            </span>
            <span class="category-product-card-grid__offers">
              {{ offersCountLabel(product) }}
            </span>
          </div>

          <ul v-if="popularAttributesByProduct(product).length" class="category-product-card-grid__attributes" role="list">
            <li
              v-for="attribute in popularAttributesByProduct(product)"
              :key="attribute.key"
              class="category-product-card-grid__attribute"
              role="listitem"
            >
              <v-icon v-if="attribute.icon" :icon="attribute.icon" size="16" class="me-1" />
              <span class="category-product-card-grid__attribute-label">{{ attribute.label }}</span>
              <span class="category-product-card-grid__attribute-value">{{ attribute.value }}</span>
            </li>
          </ul>
        </v-card-item>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AttributeConfigDto, ProductDto } from '~~/shared/api-client'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import CategoryProductCompareToggle from './CategoryProductCompareToggle.vue'
import { formatAttributeValue, resolvePopularAttributes } from '~/utils/_product-attributes'
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

const productLink = (product: ProductDto) => {
  return product.fullSlug ?? product.slug ?? undefined
}

const impactScoreValue = (product: ProductDto) => resolvePrimaryImpactScore(product)

const bestPriceLabel = (product: ProductDto) => formatBestPrice(product, t, n)

const offersCountLabel = (product: ProductDto) => formatOffersCount(product, translatePlural)

type DisplayedAttribute = {
  key: string
  label: string
  value: string
  icon?: string | null
}

const popularAttributesByProduct = (product: ProductDto): DisplayedAttribute[] => {
  const attributes = resolvePopularAttributes(product, popularAttributeConfigs.value)
  const entries: DisplayedAttribute[] = []

  attributes.forEach((attribute) => {
    const value = formatAttributeValue(attribute, t, n)

    if (!value) {
      return
    }

    entries.push({
      key: attribute.key,
      label: attribute.label,
      value,
      icon: attribute.icon ?? null,
    })
  })

  return entries
}
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &__card
    height: 100%
    display: flex
    flex-direction: column
    text-decoration: none
    transition: transform 0.2s ease, box-shadow 0.2s ease

    &:hover
      transform: translateY(-4px)
      box-shadow: 0 16px 30px rgba(21, 46, 73, 0.08)

  &__image
    border-top-left-radius: inherit
    border-top-right-radius: inherit
    background: #fff
    display: flex
    align-items: center
    justify-content: center

    :deep(img)
      object-fit: contain
      mix-blend-mode: multiply
      background: #fff

  &__body
    display: flex
    flex-direction: column
    gap: 1rem
    align-items: stretch
    text-align: left
    padding: 1.25rem
    background: rgb(var(--v-theme-surface-glass))
    border-bottom-left-radius: inherit
    border-bottom-right-radius: inherit

  &__header
    display: flex
    align-items: flex-start
    gap: 0.75rem

  &__title
    font-size: 1.125rem
    margin: 0
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))
    flex: 1 1 auto
    min-width: 0
    white-space: normal

  &__meta
    display: flex
    flex-direction: column
    align-items: flex-start
    gap: 0.25rem

  &__offers
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__price
    font-size: 1.35rem
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__score
    display: flex
    align-items: center
    min-height: 1.75rem

  &__score-fallback
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__attributes
    display: grid
    gap: 0.5rem
    padding: 0
    margin: 0
    list-style: none

  &__attribute
    display: flex
    align-items: center
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__attribute-label
    font-weight: 500
    margin-right: 0.35rem
    color: rgb(var(--v-theme-text-neutral-strong))

  &__attribute-value
    color: inherit
</style>

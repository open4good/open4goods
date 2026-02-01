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
      <ProductCard
        :product="product"
        :popular-attributes="popularAttributes"
        :sort-field="sortField"
        :field-metadata="fieldMetadata"
        :size="normalizedSize"
        :max-attributes="maxAttributes"
        :show-attribute-icons="showAttributeIcons"
        :disabled="isDisabledCategory"
        :nofollow-links="nofollowLinks"
      />
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  AttributeConfigDto,
  FieldMetadataDto,
  ProductDto,
} from '~~/shared/api-client'
import ProductCard from '~/components/product/ProductCard.vue'
import { resolveProductShortName } from '~/utils/_product-title-resolver'

const props = defineProps<{
  products: ProductDto[]
  popularAttributes?: AttributeConfigDto[]
  sortField?: string | null
  fieldMetadata?: Record<string, FieldMetadataDto>
  size?: 'compact' | 'comfortable' | 'small' | 'medium' | 'big'
  variant?: 'classic' | 'compact-tile'
  maxAttributes?: number
  showAttributeIcons?: boolean
  isCategoryDisabled?: boolean
  nofollowLinks?: boolean
}>()

const { locale } = useI18n()

const resolveCardProductName = (product: ProductDto) =>
  resolveProductShortName(product, locale.value)

// Normalize size prop to small/medium/big
const normalizedSize = computed(() => {
  if (props.size === 'compact') return 'small'
  if (props.size === 'comfortable') return 'medium'
  return props.size ?? 'medium'
})

const variant = computed(() => props.variant ?? 'classic')
const isDisabledCategory = computed(() => props.isCategoryDisabled ?? false)
</script>

<style scoped lang="sass">
.category-product-card-grid
  margin: 0

  &--disabled
    :deep(.product-card)
      filter: grayscale(1)
      opacity: 0.6
</style>

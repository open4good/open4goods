<template>
  <div
    class="product-card-embed"
    :class="`product-card-embed--size-${normalizedSize}`"
  >
    <v-skeleton-loader
      v-if="pending"
      type="image, article"
      class="product-card-embed__skeleton"
    />

    <ProductCard
      v-else-if="resolvedProduct"
      :product="resolvedProduct"
      :size="normalizedSize"
    />

    <p v-else class="product-card-embed__empty">
      {{ t('buyingGuide.widget.empty') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import ProductCard from '~/components/product/ProductCard.vue'

type ProductResolveResponse = {
  product: ProductDto | null
  resolvedBy: 'gtin' | 'brand-model' | null
  confidence: 'high' | 'low' | null
  reason?: string
}

const props = withDefaults(
  defineProps<{
    gtin?: string | number
    brand?: string
    model?: string
    size?: 'small' | 'medium' | 'big'
  }>(),
  {
    gtin: undefined,
    brand: undefined,
    model: undefined,
    size: 'medium',
  }
)

const { t } = useI18n()

const normalizedSize = computed(() => {
  if (props.size === 'small') return 'small'
  if (props.size === 'big') return 'big'
  return 'medium'
})

const normalizedGtin = computed(() => `${props.gtin ?? ''}`.trim())
const normalizedBrand = computed(() => props.brand?.trim() ?? '')
const normalizedModel = computed(() => props.model?.trim() ?? '')

const hasIdentifier = computed(
  () =>
    normalizedGtin.value.length > 0 ||
    (normalizedBrand.value.length > 0 && normalizedModel.value.length > 0)
)

const { data, pending } = await useAsyncData<ProductDto | null>(
  () =>
    `product-card-embed:${normalizedGtin.value}:${normalizedBrand.value}:${normalizedModel.value}`,
  async () => {
    if (!hasIdentifier.value) {
      return null
    }

    try {
      if (normalizedGtin.value) {
        return await $fetch<ProductDto>(
          `/api/products/${encodeURIComponent(normalizedGtin.value)}`
        )
      }

      const resolved = await $fetch<ProductResolveResponse>(
        '/api/products/resolve',
        {
          query: {
            brand: normalizedBrand.value,
            model: normalizedModel.value,
          },
        }
      )

      return resolved?.product ?? null
    } catch (error) {
      console.error('Failed to load product card embed', error)
      return null
    }
  },
  {
    watch: [normalizedGtin, normalizedBrand, normalizedModel],
  }
)

const resolvedProduct = computed(() => data.value ?? null)
</script>

<style scoped lang="sass">
.product-card-embed
  margin: 1rem 0
  max-width: 320px

  &--size-small
    max-width: 240px

  &--size-big
    max-width: 380px

  &__skeleton
    border-radius: 16px
    overflow: hidden

  &__empty
    font-size: 0.85rem
    font-style: italic
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

<template>
  <span
    class="product-embed"
    :class="[
      `product-embed--style-${style}`,
      `product-embed--size-${normalizedSize}`,
      {
        'product-embed--pending': pending,
        'product-embed--missing': !resolvedProduct,
      },
    ]"
  >
    <NuxtLink
      v-if="resolvedProduct && productLink"
      :to="productLink"
      class="product-embed__link"
      :title="hoverTitle"
      :aria-label="hoverTitle"
    >
      {{ visibleLabel }}
    </NuxtLink>

    <span v-else-if="pending" class="product-embed__placeholder" aria-hidden="true"
      >…</span
    >
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import {
  resolveProductLongName,
  resolveProductShortName,
} from '~/utils/_product-title-resolver'

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
    style?: 'text'
    size?: 's' | 'm' | 'l'
  }>(),
  {
    gtin: undefined,
    brand: undefined,
    model: undefined,
    style: 'text',
    size: 'm',
  }
)

const normalizedSize = computed(() => {
  if (props.size === 's') return 's'
  if (props.size === 'l') return 'l'
  return 'm'
})

const normalizedGtin = computed(() => `${props.gtin ?? ''}`.trim())
const normalizedBrand = computed(() => props.brand?.trim() ?? '')
const normalizedModel = computed(() => props.model?.trim() ?? '')

const hasIdentifier = computed(
  () =>
    normalizedGtin.value.length > 0 ||
    (normalizedBrand.value.length > 0 && normalizedModel.value.length > 0)
)

const { locale } = useI18n()

const { data, pending } = await useAsyncData<ProductResolveResponse | null>(
  () =>
    `product-embed:${normalizedGtin.value}:${normalizedBrand.value}:${normalizedModel.value}`,
  async () => {
    if (!hasIdentifier.value) {
      return {
        product: null,
        resolvedBy: null,
        confidence: null,
        reason: 'missing-identifier',
      }
    }

    return await $fetch<ProductResolveResponse>('/api/products/resolve', {
      query: {
        ...(normalizedGtin.value ? { gtin: normalizedGtin.value } : {}),
        ...(!normalizedGtin.value && normalizedBrand.value
          ? { brand: normalizedBrand.value }
          : {}),
        ...(!normalizedGtin.value && normalizedModel.value
          ? { model: normalizedModel.value }
          : {}),
      },
    })
  },
  {
    watch: [normalizedGtin, normalizedBrand, normalizedModel],
  }
)

const resolvedProduct = computed(() => data.value?.product ?? null)

const visibleLabel = computed(() => {
  const product = resolvedProduct.value

  if (!product) {
    return ''
  }

  const brand = product.identity?.brand?.trim()
  const model = product.identity?.model?.trim()

  if (brand && model) {
    return `${brand.toLocaleUpperCase(locale.value)} - ${model}`
  }

  return resolveProductShortName(product, locale.value)
})

const hoverTitle = computed(() => {
  const product = resolvedProduct.value

  if (!product) {
    return ''
  }

  const longName = resolveProductLongName(product, locale.value).trim()
  if (longName) {
    return longName
  }

  return visibleLabel.value
})

const productLink = computed(() => {
  const product = resolvedProduct.value

  if (!product) {
    return undefined
  }

  const slug = product.fullSlug?.trim() || product.slug?.trim()

  if (!slug) {
    return undefined
  }

  return slug.startsWith('/') ? slug : `/${slug}`
})
</script>

<style scoped lang="sass">
.product-embed
  display: inline

  &__link
    text-decoration: underline
    text-decoration-thickness: 1px
    text-underline-offset: 2px
    color: rgb(var(--v-theme-primary))
    font-weight: 500

    &:hover
      color: rgb(var(--v-theme-text-neutral-strong))

  &__placeholder
    opacity: 0.6

  &--size-s .product-embed__link
    font-size: 0.85rem

  &--size-m .product-embed__link
    font-size: 1rem

  &--size-l .product-embed__link
    font-size: 1.1rem
</style>

<template>
  <v-container class="py-10 product-page">
    <pre v-if="product">{{ JSON.stringify(product, null, 2) }}</pre>
  </v-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { extractGtinFromProductSlug, normalizeFullSlug, validateCategorySlug } from './product-helpers'

const route = useRoute()

const rawCategoryParam = route.params.categorySlug
const categorySlugInput = Array.isArray(rawCategoryParam) ? rawCategoryParam[0] ?? '' : rawCategoryParam ?? ''
const categorySlug = validateCategorySlug(categorySlugInput)

const rawProductParam = route.params.productSlug
const productSlugValue = Array.isArray(rawProductParam) ? rawProductParam[0] ?? '' : rawProductParam ?? ''
const gtin = extractGtinFromProductSlug(productSlugValue)
const productSlug = productSlugValue

const { data: productData } = await useAsyncData<ProductDto>(
  `product-${gtin}`,
  () => $fetch<ProductDto>(`/api/products/${gtin}`)
)

const product = computed(() => productData.value ?? null)
const normalizedFullSlug = computed(() => normalizeFullSlug(product.value?.fullSlug))
const currentFullSlug = `${categorySlug}/${productSlug}`

if (normalizedFullSlug.value && normalizedFullSlug.value !== currentFullSlug) {
  await navigateTo(`/${normalizedFullSlug.value}`, { redirectCode: 301 })
}
</script>

<style scoped>
.product-page {
  min-height: 70vh;
}
</style>

<route lang="ts">
export default {
  name: 'category-product-redirect',
  path: '/:categorySlug/:productSlug(\\d{5,}[-a-z0-9]*)',
}
</route>

<script setup lang="ts">
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'

const route = useRoute()

const rawCategory = route.params.categorySlug
const rawProduct = route.params.productSlug

const categorySlug = typeof rawCategory === 'string' ? rawCategory.trim() : ''
const productSlug = typeof rawProduct === 'string' ? rawProduct.trim() : ''

const productMatch = matchProductRouteFromSegments([categorySlug, productSlug])

if (!productMatch) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const normalizedCategorySlug = productMatch.categorySlug
const normalizedProductSegment = `${productMatch.gtin}-${productMatch.slug}`

await navigateTo(
  {
    name: 'slug',
    params: { slug: [normalizedCategorySlug, normalizedProductSegment] },
  },
  { replace: true, redirectCode: 301 },
)
</script>

<template>
  <div class="product-route-redirect" />
</template>

<style scoped>
.product-route-redirect {
  display: none;
}
</style>

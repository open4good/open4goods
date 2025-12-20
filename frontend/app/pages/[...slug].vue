<template>
  <ProductPage v-if="productRoute" :product-route="productRoute" />
  <CategoryPage v-else-if="categorySlug" :slug="categorySlug" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'
import ProductPage from '~/components/pages/ProductPage.vue'
import CategoryPage from '~/components/pages/CategoryPage.vue'

const route = useRoute()
const slugParam = route.params.slug
const segments = Array.isArray(slugParam)
  ? slugParam.filter((s): s is string => typeof s === 'string')
  : typeof slugParam === 'string'
    ? [slugParam]
    : []

// Try to match product
const productRoute = computed(() => matchProductRouteFromSegments(segments))

// Try to match category (single segment, letters/hyphens)

const categoryPattern = /^[a-z-]+$/
const categorySlug = computed(() => {
  if (productRoute.value) return null
  if (segments.length === 1 && categoryPattern.test(segments[0])) {
    return segments[0]
  }
  return null
})

if (!productRoute.value && !categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}
</script>

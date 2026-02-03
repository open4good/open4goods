<template>
  <ProductPage v-if="productRoute" :product-route="productRoute" />
  <XwikiFullPageRenderer v-else-if="wikiRoute" :page-id="wikiRoute.pageId" />
  <CategoryPage v-else-if="categorySlug" :slug="categorySlug" />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'
import { matchLocalizedWikiRouteByPath } from '~~/shared/utils/localized-routes'
import ProductPage from '~/components/pages/ProductPage.vue'
import CategoryPage from '~/components/pages/CategoryPage.vue'

const XwikiFullPageRenderer = defineAsyncComponent(
  () => import('~/components/cms/XwikiFullPageRenderer.vue')
)

const route = useRoute()
const slugParam = route.params.slug
const segments = Array.isArray(slugParam)
  ? slugParam.filter((s): s is string => typeof s === 'string')
  : typeof slugParam === 'string'
    ? [slugParam]
    : []

// Try to match product
const productRoute = computed(() => matchProductRouteFromSegments(segments))

// Try to match wiki page
const wikiRoute = computed(() => {
  if (productRoute.value) return null
  return matchLocalizedWikiRouteByPath(route.path)
})

// Try to match category (single segment, letters/hyphens)
const categoryPattern = /^[a-z-]+$/
const categorySlug = computed(() => {
  if (productRoute.value || wikiRoute.value) return null
  if (segments.length === 1 && categoryPattern.test(segments[0])) {
    return segments[0]
  }
  return null
})

if (!productRoute.value && !categorySlug.value && !wikiRoute.value) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}
</script>

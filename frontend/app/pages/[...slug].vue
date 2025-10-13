<template>
  <v-container class="py-10">
    <v-alert
      v-if="errorMessage"
      type="error"
      variant="tonal"
      border="start"
      class="mb-6"
    >
      {{ errorMessage }}
    </v-alert>

    <v-skeleton-loader
      v-else-if="pending"
      type="article"
      class="mb-6"
    />

    <div v-else-if="product">
      <h1 class="text-h4 font-weight-bold mb-4">
        {{ productTitle }}
      </h1>
      <p v-if="productSubtitle" class="text-body-1 mb-6">
        {{ productSubtitle }}
      </p>

      <v-card variant="outlined">
        <v-card-title class="text-h6">
          Produit (réponse API)
        </v-card-title>
        <v-card-text>
          <pre class="text-body-2 mb-0">{{ formattedProduct }}</pre>
        </v-card-text>
      </v-card>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'
import {
  isBackendNotFoundError,
  matchProductRouteFromSegments,
} from '~~/shared/utils/_product-route'

const route = useRoute()
const requestURL = useRequestURL()

const slugParam = route.params.slug
const segments = Array.isArray(slugParam)
  ? slugParam.filter((segment): segment is string => typeof segment === 'string')
  : typeof slugParam === 'string'
    ? [slugParam]
    : []

const productRoute = matchProductRouteFromSegments(segments)

if (!productRoute) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { categorySlug, gtin } = productRoute

const {
  data: productData,
  pending,
  error,
} = await useAsyncData<ProductDto | null>(
  `product-${gtin}`,
  async () => {
    try {
      return await $fetch<ProductDto>(`/api/products/${gtin}`)
    } catch (fetchError) {
      if (isBackendNotFoundError(fetchError)) {
        throw createError({
          statusCode: 404,
          statusMessage: 'Product not found',
          cause: fetchError,
        })
      }

      throw fetchError
    }
  },
  { server: true, immediate: true }
)

const product = computed(() => productData.value)

if (product.value?.fullSlug) {
  const currentPath = route.path.startsWith('/') ? route.path : `/${route.path}`
  const targetPath = product.value.fullSlug.startsWith('/')
    ? product.value.fullSlug
    : `/${product.value.fullSlug}`

  if (targetPath !== currentPath) {
    await navigateTo(targetPath, { replace: true, redirectCode: 301 })
  }
}

const productTitle = computed(() => {
  return (
    product.value?.names?.h1Title ??
    product.value?.identity?.bestName ??
    product.value?.slug ??
    `GTIN ${product.value?.gtin ?? gtin}`
  )
})

const productSubtitle = computed(() => {
  if (!product.value?.identity?.brand) {
    return null
  }

  return `${product.value.identity.brand} · ${categorySlug}`
})

const productMetaDescription = computed(() => {
  return (
    product.value?.names?.metaDescription ??
    productSubtitle.value ??
    productTitle.value
  )
})

const formattedProduct = computed(() =>
  product.value ? JSON.stringify(product.value, null, 2) : null
)

const canonicalUrl = computed(() =>
  new URL(route.fullPath, requestURL.origin).toString()
)

useSeoMeta({
  title: () => productTitle.value,
  description: () => productMetaDescription.value,
  ogTitle: () => product.value?.names?.ogTitle ?? productTitle.value,
  ogDescription: () =>
    product.value?.names?.ogDescription ?? productMetaDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'website',
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
}))

const errorMessage = computed(() => {
  if (!error.value) {
    return null
  }

  if (error.value instanceof Error) {
    return error.value.message
  }

  return String(error.value)
})
</script>

<style scoped lang="sass">
pre
  white-space: pre-wrap
  word-break: break-word
</style>

<template>
  <v-container class="py-10 product-page">
    <v-skeleton-loader
      v-if="loading"
      type="article"
      class="mb-6"
    />

    <div v-else-if="product" class="product-page__content">
      <header class="product-page__header">
        <h1 class="text-h4 text-md-h3 font-weight-bold mb-2">
          {{ t('productPage.heading', { name: productTitle }) }}
        </h1>
        <p class="text-body-1 text-medium-emphasis">
          {{ t('productPage.description', { gtin: product.gtin }) }}
        </p>
      </header>

      <section class="product-page__preview">
        <h2 class="text-h6 font-weight-semibold mb-2">
          {{ t('productPage.preview.title') }}
        </h2>
        <p class="text-body-2 mb-4">
          {{ t('productPage.preview.caption') }}
        </p>
        <pre class="product-page__json" aria-live="polite">{{ formattedJson }}</pre>
      </section>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import type { ProductDto } from '~~/shared/api-client'

const route = useRoute()
const requestURL = useRequestURL()
const { t, locale } = useI18n()

const rawCategoryParam = route.params.categorySlug
const categorySlug = Array.isArray(rawCategoryParam)
  ? rawCategoryParam[0] ?? ''
  : rawCategoryParam ?? ''
const categoryPattern = /^[a-z]+(?:-[a-z]+)*$/

if (!categoryPattern.test(categorySlug)) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const rawProductParam = route.params.gtinSlug
const gtinSlug = Array.isArray(rawProductParam)
  ? rawProductParam[0] ?? ''
  : rawProductParam ?? ''
const productPattern = /^(?<gtin>\d{6,})(?:-(?<slug>[a-z0-9-]+))$/
const productMatch = gtinSlug.match(productPattern)

if (!productMatch?.groups?.gtin) {
  throw createError({ statusCode: 404, statusMessage: 'Product not found' })
}

const gtin = Number.parseInt(productMatch.groups.gtin, 10)

if (!Number.isSafeInteger(gtin)) {
  throw createError({ statusCode: 404, statusMessage: 'Product not found' })
}

const {
  data: productData,
  status,
  error,
} = await useAsyncData(
  `product-${gtin}`,
  async () => {
    return await $fetch<ProductDto>(`/api/products/${gtin}`)
  },
  { server: true, immediate: true },
)

if (error.value) {
  throw error.value
}

const product = computed<ProductDto | null>(() => productData.value ?? null)
const loading = computed(() => status.value === 'pending')
const formattedJson = computed(() =>
  product.value ? JSON.stringify(product.value, null, 2) : t('productPage.preview.empty'),
)
const productTitle = computed(
  () =>
    product.value?.identity?.bestName ??
    product.value?.names?.h1Title ??
    product.value?.slug ??
    String(gtin),
)
const productDescription = computed(() =>
  product.value?.names?.metaDescription ??
  product.value?.names?.ogDescription ??
  t('productPage.seo.description', { name: productTitle.value }),
)

if (import.meta.server) {
  const fullSlug = product.value?.fullSlug
  if (fullSlug && fullSlug !== route.path) {
    await navigateTo(fullSlug, { redirectCode: 301 })
  }
}

if (import.meta.client) {
  watchEffect(() => {
    const fullSlug = product.value?.fullSlug
    if (!fullSlug || fullSlug === route.path) {
      return
    }

    navigateTo(fullSlug, { replace: true })
  })
}

const siteName = computed(() => String(t('siteIdentity.siteName')))
const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const ogLocale = computed(() => locale.value.replace('-', '_'))

useSeoMeta({
  title: () => t('productPage.seo.title', { name: productTitle.value }),
  description: () => productDescription.value,
  ogTitle: () => t('productPage.seo.title', { name: productTitle.value }),
  ogDescription: () => productDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'website',
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
}))
</script>

<style scoped lang="sass">
.product-page
  min-height: 70vh

  &__content
    display: flex
    flex-direction: column
    gap: 2rem

  &__header
    max-width: 720px

  &__preview
    max-width: 960px

  &__json
    background: rgba(var(--v-theme-surface-muted), 1)
    border-radius: 12px
    padding: 1.5rem
    overflow-x: auto
    font-family: 'Fira Code', 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace
    font-size: 0.875rem
    line-height: 1.4
    white-space: pre-wrap
</style>

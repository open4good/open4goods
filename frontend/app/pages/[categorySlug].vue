<template>
  <v-container class="py-10 category-page">
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
      v-else-if="pageLoading"
      type="image, article"
      class="mb-6"
    />

    <div v-else-if="category" class="category-page__content">
      <v-row class="align-center mb-8" justify="space-between">
        <v-col cols="12" md="6">
          <h1 class="text-h3 text-md-h2 font-weight-bold mb-4">
            {{ category.verticalHomeTitle }}
          </h1>
          <p v-if="category.verticalHomeDescription" class="text-body-1">
            {{ category.verticalHomeDescription }}
          </p>
        </v-col>
        <v-col v-if="heroImage" cols="12" md="5">
          <v-img
            :src="heroImage"
            :alt="category.verticalHomeTitle ?? siteName"
            height="320"
            cover
            class="rounded-lg"
          />
        </v-col>
      </v-row>

      <v-card
        v-if="category.verticalMetaDescription"
        variant="outlined"
        class="mb-8"
      >
        <v-card-title class="text-h5">
          {{ category.verticalMetaTitle ?? category.verticalHomeTitle }}
        </v-card-title>
        <v-card-text>
          <p class="text-body-1">
            {{ category.verticalMetaDescription }}
          </p>
        </v-card-text>
      </v-card>

      <v-row v-if="category.subsets?.length" class="category-page__subsets">
        <v-col
          v-for="subset in category.subsets"
          :key="subset.id ?? subset.title ?? subset.caption"
          cols="12"
          md="6"
          lg="4"
        >
          <v-card variant="outlined" class="h-100 d-flex flex-column">
            <v-img
              v-if="subset.image"
              :src="subset.image"
              :alt="subset.title ?? subset.caption ?? ''"
              height="180"
              cover
            />
            <v-card-title class="text-h6">
              {{ subset.title ?? subset.caption }}
            </v-card-title>
            <v-card-text class="d-flex flex-column gap-3">
              <p v-if="subset.description" class="text-body-2">
                {{ subset.description }}
              </p>
              <NuxtLink
                v-if="subset.url && subset.caption"
                :to="subset.url"
                class="text-body-2 font-weight-medium"
              >
                {{ subset.caption }}
              </NuxtLink>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </v-container>
</template>

<script setup lang="ts">
import { useCategories } from '~/composables/categories/useCategories'

const route = useRoute()
const { locale, t } = useI18n()
const requestURL = useRequestURL()

const rawParam = route.params.categorySlug
const slug = Array.isArray(rawParam) ? rawParam[0] ?? '' : rawParam ?? ''
const slugPattern = /^[a-z-]+$/

if (!slugPattern.test(slug)) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found' })
}

const { currentCategory, loading, error: categoriesError, selectCategoryBySlug } = useCategories()

const { data: categoryData } = await useAsyncData(
  `category-detail-${slug}`,
  async () => {
    try {
      return await selectCategoryBySlug(slug)
    } catch (err) {
      if (err instanceof Error && err.name === 'CategoryNotFoundError') {
        throw createError({ statusCode: 404, statusMessage: err.message, cause: err })
      }

      throw err
    }
  },
  { server: true, immediate: true },
)

const category = computed(() => categoryData.value ?? currentCategory.value ?? null)
const pageLoading = computed(() => loading.value && !category.value)
const errorMessage = computed(() => categoriesError.value)

const heroImage = computed(() => {
  if (!category.value) {
    return null
  }

  return (
    category.value.imageLarge ??
    category.value.imageMedium ??
    category.value.imageSmall ??
    null
  )
})

const siteName = computed(() => String(t('siteIdentity.siteName')))
const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const seoTitle = computed(
  () => category.value?.verticalMetaTitle ?? category.value?.verticalHomeTitle ?? siteName.value,
)
const seoDescription = computed(
  () => category.value?.verticalMetaDescription ?? category.value?.verticalHomeDescription ?? '',
)
const ogTitle = computed(
  () => category.value?.verticalMetaOpenGraphTitle ?? seoTitle.value,
)
const ogDescription = computed(
  () => category.value?.verticalMetaOpenGraphDescription ?? seoDescription.value,
)
const ogImage = computed(() => {
  if (!heroImage.value) {
    return undefined
  }

  try {
    return new URL(heroImage.value, requestURL.origin).toString()
  } catch (error) {
    console.error('Invalid hero image URL', error)
    return undefined
  }
})
const ogLocale = computed(() => locale.value.replace('-', '_'))

useSeoMeta({
  title: () => seoTitle.value,
  description: () => seoDescription.value,
  ogTitle: () => ogTitle.value,
  ogDescription: () => ogDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogType: 'website',
  ogImage: () => ogImage.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => category.value?.verticalHomeTitle ?? siteName.value,
})

useHead(() => ({
  link: [
    { rel: 'canonical', href: canonicalUrl.value },
  ],
}))
</script>

<style scoped lang="sass">
.category-page
  min-height: 70vh

  &__content
    display: flex
    flex-direction: column
    gap: 2rem

  &__subsets
    gap: 1.5rem

  .v-card-text
    flex-grow: 1
</style>

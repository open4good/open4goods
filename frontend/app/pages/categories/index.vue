<template>
  <div class="categories-page" data-testid="categories-index-page">
    <CategoryNavigationHero
      v-model="searchTerm"
      :title="heroTitle"
      :description="heroDescription"
      :breadcrumbs="breadcrumbs"
      :result-summary="resultSummary"
      :search-label="t('categories.navigation.hero.searchLabel')"
      :search-placeholder="t('categories.navigation.hero.searchPlaceholder')"
      :breadcrumb-aria-label="
        t('categories.navigation.hero.breadcrumbAriaLabel')
      "
    />

    <v-progress-linear
      v-if="pending"
      indeterminate
      color="primary"
      class="categories-page__loader"
      :aria-label="t('categories.navigation.loading')"
      role="status"
    />

    <v-container v-if="error" class="py-10 px-4" max-width="xl">
      <v-alert
        type="error"
        variant="tonal"
        border="start"
        prominent
        class="mb-6"
        role="alert"
      >
        {{ t('categories.navigation.error.loadFailed') }}
      </v-alert>
      <v-btn color="primary" variant="flat" @click="refresh">
        {{ t('common.actions.retry') }}
      </v-btn>
    </v-container>

    <CategoryNavigationVerticalHighlights
      v-if="navigationData"
      :verticals="highlightedVerticals"
    />

    <CategoryNavigationGrid
      v-if="navigationData"
      :categories="filteredCategories"
    />


  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CategoryNavigationDto } from '~~/shared/api-client'

import CategoryNavigationGrid from '~/components/category/navigation/CategoryNavigationGrid.vue'
import CategoryNavigationHero from '~/components/category/navigation/CategoryNavigationHero.vue'
import CategoryNavigationVerticalHighlights from '~/components/category/navigation/CategoryNavigationVerticalHighlights.vue'

const { t } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const route = useRoute()
const requestURL = useRequestURL()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

const searchTerm = ref('')

const { data, pending, error, refresh } =
  await useAsyncData<CategoryNavigationDto>(
    'category-navigation-root',
    async () =>
      $fetch<CategoryNavigationDto>('/api/categories/navigation', {
        headers: requestHeaders,
      })
  )

if (error.value && import.meta.server) {
  throw createError({
    statusCode: (error.value as { statusCode?: number })?.statusCode ?? 500,
    statusMessage:
      (error.value as { statusMessage?: string })?.statusMessage ??
      'Failed to load categories',
  })
}

const navigationData = computed(() => data.value ?? null)

const heroTitle = computed(() => {
  const category = navigationData.value?.category

  if (category?.googleCategoryId === 0) {
    return t('categories.navigation.hero.rootProductsTitle')
  }

  return category?.title ?? t('categories.navigation.hero.title')
})

const heroDescription = computed(
  () =>
    navigationData.value?.category?.vertical?.verticalHomeDescription ??
    t('categories.navigation.hero.description')
)

const breadcrumbs = computed(() => {
  const items = navigationData.value?.breadcrumbs ?? []
  const normalized = items.map((breadcrumb, index) => ({
    title: breadcrumb.title ?? '',
    link:
      index < items.length - 1 && breadcrumb.link
        ? `/categories/${breadcrumb.link}`
        : undefined,
  }))

  const trail = [
    {
      title: t('categories.navigation.hero.rootBreadcrumb'),
      link: '/categories',
    },
    ...normalized,
  ]

  return trail.filter((item, idx, array) => {
    if (!item.title?.trim()) {
      return false
    }

    if (idx === 0) {
      return true
    }

    const previous = array[idx - 1]
    return previous?.title !== item.title
  })
})

const filteredCategories = computed(() => {
  const categories = navigationData.value?.childCategories ?? []
  if (!searchTerm.value.trim()) {
    return categories
  }

  const query = searchTerm.value.trim().toLowerCase()
  return categories.filter(
    category => category.title?.toLowerCase().includes(query) ?? false
  )
})

const highlightedVerticals = computed(
  () => navigationData.value?.descendantVerticals ?? []
)

const resultSummary = computed(() =>
  translatePlural(
    'categories.navigation.hero.resultsSummary',
    filteredCategories.value.length
  )
)

const httpsOrigin = computed(() => {
  const url = new URL(requestURL.origin)
  url.protocol = 'https:'
  return url.origin
})

const canonicalUrl = computed(() => {
  const url = new URL(route.fullPath, httpsOrigin.value)
  url.protocol = 'https:'
  return url.toString()
})

const buildAbsoluteUrl = (path?: string) => {
  if (!path) {
    return canonicalUrl.value
  }

  const url = new URL(path, httpsOrigin.value)
  url.protocol = 'https:'
  return url.toString()
}

const breadcrumbJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'BreadcrumbList',
  itemListElement: breadcrumbs.value.map((breadcrumb, index) => ({
    '@type': 'ListItem',
    position: index + 1,
    name: breadcrumb.title,
    item: buildAbsoluteUrl(breadcrumb.link),
  })),
}))

const itemListJsonLd = computed(() => ({
  '@context': 'https://schema.org',
  '@type': 'ItemList',
  itemListElement: filteredCategories.value.map((category, index) => ({
    '@type': 'ListItem',
    position: index + 1,
    name: category.title ?? '',
    url: buildAbsoluteUrl(
      category.path ? `/categories/${category.path}` : undefined
    ),
  })),
}))
const ogImageUrl = computed(() =>
  new URL('/nudger-icon-512x512.png', requestURL.origin).toString()
)

useSeoMeta({
  title: () => String(heroTitle.value),
  description: () => String(heroDescription.value),
  ogTitle: () => String(heroTitle.value),
  ogDescription: () => String(heroDescription.value),
  ogUrl: () => canonicalUrl.value,
  ogType: () => 'website',
  ogImage: () => ogImageUrl.value,
})

useHead(() => ({
  link: [{ rel: 'canonical', href: canonicalUrl.value }],
  script: [
    {
      key: 'categories-index-breadcrumb-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(breadcrumbJsonLd.value),
    },
    {
      key: 'categories-index-itemlist-jsonld',
      type: 'application/ld+json',
      children: JSON.stringify(itemListJsonLd.value),
    },
  ],
}))
</script>

<style scoped>
.categories-page__loader {
  margin: -0.5rem 0 0;
}
</style>

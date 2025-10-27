<template>
  <div class="category-ecoscore">
    <CategoryHero
      v-if="category"
      :title="heroTitle"
      :description="heroDescription"
      :image="heroImage"
      :breadcrumbs="category.breadCrumb ?? []"
      :eyebrow="category.verticalMetaTitle"
    />

    <v-container v-if="category" fluid class="py-6 category-ecoscore__container">
      <v-card class="category-ecoscore__card" rounded="xl" elevation="1">
        <v-card-title class="category-ecoscore__card-title">
          {{ placeholderTitle }}
        </v-card-title>
        <v-card-text class="category-ecoscore__card-description">
          {{ placeholderDescription }}
        </v-card-text>
      </v-card>
    </v-container>

    <v-container v-else fluid class="py-10">
      <v-skeleton-loader type="image, article" />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import CategoryHero from '~/components/category/CategoryHero.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type { VerticalConfigFullDto } from '~~/shared/api-client'

const route = useRoute()
const requestURL = useRequestURL()
const { t, locale } = useI18n()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Category not found' })
}

const { selectCategoryBySlug } = useCategories()
const category = ref<VerticalConfigFullDto | null>(null)

try {
  category.value = await selectCategoryBySlug(categorySlug.value)
} catch (error) {
  if (error instanceof Error && error.name === 'CategoryNotFoundError') {
    throw createError({ statusCode: 404, statusMessage: 'Category not found', cause: error })
  }

  console.error('Failed to resolve category for ecoscore page', error)
  throw createError({ statusCode: 500, statusMessage: 'Failed to load category', cause: error })
}

const siteName = computed(() => String(t('siteIdentity.siteName')))

const heroTitle = computed(() => category.value?.verticalHomeTitle ?? siteName.value)
const heroDescription = computed(() => category.value?.verticalHomeDescription ?? null)
const heroImage = computed(() => {
  if (!category.value) {
    return null
  }

  return (
    category.value.imageMedium ??
    category.value.imageLarge ??
    category.value.imageSmall ??
    null
  )
})

const categoryLabel = computed(
  () => category.value?.verticalHomeTitle ?? category.value?.verticalMetaTitle ?? siteName.value,
)
const ecoscoreLabel = computed(() => t('siteIdentity.footer.highlightLinks.ecoscore'))
const placeholderTitle = computed(() =>
  t('category.ecoscorePage.title', {
    category: categoryLabel.value,
    label: ecoscoreLabel.value,
  }),
)
const placeholderDescription = computed(() =>
  t('category.ecoscorePage.description', {
    category: categoryLabel.value,
  }),
)

const canonicalUrl = computed(() => new URL(route.fullPath, requestURL.origin).toString())
const seoTitle = computed(() => placeholderTitle.value)
const seoDescription = computed(
  () =>
    category.value?.verticalMetaDescription ??
    category.value?.verticalHomeDescription ??
    placeholderDescription.value,
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
  ogTitle: () => seoTitle.value,
  ogDescription: () => seoDescription.value,
  ogUrl: () => canonicalUrl.value,
  ogImage: () => ogImage.value,
  ogSiteName: () => siteName.value,
  ogLocale: () => ogLocale.value,
  ogImageAlt: () => categoryLabel.value,
})
</script>

<style scoped lang="sass">
.category-ecoscore
  display: flex
  flex-direction: column
  gap: clamp(2rem, 3vw, 3rem)
  background: rgb(var(--v-theme-surface-default))

.category-ecoscore__container
  display: flex
  justify-content: center

.category-ecoscore__card
  max-width: min(720px, 100%)
  margin-inline: auto
  background: rgb(var(--v-theme-surface-glass))
  padding: clamp(1.75rem, 3vw, 2.5rem)
  text-align: center

.category-ecoscore__card-title
  font-size: clamp(1.5rem, 2.5vw, 2rem)
  font-weight: 600

.category-ecoscore__card-description
  margin-top: 0.5rem
  font-size: clamp(1rem, 1.5vw, 1.2rem)
  color: rgba(var(--v-theme-text-neutral-secondary), 0.96)
</style>

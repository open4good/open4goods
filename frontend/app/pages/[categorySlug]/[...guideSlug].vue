<script setup lang="ts">
import { computed } from 'vue'
import XwikiFullPageRenderer from '~/components/cms/XwikiFullPageRenderer.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type { VerticalConfigFullDto } from '~~/shared/api-client'

const normaliseSlug = (value: string | null | undefined) =>
  value?.trim().replace(/^\/+|\/+$/g, '').toLowerCase() ?? ''

definePageMeta({
  validate(route) {
    const raw = route.params.guideSlug
    const slug = Array.isArray(raw) ? raw.join('/') : String(raw ?? '')
    const normalised = slug.trim().replace(/^\/+|\/+$/g, '')

    if (!normalised) {
      return false
    }

    if (/^\d{5,}/.test(normalised)) {
      return false
    }

    return normalised.toLowerCase() !== 'ecoscore'
  },
})

const route = useRoute()
const { selectCategoryBySlug } = useCategories()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Category not found' })
}

const rawSlugParam = route.params.guideSlug
const slugPath = Array.isArray(rawSlugParam) ? rawSlugParam.join('/') : String(rawSlugParam ?? '')
const normalisedSlug = normaliseSlug(slugPath)

if (!normalisedSlug) {
  throw createError({ statusCode: 404, statusMessage: 'Guide not found' })
}

let categoryDetail: VerticalConfigFullDto

try {
  categoryDetail = await selectCategoryBySlug(categorySlug.value)
} catch (error) {
  if (error instanceof Error && error.name === 'CategoryNotFoundError') {
    throw createError({ statusCode: 404, statusMessage: 'Category not found', cause: error })
  }

  console.error('Failed to resolve category for wiki guide', error)
  throw createError({ statusCode: 500, statusMessage: 'Failed to load category', cause: error })
}

const matchedPage = categoryDetail.wikiPages?.find(page => normaliseSlug(page.verticalUrl) === normalisedSlug) ?? null

if (!matchedPage) {
  throw createError({ statusCode: 404, statusMessage: 'Guide not found' })
}

const pageId = matchedPage.wikiUrl?.trim().replace(/^\/+/, '') ?? null

if (!pageId) {
  throw createError({ statusCode: 404, statusMessage: 'Guide not found' })
}

const fallbackTitle = matchedPage.title ?? null
const fallbackDescription = categoryDetail.verticalHomeDescription ?? null
</script>

<template>
  <XwikiFullPageRenderer
    :page-id="pageId"
    :fallback-title="fallbackTitle"
    :fallback-description="fallbackDescription"
  />
</template>

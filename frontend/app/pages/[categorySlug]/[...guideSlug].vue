<script setup lang="ts">
import { computed, ref } from 'vue'
import XwikiFullPageRenderer from '~/components/cms/XwikiFullPageRenderer.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type { VerticalConfigFullDto } from '~~/shared/api-client'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'

const normaliseSlug = (value: string | null | undefined) =>
  value?.trim().replace(/^\/+|\/+$/g, '').toLowerCase() ?? ''

definePageMeta({
  path:
    '/:categorySlug/:guideSlug([A-Za-z][A-Za-z0-9-]*)',
  validate(route) {
    const raw = route.params.guideSlug
    const slug = Array.isArray(raw) ? raw.join('/') : String(raw ?? '')
    const normalised = slug.trim().replace(/^\/+|\/+$/g, '')

    if (!normalised) {
      return false
    }

    if (normalised.toLowerCase() === 'ecoscore') {
      return false
    }

    const category = typeof route.params.categorySlug === 'string' ? route.params.categorySlug : ''
    const slugSegments = Array.isArray(raw)
      ? raw.filter((segment): segment is string => typeof segment === 'string')
      : normalised
        ? [normalised]
        : []

    const trimmedSegments = [category, ...slugSegments]
      .map(segment => (typeof segment === 'string' ? segment.trim() : ''))
      .filter((segment): segment is string => segment.length > 0)

    return !matchProductRouteFromSegments(trimmedSegments)
  },
})

const route = useRoute()
const { selectCategoryBySlug } = useCategories()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Category not found' })
}

const rawSlugParam = route.params.guideSlug
const slugSegments = Array.isArray(rawSlugParam)
  ? rawSlugParam.filter((segment): segment is string => typeof segment === 'string')
  : typeof rawSlugParam === 'string'
    ? [rawSlugParam]
    : []

const pageId = ref<string | null>(null)
const fallbackTitle = ref<string | null>(null)
const fallbackDescription = ref<string | null>(null)

const slugPath = slugSegments.join('/')
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

const matchedPage =
  categoryDetail.wikiPages?.find(page => normaliseSlug(page.verticalUrl) === normalisedSlug) ?? null

if (!matchedPage) {
  throw createError({ statusCode: 404, statusMessage: 'Guide not found' })
}

const resolvedPageId = matchedPage.wikiUrl?.trim().replace(/^\/+/, '') ?? null

if (!resolvedPageId) {
  throw createError({ statusCode: 404, statusMessage: 'Guide not found' })
}

pageId.value = resolvedPageId
fallbackTitle.value = matchedPage.title ?? null
fallbackDescription.value = categoryDetail.verticalHomeDescription ?? null
</script>

<template>
  <XwikiFullPageRenderer
    v-if="pageId"
    :page-id="pageId"
    :fallback-title="fallbackTitle"
    :fallback-description="fallbackDescription"
  />
</template>

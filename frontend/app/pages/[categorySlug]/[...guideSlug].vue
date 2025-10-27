<script setup lang="ts">
import { computed, ref } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRouter } from 'vue-router'
import XwikiFullPageRenderer from '~/components/cms/XwikiFullPageRenderer.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type { VerticalConfigFullDto } from '~~/shared/api-client'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'

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

    return normalised.toLowerCase() !== 'ecoscore'
  },
})

const route = useRoute()
const router = useRouter()
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

const trimmedSegments = [categorySlug.value, ...slugSegments]
  .map(segment => (typeof segment === 'string' ? segment.trim() : ''))
  .filter((segment): segment is string => segment.length > 0)

const productRouteMatch = matchProductRouteFromSegments(trimmedSegments)

const pageId = ref<string | null>(null)
const fallbackTitle = ref<string | null>(null)
const fallbackDescription = ref<string | null>(null)

if (productRouteMatch) {
  const catchAllRoute = router.getRoutes().find(candidate => candidate.path === '/:slug(.*)*') ?? null

  const redirectTarget: RouteLocationRaw = catchAllRoute?.name
    ? { name: catchAllRoute.name, params: { slug: trimmedSegments } }
    : { path: `/${trimmedSegments.join('/')}` }

  if (import.meta.server) {
    const redirectResult = await navigateTo(redirectTarget, { replace: true })

    if (redirectResult) {
      throw redirectResult
    }
  } else {
    await router.replace(redirectTarget)
  }
} else {
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
}
</script>

<template>
  <XwikiFullPageRenderer
    v-if="pageId"
    :page-id="pageId"
    :fallback-title="fallbackTitle"
    :fallback-description="fallbackDescription"
  />
</template>

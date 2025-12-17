<script setup lang="ts">
import { computed, ref } from 'vue'
import GuideStickySidebar from '~/components/category/guides/GuideStickySidebar.vue'
import XwikiFullPageRenderer from '~/components/cms/XwikiFullPageRenderer.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type {
  BlogPostDto,
  CategoryBreadcrumbItemDto,
  VerticalConfigFullDto,
  WikiPageConfig,
} from '~~/shared/api-client'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'

const normaliseSlug = (value: string | null | undefined) =>
  value
    ?.trim()
    .replace(/^\/+|\/+$/g, '')
    .toLowerCase() ?? ''

definePageMeta({
  path: '/:categorySlug/:guideSlug([A-Za-z][A-Za-z0-9-]*)',
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

    const category =
      typeof route.params.categorySlug === 'string'
        ? route.params.categorySlug
        : ''
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
  ? rawSlugParam.filter(
      (segment): segment is string => typeof segment === 'string'
    )
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
    throw createError({
      statusCode: 404,
      statusMessage: 'Category not found',
      cause: error,
    })
  }

  console.error('Failed to resolve category for wiki guide', error)
  throw createError({
    statusCode: 500,
    statusMessage: 'Failed to load category',
    cause: error,
  })
}

const matchedPage =
  categoryDetail.wikiPages?.find(
    page => normaliseSlug(page.verticalUrl) === normalisedSlug
  ) ?? null

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

const truncateText = (value: string | null | undefined, limit: number) => {
  const source = value?.trim() ?? ''

  if (!source.length) {
    return ''
  }

  if (source.length <= limit) {
    return source
  }

  return `${source.slice(0, limit - 1).trimEnd()}…`
}

const resolvedGuideTitle = computed(() => {
  return (
    fallbackTitle.value?.trim() ||
    matchedPage.title?.trim() ||
    slugSegments.at(-1)?.trim() ||
    normalisedSlug
  )
})

const truncatedGuideTitle = computed(() =>
  truncateText(resolvedGuideTitle.value, 48)
)

const categoryName = computed(() => {
  return (
    categoryDetail.verticalHomeTitle?.trim() ||
    categoryDetail.verticalMetaTitle?.trim() ||
    categoryDetail.breadCrumb?.at(-1)?.title?.trim() ||
    categorySlug.value
  )
})

const categoryPath = computed(() => {
  const raw = categoryDetail.verticalHomeUrl?.trim() ?? ''

  if (!raw.length) {
    return null
  }

  return raw.startsWith('/') ? raw : `/${raw}`
})

const buildGuidePath = (guide: WikiPageConfig | null | undefined) => {
  if (!guide) {
    return null
  }

  const guideSlug = normaliseSlug(guide.verticalUrl)

  if (!guideSlug || guideSlug === normalisedSlug) {
    return null
  }

  return {
    title: guide.title?.trim() || guideSlug.replace(/[-_]/g, ' '),
    to: `/${categorySlug.value}/${guideSlug}`,
  }
}

const otherGuideLinks = computed(() => {
  const guides = categoryDetail.wikiPages ?? []

  const mapped = guides
    .map(buildGuidePath)
    .filter((item): item is { title: string; to: string } => !!item?.to)

  const unique = new Map<string, { title: string; to: string }>()
  mapped.forEach(item => {
    if (!unique.has(item.to)) {
      unique.set(item.to, item)
    }
  })

  return Array.from(unique.values())
})

const relatedPostLinks = computed(() => {
  const posts = categoryDetail.relatedPosts ?? []

  const mapped = posts
    .filter((post): post is BlogPostDto => !!post && typeof post === 'object')
    .map(post => {
      const slug = post.url?.trim().replace(/^\/+/, '') ?? ''

      if (!slug.length) {
        return null
      }

      return {
        title: post.title?.trim() || slug.replace(/[-_]/g, ' '),
        to: `/blog/${slug}`,
      }
    })
    .filter((item): item is { title: string; to: string } => !!item?.to)

  const unique = new Map<string, { title: string; to: string }>()
  mapped.forEach(item => {
    if (!unique.has(item.to)) {
      unique.set(item.to, item)
    }
  })

  return Array.from(unique.values())
})

const shouldDisplaySidebar = computed(() =>
  Boolean(
    categoryPath.value ||
    otherGuideLinks.value.length ||
    relatedPostLinks.value.length
  )
)

const categoryImage = computed(() => {
  const sources = [
    categoryDetail.imageLarge,
    categoryDetail.imageMedium,
    categoryDetail.imageSmall,
  ]

  const resolved = sources
    .map(source => source?.trim())
    .filter((source): source is string => Boolean(source && source.length))
    .at(0)

  return resolved ?? null
})

const heroBreadcrumbs = computed<CategoryBreadcrumbItemDto[]>(() => {
  const base = (categoryDetail.breadCrumb ?? []).map(item => ({ ...item }))
  const guideLeaf = truncatedGuideTitle.value

  if (guideLeaf) {
    return [...base, { title: guideLeaf }]
  }

  return base
})
</script>

<template>
  <XwikiFullPageRenderer
    v-if="pageId"
    :page-id="pageId"
    :fallback-title="fallbackTitle"
    :fallback-description="fallbackDescription"
    :breadcrumbs="heroBreadcrumbs"
    :hero-image="categoryImage"
    layout-variant="wide"
  >
    <template v-if="shouldDisplaySidebar" #sidebar>
      <GuideStickySidebar
        :category-name="categoryName"
        :category-path="categoryPath"
        :guides="otherGuideLinks"
        :posts="relatedPostLinks"
      />
    </template>
  </XwikiFullPageRenderer>
</template>

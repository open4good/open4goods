<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import GuideStickySidebar from '~/components/category/guides/GuideStickySidebar.vue'
import BuyingGuideRenderer from '~/components/docs/BuyingGuideRenderer.vue'
import { useCategories } from '~/composables/categories/useCategories'
import {
  resolveGuideDocPath,
  resolveLocaleFromRequest,
  resolvePublicGuidePath,
  useDocsContent,
  type DocsLocale,
  type DocsDoc,
} from '~/composables/useDocsContent'
import type {
  BlogPostDto,
  CategoryBreadcrumbItemDto,
  VerticalConfigFullDto,
  WikiPageConfig,
} from '~~/shared/api-client'
import { matchProductRouteFromSegments } from '~~/shared/utils/_product-route'

const XwikiFullPageRenderer = defineAsyncComponent(
  () => import('~/components/cms/XwikiFullPageRenderer.vue')
)
const CategoryPage = defineAsyncComponent(
  () => import('~/components/pages/CategoryPage.vue')
)

const normaliseSlug = (value: string | null | undefined) =>
  value
    ?.trim()
    .replace(/^\/+|\/+$/g, '')
    .toLowerCase() ?? ''

definePageMeta({
  path: '/:categorySlug/:guideSlug([A-Za-z][A-Za-z0-9-]*)',
  lazy: true,
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
const requestURL = useRequestURL()
const { selectCategoryBySlug } = useCategories()
const { getDocByPath } = useDocsContent()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({
    statusCode: 404,
    message: 'Category not found',
    fatal: false,
  })
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
  throw createError({
    statusCode: 404,
    message: 'Guide not found',
    fatal: false,
  })
}

let categoryDetail: VerticalConfigFullDto

try {
  categoryDetail = await selectCategoryBySlug(categorySlug.value)
} catch (error) {
  if (error instanceof Error && error.name === 'CategoryNotFoundError') {
    throw createError({
      statusCode: 404,
      message: 'Category not found',
      cause: error,
      fatal: false,
    })
  }

  console.error('Failed to resolve category for wiki guide', error)
  throw createError({
    statusCode: 500,
    message: 'Failed to load category',
    cause: error,
    fatal: true,
  })
}

const matchedSubCategory =
  categoryDetail.subCategories?.find(
    subCategory => normaliseSlug(subCategory.slug) === normalisedSlug
  ) ?? null

const resolveMarkdownGuide = async (): Promise<DocsDoc | null> => {
  const requestLocale = resolveLocaleFromRequest()
  const candidateLocales: DocsLocale[] = [
    requestLocale,
    ...(['fr', 'en'] as DocsLocale[]).filter(
      locale => locale !== requestLocale
    ),
  ]

  for (const locale of candidateLocales) {
    const candidate = await getDocByPath({
      path: resolveGuideDocPath({
        locale,
        categorySlug: categorySlug.value,
        guideSlug: normalisedSlug,
      }),
      locale,
    })

    if (candidate) {
      return candidate as DocsDoc
    }
  }

  return null
}

const markdownGuide = matchedSubCategory ? null : await resolveMarkdownGuide()

const matchedPage =
  categoryDetail.wikiPages?.find(
    page => normaliseSlug(page.verticalUrl) === normalisedSlug
  ) ?? null

if (!matchedSubCategory && !markdownGuide && !matchedPage) {
  throw createError({
    statusCode: 404,
    statusMessage: 'Guide not found',
    fatal: true,
  })
}

const resolvedPageId = matchedPage?.wikiUrl?.trim().replace(/^\/+/, '') ?? null

if (!matchedSubCategory && !markdownGuide && !resolvedPageId) {
  throw createError({
    statusCode: 404,
    statusMessage: 'Guide not found',
    fatal: true,
  })
}

pageId.value = resolvedPageId
fallbackTitle.value = matchedPage?.title ?? null
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
    matchedPage?.title?.trim() ||
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

const markdownBreadcrumbs = computed(() => {
  const base = (categoryDetail.breadCrumb ?? []).map(item => ({
    title: item.title,
    to: item.link,
  }))

  return [
    ...base,
    {
      title: categoryName.value,
      to:
        categoryPath.value ??
        resolvePublicGuidePath({
          categorySlug: categorySlug.value,
          guideSlug: '',
        }),
    },
  ]
})

const guideContext = computed(() => ({
  verticalId: categoryDetail.id?.trim() || null,
  categorySlug: categorySlug.value,
  categoryPath: categoryPath.value ?? `/${categorySlug.value}`,
  categoryTitle: categoryName.value,
  heroImage: categoryImage.value,
}))

const markdownCanonicalPath = computed(() =>
  resolvePublicGuidePath({
    categorySlug: categorySlug.value,
    guideSlug: normalisedSlug,
  })
)

const markdownCanonicalUrl = computed(() => {
  if (!markdownGuide || !markdownCanonicalPath.value) {
    return undefined
  }

  return new URL(markdownCanonicalPath.value, requestURL.origin).toString()
})

if (markdownGuide) {
  useSeoMeta({
    title: () => markdownGuide.title ?? resolvedGuideTitle.value,
    description: () => markdownGuide.description ?? '',
    ogTitle: () => markdownGuide.title ?? resolvedGuideTitle.value,
    ogDescription: () => markdownGuide.description ?? '',
    ogUrl: () => markdownCanonicalUrl.value,
  })

  useHead(() => ({
    link: markdownCanonicalUrl.value
      ? [{ rel: 'canonical', href: markdownCanonicalUrl.value }]
      : [],
  }))
}
</script>

<template>
  <CategoryPage
    v-if="matchedSubCategory"
    :slug="categorySlug"
    :sub-category-slug="normalisedSlug"
  />
  <BuyingGuideRenderer
    v-else-if="markdownGuide"
    :doc="markdownGuide"
    :guide-context="guideContext"
    :breadcrumbs="markdownBreadcrumbs"
  />
  <XwikiFullPageRenderer
    v-else-if="pageId"
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

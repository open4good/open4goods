<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDisplay } from 'vuetify'
import XwikiFullPageRenderer from '~/components/cms/XwikiFullPageRenderer.vue'
import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'
import StickySectionNavigation from '~/components/shared/ui/StickySectionNavigation.vue'
import { useCategories } from '~/composables/categories/useCategories'
import type { CategoryBreadcrumbItemDto, VerticalConfigFullDto, WikiPageConfig } from '~~/shared/api-client'
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
const router = useRouter()
const { selectCategoryBySlug } = useCategories()
const { t } = useI18n()
const { mdAndDown } = useDisplay()

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

const siteName = computed(() => String(t('siteIdentity.siteName')))

const categoryLabel = computed(
  () =>
    categoryDetail.verticalHomeTitle ??
    categoryDetail.verticalMetaTitle ??
    categoryDetail.verticalHomeDescription ??
    siteName.value,
)

const normalisePathSegment = (value: string | null | undefined) =>
  value?.trim().replace(/^\/+|\/+$/g, '') ?? ''

const guideTitle = computed(() => {
  const rawTitle = matchedPage.title?.trim()

  if (rawTitle?.length) {
    return rawTitle
  }

  if (fallbackTitle.value?.trim()?.length) {
    return fallbackTitle.value
  }

  const lastSegment = slugSegments.at(-1)
  return lastSegment ? lastSegment.replace(/[-_]+/g, ' ') : normalisedSlug
})

const MAX_BREADCRUMB_LEAF_LENGTH = 54

const truncatedGuideTitle = computed(() => {
  const title = guideTitle.value
  return title.length > MAX_BREADCRUMB_LEAF_LENGTH
    ? `${title.slice(0, MAX_BREADCRUMB_LEAF_LENGTH - 1)}…`
    : title
})

const breadcrumbBase = computed(() => {
  const rawItems = (categoryDetail.breadCrumb ?? []) as CategoryBreadcrumbItemDto[]

  const mapped = rawItems
    .map((item) => {
      const title = item.title?.trim() ?? ''
      const link = item.link?.trim() ?? ''

      if (!title.length && !link.length) {
        return null
      }

      return {
        title: title.length ? title : link,
        link: link.length ? link : undefined,
      }
    })
    .filter((item): item is { title: string; link?: string } => Boolean(item?.title?.length))

  if (mapped.length) {
    return mapped
  }

  const fallbackTitle = categoryLabel.value?.trim()
  if (fallbackTitle?.length) {
    return [
      {
        title: fallbackTitle,
        link: `/${normalisePathSegment(categorySlug.value)}`,
      },
    ]
  }

  return []
})

const breadcrumbItems = computed(() => {
  if (!truncatedGuideTitle.value?.length) {
    return breadcrumbBase.value
  }

  return [...breadcrumbBase.value, { title: truncatedGuideTitle.value }]
})

const breadcrumbAriaLabel = computed(() => t('category.guidePage.breadcrumbs.ariaLabel'))

const categoryImage = computed(
  () => categoryDetail.imageSmall ?? categoryDetail.imageMedium ?? categoryDetail.imageLarge ?? null,
)

const categoryLink = computed(() => `/${normalisePathSegment(categorySlug.value)}`)

type GuideNavigationItem = {
  id: string
  slug: string
  label: string
}

const navigationItems = computed<GuideNavigationItem[]>(() => {
  const pages = (categoryDetail.wikiPages ?? []) as WikiPageConfig[]
  const seen = new Set<string>()

  return pages
    .map((page) => {
      const slug = normalisePathSegment(page.verticalUrl)
      if (!slug.length) {
        return null
      }

      const id = normaliseSlug(slug)
      if (!id.length || seen.has(id)) {
        return null
      }

      seen.add(id)
      const label = page.title?.trim()?.length ? page.title.trim() : slug.split('/').pop()?.replace(/[-_]+/g, ' ') ?? slug

      return {
        id,
        slug,
        label,
      }
    })
    .filter((item): item is GuideNavigationItem => item !== null)
})

const navigationSections = computed(() =>
  navigationItems.value.map((item) => ({
    id: item.id,
    label: item.label,
    icon: 'mdi-file-document-outline',
  })),
)

const navigationItemsById = computed(() => new Map(navigationItems.value.map((item) => [item.id, item])))

const activeNavigationId = computed(() => normalisedSlug)

const navigationOrientation = computed<'vertical' | 'horizontal'>(() => (mdAndDown.value ? 'horizontal' : 'vertical'))

const navigationAriaLabel = computed(() => t('category.guidePage.navigation.ariaLabel'))

const hasNavigation = computed(() => navigationSections.value.length > 0)

const onNavigateToGuide = (sectionId: string) => {
  if (sectionId === activeNavigationId.value) {
    return
  }

  const target = navigationItemsById.value.get(sectionId)
  if (!target) {
    return
  }

  router.push({ path: `${categoryLink.value}/${target.slug}` })
}

const ctaTitle = computed(() => t('category.guidePage.cta.title', { category: categoryLabel.value }))
const ctaDescription = computed(() =>
  t('category.guidePage.cta.description', { category: categoryLabel.value }),
)
const ctaButtonLabel = computed(() => t('category.guidePage.cta.button', { category: categoryLabel.value }))
const ctaImageAlt = computed(() => t('category.guidePage.cta.imageAlt', { category: categoryLabel.value }))
</script>

<template>
  <div class="category-guide-page" data-test="category-guide-page">
    <v-container fluid class="category-guide-page__container py-8">
      <CategoryNavigationBreadcrumbs
        v-if="breadcrumbItems.length"
        class="category-guide-page__breadcrumbs"
        :items="breadcrumbItems"
        :aria-label="breadcrumbAriaLabel"
      />

      <div class="category-guide-page__layout">
        <aside
          class="category-guide-page__sidebar"
          :class="{ 'category-guide-page__sidebar--mobile': navigationOrientation === 'horizontal' }"
        >
          <v-card
            class="category-guide-page__cta"
            data-test="guide-cta"
            elevation="2"
            rounded="xl"
            variant="elevated"
          >
            <v-img
              v-if="categoryImage"
              :src="categoryImage"
              :alt="ctaImageAlt"
              class="category-guide-page__cta-image"
              cover
            />

            <div class="category-guide-page__cta-content">
              <h2 class="category-guide-page__cta-title">{{ ctaTitle }}</h2>
              <p class="category-guide-page__cta-description">{{ ctaDescription }}</p>
              <v-btn
                class="category-guide-page__cta-button"
                color="primary"
                data-test="guide-cta-button"
                size="large"
                variant="flat"
                :to="categoryLink"
                :data-to="categoryLink"
              >
                <v-icon class="me-2" icon="mdi-arrow-left" size="18" />
                {{ ctaButtonLabel }}
              </v-btn>
            </div>
          </v-card>

          <div v-if="hasNavigation" class="category-guide-page__navigation">
            <div class="category-guide-page__navigation-header">
              <v-icon icon="mdi-book-open-page-variant" size="20" />
              <span>{{ t('category.guidePage.navigation.title') }}</span>
            </div>
            <StickySectionNavigation
              data-test="guide-navigation"
              :sections="navigationSections"
              :active-section="activeNavigationId"
              :orientation="navigationOrientation"
              :aria-label="navigationAriaLabel"
              @navigate="onNavigateToGuide"
            />
          </div>
        </aside>

        <main class="category-guide-page__content" role="main">
          <XwikiFullPageRenderer
            v-if="pageId"
            :page-id="pageId"
            :fallback-title="fallbackTitle"
            :fallback-description="fallbackDescription"
          />
        </main>
      </div>
    </v-container>
  </div>
</template>

<style scoped lang="sass">
.category-guide-page
  display: block

  &__container
    max-width: 1200px

  &__breadcrumbs
    margin-bottom: 1.5rem

  &__layout
    display: grid
    grid-template-columns: minmax(0, 1fr)
    gap: 2rem

  &__sidebar
    display: flex
    flex-direction: column
    gap: 1.5rem

  &__sidebar--mobile
    position: static

  &__cta
    display: flex
    flex-direction: column
    gap: 1rem
    overflow: hidden
    background: rgba(var(--v-theme-surface-glass))

  &__cta-image
    height: 160px
    object-fit: cover

  &__cta-content
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: 1.25rem

  &__cta-title
    margin: 0
    font-size: 1.25rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__cta-description
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    line-height: 1.5

  &__cta-button
    align-self: flex-start

  &__navigation
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__navigation-header
    display: inline-flex
    align-items: center
    gap: 0.5rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__content
    min-width: 0

    :deep(.xwiki-page)
      background: transparent

  @media (min-width: 960px)
    &__layout
      grid-template-columns: minmax(220px, 280px) minmax(0, 1fr)

  @media (max-width: 959px)
    &__layout
      grid-template-columns: minmax(0, 1fr)

    &__sidebar
      order: -1

    &__cta-button
      width: 100%

    &__navigation-header
      justify-content: center
      text-align: center

    :deep(.sticky-section-navigation)
      width: 100%

    :deep(.sticky-section-navigation--horizontal)
      border-radius: 12px

    :deep(.sticky-section-navigation__list)
      justify-content: flex-start
</style>

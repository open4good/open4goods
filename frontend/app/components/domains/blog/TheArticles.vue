<script setup lang="ts">
import { useRoute, useRouter } from '#app'
import { useHead, useRequestURL, useSeoMeta } from '#imports'
import { useI18n } from 'vue-i18n'
import type { BlogTagDto } from '~~/shared/api-client'

import { useBlog } from '~/composables/blog/useBlog'
const {
  paginatedArticles,
  loading,
  error,
  pagination,
  fetchArticles,
  tags,
  selectedTag,
  fetchTags,
} = useBlog()

const buildDateIsoString = (timestamp: number) => {
  const date = new Date(timestamp)

  return Number.isNaN(date.getTime()) ? '' : date.toISOString()
}

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const currentPage = computed(() => pagination.value.page || 1)
const tagsLoading = ref(false)
const articleListId = 'blog-articles-list'

const parsePageQuery = (rawPage: unknown) => {
  const value = Array.isArray(rawPage) ? rawPage[0] : rawPage
  const parsed = Number.parseInt(String(value ?? ''), 10)

  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
}

const parseTagQuery = (rawTag: unknown) => {
  const value = Array.isArray(rawTag) ? rawTag[0] : rawTag

  if (typeof value !== 'string') {
    return null
  }

  const trimmed = value.trim()

  return trimmed.length > 0 ? trimmed : null
}

const totalPages = computed(() => pagination.value.totalPages || 0)
const totalElements = computed(() => pagination.value.totalElements || 0)
const shouldDisplayPagination = computed(() => totalPages.value > 1)
const paginationInfoMessage = computed(() =>
  t('blog.pagination.info', {
    current: currentPage.value,
    total: totalPages.value,
    count: totalElements.value,
  })
)
const paginationAriaLabelKey = 'blog.pagination.ariaLabel'
const paginationAriaLabel = computed(() => t(paginationAriaLabelKey))
const pageLinkLabel = (pageNumber: number) =>
  t('blog.pagination.pageLink', { page: pageNumber })
const buildArticleTitleId = (index: number) =>
  `blog-article-card-title-${index}`
const buildArticleSummaryId = (index: number) =>
  `blog-article-card-summary-${index}`
const buildArticleImageAlt = (title?: string | null) => {
  const sanitized = title?.trim()

  return sanitized && sanitized.length > 0
    ? sanitized
    : 'Blog article illustration'
}

const extractArticleSlug = (rawSlug: string | null | undefined) => {
  if (!rawSlug) {
    return null
  }

  const trimmed = rawSlug.trim()

  if (!trimmed) {
    return null
  }

  const withoutDomain = trimmed.replace(/^https?:\/\/[^/]+/i, '')
  const sanitized = withoutDomain.replace(/^\/*/, '')
  const segments = sanitized.split('/').filter(Boolean)

  return segments.at(-1) ?? null
}

const buildArticleLink = (
  slug: string | null | undefined
): string | undefined => {
  const normalizedSlug = extractArticleSlug(slug)

  if (!normalizedSlug) {
    return undefined
  }

  return `/blog/${normalizedSlug}`
}

const getArticleLink = (slug: string | null | undefined) =>
  buildArticleLink(slug)
const hasArticleLink = (slug: string | null | undefined) =>
  Boolean(getArticleLink(slug))

const navigateToArticle = (slug: string | null | undefined) => {
  const link = getArticleLink(slug)

  if (!link) {
    return
  }

  router.push(link)
}

const loadArticlesFromRoute = async () => {
  const targetPage = parsePageQuery(route.query.page)
  const targetTag = parseTagQuery(route.query.tag)

  await fetchArticles(targetPage, pagination.value.size, targetTag)
}

const ensureTagsLoaded = async () => {
  if (tags.value.length > 0) {
    return
  }

  tagsLoading.value = true
  try {
    await fetchTags()
  } finally {
    tagsLoading.value = false
  }
}

watch(
  () => [route.query.page, route.query.tag],
  async (_, __, onCleanup) => {
    let cancelled = false
    onCleanup(() => {
      cancelled = true
    })

    await ensureTagsLoaded()

    if (cancelled) {
      return
    }

    await loadArticlesFromRoute()
  }
)

const buildPageQuery = (pageNumber: number) => {
  const sanitizedPage = Math.max(1, Math.trunc(pageNumber))
  const nextQuery = { ...route.query }

  if (sanitizedPage === 1) {
    delete nextQuery.page
  } else {
    nextQuery.page = sanitizedPage.toString()
  }

  return nextQuery
}

const handlePageChange = async (page: number) => {
  const sanitizedPage = Math.max(1, Math.trunc(page))
  const currentQuery = buildPageQuery(sanitizedPage)

  await router.push({ query: currentQuery })
}

const buildTagQuery = (tag: string | null) => {
  const nextQuery = { ...route.query }

  if (!tag) {
    delete nextQuery.tag
  } else {
    nextQuery.tag = tag
  }

  delete nextQuery.page

  return nextQuery
}

const handleTagSelection = async (tag: string | null) => {
  const nextQuery = buildTagQuery(tag)
  await router.push({ path: '/blog', query: nextQuery })
}

type NamedTag = BlogTagDto & { name: string }

const availableTags = computed<NamedTag[]>(() =>
  tags.value
    .map(tag => ({
      ...tag,
      name: (tag.name ?? '').trim(),
    }))
    .filter((tag): tag is NamedTag => Boolean(tag.name))
)
const activeTag = computed(() => selectedTag.value)

const allTagValue = '__all__'
const tagGroupValue = computed({
  get: () => activeTag.value ?? allTagValue,
  set: value => {
    if (typeof value !== 'string') {
      return
    }

    handleTagSelection(value === allTagValue ? null : value)
  },
})

const seoPageLinks = computed(() => {
  const pages = totalPages.value

  if (pages <= 1) {
    return [1]
  }

  return Array.from({ length: pages }, (_, index) => index + 1)
})

const visibleArticles = paginatedArticles
const sanitizedTag = computed(() => {
  const tag = activeTag.value

  if (!tag) {
    return null
  }

  const trimmed = tag.trim()

  return trimmed.length > 0 ? trimmed : null
})

const baseSeoTitle = computed(() => t('blog.seo.baseTitle'))
const tagSeoTitle = computed(() =>
  sanitizedTag.value
    ? t('blog.seo.tagTitle', { tag: sanitizedTag.value })
    : baseSeoTitle.value
)
const pageSeoTitle = computed(() =>
  currentPage.value > 1
    ? t('blog.seo.pageTitle', {
        title: tagSeoTitle.value,
        page: currentPage.value,
      })
    : tagSeoTitle.value
)

const truncateToLength = (value: string, maxLength = 160) => {
  if (value.length <= maxLength) {
    return value
  }

  return `${value.slice(0, maxLength - 3).trimEnd()}...`
}

const baseSeoDescription = computed(() => t('blog.seo.description'))
const tagSeoDescription = computed(() =>
  sanitizedTag.value
    ? t('blog.seo.tagDescription', { tag: sanitizedTag.value })
    : baseSeoDescription.value
)
const articleSummaries = computed(() =>
  visibleArticles.value
    .map(article => (article.summary ?? '').trim())
    .filter(summary => summary.length > 0)
)
const seoDescription = computed(() => {
  const base = tagSeoDescription.value
  const [firstSummary] = articleSummaries.value

  if (!firstSummary) {
    return truncateToLength(base)
  }

  return truncateToLength(`${base} ${firstSummary}`)
})

const primaryArticleImage = computed(
  () =>
    visibleArticles.value.find(article => Boolean(article.image))?.image ?? null
)

let requestUrl: URL | undefined
try {
  requestUrl = useRequestURL()
} catch {
  requestUrl = undefined
}

const canonicalUrl = computed(() => {
  if (!requestUrl) {
    return undefined
  }

  const origin = requestUrl.origin || undefined
  const base = origin ? new URL('/blog', origin) : new URL('/blog', requestUrl)
  const params = new URLSearchParams()

  if (sanitizedTag.value) {
    params.set('tag', sanitizedTag.value)
  }

  if (currentPage.value > 1) {
    params.set('page', currentPage.value.toString())
  }

  params.sort()
  const query = params.toString()
  base.search = query
  base.hash = ''

  return base.toString()
})

const dateFormatter = computed(
  () =>
    new Intl.DateTimeFormat(locale.value || 'en', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      timeZone: 'UTC',
    })
)

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return dateFormatter.value.format(date)
}

const buildAbsoluteArticleLink = (slug: string | null | undefined) => {
  const relative = buildArticleLink(slug)

  if (!relative) {
    return undefined
  }

  if (!requestUrl) {
    return relative
  }

  try {
    const origin = requestUrl.origin || requestUrl.toString()
    return new URL(relative, origin).toString()
  } catch {
    return relative
  }
}

const structuredData = computed(() => {
  const articles = visibleArticles.value
    .map(article => {
      const entry: Record<string, unknown> = {
        '@type': 'BlogPosting',
      }

      const headline = article.title?.trim()
      if (headline) {
        entry.headline = headline
      }

      const description = article.summary?.trim()
      if (description) {
        entry.description = description
      }

      if (article.image) {
        entry.image = [article.image]
      }

      const url = buildAbsoluteArticleLink(article.url)
      if (url) {
        entry.url = url
      }

      if (article.createdMs) {
        const published = buildDateIsoString(article.createdMs)
        if (published) {
          entry.datePublished = published
        }
      }

      return entry
    })
    .filter(entry => Object.keys(entry).length > 1)

  const schema: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'CollectionPage',
    name: pageSeoTitle.value,
    description: seoDescription.value || undefined,
    url: canonicalUrl.value,
    inLanguage: locale.value,
    isPartOf: {
      '@type': 'Blog',
      name: baseSeoTitle.value,
    },
  }

  if (sanitizedTag.value) {
    schema.about = sanitizedTag.value
  }

  if (articles.length > 0) {
    schema.hasPart = articles
  }

  return schema
})

useSeoMeta({
  title: pageSeoTitle,
  ogTitle: pageSeoTitle,
  description: computed(() => seoDescription.value || undefined),
  ogDescription: computed(() => seoDescription.value || undefined),
  ogType: 'website',
  ogUrl: canonicalUrl,
  ogImage: computed(() => primaryArticleImage.value || undefined),
})

useHead(() => ({
  link: canonicalUrl.value
    ? [
        {
          rel: 'canonical',
          href: canonicalUrl.value,
        },
      ]
    : [],
  script: [
    {
      type: 'application/ld+json',
      children: JSON.stringify(structuredData.value),
    },
  ],
}))

defineExpose({
  pageSeoTitle,
  seoDescription,
  canonicalUrl,
  primaryArticleImage,
  structuredData,
})

await useAsyncData('blog-ssr-init', async () => {
  await Promise.all([ensureTagsLoaded(), loadArticlesFromRoute()])
  return true
})
</script>

<template>
  <section class="blog-hero" aria-labelledby="blog-hero-heading">
    <v-container class="py-0 px-4 mx-auto blog-hero__container" max-width="xl">
      <div class="blog-hero__content">
        <p class="blog-hero__eyebrow">{{ t('blog.hero.eyebrow') }}</p>
        <div class="blog-hero__copy">
          <h1 id="blog-hero-heading" class="blog-hero__title">
            {{ t('blog.hero.title') }}
          </h1>
          <p class="blog-hero__subtitle">{{ t('blog.hero.subtitle') }}</p>
        </div>
      </div>
    </v-container>
  </section>

  <v-container class="py-6 px-4 mx-auto" max-width="xl">
    <v-sheet
      v-if="availableTags.length || activeTag"
      class="mb-6 d-flex flex-column gap-3 pa-4"
      color="primary-lighten-5"
      rounded="lg"
      elevation="0"
      :aria-label="t('blog.list.tagsAriaLabel')"
      role="region"
      :aria-busy="tagsLoading"
    >
      <div class="d-flex align-center gap-2 text-primary font-weight-medium">
        <v-icon
          icon="mdi-tag-multiple"
          size="small"
          color="primary"
          aria-hidden="true"
        />
        <span class="text-subtitle-1">{{ t('blog.list.tagsTitle') }}</span>
      </div>

      <v-chip-group
        v-model="tagGroupValue"
        class="d-flex flex-wrap"
        :column="true"
        :filter="false"
        :multiple="false"
        :disabled="tagsLoading"
        :aria-label="t('blog.list.tagsAriaLabel')"
        :aria-controls="articleListId"
      >
        <v-chip
          :value="allTagValue"
          size="small"
          color="primary"
          variant="tonal"
          class="me-2 mb-2"
          :aria-label="t('blog.list.tagsAll')"
        >
          {{ t('blog.list.tagsAll') }}
        </v-chip>
        <v-chip
          v-for="tag in availableTags"
          :key="tag.name"
          :value="tag.name"
          size="small"
          color="primary"
          variant="tonal"
          class="me-2 mb-2"
          :aria-label="tag.name"
        >
          <span v-if="typeof tag.count === 'number' && tag.count > 0">
            {{
              t('blog.list.tagWithCount', { tag: tag.name, count: tag.count })
            }}
          </span>
          <span v-else>
            {{ tag.name }}
          </span>
        </v-chip>
      </v-chip-group>

      <div
        v-if="tagsLoading"
        class="d-inline-flex align-center"
        role="status"
        aria-live="polite"
      >
        <v-progress-circular
          indeterminate
          size="16"
          width="2"
          color="primary"
          aria-hidden="true"
        />
        <span class="ms-2 text-body-2 text-primary">{{
          t('blog.list.tagsLoading')
        }}</span>
      </div>
    </v-sheet>

    <v-sheet
      v-if="loading"
      class="py-10 d-flex flex-column align-center justify-center text-center gap-3"
      color="transparent"
      elevation="0"
      role="status"
      aria-live="polite"
    >
      <v-progress-circular indeterminate aria-hidden="true" />
      <p class="text-body-1 text-medium-emphasis">
        {{ t('blog.list.loading') }}
      </p>
    </v-sheet>

    <v-sheet
      v-else-if="error"
      class="py-6 d-flex flex-column align-center text-center gap-3"
      color="transparent"
      elevation="0"
    >
      <v-alert type="error" variant="tonal" role="alert">
        {{ error }}
      </v-alert>
      <v-btn color="primary" variant="tonal" @click="fetchArticles">
        {{ t('common.actions.retry') }}
      </v-btn>
    </v-sheet>

    <v-sheet
      v-else
      color="transparent"
      elevation="0"
      role="region"
      aria-live="polite"
      :aria-busy="loading"
    >
      <v-row :id="articleListId" role="list">
        <v-col
          v-for="(article, index) in visibleArticles"
          :key="article.url ?? index"
          cols="12"
          sm="6"
          md="4"
          role="listitem"
        >
          <v-card
            class="h-100 d-flex flex-column"
            elevation="6"
            hover
            rounded="lg"
            tag="article"
            :link="Boolean(getArticleLink(article.url))"
            :to="getArticleLink(article.url) || undefined"
            :aria-labelledby="buildArticleTitleId(index)"
            :aria-describedby="
              article.summary ? buildArticleSummaryId(index) : undefined
            "
          >
            <div
              v-if="article.image"
              class="d-block"
              data-test="article-image-link"
            >
              <v-img
                :src="article.image"
                :alt="buildArticleImageAlt(article.title)"
                height="200"
                cover
                :eager="index === 0"
              >
                <template #placeholder>…</template>
              </v-img>
            </div>

            <v-card-title
              :id="buildArticleTitleId(index)"
              class="text-h6 font-weight-semibold text-high-emphasis px-4 pt-4 pb-2"
            >
              <span class="text-high-emphasis" data-test="article-title-link">
                {{ article.title }}
              </span>
            </v-card-title>

            <v-card-text
              :id="buildArticleSummaryId(index)"
              class="px-4 pb-4 flex-grow-1"
            >
              <p class="text-body-2 text-medium-emphasis mb-0">
                {{ article.summary }}
              </p>
            </v-card-text>

            <v-card-actions class="px-4 py-4 align-center blog-articles__meta">
              <div
                class="d-flex flex-column gap-2"
                aria-label="Article metadata"
              >
                <div
                  v-if="article.author"
                  class="d-flex align-center text-body-2"
                >
                  <v-icon
                    size="small"
                    color="primary"
                    class="me-2"
                    aria-hidden="true"
                    >mdi-account</v-icon
                  >
                  <span class="text-primary font-weight-medium">{{
                    article.author
                  }}</span>
                </div>
                <div
                  v-if="article.createdMs"
                  class="d-flex align-center text-body-2"
                >
                  <v-icon
                    size="small"
                    color="grey"
                    class="me-2"
                    aria-hidden="true"
                    >mdi-calendar</v-icon
                  >
                  <time
                    class="text-medium-emphasis"
                    :datetime="buildDateIsoString(article.createdMs)"
                  >
                    {{ formatDate(article.createdMs) }}
                  </time>
                </div>
              </div>

              <v-spacer></v-spacer>

              <v-btn
                variant="outlined"
                size="small"
                color="primary"
                type="button"
                :disabled="!hasArticleLink(article.url)"
                data-test="article-read-more"
                :aria-label="`${t('blog.list.readMore')} - ${article.title || t('blog.list.readMore')}`"
                @click.stop="navigateToArticle(article.url)"
              >
                {{ t('blog.list.readMore') }}
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>

      <v-sheet
        v-if="shouldDisplayPagination"
        class="mt-6 d-flex flex-column align-center gap-3"
        color="transparent"
        elevation="0"
      >
        <nav :aria-label="paginationAriaLabel">
          <v-pagination
            :length="totalPages"
            :model-value="currentPage"
            :total-visible="5"
            :aria-label="paginationAriaLabelKey"
            :aria-controls="articleListId"
            @update:model-value="handlePageChange"
          />
        </nav>

        <p
          class="text-body-2 text-medium-emphasis text-center"
          aria-live="polite"
        >
          {{ paginationInfoMessage }}
        </p>

        <nav class="d-sr-only" :aria-label="paginationAriaLabel">
          <ul>
            <li v-for="pageNumber in seoPageLinks" :key="pageNumber">
              <NuxtLink :to="{ query: buildPageQuery(pageNumber) }">
                {{ pageLinkLabel(pageNumber) }}
              </NuxtLink>
            </li>
          </ul>
        </nav>
      </v-sheet>
    </v-sheet>
  </v-container>
</template>

<style scoped lang="sass">
.blog-hero
  position: relative
  width: 100%
  background: linear-gradient(
    135deg,
    rgb(var(--v-theme-hero-gradient-start)) 0%,
    rgba(var(--v-theme-hero-gradient-mid), 0.92) 55%,
    rgb(var(--v-theme-hero-gradient-end)) 100%
  )
  color: rgb(255, 255, 255)
  padding-block: clamp(1.5rem, 3.5vw, 2.5rem)
  margin-bottom: clamp(1.5rem, 4vw, 2.5rem)
  overflow: hidden
  box-shadow: 0 18px 40px -24px rgba(var(--v-theme-shadow-primary-600), 0.45)

  &::after
    content: ''
    position: absolute
    inset: 0
    background: radial-gradient(circle at 20% 20%, rgba(var(--v-theme-hero-overlay-strong), 0.18), transparent 55%)
    pointer-events: none

  &__container
    position: relative
    z-index: 1
    padding-block: clamp(1rem, 3vw, 1.75rem)

  &__content
    display: flex
    flex-direction: column
    gap: 0.45rem
    max-width: min(48rem, 100%)

  &__eyebrow
    display: inline-flex
    align-items: center
    align-self: flex-start
    padding: 0.35rem 0.9rem
    border-radius: 999px
    background-color: rgba(var(--v-theme-hero-pill-on-dark), 0.16)
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    font-size: 0.75rem
    line-height: 1.1

  &__copy
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__title
    font-weight: 700
    font-size: clamp(1.85rem, 4vw, 2.45rem)
    line-height: 1.2
    margin: 0

  &__subtitle
    margin: 0
    font-size: clamp(1rem, 2.2vw, 1.15rem)
    line-height: 1.55
    color: rgba(var(--v-theme-hero-overlay-soft), 0.84)

.blog-articles__meta
  background-color: rgb(var(--v-theme-surface-muted-contrast))
  transition: background-color 0.2s ease
</style>

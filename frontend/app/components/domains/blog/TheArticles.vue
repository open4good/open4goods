<script setup lang="ts">
import { useRoute, useRouter } from '#app'
import { useI18n } from 'vue-i18n'
import type { BlogTagDto } from '~~/shared/api-client'

import { useBlog } from '~/composables/blog/useBlog'
const {
  articles: currentPageArticles,
  paginatedArticles,
  loading,
  error,
  pagination,
  fetchArticles,
  tags,
  selectedTag,
  fetchTags,
} = useBlog()

// Format date helper
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleDateString()
}

// Debug mode - use environment variable or default to false
const debugMode = ref(false)
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const currentPage = ref(1)
const tagsLoading = ref(false)

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

watch(
  () => pagination.value.page,
  (page) => {
    currentPage.value = page || 1
  },
  { immediate: true }
)

const totalPages = computed(() => pagination.value.totalPages || 0)
const totalElements = computed(() => pagination.value.totalElements || 0)
const displayedArticlesCount = computed(() => paginatedArticles.value.length)
const shouldDisplayPagination = computed(() => totalPages.value > 1)
const paginationInfoMessage = computed(() =>
  t('blog.pagination.info', {
    current: currentPage.value,
    total: totalPages.value,
    count: totalElements.value,
  }),
)
const paginationAriaLabel = computed(() => t('blog.pagination.ariaLabel'))
const pageLinkLabel = (pageNumber: number) =>
  t('blog.pagination.pageLink', { page: pageNumber })

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

const buildArticleLink = (slug: string | null | undefined): string | undefined => {
  const normalizedSlug = extractArticleSlug(slug)

  if (!normalizedSlug) {
    return undefined
  }

  return `/blog/${normalizedSlug}`
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

await Promise.all([ensureTagsLoaded(), loadArticlesFromRoute()])

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
  },
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
    .map((tag) => ({
      ...tag,
      name: (tag.name ?? '').trim(),
    }))
    .filter((tag): tag is NamedTag => Boolean(tag.name))
)
const activeTag = computed(() => selectedTag.value)
const isTagActive = (tag: string | null) => {
  return (activeTag.value ?? null) === (tag ?? null)
}

const seoPageLinks = computed(() => {
  const pages = totalPages.value

  if (pages <= 1) {
    return [1]
  }

  return Array.from({ length: pages }, (_, index) => index + 1)
})
</script>

<template>
  <div class="blog-list">
    <!-- Debug toggle -->
    <div class="debug-toggle">
      <v-btn size="small" variant="outlined" @click="debugMode = !debugMode">
        {{ debugMode ? 'Hide' : 'Show' }} Debug Info
      </v-btn>
    </div>

    <!-- Debug information -->
    <div v-if="debugMode" class="debug-info">
      <h4>Debug Information:</h4>
      <p>Total articles reported by API: {{ totalElements }}</p>
      <p>Articles loaded for current page: {{ currentPageArticles.length }}</p>
      <p>Articles currently displayed: {{ displayedArticlesCount }}</p>
      <div
        v-for="(article, index) in paginatedArticles"
        :key="index"
        class="debug-article"
      >
        <h5>Article {{ index + 1 }}:</h5>
        <pre>{{ JSON.stringify(article, null, 2) }}</pre>
      </div>
    </div>

    <section
      v-if="availableTags.length || activeTag"
      class="tag-filter"
      :aria-label="t('blog.list.tagsAriaLabel')"
    >
      <div class="tag-filter__header">
        <v-icon icon="mdi-tag-multiple" size="small" color="primary" aria-hidden="true" />
        <span class="tag-filter__title">{{ t('blog.list.tagsTitle') }}</span>
      </div>
      <div class="tag-filter__chips">
        <button
          type="button"
          class="tag-filter__chip"
          :class="{ 'tag-filter__chip--active': isTagActive(null) }"
          :disabled="tagsLoading"
          @click="handleTagSelection(null)"
        >
          <v-chip
            color="primary"
            :variant="isTagActive(null) ? 'elevated' : 'tonal'"
            size="small"
            :disabled="tagsLoading"
          >
            {{ t('blog.list.tagsAll') }}
          </v-chip>
        </button>
        <button
          v-for="tag in availableTags"
          :key="tag.name"
          type="button"
          class="tag-filter__chip"
          :class="{ 'tag-filter__chip--active': isTagActive(tag.name) }"
          :disabled="tagsLoading"
          @click="handleTagSelection(tag.name)"
        >
          <v-chip
            color="primary"
            :variant="isTagActive(tag.name) ? 'elevated' : 'tonal'"
            size="small"
            :disabled="tagsLoading"
          >
            <span v-if="typeof tag.count === 'number' && tag.count > 0">
              {{ t('blog.list.tagWithCount', { tag: tag.name, count: tag.count }) }}
            </span>
            <span v-else>
              {{ tag.name }}
            </span>
          </v-chip>
        </button>
      </div>
      <div v-if="tagsLoading" class="tag-filter__loading">
        <v-progress-circular indeterminate size="16" width="2" color="primary" />
        <span>{{ t('blog.list.tagsLoading') }}</span>
      </div>
    </section>

    <div v-if="loading" class="loading">
      <v-progress-circular indeterminate />
      <p>{{ t('blog.list.loading') }}</p>
    </div>

    <div v-else-if="error" class="error">
      <v-alert type="error" variant="tonal">
        {{ error }}
      </v-alert>
      <v-btn class="mt-4" @click="fetchArticles"> {{ t('common.actions.retry') }} </v-btn>
    </div>

    <div v-else class="articles">
      <v-row>
        <v-col
          v-for="article in paginatedArticles"
          :key="article.url"
          cols="12"
          sm="6"
          md="4"
          lg="4"
        >
          <v-card class="article-card" elevation="6" hover>
            <!-- Image de l'article -->
            <NuxtLink
              v-if="article.image && buildArticleLink(article.url)"
              :to="buildArticleLink(article.url)"
              class="article-image-link"
              :aria-label="article.title || undefined"
              :title="article.title || undefined"
              data-test="article-image-link"
            >
              <v-img
                :src="article.image"
                :alt="article.title"
                height="200"
                cover
                class="article-image"
              >
                <template #placeholder>
                  <div class="image-placeholder">
                    <v-icon size="48" color="grey-lighten-1">
                      mdi-image-off
                    </v-icon>
                    <p class="placeholder-text">Image non disponible</p>
                  </div>
                </template>
              </v-img>
            </NuxtLink>
            <v-img
              v-else-if="article.image"
              :src="article.image"
              :alt="article.title"
              height="200"
              cover
              class="article-image"
            >
              <template #placeholder>
                <div class="image-placeholder">
                  <v-icon size="48" color="grey-lighten-1">
                    mdi-image-off
                  </v-icon>
                  <p class="placeholder-text">Image non disponible</p>
                </div>
              </template>
            </v-img>

            <!-- Contenu de la carte -->
            <v-card-title class="article-title">
              <NuxtLink
                v-if="buildArticleLink(article.url)"
                :to="buildArticleLink(article.url)"
                class="article-title-link"
                :title="article.title || undefined"
                data-test="article-title-link"
              >
                {{ article.title }}
              </NuxtLink>
              <span v-else>
                {{ article.title }}
              </span>
            </v-card-title>

            <v-card-text class="article-summary">
              <p>{{ article.summary }}</p>
            </v-card-text>

            <!-- Actions et métadonnées -->
            <v-card-actions class="article-actions">
              <div class="article-meta">
                <div class="author-info">
                  <v-icon size="small" color="primary" class="mr-1">
                    mdi-account
                  </v-icon>
                  <span class="author-name">{{ article.author }}</span>
                </div>
                <div class="date-info">
                  <v-icon size="small" color="grey" class="mr-1">
                    mdi-calendar
                  </v-icon>
                  <span class="date-text">{{
                    formatDate(article.createdMs ?? 0)
                  }}</span>
                </div>
              </div>

              <v-spacer></v-spacer>

              <NuxtLink
                v-if="buildArticleLink(article.url)"
                :to="buildArticleLink(article.url)"
                class="article-read-more-link"
                :title="article.title || undefined"
                data-test="article-read-more"
              >
                <v-btn variant="outlined" size="small" color="primary">
                  {{ t('blog.list.readMore') }}
                </v-btn>
              </NuxtLink>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>

      <div v-if="shouldDisplayPagination" class="pagination-container">
        <v-pagination
          :length="totalPages"
          :model-value="currentPage"
          :total-visible="5"
          @update:model-value="handlePageChange"
        />

        <p class="pagination-info">
          {{ paginationInfoMessage }}
        </p>

        <nav class="visually-hidden" :aria-label="paginationAriaLabel">
          <ul>
            <li v-for="pageNumber in seoPageLinks" :key="pageNumber">
              <NuxtLink :to="{ query: buildPageQuery(pageNumber) }">
                {{ pageLinkLabel(pageNumber) }}
              </NuxtLink>
            </li>
          </ul>
        </nav>
      </div>
    </div>
  </div>
</template>

<style lang="sass" scoped>
.blog-list
  max-width: 1200px
  margin: 0 auto
  padding: 20px

.debug-toggle
  margin-bottom: 20px
  text-align: center

.debug-info
  background: #f5f5f5
  border: 1px solid #ddd
  border-radius: 8px
  padding: 16px
  margin-bottom: 20px
  font-family: monospace
  font-size: 12px

.tag-filter
  margin-bottom: 24px
  padding: 16px
  border-radius: 12px
  background: #f5f9ff
  border: 1px solid rgba(25, 118, 210, 0.12)
  display: flex
  flex-direction: column
  gap: 0.75rem

  &__header
    display: flex
    align-items: center
    gap: 0.5rem
    color: #1976d2
    font-weight: 600

  &__title
    font-size: 1rem

  &__chips
    display: flex
    flex-wrap: wrap
    gap: 0.5rem

  &__chip
    appearance: none
    border: none
    background: transparent
    padding: 0
    display: inline-flex
    transition: transform 0.2s ease
    cursor: pointer

    &:disabled
      cursor: not-allowed

  &__chip--active
    transform: translateY(-2px)

  &__loading
    display: inline-flex
    align-items: center
    gap: 0.5rem
    font-size: 0.875rem
    color: #1976d2

.debug-article
  margin-bottom: 16px
  padding: 8px
  background: white
  border-radius: 4px

  h5
    margin: 0 0 8px 0
    color: #333

  pre
    margin: 0
    white-space: pre-wrap
    word-break: break-all

.loading,
.error
  text-align: center
  padding: 20px

.article-card
  height: 100%
  transition: all 0.3s ease
  border-radius: 12px
  overflow: hidden

  &:hover
    transform: translateY(-4px)
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15)

.article-image
  position: relative

.article-image-link
  display: block

.article-title-link
  color: inherit
  text-decoration: none
  display: inline-block
  width: 100%

.article-title-link:hover
  text-decoration: underline

.article-read-more-link
  text-decoration: none
  display: inline-block

.image-placeholder
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  height: 100%
  background: #f5f5f5
  color: #666

.placeholder-text
  margin-top: 8px
  font-size: 12px
  text-align: center

.article-title
  font-size: 1.1rem
  font-weight: 600
  line-height: 1.3
  color: #333
  padding: 16px 16px 8px 16px

.article-summary
  padding: 0 16px 16px 16px

  p
    color: #666
    line-height: 1.5
    margin: 0
    overflow: hidden
    text-overflow: ellipsis

.article-actions
  padding: 16px
  border-top: 1px solid #f0f0f0
  background: #fafafa

.article-meta
  display: flex
  flex-direction: column
  gap: 4px

.author-info,
.date-info
  display: flex
  align-items: center
  font-size: 0.85rem

.author-name
  color: #1976d2
  font-weight: 500

.date-text
  color: #666

.pagination-container
  margin-top: 24px
  display: flex
  flex-direction: column
  align-items: center
  gap: 8px

.pagination-info
  font-size: 0.9rem
  color: #555

.visually-hidden
  position: absolute
  width: 1px
  height: 1px
  padding: 0
  margin: -1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)
  white-space: nowrap
  border: 0

// Responsive adjustments
@media (max-width: 600px)
  .article-card
    margin-bottom: 16px

  .article-title
    font-size: 1rem
</style>

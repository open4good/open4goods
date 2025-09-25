<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { normalizeLocale, resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

import { useBlog } from '~/composables/blog/useBlog'
const {
  articles: currentPageArticles,
  paginatedArticles,
  loading,
  error,
  pagination,
  fetchArticles,
  changePage,
} = useBlog()

// Format date helper
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleDateString()
}

// Debug mode - use environment variable or default to false
const debugMode = ref(false)
const { locale, t } = useI18n()
const currentLocale = computed(() => normalizeLocale(locale.value))
const route = useRoute()
const router = useRouter()
const currentPage = ref(1)

const parsePageQuery = (rawPage: unknown) => {
  const value = Array.isArray(rawPage) ? rawPage[0] : rawPage
  const parsed = Number.parseInt(String(value ?? ''), 10)

  return Number.isFinite(parsed) && parsed > 0 ? parsed : 1
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

const navigateToArticle = (slug: string | null | undefined) => {
  const normalizedSlug = extractArticleSlug(slug)

  if (!normalizedSlug) {
    return
  }

  const path = resolveLocalizedRoutePath('blog-slug', currentLocale.value, {
    slug: normalizedSlug,
  })

  navigateTo(path)
}

watch(
  () => route.query.page,
  async (rawPage) => {
    const targetPage = parsePageQuery(rawPage)
    const shouldRequestPage =
      pagination.value.page !== targetPage || paginatedArticles.value.length === 0

    if (!shouldRequestPage) {
      return
    }

    await changePage(targetPage)
  },
  { immediate: true },
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

    <div v-if="loading" class="loading">
      <v-progress-circular indeterminate />
      <p>Chargement des articles...</p>
    </div>

    <div v-else-if="error" class="error">
      <v-alert type="error" variant="tonal">
        {{ error }}
      </v-alert>
      <v-btn class="mt-4" @click="fetchArticles"> Réessayer </v-btn>
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
            <v-img
              v-if="article.image"
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
              {{ article.title }}
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

              <v-btn
                variant="outlined"
                size="small"
                color="primary"
                @click="() => navigateToArticle(article.url)"
              >
                Lire plus
              </v-btn>
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

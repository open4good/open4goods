<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useHead, useRequestURL, useSeoMeta } from '#imports'
import type { BlogPostDto } from '~~/shared/api-client'
import { _sanitizeHtml } from '~~/shared/utils/sanitizer'
import RobustImage from '~/components/shared/images/RobustImage.vue'
import { useAuth } from '~/composables/useAuth'

interface BlogArticle extends BlogPostDto {
  /**
   * Legacy field kept for backward compatibility with earlier API payloads
   */
  content?: string
}

const props = defineProps<{
  article: BlogArticle
}>()

const article = computed(() => props.article)

const articleTitle = computed(() => article.value.title?.trim() || 'Article')
const articleSummary = computed(() => article.value.summary?.trim() ?? '')
const { t } = useI18n()
const { isLoggedIn } = useAuth()

const buildDateInfo = (timestamp?: number) => {
  if (!timestamp) {
    return null
  }

  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return null
  }

  return {
    iso: date.toISOString(),
    label: new Intl.DateTimeFormat(undefined, { dateStyle: 'long' }).format(date),
  }
}

const publishedDate = computed(() => buildDateInfo(article.value.createdMs))
const updatedDate = computed(() => {
  const info = buildDateInfo(article.value.modifiedMs)
  if (!info || (publishedDate.value && info.iso === publishedDate.value.iso)) {
    return null
  }
  return info
})

const rawBody = computed(() => article.value.body ?? article.value.content ?? '')
const plainBody = computed(() => rawBody.value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim())

const sanitizedBody = computed(() => {
  const { sanitizedHtml } = _sanitizeHtml(rawBody.value)
  return sanitizedHtml.value
})

const hasBody = computed(() => sanitizedBody.value.trim().length > 0)

const readingTimeMinutes = computed(() => {
  if (!plainBody.value) {
    return null
  }

  const WORDS_PER_MINUTE = 200
  const words = plainBody.value.split(/\s+/).filter(Boolean).length
  if (!words) {
    return null
  }

  return Math.max(1, Math.round(words / WORDS_PER_MINUTE))
})

const readingTimeLabel = computed(() => {
  if (!readingTimeMinutes.value) {
    return ''
  }

  return t('blog.article.readingTime', { minutes: readingTimeMinutes.value })
})

const categories = computed(() => (article.value.category ?? []).map((item) => item.trim()).filter(Boolean))

const buildTagLink = (tag: string) => ({
  path: '/blog',
  query: { tag },
})

const headingId = computed(() => `blog-article-${article.value.url ?? 'detail'}`)

const metaDescription = computed(() => {
  const summary = articleSummary.value
  if (summary) {
    return summary.length > 160 ? `${summary.slice(0, 157)}...` : summary
  }

  if (!plainBody.value) {
    return ''
  }

  const truncated = plainBody.value.slice(0, 160)
  return truncated.length < plainBody.value.length ? `${truncated.trimEnd()}...` : truncated
})

let requestUrl: URL | undefined
try {
  requestUrl = useRequestURL()
} catch {
  requestUrl = undefined
}

const canonicalUrl = computed(() => requestUrl?.href)

const structuredData = computed(() => {
  const schema: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: articleTitle.value,
    description: metaDescription.value || undefined,
    mainEntityOfPage: canonicalUrl.value,
  }

  if (article.value.image) {
    schema.image = [article.value.image]
  }

  if (article.value.author) {
    schema.author = {
      '@type': 'Person',
      name: article.value.author,
    }
  }

  if (publishedDate.value?.iso) {
    schema.datePublished = publishedDate.value.iso
  }

  if (updatedDate.value?.iso) {
    schema.dateModified = updatedDate.value.iso
  }

  return schema
})

useSeoMeta({
  title: articleTitle,
  ogTitle: articleTitle,
  description: computed(() => metaDescription.value || undefined),
  ogDescription: computed(() => metaDescription.value || undefined),
  ogType: 'article',
  ogImage: computed(() => article.value.image || undefined),
  twitterCard: computed(() => (article.value.image ? 'summary_large_image' : 'summary')),
  articlePublishedTime: computed(() => publishedDate.value?.iso),
  articleModifiedTime: computed(() => updatedDate.value?.iso ?? publishedDate.value?.iso),
  ogUrl: canonicalUrl,
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
</script>

<template>
  <article
    class="blog-article"
    :aria-labelledby="headingId"
    itemscope
    itemtype="https://schema.org/BlogPosting"
  >
    <header class="article-header">
      <div v-if="categories.length" class="article-categories" aria-label="Article categories">
        <v-chip
          v-for="category in categories"
          :key="category"
          class="article-category"
          color="primary"
          variant="tonal"
          size="small"
          :to="buildTagLink(category)"
          link
          data-test="article-category"
        >
          {{ category }}
        </v-chip>
      </div>

      <h1 :id="headingId" class="article-title" itemprop="headline" data-test="article-title">
        {{ articleTitle }}
      </h1>

      <p v-if="articleSummary" class="article-summary" itemprop="description" data-test="article-summary">
        {{ articleSummary }}
      </p>

      <div class="article-meta" aria-label="Article metadata">
        <span v-if="article.author" class="article-meta__item" data-test="article-author">
          <v-icon icon="mdi-account" size="small" aria-hidden="true" />
          <span itemprop="author" itemscope itemtype="https://schema.org/Person">
            <span itemprop="name">{{ article.author }}</span>
          </span>
        </span>

        <span v-if="publishedDate" class="article-meta__item" data-test="article-published">
          <v-icon icon="mdi-calendar" size="small" aria-hidden="true" />
          <time :datetime="publishedDate.iso" itemprop="datePublished">
            {{ publishedDate.label }}
          </time>
        </span>

        <span v-if="readingTimeLabel" class="article-meta__item" data-test="article-reading-time">
          <v-icon icon="mdi-timer" size="small" aria-hidden="true" />
          {{ readingTimeLabel }}
        </span>
      </div>
    </header>

    <figure v-if="article.image" class="article-hero" itemprop="image">
      <RobustImage
        :src="article.image"
        :alt="articleTitle"
        width="100%"
        height="360"
        class="article-hero__image"
      />
      <figcaption class="visually-hidden">{{ t('blog.article.featuredImageAlt', { title: articleTitle }) }}</figcaption>
    </figure>

    <v-divider class="article-divider" role="presentation" />

    <section v-if="hasBody" class="article-body" itemprop="articleBody" aria-label="Article content">
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div class="article-content" data-test="article-body" v-html="sanitizedBody" />
    </section>

    <section v-else class="article-body article-body--empty" aria-live="polite" data-test="article-empty">
      <v-alert type="info" variant="tonal">{{ t('blog.article.empty') }}</v-alert>
    </section>

    <footer class="article-footer" aria-label="Article footer">
      <v-btn
        v-if="isLoggedIn && article.editLink"
        :href="article.editLink"
        target="_blank"
        rel="noopener"
        prepend-icon="mdi-open-in-new"
        variant="text"
        size="small"
        data-test="article-edit-link"
      >
        {{ t('blog.article.edit') }}
      </v-btn>
    </footer>
  </article>
</template>

<style lang="sass" scoped>
.blog-article
  background-color: #ffffff
  border-radius: 20px
  box-shadow: 0 18px 48px rgba(25, 118, 210, 0.08)
  padding: clamp(1.5rem, 2vw, 3rem)
  display: flex
  flex-direction: column
  gap: 1.5rem

.article-header
  display: flex
  flex-direction: column
  gap: 1rem

.article-categories
  display: flex
  flex-wrap: wrap
  gap: 0.5rem

.article-title
  font-size: clamp(1.75rem, 2.5vw, 2.75rem)
  font-weight: 700
  color: #0d1b2a
  margin: 0

.article-summary
  font-size: clamp(1rem, 1.2vw, 1.25rem)
  color: #3d5a80
  margin: 0

.article-meta
  display: flex
  flex-wrap: wrap
  gap: 1rem
  align-items: center
  font-size: 0.95rem
  color: #607d8b

  &__item
    display: inline-flex
    align-items: center
    gap: 0.35rem

.article-hero
  margin: 0
  border-radius: 16px
  overflow: hidden

  &__image
    width: 100%
    height: auto

.article-divider
  opacity: 0.4

.article-body
  font-size: 1.05rem
  line-height: 1.75
  color: #1b263b

  &--empty
    display: flex
    justify-content: center

.article-content :deep(h2)
  font-size: clamp(1.5rem, 2vw, 2rem)
  margin-top: 1.5rem
  margin-bottom: 0.75rem

.article-content :deep(h3)
  font-size: clamp(1.25rem, 1.5vw, 1.5rem)
  margin-top: 1.25rem
  margin-bottom: 0.75rem

.article-content :deep(p)
  margin-bottom: 1.25rem

.article-content :deep(a)
  color: #1976d2
  text-decoration: underline
  text-decoration-color: rgba(25, 118, 210, 0.4)
  transition: color 0.2s ease

.article-content :deep(a:hover)
  color: #0d47a1

.article-content :deep(img)
  max-width: 100%
  border-radius: 12px
  margin: 1.5rem 0

.article-footer
  display: flex
  justify-content: flex-end

.visually-hidden
  position: absolute
  width: 1px
  height: 1px
  padding: 0
  margin: -1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)
  border: 0

@media (max-width: 960px)
  .blog-article
    padding: 1.5rem

  .article-meta
    gap: 0.75rem

@media (max-width: 600px)
  .article-meta
    flex-direction: column
    align-items: flex-start

  .article-footer
    justify-content: flex-start
</style>

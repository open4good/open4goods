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
  <v-sheet
    tag="article"
    class="d-flex flex-column gap-6 pa-6 pa-md-8 pa-lg-10"
    color="surface"
    elevation="8"
    rounded="xl"
    :aria-labelledby="headingId"
    itemscope
    itemtype="https://schema.org/BlogPosting"
  >
    <header class="d-flex flex-column gap-4">
      <nav
        v-if="categories.length"
        class="d-flex flex-wrap gap-2"
        aria-label="Article categories"
      >
        <v-chip
          v-for="category in categories"
          :key="category"
          color="primary"
          variant="tonal"
          size="small"
          :to="buildTagLink(category)"
          link
          data-test="article-category"
        >
          {{ category }}
        </v-chip>
      </nav>

      <h1
        :id="headingId"
        class="text-h3 text-lg-h2 font-weight-bold text-high-emphasis mb-0"
        itemprop="headline"
        data-test="article-title"
      >
        {{ articleTitle }}
      </h1>

      <p
        v-if="articleSummary"
        class="text-subtitle-1 text-medium-emphasis mb-0"
        itemprop="description"
        data-test="article-summary"
      >
        {{ articleSummary }}
      </p>

      <div class="d-flex flex-wrap align-center gap-4 text-body-2 text-medium-emphasis" aria-label="Article metadata">
        <div v-if="article.author" class="d-inline-flex align-center gap-2" data-test="article-author">
          <v-icon icon="mdi-account" size="small" aria-hidden="true" />
          <span class="v-visually-hidden">Author:</span>
          <span itemprop="author" itemscope itemtype="https://schema.org/Person">
            <span itemprop="name">{{ article.author }}</span>
          </span>
        </div>

        <div v-if="publishedDate" class="d-inline-flex align-center gap-2" data-test="article-published">
          <v-icon icon="mdi-calendar" size="small" aria-hidden="true" />
          <span class="v-visually-hidden">Published on:</span>
          <time :datetime="publishedDate.iso" itemprop="datePublished">
            {{ publishedDate.label }}
          </time>
        </div>

        <div v-if="readingTimeLabel" class="d-inline-flex align-center gap-2" data-test="article-reading-time">
          <v-icon icon="mdi-timer" size="small" aria-hidden="true" />
          <span class="v-visually-hidden">Estimated reading time:</span>
          <span>{{ readingTimeLabel }}</span>
        </div>
      </div>
    </header>

    <figure v-if="article.image" class="ma-0 rounded-lg overflow-hidden" itemprop="image">
      <RobustImage
        :src="article.image"
        :alt="t('blog.article.featuredImageAlt', { title: articleTitle })"
        width="100%"
        height="360"
        class="w-100"
      />
      <figcaption class="v-visually-hidden">{{ t('blog.article.featuredImageAlt', { title: articleTitle }) }}</figcaption>
    </figure>

    <v-divider class="opacity-50" role="presentation" />

    <section
      v-if="hasBody"
      class="text-body-1 text-high-emphasis"
      itemprop="articleBody"
      aria-label="Article content"
      role="region"
    >
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div class="v-prose text-body-1 text-high-emphasis" data-test="article-body" v-html="sanitizedBody" />
    </section>

    <section v-else class="d-flex justify-center" aria-live="polite" data-test="article-empty">
      <v-alert type="info" variant="tonal">{{ t('blog.article.empty') }}</v-alert>
    </section>

    <footer class="d-flex justify-end" aria-label="Article footer">
      <v-btn
        v-if="isLoggedIn && article.editLink"
        :href="article.editLink"
        target="_blank"
        rel="noopener noreferrer"
        prepend-icon="mdi-open-in-new"
        variant="text"
        size="small"
        data-test="article-edit-link"
      >
        {{ t('blog.article.edit') }}
      </v-btn>
    </footer>
  </v-sheet>
</template>

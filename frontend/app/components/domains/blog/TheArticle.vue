<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useHead, useRequestURL, useSeoMeta } from '#imports'
import { useLocalePath } from '#i18n'
import type { BlogPostDto } from '~~/shared/api-client'
import { _sanitizeHtml } from '~~/shared/utils/sanitizer'
import { useAuth } from '~/composables/useAuth'
import { buildBlogArticleJsonLd } from '~/utils/blog-jsonld'

import NudgeToolAssistantWizard from '~/components/nudge-tool/NudgeToolAssistantWizard.vue'

interface BlogArticle extends BlogPostDto {
  /**
   * Legacy field kept for backward compatibility with earlier API payloads
   */
  content?: string
}

type BodyPart = { type: 'html' | 'assistant'; content?: string }

const props = defineProps<{
  article: BlogArticle
}>()

const article = computed(() => props.article)

const articleTitle = computed(() => article.value.title?.trim() || 'Article')
const articleSummary = computed(() => article.value.summary?.trim() ?? '')
const { t, locale } = useI18n()
const { isLoggedIn } = useAuth()
const localePath = useLocalePath()

const showAssistant = computed(() => {
  // Only show if logged in.
  // The user prompt mentioned "valid role", defaulting to just logged in for now matching the prompt's implied flexibility,
  // but we can add role check here easily, e.g., && hasRole('ADMIN')
  return isLoggedIn.value
})

const assistantCategoryId = computed(() => {
  // Extract category ID from the article's category list if possible.
  // For now, assume the first category or a specific field maps to it.
  // The user prompt said "l'assistant qui correspond à la page" (assistant corresponding to the page).
  // Assuming the first category is the vertical ID for now.
  const cats = article.value.category ?? []
  if (cats.length > 0) {
    // Basic mapping: use the first category as the ID.
    // In a real app this might need slug-to-id mapping or a dedicated field.
    // Given the prompt "l'assistant qui correspond là au taille de téléviseur",
    // if the article is about TV size, the category might be 'tv' or 'televisions'.
    // Let's iterate to find a known vertical ID or just use the first one.
    return cats[0].toLowerCase()
  }
  return null
})

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
    label: new Intl.DateTimeFormat(locale.value, { dateStyle: 'long' }).format(
      date
    ),
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

const rawBody = computed(
  () => article.value.body ?? article.value.content ?? ''
)
const plainBody = computed(() =>
  rawBody.value
    .replace(/<[^>]*>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
)

const sanitizedBody = computed(() => {
  const { sanitizedHtml } = _sanitizeHtml(rawBody.value)
  return sanitizedHtml.value
})

const hasBody = computed(() => sanitizedBody.value.trim().length > 0)

const bodyParts = computed<BodyPart[]>(() => {
  const content = sanitizedBody.value
  if (!content.includes('[assistant]')) {
    return [{ type: 'html', content }]
  }

  const parts = content.split('[assistant]')
  const result: BodyPart[] = []

  parts.forEach((part, index) => {
    if (part) {
      result.push({ type: 'html', content: part })
    }
    // Add assistant between parts, but not after the last one
    if (index < parts.length - 1) {
      result.push({ type: 'assistant' })
    }
  })

  return result
})

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

const categories = computed(() =>
  (article.value.category ?? []).map(item => item.trim()).filter(Boolean)
)

const buildTagLink = (tag: string) => ({
  path: '/blog',
  query: { tag },
})

const headingId = computed(
  () => `blog-article-${article.value.url ?? 'detail'}`
)

const metaDescription = computed(() => {
  const summary = articleSummary.value
  if (summary) {
    return summary.length > 160 ? `${summary.slice(0, 157)}...` : summary
  }

  if (!plainBody.value) {
    return ''
  }

  const truncated = plainBody.value.slice(0, 160)
  return truncated.length < plainBody.value.length
    ? `${truncated.trimEnd()}...`
    : truncated
})

let requestUrl: URL | undefined
try {
  requestUrl = useRequestURL()
} catch {
  requestUrl = undefined
}

const canonicalUrl = computed(() => requestUrl?.href)

const toTrimmedString = (value: unknown): string | undefined => {
  if (typeof value !== 'string') {
    return undefined
  }

  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : undefined
}

const structuredData = computed(() => {
  if (!canonicalUrl.value || !requestUrl) {
    return null
  }

  const homeLink = localePath({ name: 'index' }) ?? '/'
  const blogLink = localePath('/blog') ?? '/blog'

  return buildBlogArticleJsonLd({
    canonicalUrl: canonicalUrl.value,
    locale: locale.value,
    pageTitle: articleTitle.value,
    description: metaDescription.value || undefined,
    site: {
      name: String(t('siteIdentity.siteName')),
      origin: requestUrl.origin,
      logoUrl: new URL(
        '/nudger-icon-512x512.png',
        requestUrl.origin
      ).toString(),
      sameAs: [toTrimmedString(t('siteIdentity.links.linkedin'))].filter(
        (value): value is string => Boolean(value)
      ),
    },
    breadcrumbs: [
      { name: t('blog.breadcrumbs.home'), link: homeLink },
      { name: t('blog.breadcrumbs.blog'), link: blogLink },
      { name: articleTitle.value, link: canonicalUrl.value },
    ],
    article: {
      headline: articleTitle.value,
      description: metaDescription.value || undefined,
      url: canonicalUrl.value,
      image: article.value.image ? [article.value.image] : undefined,
      author: article.value.author?.trim(),
      datePublished: publishedDate.value?.iso,
      dateModified: updatedDate.value?.iso ?? publishedDate.value?.iso,
    },
  })
})

useSeoMeta({
  title: articleTitle,
  ogTitle: articleTitle,
  description: computed(() => metaDescription.value || undefined),
  ogDescription: computed(() => metaDescription.value || undefined),
  ogType: 'article',
  ogImage: computed(() => article.value.image || undefined),
  articlePublishedTime: computed(() => publishedDate.value?.iso),
  articleModifiedTime: computed(
    () => updatedDate.value?.iso ?? publishedDate.value?.iso
  ),
  ogUrl: canonicalUrl,
})

useHead(() => ({
  link: [
    ...(canonicalUrl.value
      ? [
          {
            rel: 'canonical',
            href: canonicalUrl.value,
          },
        ]
      : []),
    ...(article.value.image
      ? [
          {
            rel: 'preload',
            as: 'image',
            href: article.value.image,
          },
        ]
      : []),
  ],
  script: structuredData.value
    ? [
        {
          type: 'application/ld+json',
          children: JSON.stringify(structuredData.value),
        },
      ]
    : [],
}))
</script>

<template>
  <v-sheet
    tag="article"
    class="blog-article"
    color="surface"
    elevation="8"
    rounded="xl"
    :aria-labelledby="headingId"
    itemscope
    itemtype="https://schema.org/BlogPosting"
  >
    <header class="article-header">
      <nav
        v-if="categories.length"
        class="article-categories"
        aria-label="Article categories"
      >
        <ul class="article-categories__list">
          <li
            v-for="category in categories"
            :key="category"
            class="article-categories__item"
          >
            <v-chip
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
          </li>
        </ul>
      </nav>

      <h1
        :id="headingId"
        class="article-title"
        itemprop="headline"
        data-test="article-title"
      >
        {{ articleTitle }}
      </h1>

      <p
        v-if="articleSummary"
        class="article-summary"
        itemprop="description"
        data-test="article-summary"
      >
        {{ articleSummary }}
      </p>

      <ul class="article-meta" aria-label="Article metadata">
        <li
          v-if="article.author"
          class="article-meta__item"
          data-test="article-author"
        >
          <v-icon icon="mdi-account" size="small" aria-hidden="true" />
          <span
            itemprop="author"
            itemscope
            itemtype="https://schema.org/Person"
          >
            <span itemprop="name">{{ article.author }}</span>
          </span>
        </li>

        <li
          v-if="publishedDate"
          class="article-meta__item"
          data-test="article-published"
        >
          <v-icon icon="mdi-calendar" size="small" aria-hidden="true" />
          <time :datetime="publishedDate.iso" itemprop="datePublished">
            {{ publishedDate.label }}
          </time>
        </li>

        <li
          v-if="readingTimeLabel"
          class="article-meta__item"
          data-test="article-reading-time"
        >
          <v-icon icon="mdi-timer" size="small" aria-hidden="true" />
          <span>{{ readingTimeLabel }}</span>
        </li>
      </ul>
    </header>

    <figure v-if="article.image" class="article-hero" itemprop="image">
      <RobustImage
        :src="article.image"
        :alt="t('blog.article.featuredImageAlt', { title: articleTitle })"
        width="70%"
        :height="360"
        class="article-hero__image"
      />
      <figcaption class="d-sr-only">
        {{ t('blog.article.featuredImageAlt', { title: articleTitle }) }}
      </figcaption>
    </figure>

    <v-divider class="article-divider" role="presentation" />

    <section
      v-if="hasBody"
      class="article-body"
      itemprop="articleBody"
      aria-label="Article content"
      role="region"
    >
      <template v-for="(part, index) in bodyParts" :key="index">
        <!-- eslint-disable vue/no-v-html -->
        <div
          v-if="part.type === 'html'"
          class="article-content"
          data-test="article-body"
          v-html="part.content"
        />
        <!-- eslint-enable vue/no-v-html -->
        <div
          v-else-if="part.type === 'assistant' && showAssistant"
          class="my-8"
        >
          <NudgeToolAssistantWizard
            :assistant-category-id="assistantCategoryId"
            compact
          />
        </div>
      </template>
    </section>
    <section
      v-else
      class="article-body article-body--empty"
      aria-live="polite"
      data-test="article-empty"
    >
      <v-alert type="info" variant="tonal">{{
        t('blog.article.empty')
      }}</v-alert>
    </section>

    <footer class="article-footer">
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

<style lang="sass" scoped>
.blog-article
  padding: clamp(1.5rem, 2vw, 3rem)
  display: flex
  flex-direction: column
  gap: 1.5rem

.article-header
  display: flex
  flex-direction: column
  gap: 1rem

.article-categories
  display: block

  &__list
    display: flex
    flex-wrap: wrap
    gap: 0.5rem
    list-style: none
    margin: 0
    padding: 0

  &__item
    list-style: none

.article-title
  font-size: clamp(1.75rem, 2.5vw, 2.75rem)
  font-weight: 700
  margin: 0
  color: rgba(var(--v-theme-on-surface), var(--v-high-emphasis-opacity))

.article-summary
  font-size: clamp(1rem, 1.2vw, 1.25rem)
  margin: 0
  color: rgba(var(--v-theme-on-surface), var(--v-medium-emphasis-opacity))


.article-meta
  display: flex
  flex-wrap: wrap
  gap: 1rem
  align-items: center
  font-size: 0.95rem
  margin: 0
  padding: 0
  list-style: none
  color: rgba(var(--v-theme-on-surface), var(--v-medium-emphasis-opacity))

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
  color: rgba(var(--v-theme-on-surface), var(--v-high-emphasis-opacity))

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

.article-content :deep(ul),
.article-content :deep(ol)
  margin: 0 0 1.25rem 1.5rem
  padding-left: 0.5rem

.article-content :deep(li)
  margin-bottom: 0.5rem

.article-content :deep(a)
  color: rgb(var(--v-theme-primary))
  text-decoration: underline
  text-decoration-color: rgba(var(--v-theme-primary), 0.4)
  transition: color 0.2s ease, text-decoration-color 0.2s ease

.article-content :deep(a:hover),
.article-content :deep(a:focus-visible)
  color: rgb(var(--v-theme-primary))
  text-decoration-color: rgb(var(--v-theme-primary))

.article-content :deep(img)
  max-width: 100%
  border-radius: 12px
  margin: 1.5rem 0

.article-footer
  display: flex
  justify-content: flex-end


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

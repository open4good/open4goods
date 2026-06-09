<template>
  <aside class="category-doc-rail" data-testid="category-doc-rail">
    <v-card
      v-if="wikiPages.length || guideLinks.length"
      class="category-doc-rail__card"
      rounded="xl"
      elevation="1"
    >
      <v-card-title class="category-doc-rail__title">
        <v-icon icon="mdi-book-open-page-variant" size="20" class="me-2" />
        {{ $t('category.documentation.guidesTitle') }}
      </v-card-title>
      <v-divider />
      <v-list density="comfortable">
        <v-list-item v-for="guide in guideLinks" :key="guide.to" :to="guide.to">
          <template #title>
            <span class="category-doc-rail__link">{{ guide.title }}</span>
          </template>
        </v-list-item>
        <v-list-item
          v-for="page in wikiPages"
          :key="page.title ?? page.verticalUrl ?? page.wikiUrl"
          v-bind="resolveWikiLinkProps(page)"
        >
          <template #title>
            <span class="category-doc-rail__link">{{ page.title }}</span>
          </template>
        </v-list-item>
      </v-list>
    </v-card>

    <v-card
      v-if="relatedPosts.length"
      class="category-doc-rail__card"
      rounded="xl"
      elevation="1"
    >
      <v-card-title class="category-doc-rail__title">
        <v-icon icon="mdi-newspaper-variant-outline" size="20" class="me-2" />
        {{ $t('category.documentation.postsTitle') }}
      </v-card-title>
      <v-divider />
      <v-list density="comfortable">
        <v-list-item
          v-for="post in relatedPosts"
          :key="post.url ?? post.title"
          :href="resolvePostUrl(post)"
        >
          <v-list-item-title>{{ post.title }}</v-list-item-title>
          <v-list-item-subtitle v-if="post.summary">
            {{ post.summary }}
          </v-list-item-subtitle>
        </v-list-item>
      </v-list>
    </v-card>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BlogPostDto, WikiPageConfig } from '~~/shared/api-client'

const props = defineProps<{
  wikiPages: WikiPageConfig[]
  guideSlugs?: string[]
  relatedPosts: BlogPostDto[]
  verticalHomeUrl?: string | null
}>()

const wikiPages = computed(() => props.wikiPages ?? [])
const guideSlugs = computed(() => props.guideSlugs ?? [])
const relatedPosts = computed(() => props.relatedPosts ?? [])

const normalizePathSegment = (value: string) => value.replace(/^\/+|\/+$/g, '')

const prettifySlug = (value: string) =>
  value
    .replace(/[-_]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/\b\w/g, char => char.toUpperCase())

const internalBasePath = computed(() => {
  const raw = props.verticalHomeUrl?.trim()
  if (!raw || /^https?:\/\//i.test(raw)) {
    return null
  }

  return `/${normalizePathSegment(raw)}`
})

const guideLinks = computed(() => {
  const basePath = internalBasePath.value

  if (!basePath) {
    return []
  }

  const wikiSlugs = new Set(
    wikiPages.value
      .map(page => page.verticalUrl?.trim())
      .filter((value): value is string => Boolean(value))
      .map(normalizePathSegment)
  )
  const uniqueSlugs = new Set<string>()

  return guideSlugs.value
    .map(slug => normalizePathSegment(slug.trim()))
    .filter(slug => {
      if (!slug || wikiSlugs.has(slug) || uniqueSlugs.has(slug)) {
        return false
      }

      uniqueSlugs.add(slug)
      return true
    })
    .map(slug => ({
      title: prettifySlug(slug),
      to: `${basePath}/${slug}`,
    }))
})

const resolveExternalWikiUrl = (page: WikiPageConfig) => {
  if (page.wikiUrl) {
    return page.wikiUrl
  }

  if (props.verticalHomeUrl && page.verticalUrl) {
    return `${props.verticalHomeUrl.replace(/\/$/, '')}/${normalizePathSegment(page.verticalUrl)}`
  }

  return page.verticalUrl ?? '#'
}

type WikiLinkProps = {
  to?: string
  href?: string
  target?: string
  rel?: string
}

const resolveWikiLinkProps = (page: WikiPageConfig): WikiLinkProps => {
  const slug = page.verticalUrl ? normalizePathSegment(page.verticalUrl) : ''
  const basePath = internalBasePath.value

  if (basePath && slug) {
    return { to: `${basePath}/${slug}`.replace(/\/+$/, '') }
  }

  const externalUrl = resolveExternalWikiUrl(page)

  return {
    href: externalUrl,
    target: '_blank',
    rel: 'noopener',
  }
}

const BLOG_POST_PATH_PREFIX = '/blog/'

const resolvePostUrl = (post: BlogPostDto) => {
  const rawUrl = post.url?.trim()

  if (!rawUrl) {
    return '#'
  }

  if (/^https?:\/\//i.test(rawUrl)) {
    return rawUrl
  }

  const normalised = rawUrl.replace(/^\/+/, '')

  if (normalised.startsWith('blog/')) {
    return `/${normalised}`
  }

  return `${BLOG_POST_PATH_PREFIX}${normalised}`
}
</script>

<style scoped lang="sass">
.category-doc-rail
  display: flex
  flex-direction: column
  gap: 1.5rem

  &__card
    background-color: rgb(var(--v-theme-surface-glass))

  &__title
    font-weight: 600
    display: flex
    align-items: center

  &__link
    display: inline-flex
    align-items: center
    gap: 0.25rem
    color: inherit

  :deep(.v-list-item-title)
    font-weight: 500
</style>

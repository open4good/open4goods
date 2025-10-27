<template>
  <aside class="category-doc-rail" data-testid="category-doc-rail">
    <v-card v-if="wikiPages.length" class="category-doc-rail__card" rounded="xl" elevation="1">
      <v-card-title class="category-doc-rail__title">
        <v-icon icon="mdi-book-open-page-variant" size="20" class="me-2" />
        {{ $t('category.documentation.guidesTitle') }}
      </v-card-title>
      <v-divider />
      <v-list density="comfortable">
        <v-list-item
          v-for="page in wikiPages"
          :key="page.title ?? page.verticalUrl ?? page.wikiUrl"
          v-bind="buildWikiLinkProps(page)"
        >
          <v-list-item-title>{{ page.title }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-card>

    <v-card v-if="relatedPosts.length" class="category-doc-rail__card" rounded="xl" elevation="1">
      <v-card-title class="category-doc-rail__title">
        <v-icon icon="mdi-newspaper-variant-outline" size="20" class="me-2" />
        {{ $t('category.documentation.postsTitle') }}
      </v-card-title>
      <v-divider />
      <v-list density="comfortable">
        <v-list-item
          v-for="post in relatedPosts"
          :key="post.url ?? post.title"
          v-bind="buildPostLinkProps(post)"
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
import type { BlogPostDto, WikiPageConfig } from '~~/shared/api-client'

const props = defineProps<{
  wikiPages: WikiPageConfig[]
  relatedPosts: BlogPostDto[]
  verticalHomeUrl?: string | null
}>()

const wikiPages = computed(() => props.wikiPages ?? [])
const relatedPosts = computed(() => props.relatedPosts ?? [])

const sanitizePathSegment = (value: string | undefined | null): string => {
  const raw = (value ?? '').trim()

  if (!raw) {
    return ''
  }

  try {
    const parsed = new URL(raw)
    return parsed.pathname.replace(/^\/+|\/+$/gu, '')
  } catch {
    return raw
      .replace(/^https?:\/\/[^/]+/iu, '')
      .replace(/^\/+|\/+$/gu, '')
  }
}

const buildWikiPath = (page: WikiPageConfig): string | null => {
  const base = sanitizePathSegment(props.verticalHomeUrl)
  const child = sanitizePathSegment(page.verticalUrl)

  if (!base || !child) {
    return null
  }

  return `/${base}/${child}`
}

const buildWikiLinkProps = (page: WikiPageConfig) => {
  const path = buildWikiPath(page)

  if (path) {
    return {
      to: path,
      nuxt: true,
    }
  }

  return {
    href: '#',
  }
}

const resolvePostPath = (post: BlogPostDto): string | null => {
  const slug = (post.url ?? '').trim()

  if (!slug) {
    return null
  }

  if (/^https?:\/\//iu.test(slug)) {
    return slug
  }

  const normalized = slug.replace(/^\/+/, '')
  const prefixed = normalized.startsWith('blog/') ? normalized : `blog/${normalized}`

  return `/${prefixed}`
}

const buildPostLinkProps = (post: BlogPostDto) => {
  const path = resolvePostPath(post)

  if (!path) {
    return { href: '#' }
  }

  if (/^https?:\/\//iu.test(path)) {
    return {
      href: path,
      target: '_blank',
      rel: 'noopener',
    }
  }

  return {
    to: path,
    nuxt: true,
  }
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

  :deep(.v-list-item-title)
    font-weight: 500
</style>

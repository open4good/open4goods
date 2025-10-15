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
          :href="resolveWikiUrl(page)"
          target="_blank"
          rel="noopener"
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
          :key="post.slug ?? post.title"
          :to="resolvePostUrl(post)"
        >
          <v-list-item-title>{{ post.title }}</v-list-item-title>
          <v-list-item-subtitle v-if="post.excerpt">
            {{ post.excerpt }}
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

const resolveWikiUrl = (page: WikiPageConfig) => {
  if (page.wikiUrl) {
    return page.wikiUrl
  }

  if (props.verticalHomeUrl && page.verticalUrl) {
    return `${props.verticalHomeUrl.replace(/\/$/, '')}/${page.verticalUrl.replace(/^\//, '')}`
  }

  return page.verticalUrl ?? '#'
}

const resolvePostUrl = (post: BlogPostDto) => {
  return `/blog/${post.slug ?? ''}`
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

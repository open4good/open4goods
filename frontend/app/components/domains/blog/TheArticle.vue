<script setup lang="ts">
import type { BlogPostDto } from '~~/shared/api-client'

interface BlogArticle extends BlogPostDto {
  content?: string
}

defineProps<{
  article: BlogArticle
}>()

// Format a timestamp into a human-readable date in French
const formatDate = (timestamp: number) =>
  new Date(timestamp).toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
</script>

<template>
  <div>
    <v-img
      v-if="article.image"
      :src="article.image"
      height="300"
      class="mb-6"
      cover
      rounded
    />

    <h1 class="text-h4 font-weight-bold mb-2">{{ article.title }}</h1>

    <div class="d-flex align-center text-grey text-subtitle-2 mb-6">
      <v-icon size="18" class="mr-1" color="primary">mdi-account</v-icon>
      {{ article.author }}
      <v-icon size="18" class="ml-4 mr-1" color="grey">mdi-calendar</v-icon>
      {{ formatDate(article.createdMs ?? 0) }}
    </div>

    <div class="article-content" v-html="article.body" />
  </div>
</template>

<style scoped lang="scss">
.article-content {
  font-size: 1.05rem;
  line-height: 1.7;
  color: #333;

  p {
    margin-bottom: 1em;
  }

  img {
    max-width: 100%;
    border-radius: 8px;
    margin: 1em 0;
  }

  h2,
  h3 {
    margin-top: 2em;
    margin-bottom: 0.5em;
    font-weight: 600;
  }

  ul,
  ol {
    margin: 1em 0;
    padding-left: 1.5em;
  }

  blockquote {
    padding: 1em;
    margin: 1em 0;
    background: #f9f9f9;
    border-left: 4px solid #1976d2;
    font-style: italic;
    color: #555;
  }
}
</style>

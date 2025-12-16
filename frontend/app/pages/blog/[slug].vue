<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { useAsyncData } from '#imports'
import { useBlog } from '~/composables/blog/useBlog'
import type { BlogPostDto } from '~~/shared/api-client'

interface BlogArticle extends BlogPostDto {
  content?: string
}

const route = useRoute()
const router = useRouter()
const { currentArticle, loading, error, fetchArticle } = useBlog()

const slug = computed(() => {
  const rawSlug = route.params.slug

  if (Array.isArray(rawSlug)) {
    return (
      rawSlug.find(
        value => typeof value === 'string' && value.trim().length > 0
      ) ?? null
    )
  }

  if (typeof rawSlug !== 'string') {
    return null
  }

  const trimmed = rawSlug.trim()

  return trimmed.length > 0 ? trimmed : null
})

await useAsyncData(
  () => (slug.value ? `blog-article-${slug.value}` : 'blog-article'),
  () => (slug.value ? fetchArticle(slug.value) : Promise.resolve(null)),
  {
    server: true,
    immediate: true,
    watch: [slug],
  }
)

const article = computed(() => currentArticle.value as BlogArticle | null)
</script>

<template>
  <v-container class="py-10 px-4 mx-auto" max-width="xl">
    <v-row>
      <v-col cols="12">
        <v-btn
          variant="text"
          prepend-icon="mdi-arrow-left"
          @click="router.back()"
        >
          Retour
        </v-btn>

        <v-skeleton-loader
          v-if="loading"
          type="heading, image, paragraph, paragraph"
          class="mt-4"
        />

        <v-alert v-else-if="error" type="error" variant="tonal" class="mt-4">
          {{ error }}
        </v-alert>

        <TheArticle v-else-if="article" :article="article" />
      </v-col>
    </v-row>
  </v-container>
</template>

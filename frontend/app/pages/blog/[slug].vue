<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { onMounted, computed } from 'vue'
import { useBlog } from '~/composables/blog/useBlog'
import type { BlogPostDto } from '~~/shared/api-client'

interface BlogArticle extends BlogPostDto {
  content?: string
}

const route = useRoute()
const router = useRouter()
const { currentArticle, loading, error, fetchArticle } = useBlog()

const article = computed(
  () => currentArticle.value as BlogArticle | null
)

// Fetch the article when the page loads
onMounted(() => {
  fetchArticle(route.params.slug as string)
})
</script>

<template>
  <v-container class="py-10">
    <v-row justify="center">
      <v-col cols="12" md="10" lg="8">
        <v-btn variant="text" prepend-icon="mdi-arrow-left" @click="router.back()">
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

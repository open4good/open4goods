<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ref, onMounted } from 'vue'
import { useBlog } from '~/composables/blog/useBlog'
import type { BlogPostDto } from '~/src/api'

interface BlogArticle extends BlogPostDto {
  content?: string
}

const route = useRoute()
const router = useRouter()
const { getArticleByUrl } = useBlog()

const article = ref<BlogArticle | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const data = await getArticleByUrl(route.params.slug as string)
    if (!data) {
      error.value = 'Article introuvable.'
    } else {
      article.value = data as BlogArticle
    }
  } catch {
    error.value = 'Erreur lors du chargement de l\u2019article.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-container class="py-10">
    <v-row justify="center">
      <v-col cols="12" md="10" lg="8">
        <v-btn variant="text" @click="router.back()" prepend-icon="mdi-arrow-left">
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

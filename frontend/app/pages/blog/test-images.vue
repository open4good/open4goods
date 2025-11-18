<template>
  <div>
    <v-container>
      <v-row>
        <v-col cols="12">
          <h1 class="text-h3 mb-6">Debug Test Page</h1>

          <v-alert type="info" variant="tonal" class="mb-6">
            Cette page permet de déboguer et tester l'affichage des données des
            articles de blog.
          </v-alert>

          <div v-if="loading" class="text-center">
            <v-progress-circular indeterminate />
            <p>Chargement des articles...</p>
          </div>

          <div v-else-if="error" class="text-center">
            <v-alert type="error">
              {{ error }}
            </v-alert>
            <v-btn class="mt-4" @click="fetchArticles"> Réessayer </v-btn>
          </div>

          <div v-else>
            <h2 class="text-h5 mb-4">Debug des articles</h2>

            <div v-for="article in articles" :key="article.url" class="mb-6">
              <h3 class="text-h6 mb-2">{{ article.title }}</h3>

              <!-- Debug de l'article complet -->
              <ImageTest :data="article" :title="`Article: ${article.title}`" />

              <!-- Debug spécifique de l'image si elle existe -->
              <div v-if="article.image" class="mt-4">
                <ImageTest
                  :data="{
                    imageUrl: article.image,
                    title: article.title,
                    alt: article.title,
                  }"
                  title="Image Data"
                />
              </div>
            </div>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { useBlog } from '~/composables/blog/useBlog'

const { articles, loading, error, fetchArticles } = useBlog()

const canonicalUrl = useCanonicalUrl()

useHead(() => ({
  link: canonicalUrl.value
    ? [
        {
          rel: 'canonical',
          href: canonicalUrl.value,
        },
      ]
    : [],
}))

useSeoMeta({
  ogUrl: () => canonicalUrl.value || undefined,
})

// Fetch articles on component mount
onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
/* Add any page-specific styles here */
</style>

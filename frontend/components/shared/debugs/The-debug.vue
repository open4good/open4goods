<script setup lang="ts">
import type {
  BlogArticleData,
  PaginatedBlogResponse,
} from '~/server/api/blog/types/blog.models'

const props = defineProps<{
  data: PaginatedBlogResponse
  title: string
}>()
// Define the debug response type
interface DebugResponse {
  success: boolean
  timestamp: string
  data?: PaginatedBlogResponse
  debug?: {
    articlesCount: number
    sampleArticle: BlogArticleData | null
    imageUrls: Array<{
      title: string
      image: string
      hasImage: boolean
      imageLength: number
    }>
  }
  error?: string
}

const loading = ref(false)
const debugData = ref<DebugResponse | null>(null)

const fetchDebugData = async () => {
  loading.value = true
  try {
    const response = await $fetch<DebugResponse>('/api/blog/test')
    debugData.value = response
  } catch (error) {
    console.error('Error fetching debug data:', error)
    debugData.value = {
      success: false,
      timestamp: new Date().toISOString(),
      error: error instanceof Error ? error.message : 'Unknown error',
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <v-container>
      <v-row>
        <v-col cols="12">
          <h1 class="text-h3 mb-6">Debug Blog API</h1>

          <v-alert type="info" variant="tonal" class="mb-6">
            {{ props.title }}
          </v-alert>

          <v-btn :loading="loading" class="mb-4" @click="fetchDebugData">
            Charger les données de debug
          </v-btn>

          <div v-if="debugData" class="debug-content">
            <h2 class="text-h5 mb-4">Données de debug</h2>

            <v-card class="mb-4">
              <v-card-title>Informations générales</v-card-title>
              <v-card-text>
                <p><strong>Succès:</strong> {{ debugData.success }}</p>
                <p><strong>Timestamp:</strong> {{ debugData.timestamp }}</p>
                <p>
                  <strong>Nombre d'articles:</strong>
                  {{ debugData.debug?.articlesCount }}
                </p>
              </v-card-text>
            </v-card>

            <v-card v-if="debugData.debug?.sampleArticle" class="mb-4">
              <v-card-title>Exemple d'article</v-card-title>
              <v-card-text>
                <pre>{{
                  JSON.stringify(debugData.debug.sampleArticle, null, 2)
                }}</pre>
              </v-card-text>
            </v-card>

            <v-card v-if="debugData.debug?.imageUrls" class="mb-4">
              <v-card-title>URLs des images</v-card-title>
              <v-card-text>
                <div
                  v-for="(imgInfo, index) in debugData.debug.imageUrls"
                  :key="index"
                  class="mb-2"
                >
                  <p>
                    <strong>{{ imgInfo.title }}</strong>
                  </p>
                  <p>
                    <strong>Image:</strong>
                    {{ imgInfo.image || 'Aucune image' }}
                  </p>
                  <p><strong>Has Image:</strong> {{ imgInfo.hasImage }}</p>
                  <p><strong>Length:</strong> {{ imgInfo.imageLength }}</p>
                  <hr v-if="index < debugData.debug.imageUrls.length - 1" />
                </div>
              </v-card-text>
            </v-card>

            <v-card v-if="debugData.error">
              <v-card-title class="text-error">Erreur</v-card-title>
              <v-card-text>
                <p>{{ debugData.error }}</p>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<style scoped>
.debug-content {
  font-family: monospace;
  font-size: 14px;
}

pre {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
}
</style>

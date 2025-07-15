<script setup lang="ts">
import { useBlog } from '~/composables/blog/useBlog'
const { articles, loading, error, fetchArticles } = useBlog()

// Format date helper
const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleDateString()
}

// Debug mode - use environment variable or default to false
const debugMode = ref(false)

// Fetch articles on component mount
onMounted(() => {
  fetchArticles()
})
</script>

<template>
  <div class="blog-list">
    <!-- Debug toggle -->
    <div class="debug-toggle">
      <v-btn size="small" variant="outlined" @click="debugMode = !debugMode">
        {{ debugMode ? 'Hide' : 'Show' }} Debug Info
      </v-btn>
    </div>

    <!-- Debug information -->
    <div v-if="debugMode" class="debug-info">
      <h4>Debug Information:</h4>
      <p>Articles count: {{ articles.length }}</p>
      <div
        v-for="(article, index) in articles"
        :key="index"
        class="debug-article"
      >
        <h5>Article {{ index + 1 }}:</h5>
        <pre>{{ JSON.stringify(article, null, 2) }}</pre>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <v-progress-circular indeterminate />
      <p>Chargement des articles...</p>
    </div>

    <div v-else-if="error" class="error">
      <v-alert type="error" variant="tonal">
        {{ error }}
      </v-alert>
      <v-btn @click="fetchArticles" class="mt-4"> Réessayer </v-btn>
    </div>

    <div v-else class="articles">
      <v-row>
        <v-col
          v-for="article in articles"
          :key="article.url"
          cols="12"
          sm="6"
          md="4"
          lg="4"
        >
          <v-card class="article-card" elevation="6" hover>
            <!-- Image de l'article -->
            <v-img
              v-if="article.image"
              :src="article.image"
              :alt="article.title"
              height="200"
              cover
              class="article-image"
            >
              <template v-slot:placeholder>
                <div class="image-placeholder">
                  <v-icon size="48" color="grey-lighten-1">
                    mdi-image-off
                  </v-icon>
                  <p class="placeholder-text">Image non disponible</p>
                </div>
              </template>
            </v-img>

            <!-- Contenu de la carte -->
            <v-card-title class="article-title">
              {{ article.title }}
            </v-card-title>

            <v-card-text class="article-summary">
              <p>{{ article.summary }}</p>
            </v-card-text>

            <!-- Actions et métadonnées -->
            <v-card-actions class="article-actions">
              <div class="article-meta">
                <div class="author-info">
                  <v-icon size="small" color="primary" class="mr-1">
                    mdi-account
                  </v-icon>
                  <span class="author-name">{{ article.author }}</span>
                </div>
                <div class="date-info">
                  <v-icon size="small" color="grey" class="mr-1">
                    mdi-calendar
                  </v-icon>
                  <span class="date-text">{{
                    formatDate(article.createdMs)
                  }}</span>
                </div>
              </div>

              <v-spacer></v-spacer>

              <v-btn
                variant="outlined"
                size="small"
                color="primary"
                @click="() => navigateTo(`/blog/${article.url}`)"
              >
                Lire plus
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </div>
</template>

<style lang="sass" scoped>
.blog-list
  max-width: 1200px
  margin: 0 auto
  padding: 20px

.debug-toggle
  margin-bottom: 20px
  text-align: center

.debug-info
  background: #f5f5f5
  border: 1px solid #ddd
  border-radius: 8px
  padding: 16px
  margin-bottom: 20px
  font-family: monospace
  font-size: 12px

.debug-article
  margin-bottom: 16px
  padding: 8px
  background: white
  border-radius: 4px

  h5
    margin: 0 0 8px 0
    color: #333

  pre
    margin: 0
    white-space: pre-wrap
    word-break: break-all

.loading,
.error
  text-align: center
  padding: 20px

.article-card
  height: 100%
  transition: all 0.3s ease
  border-radius: 12px
  overflow: hidden

  &:hover
    transform: translateY(-4px)
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15)

.article-image
  position: relative

.image-placeholder
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  height: 100%
  background: #f5f5f5
  color: #666

.placeholder-text
  margin-top: 8px
  font-size: 12px
  text-align: center

.article-title
  font-size: 1.1rem
  font-weight: 600
  line-height: 1.3
  color: #333
  padding: 16px 16px 8px 16px

.article-summary
  padding: 0 16px 16px 16px

  p
    color: #666
    line-height: 1.5
    margin: 0
    overflow: hidden
    text-overflow: ellipsis

.article-actions
  padding: 16px
  border-top: 1px solid #f0f0f0
  background: #fafafa

.article-meta
  display: flex
  flex-direction: column
  gap: 4px

.author-info,
.date-info
  display: flex
  align-items: center
  font-size: 0.85rem

.author-name
  color: #1976d2
  font-weight: 500

.date-text
  color: #666

// Responsive adjustments
@media (max-width: 600px)
  .article-card
    margin-bottom: 16px

  .article-title
    font-size: 1rem
</style>

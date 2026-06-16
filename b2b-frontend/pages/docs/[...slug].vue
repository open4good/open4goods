<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="10" xl="8">
        <v-card v-if="pending" variant="outlined" class="pa-8">
          <v-skeleton-loader type="heading, paragraph, paragraph, paragraph" />
        </v-card>

        <v-card v-else-if="!doc" variant="outlined" class="pa-8 text-center">
          <v-icon icon="mdi-file-document-outline" size="48" color="medium-emphasis" class="mb-4" />
          <div class="text-h6 mb-2">{{ t('docs.notFound') }}</div>
          <div class="text-medium-emphasis mb-6">{{ t('docs.notFoundDetail') }}</div>
          <v-btn :to="localePath('/docs')" variant="tonal" prepend-icon="mdi-arrow-left">
            {{ t('docs.backToIndex') }}
          </v-btn>
        </v-card>

        <template v-else>
          <div class="d-flex align-center ga-2 mb-4">
            <v-btn
              :to="localePath('/docs')"
              variant="text"
              size="small"
              prepend-icon="mdi-arrow-left"
              class="text-medium-emphasis"
            >
              {{ t('docs.backToIndex') }}
            </v-btn>
          </div>

          <v-card variant="outlined" class="docs-article">
            <v-card-text class="pa-6 pa-md-10">
              <ContentRenderer :value="doc" class="docs-content" />
            </v-card-text>
          </v-card>

          <div class="d-flex justify-space-between mt-4">
            <v-btn
              v-if="prevDoc"
              :to="localePath(`/docs/${prevDoc.stem?.replace(/^[a-z]+\/docs\//, '')}`)"
              variant="text"
              prepend-icon="mdi-arrow-left"
            >
              {{ prevDoc.title }}
            </v-btn>
            <v-spacer />
            <v-btn
              v-if="nextDoc"
              :to="localePath(`/docs/${nextDoc.stem?.replace(/^[a-z]+\/docs\//, '')}`)"
              variant="text"
              append-icon="mdi-arrow-right"
            >
              {{ nextDoc.title }}
            </v-btn>
          </div>
        </template>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
const { t, locale } = useI18n()
const localePath = useLocalePath()
const route = useRoute()

const slug = computed(() => {
  const raw = route.params.slug
  return Array.isArray(raw) ? raw.join('/') : (raw ?? '')
})

const contentPath = computed(() => `/${locale.value}/docs/${slug.value}`)

const { data: doc, pending } = await useAsyncData(
  `doc:${contentPath.value}`,
  () => queryCollection('pages').path(contentPath.value).first(),
  { watch: [contentPath] }
)

const { data: allDocs } = await useAsyncData(
  `docs-nav:${locale.value}`,
  () => queryCollection('pages')
    .where('path', 'LIKE', `/${locale.value}/docs/%`)
    .order('path', 'ASC')
    .all(),
  { watch: [locale] }
)

const currentIndex = computed(() => {
  if (!allDocs.value || !doc.value) return -1
  return allDocs.value.findIndex((d) => d.path === doc.value!.path)
})

const prevDoc = computed(() => {
  if (!allDocs.value || currentIndex.value <= 0) return null
  return allDocs.value[currentIndex.value - 1] ?? null
})

const nextDoc = computed(() => {
  if (!allDocs.value || currentIndex.value < 0) return null
  return allDocs.value[currentIndex.value + 1] ?? null
})

useHead({
  title: computed(() => doc.value?.title ? `${doc.value.title} — Product Data API` : 'Documentation — Product Data API')
})

useSeoMeta({
  description: computed(() => doc.value?.description ?? t('docs.seo.description'))
})

definePageMeta({
  width: 'semi-fluid'
})
</script>

<style>
.docs-content h1 { font-size: 1.75rem; font-weight: 700; margin-bottom: 1rem; margin-top: 0; }
.docs-content h2 { font-size: 1.35rem; font-weight: 600; margin-top: 2rem; margin-bottom: 0.75rem; }
.docs-content h3 { font-size: 1.1rem; font-weight: 600; margin-top: 1.5rem; margin-bottom: 0.5rem; }
.docs-content p  { margin-bottom: 1rem; line-height: 1.7; }
.docs-content ul, .docs-content ol { margin-bottom: 1rem; padding-left: 1.5rem; }
.docs-content li { margin-bottom: 0.35rem; line-height: 1.6; }
.docs-content pre { background: #f5f5f5; border-radius: 6px; padding: 1rem; overflow-x: auto; margin-bottom: 1rem; }
.docs-content code { font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: 0.85em; }
.docs-content pre code { background: none; padding: 0; }
.docs-content :not(pre) > code { background: rgba(0,0,0,.07); padding: 0.15em 0.4em; border-radius: 4px; }
.docs-content table { border-collapse: collapse; width: 100%; margin-bottom: 1rem; }
.docs-content th, .docs-content td { border: 1px solid rgba(0,0,0,.12); padding: 0.5rem 0.75rem; text-align: left; }
.docs-content th { background: rgba(0,0,0,.04); font-weight: 600; }
.docs-content a { color: rgb(var(--v-theme-primary)); text-decoration: none; }
.docs-content a:hover { text-decoration: underline; }
.docs-content hr { border: none; border-top: 1px solid rgba(0,0,0,.12); margin: 2rem 0; }
</style>

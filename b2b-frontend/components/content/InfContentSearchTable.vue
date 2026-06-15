<script setup lang="ts">
import type { ContentCatalogItem } from '~/composables/useContentCatalog'

const props = defineProps<{
  items: ContentCatalogItem[]
}>()

const { t } = useI18n()
const query = defineModel<string>('query', { default: '' })

const headers = computed(() => [
  { title: t('content.explorer.headers.title'), key: 'title' },
  { title: t('content.explorer.headers.path'), key: 'slug' },
  { title: t('content.explorer.headers.tags'), key: 'tags' }
])

const scoreMatch = (item: ContentCatalogItem, needle: string) => {
  if (!needle) {
    return 0
  }

  const title = item.title.toLowerCase()
  const description = `${item.description} ${item.content}`.toLowerCase()
  const tagText = item.tags.join(' ').toLowerCase()

  let score = 0

  if (title.includes(needle)) {
    score += 100
  }

  if (description.includes(needle)) {
    score += 40
  }

  if (tagText.includes(needle)) {
    score += 20
  }

  return score
}

const rankedItems = computed(() => {
  const needle = query.value.trim().toLowerCase()

  return [...props.items]
    .map((item) => ({ item, score: scoreMatch(item, needle) }))
    .filter(({ score }) => (needle ? score > 0 : true))
    .sort((left, right) => right.score - left.score || left.item.slug.localeCompare(right.item.slug))
    .map(({ item }) => item)
})
</script>

<template>
  <v-card variant="outlined">
    <v-card-title class="d-flex align-center ga-3">
      <v-icon icon="mdi-table-search" />
      <span>{{ t('content.explorer.index') }}</span>
      <v-spacer />
      <v-text-field
        v-model="query"
        hide-details
        density="compact"
        clearable
        prepend-inner-icon="mdi-magnify"
        style="max-width: 320px"
        :placeholder="t('content.explorer.search_placeholder')"
      />
    </v-card-title>
    <v-data-table :headers="headers" :items="rankedItems" :items-per-page="8" density="comfortable">
      <template #[`item.title`]="{ item }">
        <NuxtLink :to="item.path">{{ item.title }}</NuxtLink>
      </template>
      <template #[`item.slug`]="{ item }">
        <code>/{{ item.slug }}</code>
      </template>
      <template #[`item.tags`]="{ item }">
        <div class="d-flex ga-1 flex-wrap">
          <v-chip v-for="tag in item.tags" :key="tag" size="x-small" variant="tonal">{{ tag }}</v-chip>
        </div>
      </template>
    </v-data-table>
  </v-card>
</template>

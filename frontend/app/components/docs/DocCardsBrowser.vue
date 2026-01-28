<template>
  <section class="docs-cards">
    <div class="docs-cards__toolbar">
      <v-text-field
        v-if="searchEnabled"
        v-model="query"
        :label="t('docs.search.label')"
        :placeholder="t('docs.search.placeholder')"
        prepend-inner-icon="mdi-magnify"
        clearable
        hide-details="auto"
        class="docs-cards__search"
      />
    </div>

    <DocBrowserFilters
      :tags="tagOptions"
      :selected-tags="selectedTags"
      :tag-filter-enabled="tagFilterEnabled"
      :result-count="filteredDocs.length"
      :search-pending="searchPending"
      @update:selected-tags="selectedTags = $event"
    />

    <div v-if="groupedDocs.length === 0" class="docs-cards__empty">
      <v-alert type="info" variant="tonal">
        {{ t('docs.search.empty') }}
      </v-alert>
    </div>

    <div v-else class="docs-cards__groups">
      <div
        v-for="group in groupedDocs"
        :key="group.title"
        class="docs-cards__group"
      >
        <h3 v-if="group.title" class="docs-cards__group-title">
          {{ group.title }}
        </h3>
        <v-row dense>
          <v-col
            v-for="doc in group.items"
            :key="doc.path"
            cols="12"
            md="6"
            lg="4"
          >
            <v-card
              :to="doc.path"
              class="docs-cards__card"
              variant="outlined"
              rounded="xl"
            >
              <v-card-title class="docs-cards__card-title">
                <v-icon :icon="doc.icon || 'mdi-file-document-outline'" />
                <span>{{ doc.title }}</span>
              </v-card-title>
              <v-card-text class="docs-cards__card-text">
                <p class="docs-cards__description">
                  {{ doc.description || t('docs.labels.noDescription') }}
                </p>
                <div class="docs-cards__tags">
                  <v-chip
                    v-for="tag in doc.tags"
                    :key="tag"
                    size="x-small"
                    variant="tonal"
                    color="primary"
                  >
                    {{ tag }}
                  </v-chip>
                </div>
              </v-card-text>
              <v-divider />
              <v-card-actions class="docs-cards__actions">
                <span class="docs-cards__path">
                  {{ getRelativePath(doc.path) }}
                </span>
                <v-spacer />
                <v-tooltip location="top">
                  <template #activator="{ props: tooltipProps }">
                    <v-btn
                      icon
                      variant="text"
                      color="primary"
                      v-bind="tooltipProps"
                      :aria-label="t('docs.labels.metaTooltip')"
                    >
                      <v-icon icon="mdi-information-outline" />
                    </v-btn>
                  </template>
                  <div class="docs-cards__tooltip">
                    <p>
                      <strong>{{ t('docs.labels.updated') }}:</strong>
                      {{ doc.updatedAt || t('docs.labels.unknown') }}
                    </p>
                    <p>
                      <strong>{{ t('docs.labels.path') }}:</strong>
                      {{ doc.path }}
                    </p>
                  </div>
                </v-tooltip>
              </v-card-actions>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'

import DocBrowserFilters from '~/components/docs/DocBrowserFilters.vue'
import {
  normalizeBasePath,
  normalizeDocsLocale,
  resolveLocaleFromRequest,
  useDocsContent,
  type DocsDoc,
  type DocsLocale,
} from '~/composables/useDocsContent'

type SearchResult = {
  path: string
  title: string
  excerpt: string
  score: number
  tags: string[]
}

type DocsGroup = {
  title: string
  items: DocsDoc[]
}

const props = withDefaults(
  defineProps<{
    locale?: DocsLocale
    basePath?: string
    prefilterTags?: string[]
    searchEnabled?: boolean
    tagFilterEnabled?: boolean
    sort?: 'weight' | 'title' | 'updatedAt' | 'path'
    groupBy?: 'none' | 'directory' | 'tag'
    safeLinks?: boolean
  }>(),
  {
    locale: undefined,
    basePath: '/docs',
    prefilterTags: () => [],
    searchEnabled: true,
    tagFilterEnabled: true,
    sort: 'weight',
    groupBy: 'directory',
    safeLinks: false,
  }
)

const { t } = useI18n()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const { listDocs } = useDocsContent()

const query = ref('')
const debouncedQuery = ref('')
const selectedTags = ref<string[]>([])

const resolvedLocale = computed(() =>
  normalizeDocsLocale(props.locale ?? resolveLocaleFromRequest())
)
const resolvedBasePath = computed(() => normalizeBasePath(props.basePath))

const { data: docsList } = await useAsyncData(
  () => `docs-cards-list:${resolvedLocale.value}`,
  () =>
    listDocs({
      locale: resolvedLocale.value,
      basePath: resolvedBasePath.value,
    })
)

const allDocs = computed(() => docsList.value ?? [])

const tagOptions = computed(() => {
  const tags = new Set<string>()
  allDocs.value.forEach(doc => (doc.tags ?? []).forEach(tag => tags.add(tag)))
  return Array.from(tags).sort((a, b) => a.localeCompare(b))
})

const updateDebouncedQuery = useDebounceFn((value: string) => {
  debouncedQuery.value = value
}, 300)

watch(query, value => {
  updateDebouncedQuery(value.trim())
})

const activeTags = computed(() => [
  ...new Set([...props.prefilterTags, ...selectedTags.value]),
])

const { data: searchResults, pending: searchPending } = await useAsyncData(
  () => `docs-cards-search:${resolvedLocale.value}:${debouncedQuery.value}`,
  async () => {
    if (!props.searchEnabled || !debouncedQuery.value) {
      return [] as SearchResult[]
    }

    return $fetch<SearchResult[]>('/api/docs/search', {
      params: {
        locale: resolvedLocale.value,
        basePath: resolvedBasePath.value,
        query: debouncedQuery.value,
        tags: activeTags.value.join(','),
      },
      headers: requestHeaders,
    })
  },
  { watch: [debouncedQuery, activeTags, resolvedLocale, resolvedBasePath] }
)

const filteredDocs = computed(() => {
  const docs = allDocs.value

  if (debouncedQuery.value && searchResults.value?.length) {
    const paths = new Set(searchResults.value.map(result => result.path))
    return docs.filter(doc => paths.has(doc.path))
  }

  if (activeTags.value.length === 0) {
    return docs
  }

  return docs.filter(doc =>
    activeTags.value.every(tag => doc.tags?.includes(tag))
  )
})

const sortedDocs = computed(() => {
  const docs = [...filteredDocs.value]

  switch (props.sort) {
    case 'title':
      return docs.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? ''))
    case 'updatedAt':
      return docs.sort((a, b) =>
        (b.updatedAt ?? '').localeCompare(a.updatedAt ?? '')
      )
    case 'path':
      return docs.sort((a, b) => a.path.localeCompare(b.path))
    default:
      return docs
  }
})

const groupByDirectory = (docs: DocsDoc[]): DocsGroup[] => {
  const groups = new Map<string, DocsDoc[]>()
  docs.forEach(doc => {
    const relativePath = getRelativePath(doc.path)
    const directory = relativePath.split('/').slice(0, -1).join('/') || '/'
    const items = groups.get(directory) ?? []
    items.push(doc)
    groups.set(directory, items)
  })

  return Array.from(groups.entries()).map(([title, items]) => ({
    title,
    items,
  }))
}

const groupByTag = (docs: DocsDoc[]): DocsGroup[] => {
  const groups = new Map<string, DocsDoc[]>()
  docs.forEach(doc => {
    const tags = doc.tags?.length ? doc.tags : [t('docs.labels.untagged')]
    tags.forEach(tag => {
      const items = groups.get(tag) ?? []
      items.push(doc)
      groups.set(tag, items)
    })
  })

  return Array.from(groups.entries()).map(([title, items]) => ({
    title,
    items,
  }))
}

const groupedDocs = computed<DocsGroup[]>(() => {
  const docs = sortedDocs.value

  if (props.groupBy === 'none') {
    return [{ title: '', items: docs }]
  }

  if (props.groupBy === 'tag') {
    return groupByTag(docs)
  }

  return groupByDirectory(docs)
})

const getRelativePath = (path: string) =>
  path.replace(`${resolvedBasePath.value}/${resolvedLocale.value}/`, '')
</script>

<style scoped>
.docs-cards {
  display: grid;
  gap: 1.5rem;
}

.docs-cards__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.docs-cards__group {
  display: grid;
  gap: 1rem;
}

.docs-cards__group-title {
  font-size: 1.1rem;
  font-weight: 600;
}

.docs-cards__card {
  height: 100%;
}

.docs-cards__card-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.docs-cards__description {
  margin-bottom: 0.75rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.docs-cards__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.docs-cards__actions {
  display: flex;
  align-items: center;
  width: 100%;
}

.docs-cards__path {
  font-size: 0.75rem;
  color: rgb(var(--v-theme-text-neutral-soft));
}

.docs-cards__tooltip {
  max-width: 240px;
}
</style>

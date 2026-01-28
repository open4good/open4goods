<template>
  <section class="docs-browser">
    <div class="docs-browser__toolbar">
      <v-text-field
        v-if="searchEnabled"
        v-model="query"
        :label="t('docs.search.label')"
        :placeholder="t('docs.search.placeholder')"
        prepend-inner-icon="mdi-magnify"
        clearable
        hide-details="auto"
        class="docs-browser__search"
        density="comfortable"
      />

      <v-btn
        v-if="!mdAndUp"
        variant="tonal"
        color="primary"
        prepend-icon="mdi-menu"
        class="docs-browser__toggle"
        @click="drawer = !drawer"
      >
        {{ t('docs.browser.treeToggle') }}
      </v-btn>
    </div>

    <div class="docs-browser__layout">
      <v-navigation-drawer
        v-if="!mdAndUp"
        v-model="drawer"
        temporary
        location="left"
        width="320"
      >
        <div class="docs-browser__panel">
          <DocBrowserFilters
            :tags="tagOptions"
            :selected-tags="selectedTags"
            :tag-filter-enabled="tagFilterEnabled"
            :result-count="filteredDocs.length"
            :search-pending="searchPending"
            @update:selected-tags="selectedTags = $event"
          />

          <v-list density="compact" nav>
            <DocsTreeItem
              v-for="node in filteredTree"
              :key="node.id"
              :node="node"
              :selected-path="selectedPath"
              :density="density"
              @select="handleSelect"
            />
          </v-list>
        </div>
      </v-navigation-drawer>

      <div v-if="mdAndUp" class="docs-browser__sidebar">
        <DocBrowserFilters
          :tags="tagOptions"
          :selected-tags="selectedTags"
          :tag-filter-enabled="tagFilterEnabled"
          :result-count="filteredDocs.length"
          :search-pending="searchPending"
          @update:selected-tags="selectedTags = $event"
        />

        <v-list density="compact" nav class="docs-browser__tree">
          <DocsTreeItem
            v-for="node in filteredTree"
            :key="node.id"
            :node="node"
            :selected-path="selectedPath"
            :density="density"
            @select="handleSelect"
          />
        </v-list>
      </div>

      <div class="docs-browser__reader">
        <div class="docs-browser__results" aria-live="polite">
          {{ t('docs.search.results', { count: filteredDocs.length }) }}
        </div>

        <DocsInlineRenderer
          v-if="selectedPath"
          :slug-or-path="selectedPath"
          :locale="resolvedLocale"
          :base-path="resolvedBasePath"
          :safe-links="safeLinks"
          render-h1
        />

        <v-alert v-else type="info" variant="tonal" class="docs-browser__empty">
          {{ t('docs.search.empty') }}
        </v-alert>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'

import DocBrowserFilters from '~/components/docs/DocBrowserFilters.vue'
import DocsInlineRenderer from '~/components/docs/DocsInlineRenderer.vue'
import DocsTreeItem from '~/components/docs/DocsTreeItem.vue'
import {
  normalizeBasePath,
  normalizeDocsLocale,
  resolveLocaleFromRequest,
  useDocsContent,
  type DocsDoc,
  type DocsLocale,
  type DocsNavigationNode,
} from '~/composables/useDocsContent'

type SearchResult = {
  path: string
  title: string
  excerpt: string
  score: number
  tags: string[]
}

const props = withDefaults(
  defineProps<{
    locale?: DocsLocale
    basePath?: string
    prefilterTags?: string[]
    initialDoc?: string
    searchEnabled?: boolean
    tagFilterEnabled?: boolean
    mode?: 'tree'
    density?: 'comfortable' | 'compact'
    safeLinks?: boolean
  }>(),
  {
    locale: undefined,
    basePath: '/docs',
    prefilterTags: () => [],
    initialDoc: undefined,
    searchEnabled: true,
    tagFilterEnabled: true,
    mode: 'tree',
    density: 'comfortable',
    safeLinks: false,
  }
)

const { t } = useI18n()
const { mdAndUp } = useDisplay()
const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])
const { getNavigationTree, listDocs } = useDocsContent()

const drawer = ref(false)
const query = ref('')
const debouncedQuery = ref('')
const selectedTags = ref<string[]>([])
const selectedPath = ref<string | null>(props.initialDoc ?? null)

const resolvedLocale = computed(() =>
  normalizeDocsLocale(props.locale ?? resolveLocaleFromRequest())
)
const resolvedBasePath = computed(() => normalizeBasePath(props.basePath))

const { data: navigationTree } = await useAsyncData(
  () => `docs-browser-tree:${resolvedLocale.value}`,
  () =>
    getNavigationTree({
      locale: resolvedLocale.value,
      basePath: resolvedBasePath.value,
    })
)

const { data: docsList } = await useAsyncData(
  () => `docs-browser-list:${resolvedLocale.value}`,
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
  () => `docs-browser-search:${resolvedLocale.value}:${debouncedQuery.value}`,
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
  {
    watch: [debouncedQuery, activeTags, resolvedLocale, resolvedBasePath],
  }
)

const filteredDocs = computed<DocsDoc[]>(() => {
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

const allowedPaths = computed(
  () => new Set(filteredDocs.value.map(doc => doc.path))
)

const filterTree = (node: DocsNavigationNode): DocsNavigationNode | null => {
  const filteredChildren = node.children
    .map(child => filterTree(child))
    .filter(Boolean) as DocsNavigationNode[]

  if (node.path && allowedPaths.value.has(node.path)) {
    return { ...node, children: filteredChildren }
  }

  if (filteredChildren.length > 0) {
    return { ...node, children: filteredChildren }
  }

  return null
}

const filteredTree = computed(() => {
  const tree = navigationTree.value

  if (!tree) {
    return []
  }

  const root = filterTree(tree)
  return root?.children ?? []
})

watch(
  () => filteredDocs.value,
  docs => {
    if (docs.length === 0) {
      selectedPath.value = null
      return
    }

    if (!selectedPath.value || !allowedPaths.value.has(selectedPath.value)) {
      selectedPath.value = docs[0]?.path ?? null
    }
  },
  { immediate: true }
)

const handleSelect = (path: string) => {
  if (!allowedPaths.value.has(path)) {
    return
  }

  selectedPath.value = path
  drawer.value = false
}
</script>

<style scoped>
.docs-browser {
  display: grid;
  gap: 1.5rem;
}

.docs-browser__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
}

.docs-browser__layout {
  display: grid;
  gap: 1.5rem;
}

@media (min-width: 960px) {
  .docs-browser__layout {
    grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
    align-items: start;
  }
}

.docs-browser__sidebar {
  background: rgb(var(--v-theme-surface-default));
  border-radius: 20px;
  padding: 1.25rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
}

.docs-browser__reader {
  display: grid;
  gap: 1.5rem;
}

.docs-browser__results {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.docs-browser__empty {
  border-radius: 16px;
}
</style>

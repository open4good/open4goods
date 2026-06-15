<script setup lang="ts">
import type { ContentCatalogItem, ContentScope } from '~/composables/useContentCatalog'

const props = withDefaults(
  defineProps<{
    sectionPrefix?: string
    scope?: ContentScope
    mode?: 'default' | 'slug'
    showLocaleSelector?: boolean
  }>(),
  {
    sectionPrefix: '',
    scope: 'public',
    mode: 'default',
    showLocaleSelector: false
  }
)

const { t, locale, locales } = useI18n()
const switchLocalePath = useSwitchLocalePath()
const { items, pending } = useContentCatalog()
const selectedPath = ref<string | null>(null)
const query = ref('')
const selectedTags = ref<string[]>([])
const isSidebarOpen = ref(true)
const isMobileDrawerOpen = ref(false)

const baseFilteredItems = computed(() => {
  const prefix = props.sectionPrefix

  return (items.value || []).filter((item: ContentCatalogItem) => {
    if (prefix && !item.slug.startsWith(prefix)) {
      return false
    }

    if (props.scope === 'public' && item.scope !== 'public') {
      return false
    }

    if (props.scope === 'admin' && item.scope !== 'admin') {
      return false
    }

    return true
  })
})

const visibleItems = computed(() => {
  if (selectedTags.value.length === 0) {
    return baseFilteredItems.value
  }

  return baseFilteredItems.value.filter((item: ContentCatalogItem) => selectedTags.value.every((tag) => item.tags.includes(tag)))
})

const tagCounts = computed(() => {
  const counts = new Map<string, number>()

  for (const item of baseFilteredItems.value) {
    for (const tag of item.tags) {
      counts.set(tag, (counts.get(tag) || 0) + 1)
    }
  }

  return counts
})

const allTags = computed(() => [...tagCounts.value.keys()].sort((left, right) => left.localeCompare(right)))

const toggleTag = (tag: string) => {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter((currentTag) => currentTag !== tag)
    return
  }

  selectedTags.value = [...selectedTags.value, tag]
}

const formatCount = (count: number) => new Intl.NumberFormat(locale.value).format(count)

const localeItems = computed(() =>
  locales.value
    .map((localeEntry) => {
      const localeItem = localeEntry as { code: string, name?: string }

      return {
        code: localeItem.code,
        label: localeItem.name || localeItem.code.toUpperCase()
      }
    })
    .filter((item) => ['fr', 'en'].includes(item.code))
)

const toggleSidebar = () => {
  if (!isSidebarOpen.value) {
    isSidebarOpen.value = true
    return
  }

  isSidebarOpen.value = false
}

const onLocaleChange = (nextLocale: string) => {
  if (nextLocale === locale.value) {
    return
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const targetPath = switchLocalePath(nextLocale as any)
  if (targetPath) {
    navigateTo(targetPath)
  }
}
</script>

<template>
  <v-card class="mb-6" variant="outlined">
    <v-card-title class="d-flex align-center ga-2">
      <v-icon icon="mdi-book-open-page-variant-outline" />
      <span>{{ t('content.explorer.title') }}</span>
      <v-spacer />
      <v-select
        v-if="showLocaleSelector"
        :model-value="locale"
        :items="localeItems"
        item-title="label"
        item-value="code"
        density="compact"
        variant="outlined"
        hide-details
        prepend-inner-icon="mdi-translate"
        class="inf-locale-select"
        @update:model-value="onLocaleChange"
      />
      <v-btn
        v-if="mode === 'slug'"
        variant="text"
        size="small"
        prepend-icon="mdi-dock-left"
        @click="toggleSidebar"
      >
        {{ isSidebarOpen ? t('content.explorer.collapse') : t('content.explorer.expand') }}
      </v-btn>
    </v-card-title>
    <v-card-text>
      <v-skeleton-loader v-if="pending" type="list-item-two-line, table" />

      <template v-else>
        <div class="mb-4 d-flex ga-2 flex-wrap">
          <v-chip
            v-for="tag in allTags"
            :key="tag"
            size="small"
            :color="selectedTags.includes(tag) ? 'primary' : undefined"
            :variant="selectedTags.includes(tag) ? 'flat' : 'tonal'"
            prepend-icon="mdi-tag-outline"
            @click="toggleTag(tag)"
          >
            {{ tag }} ({{ formatCount(tagCounts.get(tag) || 0) }})
          </v-chip>
        </div>

        <div v-if="mode === 'slug'" class="slug-layout">
          <v-btn
            class="d-md-none mb-3"
            variant="tonal"
            prepend-icon="mdi-menu"
            @click="isMobileDrawerOpen = true"
          >
            {{ t('content.explorer.navigation') }}
          </v-btn>

          <v-expand-x-transition>
            <div v-if="isSidebarOpen" class="slug-layout__sidebar d-none d-md-block">
              <InfContentTree v-model:selected="selectedPath" :items="visibleItems" />
            </div>
          </v-expand-x-transition>

          <v-navigation-drawer
            v-model="isMobileDrawerOpen"
            location="left"
            temporary
            class="d-md-none"
          >
            <div class="pa-2">
              <InfContentTree v-model:selected="selectedPath" :items="visibleItems" />
            </div>
          </v-navigation-drawer>
        </div>

        <v-row v-else>
          <v-col cols="12" md="4">
            <InfContentTree v-model:selected="selectedPath" :items="visibleItems" />
          </v-col>
          <v-col cols="12" md="8">
            <InfContentSearchTable v-model:query="query" :items="visibleItems" />
          </v-col>
        </v-row>
      </template>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.inf-locale-select {
  max-width: 190px;
}

.slug-layout {
  position: relative;
}

.slug-layout__sidebar {
  max-width: 420px;
}
</style>

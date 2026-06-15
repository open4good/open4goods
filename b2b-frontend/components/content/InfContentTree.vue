<script setup lang="ts">
import type { ContentCatalogItem } from '~/composables/useContentCatalog'

const props = defineProps<{
  items: ContentCatalogItem[]
}>()

const selectedPath = defineModel<string | null>('selected', { default: null })
const { t } = useI18n()

const treeRows = computed(() =>
  props.items.map((item) => ({
    ...item,
    depth: Math.max(item.slug.split('/').length - 1, 0)
  }))
)

const goTo = (path: string) => {
  selectedPath.value = path
  navigateTo(path)
}
</script>

<template>
  <v-card variant="tonal">
    <v-card-title class="text-subtitle-1">{{ t('content.explorer.documentation') }}</v-card-title>
    <v-list density="compact" nav>
      <v-list-item
        v-for="row in treeRows"
        :key="row.path"
        :active="selectedPath === row.path"
        @click="goTo(row.path)"
      >
        <template #prepend>
          <v-icon size="16" icon="mdi-file-document-outline" :style="{ marginLeft: `${row.depth * 12}px` }" />
        </template>
        <v-list-item-title>{{ row.title }}</v-list-item-title>
        <v-list-item-subtitle class="text-caption">/{{ row.slug }}</v-list-item-subtitle>
      </v-list-item>
    </v-list>
  </v-card>
</template>

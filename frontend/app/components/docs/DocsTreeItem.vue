<template>
  <v-list-group
    v-if="hasChildren"
    :value="isExpanded"
    class="docs-tree-item__group"
  >
    <template #activator="{ props: activatorProps }">
      <v-list-item
        v-bind="activatorProps"
        :title="node.title"
        :prepend-icon="node.icon || 'mdi-folder-outline'"
        class="docs-tree-item__title"
      />
    </template>

    <DocsTreeItem
      v-for="child in node.children"
      :key="child.id"
      :node="child"
      :selected-path="selectedPath"
      :density="density"
      @select="emit('select', $event)"
    />
  </v-list-group>

  <v-list-item
    v-else
    :title="node.title"
    :prepend-icon="node.icon || 'mdi-file-document-outline'"
    :active="node.path === selectedPath"
    class="docs-tree-item__leaf"
    :density="density"
    @click="handleSelect"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { DocsNavigationNode } from '~/composables/useDocsContent'

const props = defineProps<{
  node: DocsNavigationNode
  selectedPath?: string | null
  density?: 'comfortable' | 'compact'
}>()

const emit = defineEmits<{
  (event: 'select', path: string): void
}>()

const hasChildren = computed(() => props.node.children.length > 0)
const isExpanded = computed(() => true)

const handleSelect = () => {
  if (props.node.path) {
    emit('select', props.node.path)
  }
}
</script>

<style scoped>
.docs-tree-item__group {
  --v-list-item-title-font-size: 0.95rem;
}
</style>

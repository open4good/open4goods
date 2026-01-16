<template>
  <div class="doc-browser-filters">
    <div class="doc-browser-filters__header">
      <h3 class="text-subtitle-1 font-weight-semibold">
        {{ t('docs.search.filtersTitle') }}
      </h3>
      <v-progress-circular
        v-if="searchPending"
        indeterminate
        size="20"
        width="2"
        color="primary"
        :aria-label="t('docs.search.loading')"
      />
    </div>

    <div
      class="doc-browser-filters__results"
      aria-live="polite"
      role="status"
    >
      {{ t('docs.search.results', { count: resultCount }) }}
    </div>

    <div v-if="tagFilterEnabled" class="doc-browser-filters__tags">
      <p class="text-body-2 font-weight-semibold mb-2">
        {{ t('docs.search.tagsLabel') }}
      </p>
      <v-chip-group
        v-model="internalSelectedTags"
        multiple
        column
        class="doc-browser-filters__chips"
      >
        <v-chip
          v-for="tag in tags"
          :key="tag"
          :value="tag"
          size="small"
          variant="tonal"
          color="primary"
        >
          {{ tag }}
        </v-chip>
      </v-chip-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{
  tags: string[]
  selectedTags: string[]
  tagFilterEnabled: boolean
  resultCount: number
  searchPending: boolean
}>()

const emit = defineEmits<{
  (event: 'update:selectedTags', value: string[]): void
}>()

const { t } = useI18n()

const internalSelectedTags = computed({
  get: () => props.selectedTags,
  set: value => emit('update:selectedTags', value),
})
</script>

<style scoped>
.doc-browser-filters {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.doc-browser-filters__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.doc-browser-filters__results {
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>

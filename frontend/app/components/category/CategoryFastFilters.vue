<template>
  <section v-if="subsets.length" class="category-fast-filters">
    <header class="category-fast-filters__header">
      <h2 class="category-fast-filters__title">
        {{ t('category.fastFilters.title') }}
      </h2>
      <v-btn
        v-if="activeSubsetIds.length"
        color="primary"
        variant="text"
        class="category-fast-filters__reset"
        size="small"
        @click="$emit('reset')"
      >
        <v-icon icon="mdi-close-circle" size="18" class="me-1" />
        {{ t('category.fastFilters.reset') }}
      </v-btn>
    </header>

    <v-slide-group
      class="category-fast-filters__chips"
      show-arrows
      multiple
      :model-value="activeSubsetIds"
      @update:model-value="onChipToggle"
    >
      <v-chip
        v-for="subset in subsets"
        :key="subset.id ?? subset.caption ?? subset.title ?? subset.group"
        :value="subset.id"
        :color="activeSubsetIds.includes(subset.id ?? '') ? 'primary' : undefined"
        :variant="activeSubsetIds.includes(subset.id ?? '') ? 'flat' : 'outlined'"
        rounded="lg"
        class="me-2 mb-2"
        :closable="activeSubsetIds.includes(subset.id ?? '')"
        @click:close="emitToggle(subset, false)"
      >
        <span class="category-fast-filters__chip-label">
          {{ resolveSubsetLabel(subset) }}
        </span>
      </v-chip>
    </v-slide-group>

    <div v-if="activeClauses.length" class="category-fast-filters__clauses">
      <p class="category-fast-filters__clauses-title">
        {{ t('category.fastFilters.activeClauses') }}
      </p>
      <v-chip-group column>
        <v-chip
          v-for="clause in activeClauses"
          :key="clause.id"
          closable
          variant="text"
          class="category-fast-filters__clause"
          color="primary"
          @click:close="$emit('remove-clause', clause)"
        >
          <v-icon icon="mdi-filter-variant" size="18" class="me-1" />
          <span>{{ clause.label }}</span>
        </v-chip>
      </v-chip-group>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Filter, VerticalSubsetDto } from '~~/shared/api-client'
import { useI18n } from 'vue-i18n'

import { convertSubsetCriteriaToFilters } from '~/utils/_subset-to-filters'

interface ClauseSummary {
  id: string
  subsetId: string
  filter: Filter
  label: string
  index: number
}

const props = defineProps<{
  subsets: VerticalSubsetDto[]
  activeSubsetIds: string[]
}>()

const emit = defineEmits<{
  'toggle-subset': [subsetId: string, active: boolean]
  'remove-clause': [clause: ClauseSummary]
  reset: []
}>()

const loggedSubsets = new Set<string>()

const { t } = useI18n()

const subsets = computed(() => props.subsets ?? [])
const activeSubsetIds = computed(() => props.activeSubsetIds ?? [])

const subsetMap = computed(() => {
  return subsets.value.reduce<Record<string, VerticalSubsetDto>>((accumulator, subset) => {
    if (subset.id) {
      accumulator[subset.id] = subset
    }
    return accumulator
  }, {})
})

const resolveSubsetLabel = (subset: VerticalSubsetDto): string => {
  if (!subset.title && !subset.caption && import.meta.server) {
    const cacheKey = subset.id ?? subset.group ?? 'unknown'
    if (!loggedSubsets.has(cacheKey)) {
      console.error('Category subset is missing a display label.', {
        subsetId: subset.id,
        group: subset.group,
      })
      loggedSubsets.add(cacheKey)
    }
  }

  return subset.title ?? subset.caption ?? subset.id ?? subset.group ?? 'subset'
}

const buildClauseLabel = (subsetId: string, filter: Filter): string => {
  const field = filter.field ?? 'field'

  if (filter.operator === 'term') {
    const term = filter.terms?.[0] ?? ''
    return `${field} = ${term}`
  }

  const bounds: string[] = []
  if (typeof filter.min === 'number') {
    bounds.push(t('category.fastFilters.operator.greaterThan', { value: filter.min }))
  }

  if (typeof filter.max === 'number') {
    bounds.push(t('category.fastFilters.operator.lowerThan', { value: filter.max }))
  }

  if (!bounds.length) {
    return `${field}`
  }

  return `${field}: ${bounds.join(' · ')}`
}

const activeClauses = computed<ClauseSummary[]>(() => {
  return activeSubsetIds.value.flatMap((subsetId) => {
    const subset = subsetMap.value[subsetId]
    if (!subset) {
      return []
    }

    const filters = convertSubsetCriteriaToFilters(subset)

    return filters.map((filter, index) => ({
      id: `${subsetId}-${index}`,
      subsetId,
      filter,
      index,
      label: buildClauseLabel(subsetId, filter),
    }))
  })
})

const emitToggle = (subset: VerticalSubsetDto, desired: boolean) => {
  if (!subset.id) {
    return
  }

  emit('toggle-subset', subset.id, desired)
}

const onChipToggle = (subsetIds: string[]) => {
  const current = new Set(activeSubsetIds.value)
  const next = new Set(subsetIds)

  subsets.value.forEach((subset) => {
    if (!subset.id) {
      return
    }

    const currentlyActive = current.has(subset.id)
    const shouldBeActive = next.has(subset.id)

    if (currentlyActive === shouldBeActive) {
      return
    }

    emitToggle(subset, shouldBeActive)
  })
}

defineExpose({ activeClauses })
</script>

<style scoped lang="sass">
.category-fast-filters
  background-color: rgb(var(--v-theme-surface-glass))
  border-radius: 1rem
  padding: 1.5rem
  margin-bottom: 2rem
  box-shadow: 0 24px 48px -32px rgba(var(--v-theme-shadow-primary-600), 0.3)

  &__header
    display: flex
    align-items: center
    justify-content: space-between
    gap: 1rem
    margin-bottom: 1rem

  &__title
    margin: 0
    font-size: 1.25rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__reset
    text-transform: none

  &__chips
    padding-bottom: 0.5rem

  &__chip-label
    font-weight: 500

  &__clauses
    margin-top: 1rem
    padding-top: 0.75rem
    border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6)

  &__clauses-title
    margin: 0 0 0.5rem
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__clause
    margin-bottom: 0.5rem
    justify-content: flex-start

@media (max-width: 959px)
  .category-fast-filters
    padding: 1.25rem

    &__header
      align-items: flex-start
      flex-direction: column
      gap: 0.5rem

    &__reset
      align-self: flex-end
</style>

<template>
  <div
    v-if="activeChips.length"
    class="category-active-filters"
    data-testid="category-active-filters"
    :aria-label="t('category.filters.activeTitle')"
  >
    <div class="category-active-filters__chips">
      <span class="category-active-filters__label">{{ t('category.filters.activeTitle') }}</span>

      <v-chip
        v-for="chip in activeChips"
        :key="chip.id"
        closable
        color="primary"
        variant="flat"
        size="small"
        class="category-active-filters__chip"
        @click:close="onRemoveChip(chip)"
      >
        <span>{{ chip.label }}</span>
      </v-chip>
    </div>

    <div class="category-active-filters__actions">
      <v-tooltip :text="t('category.filters.clearActive')" location="bottom">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            v-bind="tooltipProps"
            :aria-label="t('category.filters.clearActive')"
            @click="emit('clear-all')"
          >
            <v-icon icon="mdi-close-circle-outline" />
          </v-btn>
        </template>
      </v-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  FieldMetadataDto,
  Filter,
  FilterRequestDto,
  ProductFieldOptionsResponse,
} from '~~/shared/api-client'

import type { CategorySubsetClause } from '~/types/category-subset'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'

type ManualFilterChip = {
  kind: 'manual'
  id: string
  field: string
  type: 'term' | 'range'
  term: string | null
  label: string
}

type SubsetFilterChip = {
  kind: 'subset'
  id: string
  label: string
  clause: CategorySubsetClause
}

type ActiveFilterChip = ManualFilterChip | SubsetFilterChip

const props = withDefaults(
  defineProps<{
    filters: FilterRequestDto | null
    subsetClauses?: CategorySubsetClause[]
    filterOptions: ProductFieldOptionsResponse | null
  }>(),
  {
    subsetClauses: () => [],
  },
)

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'remove-subset-clause': [CategorySubsetClause]
  'clear-all': []
}>()

const activeFilters = computed(() => props.filters?.filters ?? [])
const subsetClauses = computed(() => props.subsetClauses ?? [])

const { t } = useI18n()

const fieldMetadataMap = computed<Record<string, FieldMetadataDto>>(() => {
  const entries = [
    ...(props.filterOptions?.global ?? []),
    ...(props.filterOptions?.impact ?? []),
    ...(props.filterOptions?.technical ?? []),
  ]

  return entries.reduce<Record<string, FieldMetadataDto>>((accumulator, field) => {
    if (field.mapping) {
      accumulator[field.mapping] = field
    }

    return accumulator
  }, {})
})

const manualFilterChips = computed<ManualFilterChip[]>(() => {
  return activeFilters.value.map((filter) => {
    const mapping = filter.field ?? ''
    const metadata = mapping ? fieldMetadataMap.value[mapping] : undefined
    const label = resolveFilterFieldTitle(metadata, t, mapping)

    if (filter.operator === 'term') {
      const term = filter.terms?.[0] ?? ''
      return {
        kind: 'manual' as const,
        id: `${mapping}-${term}`,
        field: mapping,
        type: 'term' as const,
        term,
        label: term ? `${label}: ${term}` : label,
      }
    }

    return {
      kind: 'manual' as const,
      id: `${mapping}-range`,
      field: mapping,
      type: 'range' as const,
      term: null,
      label: `${label}: ${filter.min ?? '–'} → ${filter.max ?? '–'}`,
    }
  })
})

const subsetFilterChips = computed<SubsetFilterChip[]>(() => {
  return subsetClauses.value.map((clause) => ({
    kind: 'subset' as const,
    id: clause.id,
    label: clause.label,
    clause,
  }))
})

const activeChips = computed<ActiveFilterChip[]>(() => {
  return [...subsetFilterChips.value, ...manualFilterChips.value]
})

const emitFilters = (filters: Filter[]) => {
  emit('update:filters', filters.length ? { filters } : {})
}

const removeManualFilter = (field: string, type: 'term' | 'range', term: string | null) => {
  const next = activeFilters.value.filter((filter) => {
    if (filter.field !== field) {
      return true
    }

    if (type === 'term') {
      return !(filter.operator === 'term' && filter.terms?.includes(term ?? ''))
    }

    return !(filter.operator === 'range')
  })

  emitFilters(next)
}

const onRemoveChip = (chip: ActiveFilterChip) => {
  if (chip.kind === 'subset') {
    emit('remove-subset-clause', chip.clause)
    return
  }

  removeManualFilter(chip.field, chip.type, chip.term)
}
</script>

<style scoped lang="sass">
.category-active-filters
  display: flex
  flex-wrap: wrap
  align-items: flex-start
  gap: 0.75rem
  padding: 0.75rem
  border-radius: 0.75rem
  background: rgb(var(--v-theme-surface-glass))

  &__chips
    flex: 1 1 auto
    display: flex
    flex-wrap: wrap
    align-items: center
    gap: 0.5rem

  &__label
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__chip
    --v-chip-height: 32px

  &__actions
    flex: 0 0 auto
    display: flex
    align-items: center
    margin-left: auto

@media (max-width: 599px)
  .category-active-filters
    padding: 0.5rem 0.75rem

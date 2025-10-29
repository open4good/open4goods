<template>
  <div class="category-active-filters" data-testid="category-active-filters">
    <div class="category-active-filters__chips">
      <v-chip
        v-for="chip in chips"
        :key="chip.id"
        class="category-active-filters__chip"
        closable
        color="primary"
        variant="flat"
        size="small"
        density="comfortable"
        @click:close="onRemoveChip(chip)"
      >
        <span>{{ chip.label }}</span>
      </v-chip>
    </div>

    <div class="category-active-filters__actions">
      <v-tooltip :text="dismissAllTooltip" location="bottom" :open-delay="125">
        <template #activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            density="comfortable"
            class="category-active-filters__dismiss"
            :aria-label="dismissAllLabel"
            v-bind="tooltipProps"
            @click="onClearAll"
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

type ManualRemovalPayload = {
  field: string
  type: 'term' | 'range'
  term: string | null
}

const props = defineProps<{
  filterOptions: ProductFieldOptionsResponse | null
  filters: FilterRequestDto | null
  subsetClauses: CategorySubsetClause[]
}>()

const emit = defineEmits<{
  'remove-manual-filter': [ManualRemovalPayload]
  'remove-subset-clause': [CategorySubsetClause]
  'clear-all': []
}>()

const { t } = useI18n()

const activeFilters = computed(() => props.filters?.filters ?? [])
const subsetClauses = computed(() => props.subsetClauses ?? [])

const fieldMetadataMap = computed<Record<string, FieldMetadataDto>>(() => {
  const entries = [
    ...(props.filterOptions?.global ?? []),
    ...(props.filterOptions?.impact ?? []),
    ...(props.filterOptions?.technical ?? []),
  ]

  return entries.reduce<Record<string, FieldMetadataDto>>((accumulator, field) => {
    if (field.mapping) {
      accumulator[String(field.mapping)] = field
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

const chips = computed<ActiveFilterChip[]>(() => {
  return [...subsetFilterChips.value, ...manualFilterChips.value]
})

const dismissAllLabel = computed(() => t('category.filters.clearAll'))
const dismissAllTooltip = computed(() => t('category.filters.tooltips.clearAll'))

const onRemoveChip = (chip: ActiveFilterChip) => {
  if (chip.kind === 'subset') {
    emit('remove-subset-clause', chip.clause)
    return
  }

  emit('remove-manual-filter', {
    field: chip.field,
    type: chip.type,
    term: chip.term,
  })
}

const onClearAll = () => {
  emit('clear-all')
}

const exposeChips = computed(() => chips.value)

defineExpose({
  chips: exposeChips,
})
</script>

<style scoped lang="sass">
.category-active-filters
  display: flex
  align-items: stretch
  gap: 0.75rem
  padding: 0.75rem 1rem
  border-radius: 1rem
  background: rgba(var(--v-theme-surface-primary-080), 0.9)
  box-shadow: 0 18px 38px -28px rgba(var(--v-theme-shadow-primary-600), 0.25)

  &__chips
    flex: 1 1 auto
    display: flex
    flex-wrap: wrap
    gap: 0.5rem

  &__chip
    border-radius: 999px

  &__actions
    flex: 0 0 auto
    display: flex
    align-items: center

  &__dismiss
    color: rgb(var(--v-theme-primary))

  @media (max-width: 959px)
    flex-direction: column
    align-items: stretch

    &__actions
      justify-content: flex-end
</style>

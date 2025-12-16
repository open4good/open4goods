<template>
  <div
    v-if="activeChips.length"
    class="category-active-filters"
    data-testid="category-active-filters"
  >
    <div class="category-active-filters__header">
      <span class="category-active-filters__title">
        {{ t('category.filters.activeTitle') }}
      </span>

      <v-tooltip
        :text="t('category.filters.clearAllTooltip')"
        location="bottom"
      >
        <template #activator="{ props: tooltipProps }">
          <v-btn
            icon
            variant="text"
            color="primary"
            class="category-active-filters__clear"
            :aria-label="t('category.filters.clearAllTooltip')"
            v-bind="tooltipProps"
            @click="emit('clear-all')"
          >
            <v-icon icon="mdi-close-circle-outline" />
          </v-btn>
        </template>
      </v-tooltip>
    </div>

    <div class="category-active-filters__chips">
      <v-chip
        v-for="chip in activeChips"
        :key="chip.id"
        closable
        :color="chip.kind === 'manual' ? 'primary' : undefined"
        :variant="chip.kind === 'manual' ? 'flat' : 'outlined'"
        size="small"
        class="category-active-filters__chip"
        :data-kind="chip.kind"
        @click:close="onRemoveChip(chip)"
      >
        <span>{{ chip.label }}</span>
      </v-chip>
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
} from '~~/shared/api-client'

import type { CategorySubsetClause } from '~/types/category-subset'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'
import { formatNumericRangeValue } from '~/utils/_number-formatting'
import { isPriceField } from './filters/price-scale'

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

const props = defineProps<{
  filters: FilterRequestDto | null
  subsetClauses: CategorySubsetClause[]
  fieldMetadata: Record<string, FieldMetadataDto>
}>()

const emit = defineEmits<{
  'remove-filter': [field: string, type: 'term' | 'range', term: string | null]
  'remove-subset-clause': [CategorySubsetClause]
  'clear-all': []
}>()

const { t, n } = useI18n()

const activeFilters = computed(() => props.filters?.filters ?? [])
const subsetClauses = computed(() => props.subsetClauses ?? [])
const metadata = computed(() => props.fieldMetadata ?? {})

const formatChipBoundary = (
  value: number | string | null | undefined,
  isPrice: boolean
) => {
  return formatNumericRangeValue(value, n, { isPrice })
}

const createManualLabel = (filter: Filter) => {
  const mapping = filter.field ?? ''
  const fieldLabel = resolveFilterFieldTitle(
    metadata.value[mapping],
    t,
    mapping
  )

  if (filter.operator === 'term') {
    const term = filter.terms?.[0] ?? ''
    return term ? `${fieldLabel}: ${term}` : fieldLabel
  }

  const priceField = isPriceField(mapping)
  const minLabel = formatChipBoundary(filter.min ?? null, priceField)
  const maxLabel = formatChipBoundary(filter.max ?? null, priceField)
  return `${fieldLabel}: ${minLabel} → ${maxLabel}`
}

const manualFilterChips = computed<ManualFilterChip[]>(() => {
  return activeFilters.value.map(filter => {
    const mapping = filter.field ?? ''

    if (filter.operator === 'term') {
      const term = filter.terms?.[0] ?? ''
      return {
        kind: 'manual' as const,
        id: `${mapping}-${term}`,
        field: mapping,
        type: 'term' as const,
        term,
        label: createManualLabel(filter),
      }
    }

    return {
      kind: 'manual' as const,
      id: `${mapping}-range`,
      field: mapping,
      type: 'range' as const,
      term: null,
      label: createManualLabel(filter),
    }
  })
})

const subsetFilterChips = computed<SubsetFilterChip[]>(() => {
  return subsetClauses.value.map(clause => ({
    kind: 'subset' as const,
    id: clause.id,
    label: clause.label,
    clause,
  }))
})

const activeChips = computed<ActiveFilterChip[]>(() => {
  return [...subsetFilterChips.value, ...manualFilterChips.value]
})

const onRemoveChip = (chip: ActiveFilterChip) => {
  if (chip.kind === 'subset') {
    emit('remove-subset-clause', chip.clause)
    return
  }

  emit('remove-filter', chip.field, chip.type, chip.term)
}
</script>

<style scoped lang="sass">
.category-active-filters
  display: flex
  flex-direction: column
  gap: 0.75rem
  padding: 0.75rem 1rem
  background: rgb(var(--v-theme-surface-glass))
  border-radius: 0.75rem

  &__header
    display: flex
    align-items: center
    justify-content: space-between
    gap: 0.75rem

  &__title
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__chips
    display: flex
    flex-wrap: wrap
    gap: 0.5rem

  &__chip
    --v-chip-padding-x: 0.75rem

  &__clear
    margin-left: auto

@media (max-width: 599px)
  .category-active-filters
    padding: 0.75rem
</style>

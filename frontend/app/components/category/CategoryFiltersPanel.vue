<template>
  <div class="category-filters" data-testid="category-filters">
    <div v-if="activeChips.length" class="category-filters__active">
      <div class="category-filters__chips">
        <v-chip
          v-for="chip in activeChips"
          :key="chip.id"
          closable
          color="primary"
          variant="flat"
          size="small"
          @click:close="onRemoveChip(chip)"
        >
          <span>{{ chip.label }}</span>
        </v-chip>
      </div>
    </div>

    <v-expansion-panels multiple class="category-filters__panels">
      <v-expansion-panel value="global" expand-icon="mdi-chevron-down">
        <template #title>
          <div class="category-filters__title">
            <v-icon icon="mdi-tune-variant" size="20" class="category-filters__title-icon" />
            <span>{{ t('category.filters.globalTitle') }}</span>
          </div>
        </template>
        <template #text>
          <div class="category-filters__section">
            <CategoryFilterList
              :fields="filterOptions?.global ?? []"
              :aggregations="aggregationMap"
              :active-filters="activeFilters"
              @update-range="updateRangeFilter"
              @update-terms="updateTermsFilter"
            />
          </div>
        </template>
      </v-expansion-panel>

      <v-expansion-panel value="impact" expand-icon="mdi-chevron-down">
        <template #title>
          <div class="category-filters__title">
            <v-icon icon="mdi-leaf" size="20" class="category-filters__title-icon" />
            <span>{{ t('category.filters.impactTitle') }}</span>
          </div>
        </template>
        <template #text>
          <div class="category-filters__section">
            <CategoryFilterList
              :fields="impactPrimary"
              :aggregations="aggregationMap"
              :active-filters="activeFilters"
              @update-range="updateRangeFilter"
              @update-terms="updateTermsFilter"
            />

            <div v-if="impactRemaining.length" class="category-filters__see-more">
              <v-btn
                variant="text"
                density="comfortable"
                color="primary"
                @click="toggleImpactExpansion"
              >
                {{ impactExpanded ? t('category.filters.hideImpact') : t('category.filters.showMoreImpact') }}
              </v-btn>

              <CategoryFilterList
                v-if="impactExpanded"
                :fields="impactRemaining"
                :aggregations="aggregationMap"
                :active-filters="activeFilters"
                class="mt-3"
                @update-range="updateRangeFilter"
                @update-terms="updateTermsFilter"
              />
            </div>
          </div>
        </template>
      </v-expansion-panel>

      <v-expansion-panel value="technical" expand-icon="mdi-chevron-down">
        <template #title>
          <div class="category-filters__title">
            <v-icon icon="mdi-cog" size="20" class="category-filters__title-icon" />
            <span>{{ t('category.filters.technicalTitle') }}</span>
          </div>
        </template>
        <template #text>
          <div class="category-filters__section">
            <CategoryFilterList
              :fields="technicalPrimary"
              :aggregations="aggregationMap"
              :active-filters="activeFilters"
              @update-range="updateRangeFilter"
              @update-terms="updateTermsFilter"
            />

            <div v-if="technicalRemaining.length" class="category-filters__see-more">
              <v-btn
                variant="text"
                density="comfortable"
                color="primary"
                @click="toggleTechnicalExpansion"
              >
                {{ technicalExpanded ? t('category.filters.hideTechnical') : t('category.filters.showMoreTechnical') }}
              </v-btn>

              <CategoryFilterList
                v-if="technicalExpanded"
                :fields="technicalRemaining"
                :aggregations="aggregationMap"
                :active-filters="activeFilters"
                class="mt-3"
                @update-range="updateRangeFilter"
                @update-terms="updateTermsFilter"
              />
            </div>
          </div>
        </template>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script setup lang="ts">
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
  FilterRequestDto,
  ProductFieldOptionsResponse,
} from '~~/shared/api-client'
import { useI18n } from 'vue-i18n'

import CategoryFilterList from './filters/CategoryFilterList.vue'
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

const props = defineProps<{
  filterOptions: ProductFieldOptionsResponse | null
  aggregations: AggregationResponseDto[]
  filters: FilterRequestDto | null
  impactExpanded: boolean
  technicalExpanded: boolean
  subsetClauses: CategorySubsetClause[]
}>()

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'update:impactExpanded': [boolean]
  'update:technicalExpanded': [boolean]
  'remove-subset-clause': [CategorySubsetClause]
}>()

const activeFilters = computed(() => props.filters?.filters ?? [])
const subsetClauses = computed(() => props.subsetClauses ?? [])

const { t } = useI18n()

const aggregationMap = computed<Record<string, AggregationResponseDto>>(() => {
  return (props.aggregations ?? []).reduce<Record<string, AggregationResponseDto>>((accumulator, aggregation) => {
    if (aggregation.field) {
      accumulator[aggregation.field] = aggregation
    }

    return accumulator
  }, {})
})

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

const impactPrimary = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.impact ?? []
  const ecoscore = entries.filter((item) => item.mapping === 'scores.ECOSCORE.value')
  return ecoscore.length ? ecoscore : entries.slice(0, 1)
})

const impactRemaining = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.impact ?? []
  const primary = new Set(impactPrimary.value.map((entry) => entry.mapping))
  return entries.filter((entry) => entry.mapping && !primary.has(entry.mapping))
})

const technicalPrimary = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.technical ?? []
  return entries.slice(0, 3)
})

const technicalRemaining = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.technical ?? []
  return entries.slice(3)
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

const updateRangeFilter = (field: string, range: { min?: number; max?: number }) => {
  const current = activeFilters.value.filter((filter) => filter.field !== field)

  if (range.min == null && range.max == null) {
    emitFilters(current)
    return
  }

  emitFilters([
    ...current,
    {
      field,
      operator: 'range',
      min: range.min,
      max: range.max,
    },
  ])
}

const updateTermsFilter = (field: string, terms: string[]) => {
  const current = activeFilters.value.filter((filter) => filter.field !== field)

  if (!terms.length) {
    emitFilters(current)
    return
  }

  emitFilters([
    ...current,
    {
      field,
      operator: 'term',
      terms,
    },
  ])
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

const toggleImpactExpansion = () => {
  emit('update:impactExpanded', !props.impactExpanded)
}

const toggleTechnicalExpansion = () => {
  emit('update:technicalExpanded', !props.technicalExpanded)
}

defineExpose({ activeChips })
</script>

<style scoped lang="sass">
.category-filters
  display: flex
  flex-direction: column
  gap: 1rem

  &__active
    padding: 0.5rem
    background: rgb(var(--v-theme-surface-glass))
    border-radius: 0.75rem

  &__chips
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__panels
    background: transparent
    :deep(.v-expansion-panel)
      background: rgb(var(--v-theme-surface-default))
      border-radius: 0.75rem
      margin-bottom: 0.75rem

  &__section
    padding: 1rem

  &__see-more
    margin-top: 1rem
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__title
    display: inline-flex
    align-items: center
    gap: 0.5rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__title-icon
    color: rgba(var(--v-theme-primary), 0.85)

@media (max-width: 959px)
  .category-filters__section
    padding: 0.75rem
</style>

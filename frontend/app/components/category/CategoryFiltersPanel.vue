<template>
  <div class="category-filters" data-testid="category-filters">
    <div v-if="activeFilterChips.length" class="category-filters__active">
      <v-chip-group column>
        <v-chip
          v-for="chip in activeFilterChips"
          :key="chip.id"
          closable
          color="primary"
          variant="flat"
          size="small"
          @click:close="removeFilter(chip.field, chip.type, chip.term)"
        >
          <v-icon icon="mdi-close-circle" size="16" class="me-1" />
          <span>{{ chip.label }}</span>
        </v-chip>
      </v-chip-group>
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

const props = defineProps<{
  filterOptions: ProductFieldOptionsResponse | null
  aggregations: AggregationResponseDto[]
  filters: FilterRequestDto | null
  impactExpanded: boolean
  technicalExpanded: boolean
}>()

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'update:impactExpanded': [boolean]
  'update:technicalExpanded': [boolean]
}>()

const activeFilters = computed(() => props.filters?.filters ?? [])

const { t } = useI18n()

const aggregationMap = computed<Record<string, AggregationResponseDto>>(() => {
  return (props.aggregations ?? []).reduce<Record<string, AggregationResponseDto>>((accumulator, aggregation) => {
    if (aggregation.field) {
      accumulator[aggregation.field] = aggregation
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

const activeFilterChips = computed(() => {
  return activeFilters.value.map((filter) => {
    if (filter.operator === 'term') {
      const term = filter.terms?.[0] ?? ''
      return {
        id: `${filter.field}-${term}`,
        field: filter.field ?? '',
        type: 'term' as const,
        term,
        label: `${filter.field}: ${term}`,
      }
    }

    return {
      id: `${filter.field}-range`,
      field: filter.field ?? '',
      type: 'range' as const,
      term: null,
      label: `${filter.field}: ${filter.min ?? '–'} → ${filter.max ?? '–'}`,
    }
  })
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

const removeFilter = (field: string, type: 'term' | 'range', term: string | null) => {
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

const toggleImpactExpansion = () => {
  emit('update:impactExpanded', !props.impactExpanded)
}

const toggleTechnicalExpansion = () => {
  emit('update:technicalExpanded', !props.technicalExpanded)
}

defineExpose({ activeFilterChips })
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

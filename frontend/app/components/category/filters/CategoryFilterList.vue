<template>
  <div :class="['category-filter-list', `category-filter-list--${mode}`]">
    <template v-if="mode === 'grid'">
      <component
        :is="resolveComponent(field)"
        v-for="field in fields"
        :key="field.mapping ?? field.title"
        :field="field"
        :aggregation="aggregations[field.mapping ?? '']"
        :baseline-aggregation="baselineAggregations[field.mapping ?? '']"
        :model-value="findActiveFilter(field.mapping)"
        class="category-filter-list__item"
        @update:model-value="onFilterChange(field, $event)"
      />
    </template>

    <template v-else>
      <v-row dense>
        <v-col
          v-for="field in fields"
          :key="field.mapping ?? field.title"
          cols="12"
          sm="6"
          md="3"
        >
          <component
            :is="resolveComponent(field)"
            :field="field"
            :aggregation="aggregations[field.mapping ?? '']"
            :baseline-aggregation="baselineAggregations[field.mapping ?? '']"
            :model-value="findActiveFilter(field.mapping)"
            @update:model-value="onFilterChange(field, $event)"
          />
        </v-col>
      </v-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import CategoryFilterNumeric from './CategoryFilterNumeric.vue'
import CategoryFilterCondition from './CategoryFilterCondition.vue'
import CategoryFilterTerms from './CategoryFilterTerms.vue'

const props = withDefaults(
  defineProps<{
    fields: FieldMetadataDto[]
    aggregations: Record<string, AggregationResponseDto>
    baselineAggregations?: Record<string, AggregationResponseDto>
    activeFilters: Filter[]
    searchType?: string | null
    mode?: 'grid' | 'row'
  }>(),
  {
    baselineAggregations: () => ({}) as Record<string, AggregationResponseDto>,
    mode: 'grid',
    searchType: null,
  }
)

const emit = defineEmits<{
  'update-range': [field: string, payload: { min?: number; max?: number }]
  'update-terms': [field: string, terms: string[]]
  'update:searchType': [value: string | null]
}>()

const resolveComponent = (field: FieldMetadataDto) => {
  if (field.mapping === 'price.conditions') {
    return CategoryFilterCondition
  }

  return field.valueType === 'numeric'
    ? CategoryFilterNumeric
    : CategoryFilterTerms
}

const findActiveFilter = (field?: string | null) => {
  return props.activeFilters.find(filter => filter.field === field) ?? null
}

const onFilterChange = (field: FieldMetadataDto, filter: Filter | null) => {
  const mapping = field.mapping
  if (!mapping) {
    return
  }

  if (!filter) {
    if (field.valueType === 'numeric') {
      emit('update-range', mapping, { min: undefined, max: undefined })
    } else {
      emit('update-terms', mapping, [])
    }
    return
  }

  if (filter.operator === 'range') {
    emit('update-range', mapping, { min: filter.min, max: filter.max })
    return
  }

  emit('update-terms', mapping, filter.terms ?? [])
}
</script>

<style scoped lang="sass">
.category-filter-list
  display: grid
  gap: 1.5rem

  &--grid
    grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr))
    align-items: stretch



  &__item
    display: flex
    flex-direction: column
    min-width: 0



  &__search-toggle
    align-items: center
    :deep(.v-btn)
      text-transform: none
      font-weight: 600
      padding-inline: 1.25rem
</style>

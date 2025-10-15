<template>
  <div class="category-filter-list">
    <component
      :is="resolveComponent(field)"
      v-for="field in fields"
      :key="field.mapping ?? field.title"
      :field="field"
      :aggregation="aggregations[field.mapping ?? '']"
      :model-value="findActiveFilter(field.mapping)"
      class="category-filter-list__item"
      @update:model-value="onFilterChange(field, $event)"
    />
  </div>
</template>

<script setup lang="ts">
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'

import CategoryFilterNumeric from './CategoryFilterNumeric.vue'
import CategoryFilterTerms from './CategoryFilterTerms.vue'

const props = defineProps<{
  fields: FieldMetadataDto[]
  aggregations: Record<string, AggregationResponseDto>
  activeFilters: Filter[]
}>()

const emit = defineEmits<{
  'update-range': [field: string, payload: { min?: number; max?: number }]
  'update-terms': [field: string, terms: string[]]
}>()

const resolveComponent = (field: FieldMetadataDto) => {
  return field.valueType === 'numeric' ? CategoryFilterNumeric : CategoryFilterTerms
}

const findActiveFilter = (field?: string | null) => {
  return props.activeFilters.find((filter) => filter.field === field) ?? null
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
  display: flex
  flex-direction: column
  gap: 1.5rem
</style>

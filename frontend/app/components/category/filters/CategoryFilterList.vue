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
      <v-menu
        v-for="field in fields"
        :key="field.mapping ?? field.title"
        :close-on-content-click="false"
        location="bottom start"
        offset="8"
      >
        <template #activator="{ props: menuProps }">
          <v-btn
            v-bind="menuProps"
            :color="isActive(field) ? 'primary' : undefined"
            :variant="isActive(field) ? 'flat' : 'outlined'"
            append-icon="mdi-chevron-down"
            class="text-none"
          >
            {{ resolveTitle(field) }}

            <v-avatar
              v-if="getFilterCount(field) > 0"
              color="white"
              size="20"
              class="ms-2 text-primary font-weight-bold"
              style="font-size: 12px"
            >
              {{ getFilterCount(field) }}
            </v-avatar>
          </v-btn>
        </template>

        <v-sheet min-width="320" class="pa-4 rounded-lg elevation-4 border">
          <component
            :is="resolveComponent(field)"
            :field="field"
            :aggregation="aggregations[field.mapping ?? '']"
            :baseline-aggregation="baselineAggregations[field.mapping ?? '']"
            :model-value="findActiveFilter(field.mapping)"
            @update:model-value="onFilterChange(field, $event)"
          />
        </v-sheet>
      </v-menu>
    </template>
  </div>
</template>

<script setup lang="ts">
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'

import CategoryFilterNumeric from './CategoryFilterNumeric.vue'
import CategoryFilterCondition from './CategoryFilterCondition.vue'
import CategoryFilterTerms from './CategoryFilterTerms.vue'

const props = withDefaults(
  defineProps<{
    fields: FieldMetadataDto[]
    aggregations: Record<string, AggregationResponseDto>
    baselineAggregations?: Record<string, AggregationResponseDto>
    activeFilters: Filter[]
    mode?: 'grid' | 'row'
  }>(),
  {
    baselineAggregations: () => ({}) as Record<string, AggregationResponseDto>,
    mode: 'grid',
  }
)

const emit = defineEmits<{
  'update-range': [field: string, payload: { min?: number; max?: number }]
  'update-terms': [field: string, terms: string[]]
}>()

const { t } = useI18n()

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

const isActive = (field: FieldMetadataDto) => {
  return !!findActiveFilter(field.mapping)
}

const getFilterCount = (field: FieldMetadataDto) => {
  const filter = findActiveFilter(field.mapping)
  if (!filter) return 0

  if (filter.operator === 'term') {
    return filter.terms?.length ?? 0
  }

  if (filter.operator === 'range') {
    // If range is active, count as 1
    return filter.min !== undefined || filter.max !== undefined ? 1 : 0
  }

  return 0
}

const resolveTitle = (field: FieldMetadataDto) => {
  return resolveFilterFieldTitle(field, t)
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

  &--row
    display: flex
    flex-direction: row
    flex-wrap: wrap
    gap: 0.75rem
    align-items: center

  &__item
    display: flex
    flex-direction: column
    min-width: 0
</style>

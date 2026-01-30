<template>
  <div :class="['category-filter-list', `category-filter-list--${mode}`]">
    <!-- Grid Mode (Mobile Drawer) -->
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

    <!-- Bar Mode (Desktop) -->
    <template v-else-if="mode === 'bar'">
      <div v-for="field in fields" :key="field.mapping ?? field.title">
        <v-menu
          :close-on-content-click="false"
          location="bottom start"
          offset="8"
        >
          <template #activator="{ props: menuProps }">
            <v-btn
              v-bind="menuProps"
              variant="tonal"
              :color="isFilterActive(field.mapping) ? 'primary' : undefined"
              class="category-filter-list__bar-btn"
              prepend-icon="mdi-filter-variant"
              rounded="pill"
            >
              {{ field.title }}
              <v-badge
                v-if="isFilterActive(field.mapping)"
                dot
                color="primary"
                inline
                class="ms-2"
              />
              <v-icon icon="mdi-chevron-down" end size="small" />
            </v-btn>
          </template>

          <v-card
            min-width="300"
            max-width="400"
            class="pa-4"
            rounded="xl"
            elevation="3"
          >
            <component
              :is="resolveComponent(field)"
              :field="field"
              :aggregation="aggregations[field.mapping ?? '']"
              :baseline-aggregation="baselineAggregations[field.mapping ?? '']"
              :model-value="findActiveFilter(field.mapping)"
              @update:model-value="onFilterChange(field, $event)"
            />
          </v-card>
        </v-menu>
      </div>
    </template>

    <!-- Row Mode (Legacy/Fallback) -->
    <template v-else>
      <v-row dense class="category-filter-list__row">
        <v-col
          v-for="field in fields"
          :key="field.mapping ?? field.title"
          cols="12"
          sm="6"
          class="category-filter-list__col"
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
    mode?: 'grid' | 'row' | 'bar'
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

const isFilterActive = (field?: string | null): boolean => {
  return !!findActiveFilter(field)
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

  &--grid
    display: grid
    grid-template-columns: repeat(auto-fit, minmax(min(280px, 100%), 1fr))
    align-items: stretch

  &--bar
    flex-direction: row
    flex-wrap: wrap
    gap: 0.75rem
    align-items: center

  &--row
    display: block

  &__row
    align-items: stretch

  &__col
    min-width: 480px

  &__item
    display: flex
    flex-direction: column
    min-width: 0

  &__bar-btn
    text-transform: none
    font-weight: 500
    letter-spacing: 0

  &__search-toggle
    align-items: center
    :deep(.v-btn)
      text-transform: none
      font-weight: 600
      padding-inline: 1.25rem
</style>

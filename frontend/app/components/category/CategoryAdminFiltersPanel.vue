<template>
  <section
    class="category-admin-panel"
    aria-labelledby="category-admin-panel-title"
  >
    <header class="category-admin-panel__header">
      <p id="category-admin-panel-title" class="category-admin-panel__eyebrow">
        {{ t('category.admin.title') }}
      </p>
      <p class="category-admin-panel__helper">
        {{ t('category.admin.helper') }}
      </p>
      <p v-if="fieldHelper" class="category-admin-panel__field-helper">
        {{ fieldHelper }}
      </p>
    </header>

    <CategoryFilterList
      v-if="fields.length"
      :fields="fields"
      :aggregations="aggregationMap"
      :baseline-aggregations="baselineAggregationMap"
      :active-filters="activeFilters"
      class="category-admin-panel__filters"
      @update-range="updateRangeFilter"
      @update-terms="updateTermsFilter"
    />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
  FilterRequestDto,
} from '~~/shared/api-client'
import CategoryFilterList from './filters/CategoryFilterList.vue'

const props = withDefaults(
  defineProps<{
    fields: FieldMetadataDto[]
    aggregations: AggregationResponseDto[]
    baselineAggregations?: AggregationResponseDto[]
    filters: FilterRequestDto | null
  }>(),
  {
    baselineAggregations: () => [],
  }
)

const emit = defineEmits<{ 'update:filters': [FilterRequestDto] }>()

const { t } = useI18n()

const activeFilters = computed(() => props.filters?.filters ?? [])

const aggregationMap = computed<Record<string, AggregationResponseDto>>(() => {
  return props.aggregations.reduce<Record<string, AggregationResponseDto>>(
    (accumulator, aggregation) => {
      if (aggregation.field) {
        accumulator[aggregation.field] = aggregation
      }

      return accumulator
    },
    {}
  )
})

const baselineAggregationMap = computed<Record<string, AggregationResponseDto>>(
  () => {
    return props.baselineAggregations.reduce<
      Record<string, AggregationResponseDto>
    >((accumulator, aggregation) => {
      if (aggregation.field && !(aggregation.field in accumulator)) {
        accumulator[aggregation.field] = aggregation
      }

      return accumulator
    }, {})
  }
)

const fieldHelper = computed(() => {
  if (!props.fields.length) {
    return ''
  }

  const helperKey = `category.admin.filters.${props.fields[0]?.mapping ?? ''}.helper`
  const translated = t(helperKey)
  return translated !== helperKey ? translated : ''
})

const emitFilters = (filters: Filter[]) => {
  emit('update:filters', filters.length ? { filters } : {})
}

const updateRangeFilter = (
  field: string,
  range: { min?: number; max?: number }
) => {
  const current = activeFilters.value.filter(filter => filter.field !== field)

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
  const current = activeFilters.value.filter(filter => filter.field !== field)

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
</script>

<style scoped lang="sass">
.category-admin-panel
  display: flex
  flex-direction: column
  gap: 1rem
  padding: 1.25rem
  border-radius: 1rem
  border: 1px solid rgba(var(--v-theme-error), 0.35)
  background: rgba(var(--v-theme-error), 0.08)
  box-shadow: 0 18px 32px -24px rgba(var(--v-theme-error), 0.3)

  &__header
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__eyebrow
    margin: 0
    font-size: 0.75rem
    font-weight: 700
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgb(var(--v-theme-error))

  &__helper
    margin: 0
    font-size: 0.88rem
    color: rgba(var(--v-theme-error), 0.95)

  &__field-helper
    margin: 0
    font-size: 0.8rem
    color: rgba(var(--v-theme-error), 0.85)

  &__filters
    padding-top: 0.5rem

@media (max-width: 959px)
  .category-admin-panel
    padding: 1rem
</style>

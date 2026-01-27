<template>
  <div class="category-filters" data-testid="category-filters">
    <section class="category-filters__section">
      <h2
        class="category-filters__title cursor-pointer"
        @click="isGlobalOpen = !isGlobalOpen"
      >
        <div class="d-flex align-center flex-grow-1 gap-2">
          <v-icon
            icon="mdi-tune-variant"
            size="20"
            class="category-filters__title-icon"
          />
          <span>{{ t('category.filters.globalTitle') }}</span>
        </div>
        <v-icon
          :icon="isGlobalOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          color="medium-emphasis"
        />
      </h2>
      <v-expand-transition>
        <div v-show="isGlobalOpen" class="category-filters__section-body">
          <CategoryFilterList
            :fields="filterOptions?.global ?? []"
            :aggregations="aggregationMap"
            :baseline-aggregations="baselineAggregationMap"
            :active-filters="activeFilters"
            @update-range="updateRangeFilter"
            @update-terms="updateTermsFilter"
          />
        </div>
      </v-expand-transition>
    </section>

    <section class="category-filters__section">
      <h2
        class="category-filters__title cursor-pointer"
        @click="isImpactOpen = !isImpactOpen"
      >
        <div class="d-flex align-center flex-grow-1 gap-2">
          <v-icon
            icon="mdi-leaf"
            size="20"
            class="category-filters__title-icon"
          />
          <span>{{ t('category.filters.impactTitle') }}</span>
        </div>
        <v-icon
          :icon="isImpactOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          color="medium-emphasis"
        />
      </h2>
      <v-expand-transition>
        <div v-show="isImpactOpen" class="category-filters__section-body">
          <CategoryFilterList
            :fields="impactPrimary"
            :aggregations="aggregationMap"
            :baseline-aggregations="baselineAggregationMap"
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
              {{
                impactExpanded
                  ? t('category.filters.hideImpact')
                  : t('category.filters.showMoreImpact')
              }}
            </v-btn>

            <CategoryFilterList
              v-if="impactExpanded"
              :fields="impactRemaining"
              :aggregations="aggregationMap"
              :baseline-aggregations="baselineAggregationMap"
              :active-filters="activeFilters"
              class="mt-3"
              @update-range="updateRangeFilter"
              @update-terms="updateTermsFilter"
            />
          </div>
        </div>
      </v-expand-transition>
    </section>

    <section class="category-filters__section">
      <h2
        class="category-filters__title cursor-pointer"
        @click="isTechnicalOpen = !isTechnicalOpen"
      >
        <div class="d-flex align-center flex-grow-1 gap-2">
          <v-icon
            icon="mdi-cog"
            size="20"
            class="category-filters__title-icon"
          />
          <span>{{ t('category.filters.technicalTitle') }}</span>
        </div>
        <v-icon
          :icon="isTechnicalOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          color="medium-emphasis"
        />
      </h2>
      <v-expand-transition>
        <div v-show="isTechnicalOpen" class="category-filters__section-body">
          <CategoryFilterList
            :fields="technicalPrimary"
            :aggregations="aggregationMap"
            :baseline-aggregations="baselineAggregationMap"
            :active-filters="activeFilters"
            @update-range="updateRangeFilter"
            @update-terms="updateTermsFilter"
          />

          <div
            v-if="technicalRemaining.length"
            class="category-filters__see-more"
          >
            <v-btn
              variant="text"
              density="comfortable"
              color="primary"
              @click="toggleTechnicalExpansion"
            >
              {{
                technicalExpanded
                  ? t('category.filters.hideTechnical')
                  : t('category.filters.showMoreTechnical')
              }}
            </v-btn>

            <CategoryFilterList
              v-if="technicalExpanded"
              :fields="technicalRemaining"
              :aggregations="aggregationMap"
              :baseline-aggregations="baselineAggregationMap"
              :active-filters="activeFilters"
              class="mt-3"
              @update-range="updateRangeFilter"
              @update-terms="updateTermsFilter"
            />
          </div>
        </div>
      </v-expand-transition>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
  FilterRequestDto,
  ProductFieldOptionsResponse,
} from '~~/shared/api-client'

import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

import CategoryFilterList from './filters/CategoryFilterList.vue'
const props = withDefaults(
  defineProps<{
    filterOptions: ProductFieldOptionsResponse | null
    aggregations: AggregationResponseDto[]
    baselineAggregations?: AggregationResponseDto[]
    filters: FilterRequestDto | null
    impactExpanded: boolean
    technicalExpanded: boolean
  }>(),
  {
    baselineAggregations: () => [],
  }
)

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'update:impactExpanded': [boolean]
  'update:technicalExpanded': [boolean]
}>()

const activeFilters = computed(() => props.filters?.filters ?? [])
const isGlobalOpen = ref(false)
const isImpactOpen = ref(false)
const isTechnicalOpen = ref(false)

const { t } = useI18n()

const aggregationMap = computed<Record<string, AggregationResponseDto>>(() => {
  return (props.aggregations ?? []).reduce<
    Record<string, AggregationResponseDto>
  >((accumulator, aggregation) => {
    if (aggregation.field) {
      accumulator[aggregation.field] = aggregation
    }

    return accumulator
  }, {})
})

const baselineAggregationMap = computed<Record<string, AggregationResponseDto>>(
  () => {
    return (props.baselineAggregations ?? []).reduce<
      Record<string, AggregationResponseDto>
    >((accumulator, aggregation) => {
      if (aggregation.field && !(aggregation.field in accumulator)) {
        accumulator[aggregation.field] = aggregation
      }

      return accumulator
    }, {})
  }
)

const impactPrimary = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.impact ?? []
  const ecoscore = entries.filter(
    item => item.mapping === ECOSCORE_RELATIVE_FIELD
  )
  return ecoscore.length ? ecoscore : entries.slice(0, 1)
})

const impactRemaining = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.impact ?? []
  const primary = new Set(impactPrimary.value.map(entry => entry.mapping))
  return entries.filter(entry => entry.mapping && !primary.has(entry.mapping))
})

const technicalPrimary = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.technical ?? []
  return entries.slice(0, 3)
})

const technicalRemaining = computed<FieldMetadataDto[]>(() => {
  const entries = props.filterOptions?.technical ?? []
  return entries.slice(3)
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

const toggleImpactExpansion = () => {
  emit('update:impactExpanded', !props.impactExpanded)
}

const toggleTechnicalExpansion = () => {
  emit('update:technicalExpanded', !props.technicalExpanded)
}
</script>

<style scoped lang="sass">
.category-filters
  display: flex
  flex-direction: column
  gap: 1rem

  &__section
    padding: 1rem
    border-radius: 0.75rem
    background: rgb(var(--v-theme-surface-default))
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3)
    display: flex
    flex-direction: column
    gap: 1rem

  &__section-body
    display: flex
    flex-direction: column
    gap: 1rem

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
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-strong))

  &__title-icon
    color: rgba(var(--v-theme-primary), 0.85)

@media (max-width: 959px)
  .category-filters__section
    padding: 0.75rem
</style>

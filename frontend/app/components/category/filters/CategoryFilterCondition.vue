<template>
  <div class="category-filter-condition">
    <p class="category-filter-condition__title">{{ displayTitle }}</p>
    <div class="category-filter-condition__options">
      <v-checkbox
        v-for="option in conditionOptions"
        :key="option.key"
        :model-value="localTerms.includes(option.key)"
        density="comfortable"
        hide-details
        color="primary"
        class="category-filter-condition__checkbox"
        @update:model-value="onCheckboxChange(option.key, $event)"
      >
        <template #label>
          <div class="category-filter-condition__label">
            <span>{{ option.label }}</span>
            <span class="category-filter-condition__count">
              {{ option.count }}
            </span>
          </div>
        </template>
      </v-checkbox>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))
const localTerms = ref<string[]>(props.modelValue?.terms ?? [])

watch(
  () => props.modelValue,
  next => {
    localTerms.value = next?.operator === 'term' ? [...(next.terms ?? [])] : []
  },
  { immediate: true }
)

const CONDITION_KEYS = ['NEW', 'OCCASION'] as const

const resolveCount = (key: string) => {
  const activeBuckets = props.aggregation?.buckets ?? []
  const baselineBuckets = props.baselineAggregation?.buckets ?? []

  const bucket =
    activeBuckets.find(item => item.key === key) ??
    baselineBuckets.find(item => item.key === key)

  return bucket?.count ?? 0
}

const conditionOptions = computed(() => [
  {
    key: CONDITION_KEYS[0],
    label: t('category.filters.condition.new'),
    count: resolveCount(CONDITION_KEYS[0]),
  },
  {
    key: CONDITION_KEYS[1],
    label: t('category.filters.condition.used'),
    count: resolveCount(CONDITION_KEYS[1]),
  },
])

const onCheckboxChange = (term: string, selected: boolean | null) => {
  const next = new Set(localTerms.value)

  if (!selected) {
    next.delete(term)
  } else {
    next.add(term)
  }

  localTerms.value = Array.from(next)

  emit(
    'update:modelValue',
    localTerms.value.length
      ? {
          field: props.field.mapping,
          operator: 'term',
          terms: [...localTerms.value],
        }
      : null
  )
}
</script>

<style scoped lang="sass">
.category-filter-condition
  display: flex
  flex-direction: column
  gap: 0.75rem

  &__title
    margin: 0
    font-size: 1rem
    font-weight: 600

  &__options
    display: grid
    gap: 0.5rem

  &__checkbox
    padding: 0.5rem 0.75rem
    border-radius: 0.75rem
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5)
    background: rgba(var(--v-theme-surface-primary-050), 0.6)

    :deep(.v-selection-control__wrapper)
      margin-inline-end: 0.5rem

  &__label
    display: flex
    align-items: center
    justify-content: space-between
    width: 100%
    gap: 0.75rem

  &__count
    font-size: 0.85rem
    color: rgb(var(--v-theme-text-neutral-secondary))
</style>

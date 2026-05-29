<template>
  <div class="category-offers-toggle">
    <span class="category-offers-toggle__label">
      {{ t('category.filters.fields.condition') }}
    </span>

    <v-btn-toggle
      :model-value="selected"
      multiple
      density="comfortable"
      variant="outlined"
      divided
      color="primary"
      class="category-offers-toggle__group"
      @update:model-value="onToggle"
    >
      <v-btn
        v-for="option in options"
        :key="option.key"
        :value="option.key"
        size="small"
        class="category-offers-toggle__btn"
      >
        <NudgeConditionNewIcon v-if="option.key === 'NEW'" class="me-2" />
        <NudgeConditionUsedIcon v-else class="me-2" />
        <span>{{ option.label }}</span>
        <span v-if="option.count > 0" class="category-offers-toggle__count">
          {{ option.count }}
        </span>
      </v-btn>
    </v-btn-toggle>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AggregationResponseDto, Filter } from '~~/shared/api-client'
import NudgeConditionNewIcon from '~/components/nudge-tool/NudgeConditionNewIcon.vue'
import NudgeConditionUsedIcon from '~/components/nudge-tool/NudgeConditionUsedIcon.vue'

const props = defineProps<{
  aggregation?: AggregationResponseDto
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [string[]] }>()

const { t } = useI18n()

const CONDITION_KEYS = ['NEW', 'OCCASION'] as const

const selected = computed(() =>
  props.modelValue?.operator === 'term' ? (props.modelValue.terms ?? []) : []
)

const resolveCount = (key: string) => {
  const activeBuckets = props.aggregation?.buckets ?? []
  const baselineBuckets = props.baselineAggregation?.buckets ?? []
  const bucket =
    activeBuckets.find(item => item.key === key) ??
    baselineBuckets.find(item => item.key === key)
  return bucket?.count ?? 0
}

const options = computed(() => [
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

const onToggle = (next: string[]) => {
  emit('update:modelValue', next)
}
</script>

<style scoped lang="sass">
.category-offers-toggle
  display: flex
  flex-direction: column
  gap: 0.5rem
  padding: 1rem
  border-radius: 0.75rem
  background: rgb(var(--v-theme-surface-default))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3)

  &__label
    font-weight: 600
    font-size: 1rem
    color: rgb(var(--v-theme-text-neutral-strong))

  &__group
    width: 100%

  &__btn
    flex: 1 1 0
    text-transform: none
    letter-spacing: 0

  &__count
    margin-inline-start: 0.4rem
    font-size: 0.7rem
    font-weight: 700
    opacity: 0.75
</style>

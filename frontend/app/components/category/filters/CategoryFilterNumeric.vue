<template>
  <div class="category-filter-numeric">
    <div class="category-filter-numeric__header">
      <p class="category-filter-numeric__title">{{ displayTitle }}</p>
      <p v-if="rangeLabel" class="category-filter-numeric__range">
        {{ rangeLabel }}
      </p>
    </div>

    <v-range-slider
      v-model="localValue"
      :min="bounds.min"
      :max="bounds.max"
      :step="sliderStep"
      :aria-label="ariaLabel"
      class="category-filter-numeric__slider"
      thumb-label="always"
      color="primary"
      @end="emitRange"
    />

    <div v-if="aggregation?.buckets?.length" class="category-filter-numeric__buckets">
      <div
        v-for="bucket in aggregation.buckets"
        :key="bucket.key"
        class="category-filter-numeric__bucket"
      >
        <span class="category-filter-numeric__bucket-label">
          {{ formatBucketLabel(bucket.key, bucket.to) }}
        </span>
        <v-progress-linear
          :model-value="bucket.count ?? 0"
          :max="maxBucketCount"
          height="6"
          color="primary"
        />
        <span class="category-filter-numeric__bucket-count">{{ bucket.count ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AggregationResponseDto, FieldMetadataDto, Filter } from '~~/shared/api-client'

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

const displayTitle = computed(() => props.field.title ?? props.field.mapping ?? '')

const ariaLabel = computed(() => `${displayTitle.value} ${t('category.filters.rangeAriaSuffix')}`)

const bounds = computed(() => {
  const min = props.aggregation?.min ?? (props.aggregation?.buckets?.[0]?.key ? Number(props.aggregation?.buckets?.[0]?.key) : 0)
  const max = props.aggregation?.max ?? (props.aggregation?.buckets?.at(-1)?.to ?? min)

  if (props.modelValue?.operator === 'range') {
    return {
      min: props.modelValue.min ?? min,
      max: props.modelValue.max ?? max,
    }
  }

  return { min, max }
})

const sliderStep = computed(() => {
  const buckets = props.aggregation?.buckets ?? []
  if (buckets.length > 1) {
    const first = Number(buckets[0].key ?? 0)
    const second = Number(buckets[1].key ?? 0)
    const diff = Math.abs(second - first)
    return Number.isFinite(diff) && diff > 0 ? diff : undefined
  }

  return props.field.aggregationConfiguration?.interval
})

const localValue = ref<[number, number]>([bounds.value.min, bounds.value.max])

watch(
  () => bounds.value,
  (next) => {
    localValue.value = [next.min, next.max]
  },
  { immediate: true },
)

watch(
  () => props.modelValue,
  (filter) => {
    if (filter?.operator === 'range') {
      localValue.value = [filter.min ?? bounds.value.min, filter.max ?? bounds.value.max]
    }
  },
)

const maxBucketCount = computed(() => {
  return Math.max(...(props.aggregation?.buckets?.map((bucket) => bucket.count ?? 0) ?? [0]), 1)
})

const rangeLabel = computed(() => {
  if (!props.modelValue || props.modelValue.operator !== 'range') {
    return null
  }

  const { min, max } = props.modelValue
  if (min == null && max == null) {
    return null
  }

  return `${min ?? '–'} → ${max ?? '–'}`
})

const emitRange = () => {
  const [min, max] = localValue.value

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min,
    max,
  })
}

const formatBucketLabel = (from?: string, to?: number) => {
  if (from == null && to == null) {
    return ''
  }

  if (to == null) {
    return `${from}+`
  }

  return `${from ?? '–'} → ${to}`
}
</script>

<style scoped lang="sass">
.category-filter-numeric
  display: flex
  flex-direction: column
  gap: 0.75rem

  &__header
    display: flex
    justify-content: space-between
    align-items: baseline

  &__title
    font-size: 1rem
    font-weight: 600
    margin: 0

  &__range
    margin: 0
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__slider
    margin-top: 0.25rem

  &__buckets
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__bucket
    display: grid
    grid-template-columns: auto 1fr auto
    gap: 0.75rem
    align-items: center

  &__bucket-label
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__bucket-count
    font-size: 0.75rem
    font-variant-numeric: tabular-nums
</style>

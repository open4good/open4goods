<template>
  <div class="category-filter-numeric">
    <div class="category-filter-numeric__header">
      <p class="category-filter-numeric__title">{{ displayTitle }}</p>
      <p v-if="rangeLabel" class="category-filter-numeric__range">
        {{ rangeLabel }}
      </p>
    </div>

    <!-- Lightweight distribution sparkline (no axes, labels or tooltips). -->
    <div
      v-if="showSparkline"
      class="category-filter-numeric__spark"
      aria-hidden="true"
    >
      <button
        v-for="bar in sparkBars"
        :key="bar.key"
        type="button"
        class="category-filter-numeric__spark-bar"
        :class="{
          'category-filter-numeric__spark-bar--active': isBarActive(bar),
          'category-filter-numeric__spark-bar--empty': bar.count === 0,
        }"
        :style="{ height: `${Math.max(bar.heightPct, 4)}%` }"
        :tabindex="-1"
        @click="onBarClick(bar)"
      />
    </div>

    <v-range-slider
      v-model="localValue"
      :min="sliderBounds.min"
      :max="sliderBounds.max"
      :step="sliderStep"
      :aria-label="ariaLabel"
      class="category-filter-numeric__slider"
      track-size="2"
      thumb-size="12"
      thumb-label
      color="primary"
      hide-details="auto"
      @end="emitRange"
    >
      <template #thumb-label="{ modelValue: thumbValue }">
        {{
          formatSliderValue(
            Array.isArray(thumbValue) ? thumbValue[0] : thumbValue
          )
        }}
      </template>
    </v-range-slider>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'
import { formatNumericRangeValue } from '~/utils/_number-formatting'
import {
  clampSliderRange,
  isPriceField,
  priceToSliderValue,
  sliderValueToPrice,
} from './price-scale'
import { resolveFacetUnit } from '~~/shared/utils/facet-normalization'

type RangeBucketDatum = {
  key: string
  heightPct: number
  count: number
  from?: number
  to?: number
  missing: boolean
}

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t, n, locale } = useI18n()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))
const valueUnit = computed(() => resolveFacetUnit(props.field.mapping))

const ariaLabel = computed(
  () => `${displayTitle.value} ${t('category.filters.rangeAriaSuffix')}`
)

const parseNumericBound = (
  value: string | number | null | undefined
): number | undefined => {
  if (value == null) {
    return undefined
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined
  }

  const parsed = Number(value)
  if (Number.isFinite(parsed)) {
    return parsed
  }

  const parsedDate = Date.parse(String(value))
  return Number.isFinite(parsedDate) ? parsedDate : undefined
}

const resolveAggregationBounds = (
  aggregation?: AggregationResponseDto
): { min: number; max: number } | undefined => {
  if (!aggregation) {
    return undefined
  }

  const buckets = aggregation.buckets ?? []
  const firstBucket = buckets[0]
  const lastBucket = buckets[buckets.length - 1]

  const derivedMin = parseNumericBound(firstBucket?.key)
  const derivedMax =
    parseNumericBound(lastBucket?.to) ??
    parseNumericBound(lastBucket?.key) ??
    derivedMin

  const min = aggregation.min ?? derivedMin
  const max = aggregation.max ?? derivedMax

  if (min == null || max == null) {
    return undefined
  }

  return { min, max }
}

const numericBounds = computed(() => {
  const baseline = resolveAggregationBounds(props.baselineAggregation)
  if (baseline) {
    return baseline
  }

  const current = resolveAggregationBounds(props.aggregation)
  if (current) {
    return current
  }

  return { min: 0, max: 0 }
})

const priceField = computed(() => isPriceField(props.field.mapping))
const dateField = computed(() =>
  ['creationDate', 'lastChange'].includes(props.field.mapping ?? '')
)
const DATE_SLIDER_STEP = 24 * 60 * 60 * 1000

const sliderStep = computed(() => {
  if (priceField.value) {
    return 1
  }
  if (dateField.value) {
    return DATE_SLIDER_STEP
  }

  const buckets = props.aggregation?.buckets ?? []
  if (buckets.length > 1) {
    const [firstBucket, secondBucket] = buckets
    const first = Number(firstBucket?.key ?? 0)
    const second = Number(secondBucket?.key ?? 0)
    const diff = Math.abs(second - first)
    return Number.isFinite(diff) && diff > 0 ? diff : undefined
  }

  return props.field.aggregationConfiguration?.interval
})

const sliderBounds = computed(() => {
  if (!priceField.value) {
    return numericBounds.value
  }

  return {
    min: priceToSliderValue(numericBounds.value.min),
    max: priceToSliderValue(numericBounds.value.max),
  }
})

const resolveSliderRange = (
  min?: number | null,
  max?: number | null
): [number, number] => {
  if (priceField.value) {
    const resolvedMin = priceToSliderValue(min ?? numericBounds.value.min)
    const resolvedMax = priceToSliderValue(max ?? numericBounds.value.max)
    return clampSliderRange([resolvedMin, resolvedMax])
  }

  const fallbackMin = min ?? numericBounds.value.min
  const fallbackMax = max ?? numericBounds.value.max
  return clampSliderRange([fallbackMin, fallbackMax])
}

const toNumericRange = (range: [number, number]): [number, number] => {
  if (!priceField.value) {
    return clampSliderRange(range)
  }

  const [start, end] = range
  return clampSliderRange([sliderValueToPrice(start), sliderValueToPrice(end)])
}

const formatSliderValue = (value: number): string => {
  if (!Number.isFinite(value)) {
    return '–'
  }

  if (dateField.value) {
    return new Intl.DateTimeFormat(locale.value, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(new Date(value))
  }

  const resolvedValue = priceField.value ? sliderValueToPrice(value) : value
  const formatted = formatNumericRangeValue(resolvedValue, n, {
    isPrice: priceField.value,
  })

  return valueUnit.value ? `${formatted} ${valueUnit.value}` : formatted
}

const formatBoundary = (value: number | string | null | undefined): string => {
  if (dateField.value) {
    const parsed =
      typeof value === 'number' ? value : Date.parse(String(value ?? ''))
    if (!Number.isFinite(parsed)) {
      return '–'
    }
    return new Intl.DateTimeFormat(locale.value, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(new Date(parsed))
  }

  const formatted = formatNumericRangeValue(value, n, {
    isPrice: priceField.value,
  })
  return valueUnit.value ? `${formatted} ${valueUnit.value}` : formatted
}

const localValue = ref<[number, number]>([
  sliderBounds.value.min,
  sliderBounds.value.max,
])

watch(
  () => sliderBounds.value,
  next => {
    localValue.value = [next.min, next.max]
  },
  { immediate: true }
)

watch(
  () => props.modelValue,
  filter => {
    if (filter?.operator === 'range') {
      localValue.value = resolveSliderRange(filter.min, filter.max)
      return
    }

    localValue.value = [sliderBounds.value.min, sliderBounds.value.max]
  }
)

const buckets = computed(() => props.aggregation?.buckets ?? [])

const hasInformativeBuckets = computed(() => {
  const nonEmpty = buckets.value.filter(bucket => (bucket.count ?? 0) > 0)
  return nonEmpty.length > 1
})

const maxBucketCount = computed(() =>
  Math.max(1, ...buckets.value.map(bucket => bucket.count ?? 0))
)

const sparkBars = computed<RangeBucketDatum[]>(() =>
  buckets.value.map((bucket, index) => {
    const count = bucket.count ?? 0
    return {
      key: `${bucket.key ?? index}-${bucket.to ?? ''}`,
      heightPct: Math.round((count / maxBucketCount.value) * 100),
      count,
      from: parseNumericBound(bucket.key),
      to: parseNumericBound(bucket.to),
      missing: bucket.missing ?? false,
    }
  })
)

const showSparkline = computed(
  () => sparkBars.value.length > 0 && hasInformativeBuckets.value
)

const hasActiveRange = computed(
  () =>
    props.modelValue?.operator === 'range' &&
    (props.modelValue.min != null || props.modelValue.max != null)
)

const selectedNumericRange = computed(() => toNumericRange(localValue.value))

const isBarActive = (bar: RangeBucketDatum): boolean => {
  if (!hasActiveRange.value || bar.missing) {
    return false
  }
  const [min, max] = selectedNumericRange.value
  const from = bar.from ?? min
  const to = bar.to ?? max
  return to >= min && from <= max
}

const rangeLabel = computed(() => {
  if (!props.modelValue || props.modelValue.operator !== 'range') {
    return null
  }

  const { min, max } = props.modelValue
  if (min == null && max == null) {
    return null
  }

  return `${formatBoundary(min)} → ${formatBoundary(max)}`
})

const emitRange = () => {
  if (!props.field.mapping) {
    return
  }

  const [min, max] = toNumericRange(localValue.value)

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min,
    max,
  })
}

const onBarClick = (bar: RangeBucketDatum) => {
  if (bar.missing || bar.from == null || !props.field.mapping) {
    return
  }

  const [min, max] = resolveSliderRange(bar.from, bar.to)
  localValue.value = [min, max]

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min: bar.from,
    max: bar.to,
  })
}
</script>

<style scoped lang="sass">
.category-filter-numeric
  display: flex
  flex-direction: column

  &__header
    display: flex
    justify-content: space-between
    align-items: baseline
    margin-bottom: 0.5rem

  &__title
    font-size: 1rem
    font-weight: 600
    margin: 0

  &__range
    margin: 0
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__spark
    display: flex
    align-items: flex-end
    gap: 2px
    height: 36px
    margin-bottom: 0.25rem
    padding-inline: 2px

  &__spark-bar
    flex: 1 1 0
    min-width: 2px
    border: 0
    padding: 0
    border-radius: 2px 2px 0 0
    background: rgba(var(--v-theme-primary), 0.18)
    cursor: pointer
    transition: height 0.25s ease, background-color 0.2s ease

    &:hover
      background: rgba(var(--v-theme-primary), 0.4)

    &--active
      background: rgba(var(--v-theme-primary), 0.7)

    &--empty
      cursor: default
      background: rgba(var(--v-theme-on-surface), 0.06)

  &__slider
    margin-top: 0
    width: 100%
    min-width: 0
</style>

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
      :min="sliderBounds.min"
      :max="sliderBounds.max"
      :step="sliderStep"
      :aria-label="ariaLabel"
      class="category-filter-numeric__slider"
      thumb-label="always"
      color="primary"
      @end="emitRange"
    />

    <ClientOnly>
      <VueECharts
        v-if="chartOptions"
        :option="chartOptions"
        :autoresize="true"
        class="category-filter-numeric__chart"
        role="img"
        :aria-label="chartAriaLabel"
        :style="{ height: chartHeight }"
        @click="onBarClick"
      />
      <template #fallback>
        <div
          v-if="hasBuckets"
          class="category-filter-numeric__chart-placeholder"
          :style="{ height: chartHeight }"
          aria-hidden="true"
        />
      </template>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import type { AggregationResponseDto, FieldMetadataDto, Filter } from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { CallbackDataParams } from 'echarts/types/dist/shared'
import type { TooltipComponentOption } from 'echarts/components'
import { clampSliderRange, isPriceField, priceToSliderValue, sliderValueToPrice } from './price-scale'

type RangeBucketDatum = {
  value: number
  range: string
  count: number
  from?: number
  to?: number
  missing?: boolean
}

const MAX_CHART_HEIGHT = 250
const MAX_VISIBLE_LABELS = 6

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))

const ariaLabel = computed(() => `${displayTitle.value} ${t('category.filters.rangeAriaSuffix')}`)

const numericBounds = computed(() => {
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

const priceField = computed(() => isPriceField(props.field.mapping))

const sliderStep = computed(() => {
  if (priceField.value) {
    return 1
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

const resolveSliderRange = (min?: number | null, max?: number | null): [number, number] => {
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

const localValue = ref<[number, number]>([sliderBounds.value.min, sliderBounds.value.max])

watch(
  () => sliderBounds.value,
  (next) => {
    localValue.value = [next.min, next.max]
  },
  { immediate: true },
)

watch(
  () => props.modelValue,
  (filter) => {
    if (filter?.operator === 'range') {
      localValue.value = resolveSliderRange(filter.min, filter.max)
      return
    }

    localValue.value = [sliderBounds.value.min, sliderBounds.value.max]
  },
)

const buckets = computed(() => props.aggregation?.buckets ?? [])

const hasBuckets = computed(() => buckets.value.length > 0)

const toRgbColor = (value: string | undefined, fallbackRaw: string) => {
  const normalized = value?.trim() || fallbackRaw
  if (normalized.startsWith('#') || normalized.startsWith('rgb')) {
    return normalized
  }

  return `rgb(${normalized})`
}

const chartColorVar = import.meta.client
  ? useCssVar('--v-theme-chart-range-bar', document.documentElement, { initialValue: '29, 78, 216' })
  : ref('29, 78, 216')

const axisColorVar = import.meta.client
  ? useCssVar('--v-theme-text-neutral-secondary', document.documentElement, { initialValue: '71, 84, 103' })
  : ref('71, 84, 103')

const labelColorVar = import.meta.client
  ? useCssVar('--v-theme-text-neutral-strong', document.documentElement, { initialValue: '16, 24, 40' })
  : ref('16, 24, 40')

const chartColor = computed(() => toRgbColor(chartColorVar.value, '29, 78, 216'))
const axisColor = computed(() => toRgbColor(axisColorVar.value, '71, 84, 103'))
const labelColor = computed(() => toRgbColor(labelColorVar.value, '16, 24, 40'))
const gridLineColor = computed(() => toRgbColor(axisColorVar.value, '229, 231, 235'))

const chartHeight = computed(() => {
  if (!buckets.value.length) {
    return '0px'
  }

  const baseHeight = 220
  const extraHeight = Math.max(0, buckets.value.length - 6) * 16

  return `${Math.min(MAX_CHART_HEIGHT, baseHeight + extraHeight)}px`
})

const xAxisLabelRotation = computed(() => {
  if (buckets.value.length > 6) {
    return 45
  }

  if (buckets.value.length > 3) {
    return 20
  }

  return 0
})

const labelStep = computed(() => {
  const total = buckets.value.length
  if (total <= MAX_VISIBLE_LABELS) {
    return 1
  }

  return Math.max(1, Math.ceil(total / MAX_VISIBLE_LABELS))
})

const chartOptions = computed<EChartsOption | null>(() => {
  if (!buckets.value.length) {
    return null
  }

  const data: RangeBucketDatum[] = buckets.value.map((bucket) => ({
    value: bucket.count ?? 0,
    range: formatBucketLabel(bucket.key, bucket.to, bucket.missing),
    count: bucket.count ?? 0,
    from: parseNumericBound(bucket.key),
    to: parseNumericBound(bucket.to),
    missing: bucket.missing ?? false,
  }))

  const totalBuckets = data.length

  const shouldDisplayLabelAtIndex = (index: number) => {
    if (totalBuckets <= MAX_VISIBLE_LABELS) {
      return true
    }

    if (index === 0 || index === totalBuckets - 1) {
      return true
    }

    return index % labelStep.value === 0
  }

  const tooltipFormatter: TooltipComponentOption['formatter'] = (rawParams) => {
    const params = Array.isArray(rawParams) ? rawParams[0] : rawParams
    const bucket = (params as CallbackDataParams).data as RangeBucketDatum | undefined
    if (!bucket) {
      return ''
    }

    const countLabel = translatePlural('category.products.resultsCount', bucket.count)
    return `<strong>${bucket.range}</strong><br />${countLabel}`
  }

  const series = {
    type: 'bar',
    data,
    barWidth: 28,
    itemStyle: {
      color: chartColor.value,
      borderRadius: [6, 6, 6, 6],
    },
    label: {
      show: true,
      position: 'top',
      color: labelColor.value,
      fontSize: 11,
      formatter: ({ data, dataIndex }: { data?: RangeBucketDatum; dataIndex?: number }) => {
        const index = dataIndex ?? 0
        const bucket = data
        if (!bucket || !shouldDisplayLabelAtIndex(index)) {
          return ''
        }

        return String(bucket.count)
      },
    },
    emphasis: {
      focus: 'self',
    },
    cursor: 'pointer',
  }

  const options = {
    grid: {
      top: 32,
      bottom: 56,
      left: 36,
      right: 24,
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      appendToBody: true,
      confine: true,
      formatter: tooltipFormatter,
    },
    xAxis: {
      type: 'category',
      data: data.map((bucket) => bucket.range),
      axisLine: {
        show: true,
        lineStyle: {
          color: gridLineColor.value,
        },
      },
      axisTick: { show: false },
      axisLabel: {
        color: axisColor.value,
        fontSize: 11,
        interval: 0,
        rotate: xAxisLabelRotation.value,
        overflow: 'truncate',
        hideOverlap: true,
        formatter: (value: string, index: number) => (shouldDisplayLabelAtIndex(index) ? value : ''),
      },
      boundaryGap: true,
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: {
          color: gridLineColor.value,
          opacity: 0.3,
        },
      },
      axisLabel: {
        color: axisColor.value,
        fontSize: 11,
      },
    },
    series: [series],
    animation: false,
  } as EChartsOption

  return options
})

const chartAriaLabel = computed(() => {
  if (!buckets.value.length) {
    return ''
  }

  const entries = buckets.value
    .map((bucket) => {
      const range = formatBucketLabel(bucket.key, bucket.to, bucket.missing)
      const countLabel = translatePlural('category.products.resultsCount', bucket.count ?? 0)
      return `${range}: ${countLabel}`
    })
    .join('; ')

  return `${displayTitle.value}. ${entries}`
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

const onBarClick = (params: CallbackDataParams) => {
  const bucket = params.data as RangeBucketDatum | undefined
  if (!bucket || bucket.missing || !props.field.mapping) {
    return
  }

  const [min, max] = resolveSliderRange(bucket.from, bucket.to)
  localValue.value = [min, max]

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min: bucket.from,
    max: bucket.to,
  })
}

const parseNumericBound = (value: string | number | null | undefined) => {
  if (value == null) {
    return undefined
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const formatBucketLabel = (from?: string | number | null, to?: number | null, missing?: boolean) => {
  if (missing) {
    return t('category.filters.missingLabel')
  }

  if (from == null && to == null) {
    return ''
  }

  if (to == null) {
    return `${from ?? '–'}+`
  }

  return `${from ?? '–'} → ${to}`
}
</script>

<style scoped lang="sass">
.category-filter-numeric
  display: flex
  flex-direction: column
  gap: 0.75rem
  height: 100%

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
    width: 100%
    min-width: 0

  &__chart
    width: 100%
    min-height: 7.5rem
    max-height: 15.625rem

  &__chart-placeholder
    width: 100%
    border-radius: 0.5rem
    background: rgba(var(--v-theme-surface-primary-080), 0.6)
    max-height: 15.625rem
</style>

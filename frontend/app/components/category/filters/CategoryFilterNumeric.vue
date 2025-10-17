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
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { CallbackDataParams } from 'echarts/types/dist/shared'
import type { TooltipComponentOption } from 'echarts/components'

type RangeBucketDatum = {
  value: number
  rangeLabel: string
  count: number
  min: number
  max: number
  selected?: boolean
}

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()
const { translatePlural } = usePluralizedTranslation()

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
    const [firstBucket, secondBucket] = buckets
    const first = Number(firstBucket?.key ?? 0)
    const second = Number(secondBucket?.key ?? 0)
    const diff = Math.abs(second - first)
    return Number.isFinite(diff) && diff > 0 ? diff : undefined
  }

  return props.field.aggregationConfiguration?.interval
})

const localValue = ref<[number, number]>([bounds.value.min, bounds.value.max])

const buckets = computed(() => props.aggregation?.buckets ?? [])

const hasBuckets = computed(() => buckets.value.length > 0)

const selectedRangeIndex = ref<number | null>(null)

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

const chartHeight = computed(() => {
  if (!buckets.value.length) {
    return '0px'
  }

  return `${Math.max(buckets.value.length * 32, 120)}px`
})

const chartOptions = computed<EChartsOption | null>(() => {
  if (!buckets.value.length) {
    return null
  }

  const data: RangeBucketDatum[] = buckets.value.map((bucket, index) => {
    const parsedMin = bucket.key != null ? Number(bucket.key) : Number.NaN
    const parsedMax = bucket.to != null ? Number(bucket.to) : Number.NaN
    const normalizedMin = Number.isFinite(parsedMin) ? parsedMin : bounds.value.min
    const normalizedMax = Number.isFinite(parsedMax) ? parsedMax : bounds.value.max

    return {
      value: bucket.count ?? 0,
      rangeLabel: formatBucketLabel(bucket.key, bucket.to),
      count: bucket.count ?? 0,
      min: normalizedMin,
      max: normalizedMax,
      selected: selectedRangeIndex.value === index,
    }
  })

  const tooltipFormatter: TooltipComponentOption['formatter'] = (rawParams) => {
    const params = Array.isArray(rawParams) ? rawParams[0] : rawParams
    const bucket = (params as CallbackDataParams).data as RangeBucketDatum | undefined
    if (!bucket) {
      return ''
    }

    const countLabel = translatePlural('category.products.resultsCount', bucket.count)
    return `<strong>${bucket.rangeLabel}</strong><br />${countLabel}`
  }

  const series = {
    type: 'bar',
    datasetIndex: 0,
    encode: { x: 'value', y: 'rangeLabel' },
    barWidth: 24,
    barCategoryGap: '0%',
    barGap: '0%',
    itemStyle: {
      color: chartColor.value,
      borderRadius: [6, 6, 6, 6],
      opacity: 0.75,
    },
    selectedMode: 'single',
    select: {
      itemStyle: {
        color: chartColor.value,
        opacity: 1,
      },
      label: {
        color: labelColor.value,
      },
    },
    label: {
      show: true,
      position: 'right',
      color: labelColor.value,
      fontSize: 11,
      formatter: ({ data }: { data?: RangeBucketDatum }) => {
        const bucket = data
        return bucket ? String(bucket.count) : ''
      },
    },
    emphasis: {
      focus: 'self',
    },
  }

  const options = {
    dataset: [
      {
        source: data.map((bucket) => ({
          ...bucket,
        })),
        dimensions: ['rangeLabel', 'value', 'count', 'min', 'max', 'selected'],
      },
    ],
    grid: {
      top: 8,
      bottom: 8,
      left: 4,
      right: 48,
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      appendToBody: true,
      confine: true,
      formatter: tooltipFormatter,
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: {
        color: axisColor.value,
        fontSize: 11,
      },
    },
    yAxis: {
      type: 'category',
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: axisColor.value,
        fontSize: 11,
      },
      splitLine: { show: false },
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
      const range = formatBucketLabel(bucket.key, bucket.to)
      const countLabel = translatePlural('category.products.resultsCount', bucket.count ?? 0)
      return `${range}: ${countLabel}`
    })
    .join('; ')

  return `${displayTitle.value}. ${entries}`
})

const updateSelectedIndexFromFilter = (filter: Filter | null | undefined) => {
  if (!filter || filter.operator !== 'range') {
    selectedRangeIndex.value = null
    return
  }

  const targetMin = filter.min
  const targetMax = filter.max

  const index = buckets.value.findIndex((bucket) => {
    const parsedMin = bucket.key != null ? Number(bucket.key) : Number.NaN
    const parsedMax = bucket.to != null ? Number(bucket.to) : Number.NaN
    const normalizedMin = Number.isFinite(parsedMin) ? parsedMin : bounds.value.min
    const normalizedMax = Number.isFinite(parsedMax) ? parsedMax : bounds.value.max

    const minMatches = targetMin == null || normalizedMin === targetMin
    const maxMatches = targetMax == null || normalizedMax === targetMax

    return minMatches && maxMatches
  })

  selectedRangeIndex.value = index >= 0 ? index : null
}

watch(
  () => bounds.value,
  (next) => {
    const filter = props.modelValue
    if (filter?.operator === 'range') {
      const nextMin = filter.min ?? next.min
      const nextMax = filter.max ?? next.max
      localValue.value = [nextMin, nextMax]
    } else {
      localValue.value = [next.min, next.max]
    }

    updateSelectedIndexFromFilter(props.modelValue)
  },
  { immediate: true },
)

watch(
  () => props.modelValue,
  (filter) => {
    if (filter?.operator === 'range') {
      const nextMin = filter.min ?? bounds.value.min
      const nextMax = filter.max ?? bounds.value.max
      localValue.value = [nextMin, nextMax]
      updateSelectedIndexFromFilter(filter)
      return
    }

    localValue.value = [bounds.value.min, bounds.value.max]
    selectedRangeIndex.value = null
  },
  { immediate: true },
)

watch(
  () => buckets.value,
  () => {
    updateSelectedIndexFromFilter(props.modelValue)
  },
  { immediate: true },
)

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

const applyBucketRange = (bucket: RangeBucketDatum, index: number) => {
  const min = bucket.min
  const max = bucket.max

  localValue.value = [min, max]
  selectedRangeIndex.value = index

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min,
    max,
  })
}

const onBarClick = (params: CallbackDataParams) => {
  const bucket = params.data as RangeBucketDatum | undefined
  if (!bucket || params.dataIndex == null) {
    return
  }

  applyBucketRange(bucket, params.dataIndex)
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

  &__chart
    width: 100%
    min-height: 7.5rem

  &__chart-placeholder
    width: 100%
    border-radius: 0.5rem
    background: rgba(var(--v-theme-surface-primary-080), 0.6)
</style>

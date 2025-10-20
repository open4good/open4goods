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

type RangeBucketDatum = {
  value: number
  range: string
  count: number
}

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
      return
    }

    localValue.value = [bounds.value.min, bounds.value.max]
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

  return `${Math.max(buckets.value.length * 32, 120)}px`
})

const chartOptions = computed<EChartsOption | null>(() => {
  if (!buckets.value.length) {
    return null
  }

  const data: RangeBucketDatum[] = buckets.value.map((bucket) => ({
    value: bucket.count ?? 0,
    range: formatBucketLabel(bucket.key, bucket.to),
    count: bucket.count ?? 0,
  }))

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
    barWidth: 12,
    itemStyle: {
      color: chartColor.value,
      borderRadius: [6, 6, 6, 6],
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
    grid: {
      top: 8,
      bottom: 8,
      left: 4,
      right: 48,
      containLabel: false,
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
    yAxis: {
      type: 'category',
      data: data.map((_, index) => index + 1),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
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

  &__chart
    width: 100%
    min-height: 7.5rem

  &__chart-placeholder
    width: 100%
    border-radius: 0.5rem
    background: rgba(var(--v-theme-surface-primary-080), 0.6)
</style>

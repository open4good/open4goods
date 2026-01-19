<template>
  <div class="category-filter-numeric">
    <div class="category-filter-numeric__header">
      <p class="category-filter-numeric__title">{{ displayTitle }}</p>
      <p v-if="rangeLabel" class="category-filter-numeric__range">
        {{ rangeLabel }}
      </p>
    </div>

    <div ref="chartContainer" class="category-filter-numeric__chart-container">
      <ClientOnly>
        <VueECharts
          v-if="chartOptions && canRenderChart"
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
import { defineAsyncComponent, ref, computed } from 'vue'
import { useElementSize } from '@vueuse/core'
import type {
  AggregationResponseDto,
  FieldMetadataDto,
  Filter,
} from '~~/shared/api-client'
import { resolveFilterFieldTitle } from '~/utils/_field-localization'
import { formatNumericRangeValue } from '~/utils/_number-formatting'
import type { EChartsOption } from 'echarts'
import type { CallbackDataParams } from 'echarts/types/dist/shared'
import type { TooltipComponentOption } from 'echarts/components'
import {
  clampSliderRange,
  isPriceField,
  priceToSliderValue,
  sliderValueToPrice,
} from './price-scale'
import { ensureECharts } from '~/utils/echarts-loader'

let echartsRegistered = false

const VueECharts = defineAsyncComponent(async () => {
  if (import.meta.client) {
    const echarts = await ensureECharts([
      'BarChart',
      'GridComponent',
      'TooltipComponent',
      'CanvasRenderer',
    ])

    if (echarts && !echartsRegistered) {
      echartsRegistered = true
      const { core, modules } = echarts
      core.use(modules)
    }
  }

  const module = await import(
    /* webpackChunkName: "vendor-echarts" */ 'vue-echarts'
  )

  return module.default
})

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
  baselineAggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()
const chartContainer = ref<HTMLElement | null>(null)
const { width } = useElementSize(chartContainer)
const canRenderChart = computed(() => width.value > 0)

const { t, n, locale } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const displayTitle = computed(() => resolveFilterFieldTitle(props.field, t))

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
  return formatNumericRangeValue(resolvedValue, n, {
    isPrice: priceField.value,
  })
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

  return formatNumericRangeValue(value, n, { isPrice: priceField.value })
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

const hasBuckets = computed(() => buckets.value.length > 0)

const toColorWithAlpha = (
  value: string | undefined,
  fallbackRaw: string,
  alpha = 1
) => {
  const normalized = value?.trim()
  if (!normalized) {
    return alpha === 1
      ? `rgb(${fallbackRaw})`
      : `rgba(${fallbackRaw}, ${alpha})`
  }

  if (normalized.startsWith('#')) {
    return normalized
  }

  if (normalized.startsWith('rgba')) {
    return normalized
  }

  if (normalized.startsWith('rgb')) {
    if (alpha === 1) {
      return normalized
    }

    return normalized.replace(/^rgb\((.+)\)$/i, `rgba($1, ${alpha})`)
  }

  return alpha === 1 ? `rgb(${normalized})` : `rgba(${normalized}, ${alpha})`
}

const chartColorVar = import.meta.client
  ? useCssVar('--v-theme-chart-range-bar', document.documentElement, {
      initialValue: '33, 150, 243',
    })
  : ref('33, 150, 243')

const axisColorVar = import.meta.client
  ? useCssVar('--v-theme-text-neutral-secondary', document.documentElement, {
      initialValue: '71, 84, 103',
    })
  : ref('71, 84, 103')

const labelColorVar = import.meta.client
  ? useCssVar('--v-theme-text-neutral-strong', document.documentElement, {
      initialValue: '16, 24, 40',
    })
  : ref('16, 24, 40')

const chartBarColor = computed(() =>
  toColorWithAlpha(chartColorVar.value, '33, 150, 243', 0.75)
)
const chartBarHoverColor = computed(() =>
  toColorWithAlpha(chartColorVar.value, '33, 150, 243', 1)
)
const axisColor = computed(() =>
  toColorWithAlpha(axisColorVar.value, '71, 84, 103')
)
const labelColor = computed(() =>
  toColorWithAlpha(labelColorVar.value, '16, 24, 40')
)
const gridLineColor = computed(() =>
  toColorWithAlpha(axisColorVar.value, '229, 231, 235')
)
const pointerShadowColor = computed(() =>
  toColorWithAlpha(axisColorVar.value, '148, 163, 184', 0.18)
)
const CHART_HORIZONTAL_PADDING = 8

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

  const data: RangeBucketDatum[] = buckets.value.map(bucket => ({
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

  const tooltipFormatter: TooltipComponentOption['formatter'] = rawParams => {
    const params = Array.isArray(rawParams) ? rawParams[0] : rawParams
    const bucket = (params as CallbackDataParams).data as
      | RangeBucketDatum
      | undefined
    if (!bucket) {
      return ''
    }

    const countLabel = translatePlural(
      'category.products.resultsCount',
      bucket.count
    )
    return `<strong>${bucket.range}</strong><br />${countLabel}`
  }

  const series = {
    type: 'bar',
    data,
    barWidth: 28,
    itemStyle: {
      color: chartBarColor.value,
      borderRadius: [6, 6, 6, 6],
    },
    label: {
      show: true,
      position: 'top',
      color: labelColor.value,
      fontSize: 11,
      formatter: ({
        data,
        dataIndex,
      }: {
        data?: RangeBucketDatum
        dataIndex?: number
      }) => {
        const index = dataIndex ?? 0
        const bucket = data
        if (!bucket || !shouldDisplayLabelAtIndex(index)) {
          return ''
        }

        return String(bucket.count)
      },
    },
    emphasis: {
      focus: 'series',
      itemStyle: {
        color: chartBarHoverColor.value,
      },
    },
    cursor: 'pointer',
  }

  const options = {
    grid: {
      top: 32,
      bottom: 56,
      left: CHART_HORIZONTAL_PADDING,
      right: CHART_HORIZONTAL_PADDING,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: pointerShadowColor.value,
        },
      },
      appendToBody: true,
      confine: true,
      formatter: tooltipFormatter,
    },
    xAxis: {
      type: 'category',
      data: data.map(bucket => bucket.range),
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
        formatter: (value: string, index: number) =>
          shouldDisplayLabelAtIndex(index) ? value : '',
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
    .map(bucket => {
      const range = formatBucketLabel(bucket.key, bucket.to, bucket.missing)
      const countLabel = translatePlural(
        'category.products.resultsCount',
        bucket.count ?? 0
      )
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

const formatBucketLabel = (
  from?: string | number | null,
  to?: number | null,
  missing?: boolean
) => {
  if (missing) {
    return t('category.filters.missingLabel')
  }

  if (from == null && to == null) {
    return ''
  }

  if (to == null) {
    return `${formatBoundary(from)}+`
  }

  return `${formatBoundary(from)} → ${formatBoundary(to)}`
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

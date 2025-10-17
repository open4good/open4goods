<template>
  <div class="category-filter-numeric">
    <div class="category-filter-numeric__header">
      <div class="category-filter-numeric__heading">
        <p class="category-filter-numeric__title">{{ displayTitle }}</p>
        <p v-if="overallRangeLabel" class="category-filter-numeric__caption">
          {{ overallRangeLabel }}
        </p>
      </div>
      <div class="category-filter-numeric__actions">
        <p v-if="rangeLabel" class="category-filter-numeric__range">
          {{ rangeLabel }}
        </p>
        <v-btn
          variant="text"
          density="comfortable"
          class="category-filter-numeric__reset"
          :disabled="!hasActiveRange"
          @click="clearSelection"
        >
          {{ t('category.filters.rangeClear') }}
        </v-btn>
      </div>
    </div>

    <p v-if="selectionHint" class="category-filter-numeric__hint">
      {{ selectionHint }}
    </p>

    <div v-if="hasBuckets" class="category-filter-numeric__chart-wrapper">
      <ClientOnly>
        <VChart
          :option="chartOptions"
          :on-events="chartEvents"
          :style="{ height: chartHeight }"
          :aria-label="ariaLabel"
          role="img"
          autoresize
          class="category-filter-numeric__chart"
        />
      </ClientOnly>
    </div>
    <div v-else class="category-filter-numeric__empty">
      {{ t('category.filters.noMatch') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useCssVar } from '@vueuse/core'
import type { EChartsOption } from 'echarts'
import { use as useECharts } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { VChart } from 'vue-echarts'
import { useI18n } from 'vue-i18n'
import type { AggregationResponseDto, FieldMetadataDto, Filter } from '~~/shared/api-client'

useECharts([GridComponent, TooltipComponent, BarChart, CanvasRenderer])

type NormalizedBucket = {
  from?: number
  to?: number
  count: number
  label: string
}

const props = defineProps<{
  field: FieldMetadataDto
  aggregation?: AggregationResponseDto
  modelValue?: Filter | null
}>()

const emit = defineEmits<{ 'update:modelValue': [Filter | null] }>()

const { t } = useI18n()

const cssVarTarget = ref<HTMLElement | null>(null)
if (import.meta.client) {
  cssVarTarget.value = document.documentElement
}

const accentPrimary = useCssVar('--v-theme-accent-primary-highlight', cssVarTarget)
const textNeutralSecondaryVar = useCssVar('--v-theme-text-neutral-secondary', cssVarTarget)
const borderPrimaryStrongVar = useCssVar('--v-theme-border-primary-strong', cssVarTarget)

const displayTitle = computed(() => props.field.title ?? props.field.mapping ?? '')

const ariaLabel = computed(() => `${displayTitle.value} ${t('category.filters.rangeAriaSuffix')}`)

const toNumber = (value?: number | string | null) => {
  if (value == null) {
    return undefined
  }

  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

const buckets = computed<NormalizedBucket[]>(() => {
  return (props.aggregation?.buckets ?? []).map((bucket) => ({
    from: toNumber(bucket.key as number | string | null),
    to: toNumber(bucket.to as number | string | null),
    count: bucket.count ?? 0,
    label: formatBucketLabel(bucket.key, bucket.to),
  }))
})

const hasBuckets = computed(() => buckets.value.length > 0)

const selectionHint = computed(() => (hasBuckets.value ? t('category.filters.rangeSelectionHint') : ''))

const chartHeight = computed(() => `${Math.max(buckets.value.length * 36, 160)}px`)

const hasActiveRange = computed(() => {
  return props.modelValue?.operator === 'range' && (props.modelValue.min != null || props.modelValue.max != null)
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

const overallRangeLabel = computed(() => {
  const min = props.aggregation?.min
  const max = props.aggregation?.max

  if (min == null && max == null) {
    return null
  }

  return `${min ?? '–'} → ${max ?? '–'}`
})

const pendingStartIndex = ref<number | null>(null)

watch(
  () => props.modelValue,
  () => {
    pendingStartIndex.value = null
  },
)

watch(hasBuckets, (available) => {
  if (!available) {
    pendingStartIndex.value = null
  }
})

const accentColor = computed(() => {
  const value = accentPrimary.value?.trim()
  return value ? `rgb(${value})` : '#2196F3'
})

const accentMutedColor = computed(() => {
  const value = accentPrimary.value?.trim()
  return value ? `rgba(${value}, 0.25)` : 'rgba(33, 150, 243, 0.25)'
})

const textNeutralSecondaryColor = computed(() => {
  const value = textNeutralSecondaryVar.value?.trim()
  return value ? `rgb(${value})` : '#475467'
})

const gridLineColor = computed(() => {
  const value = borderPrimaryStrongVar.value?.trim()
  return value ? `rgba(${value}, 0.45)` : 'rgba(198, 221, 244, 0.45)'
})

const activeRangeIndices = computed(() => getIndicesForFilter(props.modelValue))

const highlightedIndices = computed(() => {
  const indices = new Set<number>()
  const { start, end } = activeRangeIndices.value
  if (start != null && end != null) {
    for (let index = start; index <= end; index += 1) {
      indices.add(index)
    }
  }

  if (pendingStartIndex.value != null) {
    indices.add(pendingStartIndex.value)
  }

  return indices
})

const chartData = computed(() => {
  return buckets.value.map((bucket, index) => ({
    value: bucket.count,
    itemStyle: {
      color: highlightedIndices.value.has(index) ? accentColor.value : accentMutedColor.value,
      borderRadius: [0, 8, 8, 0],
    },
  }))
})

const chartOptions = computed<EChartsOption>(() => {
  if (!buckets.value.length) {
    return {}
  }

  return {
    animation: false,
    grid: { left: 0, right: 16, top: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const index = 'dataIndex' in params ? Number(params.dataIndex) : NaN
        if (!Number.isFinite(index)) {
          return ''
        }

        const bucket = buckets.value[index]
        const label = bucket?.label ?? ''
        const count = bucket?.count ?? 0
        const countLabel = t('category.products.resultsCount.other', { count })
        return `<strong>${label}</strong><br/>${countLabel}`
      },
      extraCssText: 'text-align: left',
    },
    xAxis: {
      type: 'value',
      axisLabel: {
        color: textNeutralSecondaryColor.value,
      },
      splitLine: {
        lineStyle: {
          color: gridLineColor.value,
        },
      },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: buckets.value.map((bucket) => bucket.label),
      axisLabel: {
        color: textNeutralSecondaryColor.value,
        overflow: 'truncate',
      },
    },
    series: [
      {
        type: 'bar',
        barWidth: 18,
        data: chartData.value,
        emphasis: {
          itemStyle: {
            color: accentColor.value,
          },
        },
      },
    ],
  }
})

const chartEvents = {
  click: handleChartClick,
  dblclick: () => {
    pendingStartIndex.value = null
    if (hasActiveRange.value) {
      clearSelection()
    }
  },
}

function handleChartClick(event: { dataIndex?: number }) {
  if (event?.dataIndex == null) {
    return
  }

  const index = Number(event.dataIndex)
  if (!Number.isFinite(index)) {
    return
  }

  if (pendingStartIndex.value == null) {
    pendingStartIndex.value = index
    return
  }

  const start = pendingStartIndex.value
  const end = index
  pendingStartIndex.value = null
  emitSelection(Math.min(start, end), Math.max(start, end))
}

function getIndicesForFilter(filter?: Filter | null) {
  if (!filter || filter.operator !== 'range' || !buckets.value.length) {
    return { start: null, end: null }
  }

  const min = filter.min ?? Number.NEGATIVE_INFINITY
  const max = filter.max ?? Number.POSITIVE_INFINITY

  let startIndex = 0
  let endIndex = buckets.value.length - 1

  if (Number.isFinite(min)) {
    const found = buckets.value.findIndex((bucket) => {
      const bucketEnd = bucket.to ?? Number.POSITIVE_INFINITY
      return min < bucketEnd + Number.EPSILON
    })
    startIndex = found === -1 ? buckets.value.length - 1 : found
  }

  if (Number.isFinite(max)) {
    for (let index = buckets.value.length - 1; index >= 0; index -= 1) {
      const bucketStart = buckets.value[index].from ?? Number.NEGATIVE_INFINITY
      if (max >= bucketStart - Number.EPSILON) {
        endIndex = index
        break
      }
    }
  }

  if (startIndex > endIndex) {
    ;[startIndex, endIndex] = [endIndex, startIndex]
  }

  return { start: startIndex, end: endIndex }
}

function emitSelection(startIndex: number, endIndex: number) {
  const mapping = props.field.mapping
  if (!mapping) {
    return
  }

  const startBucket = buckets.value[startIndex]
  const endBucket = buckets.value[endIndex]

  if (!startBucket || !endBucket) {
    return
  }

  emit('update:modelValue', {
    field: mapping,
    operator: 'range',
    min: startBucket.from ?? props.aggregation?.min ?? undefined,
    max: endBucket.to ?? props.aggregation?.max ?? undefined,
  })
}

function clearSelection() {
  emit('update:modelValue', null)
}

function formatBucketLabel(from?: unknown, to?: unknown) {
  if (from == null && to == null) {
    return ''
  }

  const fromLabel = from == null ? '–' : String(from)

  if (to == null) {
    return `${fromLabel}+`
  }

  return `${fromLabel} → ${to}`
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
    align-items: flex-start
    gap: 1rem

  &__heading
    display: flex
    flex-direction: column
    gap: 0.25rem

  &__title
    font-size: 1rem
    font-weight: 600
    margin: 0

  &__caption
    margin: 0
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__actions
    display: flex
    align-items: center
    gap: 0.75rem

  &__range
    margin: 0
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-variant-numeric: tabular-nums

  &__reset:deep(.v-btn__content)
    text-transform: none

  &__hint
    margin: 0
    font-size: 0.75rem
    color: rgb(var(--v-theme-text-neutral-soft))

  &__chart-wrapper
    border-radius: 0.75rem
    background: rgb(var(--v-theme-surface-glass))
    padding: 0.75rem

  &__chart
    width: 100%

  &__empty
    font-size: 0.875rem
    color: rgb(var(--v-theme-text-neutral-secondary))
    background: rgb(var(--v-theme-surface-glass))
    border-radius: 0.75rem
    padding: 0.75rem
    text-align: center

@media (max-width: 959px)
  .category-filter-numeric__chart-wrapper
    padding: 0.5rem
</style>

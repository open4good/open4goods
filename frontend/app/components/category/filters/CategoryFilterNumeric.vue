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
      <Bar
        v-if="hasBuckets"
        :data="chartData"
        :options="chartOptions"
        :plugins="chartPlugins"
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
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  LinearScale,
  Tooltip,
  type ActiveElement,
  type ChartData,
  type ChartOptions,
  type Plugin,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Tooltip)

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

const chartHeight = computed(() => {
  if (!buckets.value.length) {
    return '0px'
  }

  return `${Math.max(buckets.value.length * 32, 120)}px`
})

const chartData = computed<ChartData<'bar'>>(() => ({
  labels: buckets.value.map((bucket) => formatBucketLabel(bucket.key, bucket.to)),
  datasets: [
    {
      label: displayTitle.value,
      data: buckets.value.map((bucket) => bucket.count ?? 0),
      backgroundColor: chartColor.value,
      hoverBackgroundColor: chartColor.value,
      borderRadius: 6,
      borderSkipped: false,
      barPercentage: 1,
      categoryPercentage: 1,
    },
  ],
}))

const normalizeBoundary = (value: number | string | undefined, fallback: number) => {
  if (value == null) {
    return fallback
  }

  const numeric = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(numeric) ? numeric : fallback
}

const applyRange = (min?: number | string, max?: number) => {
  const normalizedMin = normalizeBoundary(min, bounds.value.min)
  const normalizedMax = normalizeBoundary(max, bounds.value.max)

  localValue.value = [normalizedMin, normalizedMax]

  emit('update:modelValue', {
    field: props.field.mapping,
    operator: 'range',
    min: normalizedMin,
    max: normalizedMax,
  })
}

const chartOptions = computed<ChartOptions<'bar'>>(() => ({
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y',
  animation: false,
  layout: {
    padding: {
      right: 40,
      top: 8,
      bottom: 8,
      left: 0,
    },
  },
  scales: {
    x: {
      beginAtZero: true,
      ticks: {
        color: axisColor.value,
        font: {
          size: 11,
        },
      },
      grid: {
        display: false,
        drawBorder: false,
      },
      border: {
        display: false,
      },
    },
    y: {
      ticks: {
        color: axisColor.value,
        font: {
          size: 11,
        },
        padding: 8,
      },
      grid: {
        display: false,
        drawBorder: false,
      },
      border: {
        display: false,
      },
    },
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      displayColors: false,
      callbacks: {
        title: (items) => items[0]?.label ?? '',
        label: (item) => translatePlural('category.products.resultsCount', Number(item.raw ?? 0)),
      },
    },
  },
  onClick: (_event, elements) => {
    if (!elements.length) {
      return
    }

    const [{ index }] = elements as ActiveElement[]
    const bucket = buckets.value[index]
    if (!bucket || bucket.missing) {
      return
    }

    applyRange(bucket.key, bucket.to)
  },
  datasets: {
    bar: {
      borderRadius: 6,
      borderSkipped: false,
      barPercentage: 1,
      categoryPercentage: 1,
    },
  },
}))

const chartPlugins = computed<Plugin<'bar'>[]>(() => [
  {
    id: 'rangeValueLabels',
    afterDatasetsDraw(chart) {
      const { ctx } = chart
      const dataset = chart.getDatasetMeta(0)
      ctx.save()
      ctx.fillStyle = labelColor.value
      ctx.font = '11px var(--v-font-family, "Inter", sans-serif)'
      ctx.textAlign = 'left'
      ctx.textBaseline = 'middle'

      dataset.data.forEach((element, index) => {
        const rawValue = chart.data.datasets[0]?.data?.[index]
        const numericValue = typeof rawValue === 'number' ? rawValue : Number(rawValue ?? 0)
        if (!Number.isFinite(numericValue)) {
          return
        }

        const barElement = element as BarElement
        const text = String(numericValue)
        const padding = 8
        const measured = ctx.measureText(text).width
        const maxX = chart.chartArea.right - measured
        const minX = chart.chartArea.left + padding
        const proposed = barElement.x + padding
        const textX = Math.min(Math.max(proposed, minX), maxX)
        ctx.fillText(text, textX, barElement.y)
      })

      ctx.restore()
    },
  },
])

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
  applyRange(min, max)
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

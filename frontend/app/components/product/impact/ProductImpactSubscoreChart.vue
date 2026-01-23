<template>
  <div v-if="hasData" class="impact-subscore-chart">
    <v-btn-toggle
      v-if="hasNormalizedView"
      v-model="viewMode"
      density="compact"
      class="impact-subscore-chart__toggle"
      mandatory
    >
      <v-btn value="absolute" variant="text" size="small">
        {{ t('product.impact.subscoreChart.toggleAbsolute') }}
      </v-btn>
      <v-btn value="normalized" variant="text" size="small">
        {{ t('product.impact.subscoreChart.toggleNormalized') }}
      </v-btn>
    </v-btn-toggle>
    <ClientOnly>
      <VueECharts
        v-if="chartOption"
        :option="chartOption"
        :autoresize="true"
        class="impact-subscore-chart__echart"
      />
      <template #fallback>
        <div class="impact-subscore-chart__placeholder" />
      </template>
    </ClientOnly>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import type { DistributionBucket, ScoreNormalizationParams } from './impact-types'
import { ensureECharts } from '~/utils/echarts-loader'

let echartsRegistered = false

const props = defineProps<{
  distribution: DistributionBucket[]
  label: string
  averageValue?: number | null
  currentValue?: number | null
  normalizedCurrentValue?: number | null
  normalizationMethod?: string | null
  normalizationParams?: ScoreNormalizationParams | null
  scale?: { min?: number | null; max?: number | null } | null
  impactBetterIs?: 'GREATER' | 'LOWER' | null
  stdDev?: number | null
  productName?: string
}>()

const { t } = useI18n()
const viewMode = ref<'absolute' | 'normalized'>('absolute')

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

const filteredDistribution = computed(() =>
  props.distribution.filter(
    bucket => bucket.label.toUpperCase() !== 'ES-UNKNOWN'
  )
)

const hasData = computed(() => filteredDistribution.value.length > 0)
const hasNormalizedView = computed(() => resolveNormalizationAvailability().supported)

type BucketRange = {
  min?: number
  max?: number
  exact?: number
}

const parseNumber = (value: string): number | null => {
  const normalized = value.replace(/,/g, '.')
  const match = normalized.match(/-?\d+(\.\d+)?/)
  if (!match) {
    return null
  }

  const parsed = Number.parseFloat(match[0])
  return Number.isFinite(parsed) ? parsed : null
}

const parseBucketRange = (label: string): BucketRange | null => {
  const trimmed = label.trim()
  if (!trimmed) {
    return null
  }

  const rangeMatch = trimmed.match(
    /(-?\d+(?:[.,]\d+)?)\s*[-–—]\s*(-?\d+(?:[.,]\d+)?)/
  )
  if (rangeMatch) {
    const min = parseNumber(rangeMatch[1])
    const max = parseNumber(rangeMatch[2])
    if (min != null && max != null) {
      return { min, max }
    }
  }

  if (trimmed.includes('+')) {
    const min = parseNumber(trimmed)
    if (min != null) {
      return { min }
    }
  }

  if (/[<≤]/.test(trimmed)) {
    const max = parseNumber(trimmed)
    if (max != null) {
      return { max }
    }
  }

  if (/[>≥]/.test(trimmed)) {
    const min = parseNumber(trimmed)
    if (min != null) {
      return { min }
    }
  }

  const exact = parseNumber(trimmed)
  if (exact != null) {
    return { exact }
  }

  return null
}

const resolveBucketIndex = (
  value: number | null | undefined,
  buckets: DistributionBucket[]
): number | null => {
  if (value == null || !Number.isFinite(value)) {
    return null
  }

  const ranges = buckets.map(bucket => parseBucketRange(bucket.label))

  for (let index = 0; index < ranges.length; index += 1) {
    const range = ranges[index]
    if (!range) {
      continue
    }

    if (range.exact != null && value === range.exact) {
      return index
    }

    if (range.min != null && range.max != null) {
      if (value >= range.min && value <= range.max) {
        return index
      }
      continue
    }

    if (range.min != null && range.max == null && value >= range.min) {
      return index
    }

    if (range.max != null && range.min == null && value <= range.max) {
      return index
    }
  }

  const numericCandidates = ranges
    .map((range, index) => {
      if (!range) {
        return null
      }

      if (range.exact != null) {
        return { index, value: range.exact }
      }

      if (range.min != null && range.max != null) {
        return { index, value: (range.min + range.max) / 2 }
      }

      if (range.min != null) {
        return { index, value: range.min }
      }

      if (range.max != null) {
        return { index, value: range.max }
      }

      return null
    })
    .filter(
      (candidate): candidate is { index: number; value: number } =>
        candidate != null
    )

  if (!numericCandidates.length) {
    return null
  }

  const closest = numericCandidates.reduce((best, candidate) =>
    Math.abs(candidate.value - value) < Math.abs(best.value - value)
      ? candidate
      : best
  )

  return closest.index
}

const resolveProductName = computed(() => {
  const candidate = props.productName?.trim()
  return candidate?.length
    ? candidate
    : t('product.impact.subscoreChart.unknownProduct')
})

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  const displayDistribution =
    viewMode.value === 'normalized' && hasNormalizedView.value
      ? buildNormalizedDistribution()
      : filteredDistribution.value
  const dataValues = displayDistribution.map(bucket => bucket.value)
  const maxValue = Math.max(0, ...dataValues)
  const yAxisMax = Math.max(1, Math.ceil(maxValue * 1.2))
  const productValue =
    viewMode.value === 'normalized' && hasNormalizedView.value
      ? props.normalizedCurrentValue ?? null
      : props.currentValue
  const averageValue =
    viewMode.value === 'normalized' && hasNormalizedView.value
      ? resolveNormalizedAverage()
      : props.averageValue
  const productIndex = resolveBucketIndex(productValue, displayDistribution)
  const averageIndex = resolveBucketIndex(averageValue, displayDistribution)

  const buildMarkerSeries = (
    name: string,
    index: number | null,
    color: string,
    tooltipKey: string,
    tooltipParams: Record<string, string>
  ) => {
    if (index == null) {
      return null
    }

    return {
      type: 'bar',
      name,
      barGap: '-100%',
      barWidth: '90%',
      data: filteredDistribution.value.map((_, bucketIndex) =>
        bucketIndex === index ? yAxisMax : 0
      ),
      itemStyle: {
        color,
      },
      emphasis: {
        itemStyle: {
          opacity: 0.35,
        },
      },
      tooltip: {
        trigger: 'item',
        position: 'top',
        formatter: () => t(tooltipKey, tooltipParams),
      },
      z: 1,
    }
  }

  const markerSeries = [
    buildMarkerSeries(
      'average',
      averageIndex,
      'rgba(148, 163, 184, 0.28)',
      'product.impact.subscoreChart.markerAverage',
      { scoreLabel: props.label }
    ),
    buildMarkerSeries(
      'product',
      productIndex,
      'rgba(33, 150, 243, 0.22)',
      'product.impact.subscoreChart.markerProduct',
      { productName: resolveProductName.value }
    ),
  ].filter(
    (
      series
    ): series is NonNullable<ReturnType<typeof buildMarkerSeries>> =>
      series != null
  )

  return {
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: displayDistribution.map(bucket => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value', max: yAxisMax },
    series: [
      {
        type: 'bar',
        name: props.label,
        data: dataValues,
        itemStyle: {
          color: 'rgba(33, 150, 243, 0.75)',
        },
        z: 2,
      },
      ...markerSeries,
    ],
  }
})

const resolveNormalizationAvailability = () => {
  const method = (props.normalizationMethod ?? 'SIGMA').toUpperCase()
  if (method === 'SIGMA') {
    return {
      supported:
        typeof props.averageValue === 'number' &&
        typeof props.stdDev === 'number',
    }
  }
  if (method === 'MINMAX_FIXED') {
    return {
      supported:
        typeof props.normalizationParams?.fixedMin === 'number' &&
        typeof props.normalizationParams?.fixedMax === 'number',
    }
  }
  if (method === 'FIXED_MAPPING') {
    return {
      supported: Boolean(props.normalizationParams?.mapping),
    }
  }
  if (method === 'BINARY') {
    return {
      supported: typeof props.normalizationParams?.threshold === 'number',
    }
  }
  if (method === 'CONSTANT') {
    return {
      supported: typeof props.normalizationParams?.constantValue === 'number',
    }
  }

  return { supported: false }
}

const resolveScaleMin = () => props.scale?.min ?? 0
const resolveScaleMax = () => props.scale?.max ?? 5

const normalizeValue = (value: number | null | undefined): number | null => {
  if (value == null || !Number.isFinite(value)) {
    return null
  }

  const method = (props.normalizationMethod ?? 'SIGMA').toUpperCase()
  if (method === 'SIGMA') {
    if (
      typeof props.averageValue !== 'number' ||
      typeof props.stdDev !== 'number'
    ) {
      return null
    }
    const sigmaK = props.normalizationParams?.sigmaK ?? 2
    const lowerBound = props.averageValue - sigmaK * props.stdDev
    const upperBound = props.averageValue + sigmaK * props.stdDev
    if (Math.abs(upperBound - lowerBound) < 0.000001) {
      return (resolveScaleMin() + resolveScaleMax()) / 2
    }
    const normalized = (value - lowerBound) / (upperBound - lowerBound)
    const scaled = normalized * resolveScaleMax()
    return applyImpactDirection(clampValue(scaled))
  }

  if (method === 'MINMAX_FIXED') {
    const min = props.normalizationParams?.fixedMin
    const max = props.normalizationParams?.fixedMax
    if (typeof min !== 'number' || typeof max !== 'number') {
      return null
    }
    const normalized = (value - min) / (max - min)
    const scaled = normalized * resolveScaleMax()
    return applyImpactDirection(clampValue(scaled))
  }

  if (method === 'FIXED_MAPPING') {
    const mapping = props.normalizationParams?.mapping
    if (!mapping) {
      return null
    }
    const key = Number.isFinite(value) ? String(value) : ''
    const mapped = mapping[key] ?? mapping[String(value)]
    if (typeof mapped !== 'number') {
      return null
    }
    return applyImpactDirection(clampValue(mapped))
  }

  if (method === 'BINARY') {
    const threshold = props.normalizationParams?.threshold
    if (typeof threshold !== 'number') {
      return null
    }
    const greaterIsPass = props.normalizationParams?.greaterIsPass ?? true
    const pass = greaterIsPass ? value >= threshold : value <= threshold
    return applyImpactDirection(pass ? resolveScaleMax() : resolveScaleMin())
  }

  if (method === 'CONSTANT') {
    const constantValue = props.normalizationParams?.constantValue
    if (typeof constantValue !== 'number') {
      return null
    }
    return applyImpactDirection(clampValue(constantValue))
  }

  return null
}

const clampValue = (value: number) => {
  const min = resolveScaleMin()
  const max = resolveScaleMax()
  return Math.max(min, Math.min(max, value))
}

const applyImpactDirection = (value: number) => {
  if (props.impactBetterIs !== 'LOWER') {
    return value
  }
  return resolveScaleMax() - value + resolveScaleMin()
}

const buildNormalizedDistribution = () =>
  filteredDistribution.value.map(bucket => {
    const range = parseBucketRange(bucket.label)
    if (!range) {
      return bucket
    }

    if (range.exact != null) {
      const normalized = normalizeValue(range.exact)
      return {
        ...bucket,
        label: normalized != null ? formatBucketValue(normalized) : bucket.label,
      }
    }

    const min = range.min != null ? normalizeValue(range.min) : null
    const max = range.max != null ? normalizeValue(range.max) : null
    if (min != null && max != null) {
      return {
        ...bucket,
        label: `${formatBucketValue(min)}–${formatBucketValue(max)}`,
      }
    }

    if (min != null) {
      return {
        ...bucket,
        label: `≥ ${formatBucketValue(min)}`,
      }
    }

    if (max != null) {
      return {
        ...bucket,
        label: `≤ ${formatBucketValue(max)}`,
      }
    }

    return bucket
  })

const formatBucketValue = (value: number) =>
  new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
  }).format(value)

const resolveNormalizedAverage = () => normalizeValue(props.averageValue)
</script>

<style scoped>
.impact-subscore-chart {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.impact-subscore-chart__toggle {
  align-self: flex-start;
}

.impact-subscore-chart__echart {
  height: 220px;
}

.impact-subscore-chart__placeholder {
  height: 220px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.15);
}
</style>

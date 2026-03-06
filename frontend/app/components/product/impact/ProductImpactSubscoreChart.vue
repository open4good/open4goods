<template>
  <div v-if="hasData" class="impact-subscore-chart">
    <p class="impact-subscore-chart__title">
      {{ t('product.impact.subscoreChart.title') }}
    </p>
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
import { computed, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import type { DistributionBucket } from './impact-types'
import { ensureECharts } from '~/utils/echarts-loader'

let echartsRegistered = false

const props = defineProps<{
  distribution: DistributionBucket[]
  label: string
  averageValue?: number | null
  currentValue?: number | null
  impactBetterIs?: 'GREATER' | 'LOWER' | null
  productName?: string
  numericMapping?: Record<string, number> | null
}>()

const { t } = useI18n()

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

const COLOR_BAD: [number, number, number] = [239, 68, 68]
const COLOR_AVG: [number, number, number] = [33, 150, 243]
const COLOR_GOOD: [number, number, number] = [34, 197, 94]

const interpolateColor = (
  color1: [number, number, number],
  color2: [number, number, number],
  factor: number
): [number, number, number] => {
  return [
    Math.round(color1[0] + (color2[0] - color1[0]) * factor),
    Math.round(color1[1] + (color2[1] - color1[1]) * factor),
    Math.round(color1[2] + (color2[2] - color1[2]) * factor),
  ]
}

const resolveProductColor = (
  index: number,
  averageIndex: number | null,
  count: number,
  betterIs: string | null | undefined
): [number, number, number] => {
  if (averageIndex == null) {
    // Fallback: simple gradient Red -> Green
    const isLowerBetter = betterIs === 'LOWER'
    const bestIndex = isLowerBetter ? 0 : count - 1
    const worstIndex = isLowerBetter ? count - 1 : 0
    const total = Math.abs(bestIndex - worstIndex) || 1
    const factor = Math.abs(index - worstIndex) / total
    return interpolateColor(COLOR_BAD, COLOR_GOOD, factor)
  }

  const isLowerBetter = betterIs === 'LOWER'
  const worstIndex = isLowerBetter ? count - 1 : 0
  const bestIndex = isLowerBetter ? 0 : count - 1

  // Gradient 1: Worst -> Average (Red -> Blue)
  const distToWorst = Math.abs(index - worstIndex)
  const spanToAvg = Math.abs(averageIndex - worstIndex)

  // Check if we are in the "bad to average" segment
  // Logic: Is index strictly "between" worstIndx and averageIndex ?
  // Or rather: is it closer to worst than average to best?

  // Robust check:
  const min1 = Math.min(worstIndex, averageIndex)
  const max1 = Math.max(worstIndex, averageIndex)
  if (index >= min1 && index <= max1) {
    if (spanToAvg === 0) return COLOR_AVG
    return interpolateColor(COLOR_BAD, COLOR_AVG, distToWorst / spanToAvg)
  }

  // Gradient 2: Average -> Best (Blue -> Green)
  const min2 = Math.min(averageIndex, bestIndex)
  const max2 = Math.max(averageIndex, bestIndex)
  if (index >= min2 && index <= max2) {
    const spanToBest = Math.abs(bestIndex - averageIndex)
    if (spanToBest === 0) return COLOR_AVG
    const distFromAvg = Math.abs(index - averageIndex)
    return interpolateColor(COLOR_AVG, COLOR_GOOD, distFromAvg / spanToBest)
  }

  // Fallback if somehow outside (should not happen with inclusive ranges covering 0 to count-1)
  return COLOR_AVG
}

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  const displayDistribution = buildAbsoluteDistribution()
  const dataValues = displayDistribution.map(bucket => bucket.value)
  const maxValue = Math.max(0, ...dataValues)
  const yAxisMax = Math.max(1, Math.ceil(maxValue * 1.2))
  const productValue = props.currentValue
  const averageValue = props.averageValue
  const productIndex = resolveBucketIndex(productValue, displayDistribution)
  const averageIndex = resolveBucketIndex(averageValue, displayDistribution)

  const buildMarkerSeries = (
    name: string,
    index: number | null,
    color: string,
    tooltipKey: string,
    tooltipParams: Record<string, string>,
    labelText: string,
    labelColor: string
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
      label: {
        show: true,
        position: 'top',
        formatter: (params: { dataIndex: number }) =>
          params.dataIndex === index ? labelText : '',
        fontSize: 11,
        fontWeight: 600,
        color: labelColor,
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

  const productBaseColor =
    productIndex != null
      ? resolveProductColor(
          productIndex,
          averageIndex,
          displayDistribution.length,
          props.impactBetterIs
        )
      : COLOR_BAD

  const markerSeries = [
    buildMarkerSeries(
      'average',
      averageIndex,
      'rgba(148, 163, 184, 0.35)',
      'product.impact.subscoreChart.markerAverage',
      { scoreLabel: props.label },
      t('product.impact.subscoreChart.averageLabel'),
      '#64748b'
    ),
    buildMarkerSeries(
      'product',
      productIndex,
      `rgba(${productBaseColor.join(', ')}, 0.35)`,
      'product.impact.subscoreChart.markerProduct',
      { productName: resolveProductName.value },
      t('product.impact.subscoreChart.productLabel'),
      `rgb(${productBaseColor.join(', ')})`
    ),
  ].filter(
    (series): series is NonNullable<ReturnType<typeof buildMarkerSeries>> =>
      series != null
  )

  return {
    grid: { top: 36, left: 40, right: 20, bottom: 40 },
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

const buildAbsoluteDistribution = () => {
  if (!props.numericMapping) {
    return filteredDistribution.value
  }

  // Create reverse mapping (value -> label)
  const reverseMapping = new Map<number, string>()
  Object.entries(props.numericMapping).forEach(([label, value]) => {
    reverseMapping.set(value, label)
  })

  return filteredDistribution.value.map(bucket => {
    const range = parseBucketRange(bucket.label)
    if (range && range.exact != null) {
      // Check for exact match with tolerance for floating point
      for (const [value, label] of reverseMapping.entries()) {
        if (Math.abs(value - range.exact) < 0.001) {
          return {
            ...bucket,
            label,
          }
        }
      }
    }
    return bucket
  })
}
</script>

<style scoped>
.impact-subscore-chart {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.impact-subscore-chart__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
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

<template>
  <div v-if="hasData" class="impact-subscore-chart">
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
  productName?: string
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

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  const dataValues = filteredDistribution.value.map(bucket => bucket.value)
  const maxValue = Math.max(0, ...dataValues)
  const yAxisMax = Math.max(1, Math.ceil(maxValue * 1.2))
  const productIndex = resolveBucketIndex(
    props.currentValue,
    filteredDistribution.value
  )
  const averageIndex = resolveBucketIndex(
    props.averageValue,
    filteredDistribution.value
  )

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
      data: filteredDistribution.value.map(bucket => bucket.label),
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
</script>

<style scoped>
.impact-subscore-chart {
  margin-top: auto;
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

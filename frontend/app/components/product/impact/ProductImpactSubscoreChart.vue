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
import { computed } from 'vue'
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import type { DistributionBucket } from './impact-types'
import { ensureImpactECharts } from './echarts-setup'

const props = defineProps<{
  distribution: DistributionBucket[]
  label: string
  relativeValue: number | null
  productName: string
  percent?: number | null
}>()

const ARROW_SYMBOL = 'path://M12 24L24 8H16V0H8V8H0L12 24Z'

const computeIndicatorColor = (percent?: number | null): string => {
  if (percent == null || Number.isNaN(percent)) {
    return '#2563eb'
  }

  const clamped = Math.min(100, Math.max(0, percent))
  const hue = (clamped / 100) * 120
  return `hsl(${hue.toFixed(2)}, 75%, 45%)`
}

ensureImpactECharts()

const hasData = computed(() => props.distribution.length > 0)

const maxBucketValue = computed(() =>
  props.distribution.reduce((max, bucket) => Math.max(max, bucket.value), 0)
)

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  const markPointLabel = (() => {
    if (props.relativeValue == null) {
      return null
    }

    const relativeValue = props.relativeValue

    const closest = props.distribution.reduce<DistributionBucket | null>((candidate, bucket) => {
      const labelNumber = Number(bucket.label)
      if (!Number.isFinite(labelNumber)) {
        return candidate
      }

      if (!candidate) {
        return bucket
      }

      const candidateDistance = Math.abs(Number(candidate.label) - relativeValue)
      const currentDistance = Math.abs(labelNumber - relativeValue)

      return candidateDistance <= currentDistance ? candidate : bucket
    }, null)

    return closest?.label ?? null
  })()

  const indicatorColor = computeIndicatorColor(props.percent ?? null)

  const yAxisMax = maxBucketValue.value > 0 ? maxBucketValue.value * 1.12 : undefined
  const indicatorYAxis = yAxisMax ?? maxBucketValue.value

  return {
    grid: { top: 52, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: props.distribution.map((bucket) => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value', max: yAxisMax },
    series: [
      {
        type: 'bar',
        name: props.label,
        data: props.distribution.map((bucket) => bucket.value),
        itemStyle: {
          color: 'rgba(33, 150, 243, 0.75)',
        },
        markLine:
          props.relativeValue != null && markPointLabel
            ? {
                symbol: ['none', ARROW_SYMBOL],
                symbolSize: [24, 24],
                symbolOffset: [0, -12],
                lineStyle: {
                  color: indicatorColor,
                  width: 2,
                  type: 'dashed',
                },
                label: {
                  formatter: props.productName,
                  color: indicatorColor,
                  fontWeight: 600,
                  distance: 18,
                  position: 'end',
                  backgroundColor: 'rgba(255, 255, 255, 0.92)',
                  padding: [4, 10],
                  borderRadius: 12,
                },
                data: [
                  {
                    xAxis: markPointLabel,
                    yAxis: indicatorYAxis,
                  },
                ],
              }
            : undefined,
      },
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

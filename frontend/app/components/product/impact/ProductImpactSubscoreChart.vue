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
}>()

ensureImpactECharts()

const hasData = computed(() => props.distribution.length > 0)

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

      return candidateDistance < currentDistance ? bucket : candidate
    }, null)

    return closest?.label ?? null
  })()

  return {
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: props.distribution.map((bucket) => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value' },
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
                symbol: 'none',
                label: {
                  formatter: props.productName,
                  color: '#1f2937',
                },
                data: [
                  {
                    xAxis: markPointLabel,
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

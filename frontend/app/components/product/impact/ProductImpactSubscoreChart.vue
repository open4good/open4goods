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
}>()

ensureImpactECharts()

const filteredDistribution = computed(() =>
  props.distribution.filter(
    bucket => bucket.label.toUpperCase() !== 'ES-UNKNOWN'
  )
)

const hasData = computed(() => filteredDistribution.value.length > 0)

const chartOption = computed<EChartsOption | null>(() => {
  if (!hasData.value) {
    return null
  }

  return {
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: filteredDistribution.value.map(bucket => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        name: props.label,
        data: filteredDistribution.value.map(bucket => bucket.value),
        itemStyle: {
          color: 'rgba(33, 150, 243, 0.75)',
        },
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

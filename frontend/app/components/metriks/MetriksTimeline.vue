<script setup lang="ts">
/**
 * MetriksTimeline — Small inline ECharts line chart showing the
 * historical evolution of a single metric. Used inside expandable
 * table rows in MetriksTable.
 */
import type { MetrikWithTrend } from '~/types/metriks'
import { formatMetrikValue } from '~/composables/useMetriks'

const props = withDefaults(
  defineProps<{
    /** The enriched metric whose history to render. */
    metrik: MetrikWithTrend
    /** Chart height in pixels. */
    height?: number
  }>(),
  {
    height: 120,
  }
)

const VChart = shallowRef<typeof import('vue-echarts').default | null>(null)
const chartReady = ref(false)

const CHART_COLOR = '#1976D2'

/** Build the ECharts option from history data points. */
const chartOption = computed(() => {
  const history = props.metrik.history
  if (history.length === 0) return null

  const dates = history.map(p => p.date)
  const values = history.map(p => p.value)
  const unit = props.metrik.unit

  return {
    tooltip: {
      trigger: 'axis' as const,
      formatter: (
        params: Array<{
          axisValueLabel: string
          value: number | null
        }>
      ) => {
        if (!Array.isArray(params) || params.length === 0) return ''
        const p = params[0]!
        return `<strong>${p.axisValueLabel}</strong><br/>${formatMetrikValue(p.value, unit)}`
      },
    },
    grid: {
      left: '4%',
      right: '4%',
      top: '10%',
      bottom: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      data: dates,
      axisLabel: {
        fontSize: 10,
        rotate: dates.length > 8 ? 30 : 0,
      },
      axisLine: { show: false },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value' as const,
      splitLine: { lineStyle: { type: 'dashed' as const, opacity: 0.3 } },
      axisLabel: { fontSize: 10 },
    },
    series: [
      {
        type: 'line' as const,
        data: values,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: CHART_COLOR, width: 2 },
        itemStyle: { color: CHART_COLOR },
        areaStyle: {
          color: {
            type: 'linear' as const,
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${CHART_COLOR}30` },
              { offset: 1, color: `${CHART_COLOR}05` },
            ],
          },
        },
      },
    ],
  }
})

/** Lazy-load ECharts + vue-echarts on client side. */
async function initChart(): Promise<void> {
  if (import.meta.server) return

  const { ensureECharts } = await import('~/utils/echarts-loader')
  const result = await ensureECharts([
    'LineChart',
    'CanvasRenderer',
    'GridComponent',
    'TooltipComponent',
  ])

  if (result) {
    const { use } = result.core
    use(result.modules)
  }

  const mod = await import('vue-echarts')
  VChart.value = mod.default
  chartReady.value = true
}

onMounted(() => {
  initChart()
})
</script>

<template>
  <div class="metriks-timeline pa-3">
    <!-- Chart -->
    <div
      v-if="chartReady && VChart && chartOption"
      :style="{ height: `${height}px` }"
    >
      <component
        :is="VChart"
        :option="chartOption"
        autoresize
        style="height: 100%; width: 100%"
      />
    </div>

    <!-- Loading -->
    <div
      v-else-if="metrik.history.length > 0"
      class="d-flex justify-center align-center"
      :style="{ height: `${height}px` }"
    >
      <v-progress-circular indeterminate size="24" color="primary" />
    </div>

    <!-- No data -->
    <div
      v-else
      class="d-flex justify-center align-center text-medium-emphasis text-caption"
      :style="{ height: `${height}px` }"
    >
      {{ $t('metriks.chart.empty') }}
    </div>
  </div>
</template>

<style scoped>
.metriks-timeline {
  background: rgba(var(--v-theme-surface-muted), 0.5);
  border-radius: 8px;
}
</style>

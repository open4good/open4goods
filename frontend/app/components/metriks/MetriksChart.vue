<script setup lang="ts">
/**
 * MetriksChart — ECharts time-series chart showing the evolution of selected metrics.
 *
 * Uses the existing echarts-loader from the project for lazy-loading.
 */
import type { MetrikWithTrend } from '~/types/metriks'
import { formatMetrikValue } from '~/composables/useMetriks'

const props = defineProps<{
  /** Metrics currently selected for charting. */
  selectedMetriks: MetrikWithTrend[]
}>()

const { t } = useI18n()

const chartRef = ref<InstanceType<typeof import('vue-echarts').default> | null>(
  null
)
const VChart = shallowRef<typeof import('vue-echarts').default | null>(null)
const chartReady = ref(false)

/** Distinct colors for chart series. */
const SERIES_COLORS = [
  '#1976D2',
  '#43A047',
  '#FB8C00',
  '#E53935',
  '#8E24AA',
  '#00ACC1',
  '#5C6BC0',
  '#D81B60',
  '#F4511E',
  '#00897B',
]

/** Compute ECharts option from selected metrics. */
const chartOption = computed(() => {
  if (props.selectedMetriks.length === 0) return null

  // Collect all unique dates across all selected metrics
  const dateSet = new Set<string>()
  for (const m of props.selectedMetriks) {
    for (const point of m.history) {
      dateSet.add(point.date)
    }
  }
  const dates = Array.from(dateSet).sort(
    (a, b) => new Date(a).getTime() - new Date(b).getTime()
  )

  const series = props.selectedMetriks.map((m, idx) => {
    const values = dates.map(date => {
      const point = m.history.find(p => p.date === date)
      return point?.value ?? null
    })
    return {
      name: m.name,
      type: 'bar' as const,
      data: values,
      itemStyle: {
        color: SERIES_COLORS[idx % SERIES_COLORS.length],
        borderRadius: [4, 4, 0, 0],
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 8,
          shadowColor: 'rgba(0, 0, 0, 0.15)',
        },
      },
    }
  })

  return {
    tooltip: {
      trigger: 'axis' as const,
      axisPointer: { type: 'shadow' as const },
      formatter: (
        params: Array<{
          seriesName: string
          value: number | null
          axisValueLabel: string
          color: string
        }>
      ) => {
        if (!Array.isArray(params) || params.length === 0) return ''
        let html = `<strong>${params[0].axisValueLabel}</strong><br/>`
        for (const p of params) {
          const metrik = props.selectedMetriks.find(
            m => m.name === p.seriesName
          )
          const unit = metrik?.unit ?? 'count'
          const formatted = formatMetrikValue(p.value, unit)
          html += `<span style="color:${p.color}">●</span> ${p.seriesName}: <strong>${formatted}</strong><br/>`
        }
        return html
      },
    },
    legend: {
      show: props.selectedMetriks.length > 1,
      bottom: 0,
      type: 'scroll' as const,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: props.selectedMetriks.length > 1 ? '15%' : '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      data: dates,
      axisLabel: {
        rotate: dates.length > 8 ? 45 : 0,
      },
    },
    yAxis: {
      type: 'value' as const,
    },
    series,
  }
})

/** Lazy-load ECharts + vue-echarts on client side. */
async function initChart(): Promise<void> {
  if (import.meta.server) return

  const { ensureECharts } = await import('~/utils/echarts-loader')
  const result = await ensureECharts([
    'BarChart',
    'CanvasRenderer',
    'GridComponent',
    'TooltipComponent',
    'LegendComponent',
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
  <v-card variant="outlined" rounded="lg">
    <v-card-title class="d-flex align-center ga-3 pa-4">
      <v-icon icon="mdi-chart-bar" color="primary" />
      <span>{{ t('metriks.chart.title') }}</span>
      <v-spacer />
      <v-chip
        v-if="selectedMetriks.length > 0"
        size="small"
        color="primary"
        variant="tonal"
      >
        {{ selectedMetriks.length }} {{ t('metriks.chart.selected') }}
      </v-chip>
    </v-card-title>

    <v-card-text>
      <!-- Empty state -->
      <v-alert
        v-if="selectedMetriks.length === 0"
        type="info"
        variant="tonal"
        class="mb-0"
      >
        {{ t('metriks.chart.empty') }}
      </v-alert>

      <!-- Chart -->
      <div
        v-else-if="chartReady && VChart && chartOption"
        style="height: 400px"
      >
        <component
          :is="VChart"
          ref="chartRef"
          :option="chartOption"
          autoresize
          style="height: 100%; width: 100%"
        />
      </div>

      <!-- Loading -->
      <div
        v-else-if="selectedMetriks.length > 0"
        class="d-flex justify-center py-8"
      >
        <v-progress-circular indeterminate color="primary" />
      </div>
    </v-card-text>
  </v-card>
</template>

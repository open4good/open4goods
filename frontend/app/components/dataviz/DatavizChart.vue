<template>
  <v-card
    class="dataviz-chart"
    :class="{ 'dataviz-chart--loading': loading }"
    elevation="2"
    rounded="xl"
    data-test="dataviz-chart"
  >
    <div class="dataviz-chart__header">
      <h3 class="dataviz-chart__title">{{ preset.title }}</h3>
      <p v-if="preset.description" class="dataviz-chart__description">
        {{ preset.description }}
      </p>
    </div>

    <div class="dataviz-chart__body">
      <v-skeleton-loader
        v-if="loading"
        type="image"
        class="dataviz-chart__skeleton"
      />
      <div
        v-else-if="chartData"
        ref="chartContainer"
        class="dataviz-chart__canvas"
      />
      <div v-else class="dataviz-chart__empty">
        <v-icon icon="mdi-chart-bar" size="48" color="text-neutral-soft" />
        <p>{{ t('dataviz.chart.noData') }}</p>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import {
  ref,
  watch,
  onMounted,
  onBeforeUnmount,
  shallowRef,
  nextTick,
} from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  DatavizChartPresetDto,
  DatavizChartQueryResponseDto,
} from '~~/shared/api-client'
import type { EChartsType } from 'echarts/core'
import { ensureECharts } from '~/utils/echarts-loader'

const props = defineProps<{
  preset: DatavizChartPresetDto
  chartData: DatavizChartQueryResponseDto | null
  loading: boolean
}>()

const { t } = useI18n()
const chartContainer = ref<HTMLElement | null>(null)
const chartInstance = shallowRef<EChartsType | null>(null)

const buildChartOption = (data: DatavizChartQueryResponseDto) => {
  const labels = data.labels ?? []
  const values = data.values ?? []
  const chartType = data.chartType ?? props.preset.chartType ?? 'bar'

  const baseOption = {
    tooltip: { trigger: 'axis' as const },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category' as const,
      data: labels,
      axisLabel: {
        rotate: labels.length > 8 ? 30 : 0,
        fontSize: 11,
      },
    },
    yAxis: { type: 'value' as const },
    series: [
      {
        data: values,
        type:
          chartType === 'donut'
            ? 'bar'
            : chartType === 'histogram'
              ? 'bar'
              : chartType === 'scatter'
                ? 'scatter'
                : chartType === 'kpi'
                  ? 'bar'
                  : chartType === 'pareto'
                    ? 'bar'
                    : chartType,
        smooth: chartType === 'line',
        itemStyle: {
          borderRadius:
            chartType === 'bar' || chartType === 'histogram'
              ? [4, 4, 0, 0]
              : undefined,
        },
      },
    ],
    color: ['#1976D2', '#43A047', '#FF9800', '#E91E63', '#9C27B0', '#00BCD4'],
  }

  if (chartType === 'donut') {
    return {
      tooltip: { trigger: 'item' as const },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          data: labels.map((label, i) => ({ name: label, value: values[i] })),
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
            },
          },
        },
      ],
      color: ['#1976D2', '#43A047', '#FF9800', '#E91E63', '#9C27B0', '#00BCD4'],
    }
  }

  if (chartType === 'scatter') {
    return {
      ...baseOption,
      xAxis: { type: 'value' as const },
      series: [
        {
          type: 'scatter',
          data: labels.map((_, i) => [i, values[i]]),
          symbolSize: 8,
        },
      ],
    }
  }

  return baseOption
}

const renderChart = async () => {
  if (!chartContainer.value || !props.chartData) {
    return
  }

  const echartsResult = await ensureECharts([
    'BarChart',
    'LineChart',
    'ScatterChart',
    'GridComponent',
    'TooltipComponent',
    'LegendComponent',
    'CanvasRenderer',
  ])

  if (!echartsResult) {
    return
  }

  const { core, modules } = echartsResult
  core.use(modules)

  if (chartInstance.value) {
    chartInstance.value.dispose()
  }

  chartInstance.value = core.init(chartContainer.value)
  chartInstance.value.setOption(buildChartOption(props.chartData))
}

const handleResize = () => {
  chartInstance.value?.resize()
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', handleResize)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', handleResize)
  }
  chartInstance.value?.dispose()
})

watch(
  () => props.chartData,
  async () => {
    await nextTick()
    renderChart()
  }
)
</script>

<style scoped lang="scss">
.dataviz-chart {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  backdrop-filter: blur(8px);

  &--loading {
    min-height: 320px;
  }

  &__header {
    padding: 20px 24px 8px;
  }

  &__title {
    font-size: 1rem;
    font-weight: 600;
    color: rgb(var(--v-theme-text-neutral-strong));
    margin: 0;
  }

  &__description {
    font-size: 0.82rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin: 4px 0 0;
    line-height: 1.4;
  }

  &__body {
    flex: 1;
    padding: 8px 16px 20px;
    min-height: 280px;
  }

  &__canvas {
    width: 100%;
    height: 280px;
  }

  &__skeleton {
    height: 200px;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 200px;
    gap: 8px;
    color: rgb(var(--v-theme-text-neutral-soft));
    font-size: 0.85rem;
  }
}
</style>

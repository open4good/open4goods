<template>
  <article class="impact-radar" role="img" :aria-label="ariaLabel">
    <ClientOnly>
      <VueECharts
        v-if="option"
        :option="option"
        :autoresize="true"
        class="impact-radar__chart"
      />
      <template #fallback>
        <div class="impact-radar__placeholder" />
      </template>
    </ClientOnly>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import { useI18n } from 'vue-i18n'
import { ensureImpactECharts } from './echarts-setup'

interface RadarAxisEntry {
  id: string
  name: string
}

interface RadarSeriesEntry {
  label: string
  values: Array<number | null>
  lineColor: string
  areaColor: string
  symbolColor: string
}

const props = defineProps<{ axes: RadarAxisEntry[]; series: RadarSeriesEntry[]; productName: string }>()

ensureImpactECharts()

const { t } = useI18n()

const ariaLabel = computed(() => t('product.impact.radarAria', { product: props.productName }))

const option = computed<EChartsOption | null>(() => {
  if (!props.axes.length || !props.series.length) {
    return null
  }

  const indicator = props.axes.map((entry) => ({ name: entry.name, max: 5 }))
  const seriesData = props.series.map((entry) => ({
    value: entry.values.map((value) => (typeof value === 'number' && Number.isFinite(value) ? value : null)),
    name: entry.label,
    areaStyle: { color: entry.areaColor },
    lineStyle: { color: entry.lineColor, width: 2 },
    itemStyle: { color: entry.symbolColor },
    symbolSize: 6,
  }))

  return {
    color: props.series.map((entry) => entry.lineColor),
    tooltip: { trigger: 'item' },
    legend: {
      data: props.series.map((entry) => entry.label),
      bottom: 0,
      left: 'center',
      padding: [16, 24, 0, 24],
      itemGap: 12,
      icon: 'circle',
    },
    radar: {
      indicator,
      radius: '64%',
      center: ['50%', '45%'],
      axisName: {
        fontSize: 13,
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(33, 150, 243, 0.12)', 'rgba(33, 150, 243, 0.05)'],
        },
      },
    },
    series: [
      {
        type: 'radar',
        data: seriesData,
      },
    ],
  }
})
</script>

<style scoped>
.impact-radar {
  width: 100%;
  height: 100%;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-radar__chart {
  height: 360px;
}

.impact-radar__placeholder {
  height: 360px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.12);
}
</style>

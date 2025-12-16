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
  attributeValue: string | null
}

interface RadarSeriesEntry {
  label: string
  values: Array<number | null>
  lineColor: string
  areaColor: string
  symbolColor: string
}

const props = defineProps<{
  axes: RadarAxisEntry[]
  series: RadarSeriesEntry[]
  productName: string
}>()

ensureImpactECharts()

const { t } = useI18n()

const ariaLabel = computed(() =>
  t('product.impact.radarAria', { product: props.productName })
)

const option = computed<EChartsOption | null>(() => {
  if (!props.axes.length || !props.series.length) {
    return null
  }

  const finiteValues = props.series
    .flatMap(entry => entry.values)
    .filter(
      (value): value is number =>
        typeof value === 'number' && Number.isFinite(value)
    )

  const maxObservedValue = finiteValues.length ? Math.max(...finiteValues) : 5
  const paddedMax = maxObservedValue > 0 ? maxObservedValue * 1.1 : 5

  const indicator = props.axes.map(entry => ({
    name: entry.name,
    max: paddedMax,
  }))
  const seriesData = props.series.map(entry => ({
    value: entry.values.map(value =>
      typeof value === 'number' && Number.isFinite(value) ? value : null
    ),
    name: entry.label,
    areaStyle: { color: entry.areaColor },
    lineStyle: { color: entry.lineColor, width: 2 },
    itemStyle: { color: entry.symbolColor },
    symbolSize: 6,
  }))

  return {
    color: props.series.map(entry => entry.lineColor),
    tooltip: {
      trigger: 'item',
      formatter: params => {
        if (
          !params ||
          typeof params !== 'object' ||
          !Array.isArray((params as { value?: unknown }).value)
        ) {
          return ''
        }

        const value = (params as { value: unknown[] }).value
        const seriesName = (params as { seriesName?: string }).seriesName ?? ''

        const lines = [`<strong>${seriesName}</strong>`]
        value.forEach((entryValue, index) => {
          const axis = props.axes[index]
          const numericValue =
            typeof entryValue === 'number' && Number.isFinite(entryValue)
              ? entryValue
              : null
          const renderedValue = numericValue != null ? numericValue : '–'
          const attributeLabel = axis?.attributeValue
            ? ` (${axis.attributeValue})`
            : ''

          lines.push(`${axis?.name ?? ''}: ${renderedValue}${attributeLabel}`)
        })

        return lines.join('<br/>')
      },
    },
    legend: {
      data: props.series.map(entry => entry.label),
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

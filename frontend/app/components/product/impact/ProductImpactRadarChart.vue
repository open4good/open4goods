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

const props = defineProps<{ values: Array<{ name: string; value: number }>; productName: string }>()

ensureImpactECharts()

const { t } = useI18n()

const ariaLabel = computed(() => t('product.impact.radarAria', { product: props.productName }))

const option = computed<EChartsOption | null>(() => {
  if (!props.values.length) {
    return null
  }

  return {
    tooltip: {},
    radar: {
      indicator: props.values.map((entry) => ({ name: entry.name, max: 5 })),
      radius: '70%',
      splitArea: {
        areaStyle: {
          color: ['rgba(33, 150, 243, 0.12)', 'rgba(33, 150, 243, 0.05)'],
        },
      },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: props.values.map((entry) => entry.value),
            name: props.productName,
            areaStyle: { color: 'rgba(33, 150, 243, 0.35)' },
            lineStyle: { color: 'rgba(33, 150, 243, 0.85)', width: 2 },
            symbolSize: 6,
          },
        ],
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

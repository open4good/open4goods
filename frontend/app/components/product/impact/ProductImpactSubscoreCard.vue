<template>
  <article class="impact-subscore">
    <header class="impact-subscore__header">
      <div>
        <h4 class="impact-subscore__title">{{ score.label }}</h4>
        <p v-if="score.description" class="impact-subscore__description">{{ score.description }}</p>
      </div>
      <span v-if="score.letter" class="impact-subscore__letter">{{ score.letter }}</span>
    </header>

    <div class="impact-subscore__score">
      <ImpactScore :score="relativeScore" :max="5" show-value size="medium" />
    </div>

    <div v-if="absoluteValue" class="impact-subscore__absolute">
      <span class="impact-subscore__absolute-label">{{ $t('product.impact.absoluteValue') }}</span>
      <span class="impact-subscore__absolute-value">{{ absoluteValue }}</span>
    </div>

    <div v-if="score.energyLetter" class="impact-subscore__badge">
      <span class="impact-subscore__energy">{{ score.energyLetter }}</span>
    </div>

    <div v-if="hasDistribution" class="impact-subscore__chart">
      <ClientOnly>
        <VueECharts
          v-if="histogramOption"
          :option="histogramOption"
          :autoresize="true"
          class="impact-subscore__echart"
        />
        <template #fallback>
          <div class="impact-subscore__chart-placeholder" />
        </template>
      </ClientOnly>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VueECharts from 'vue-echarts'
import type { EChartsOption } from 'echarts'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { DistributionBucket, ScoreView } from './impact-types'
import { ensureImpactECharts } from './echarts-setup'

const props = defineProps<{
  score: ScoreView
  productName: string
}>()

ensureImpactECharts()

const { n } = useI18n()

const relativeScore = computed(() => (props.score.relativeValue ?? 0) || 0)

const absoluteValue = computed(() => {
  const value = props.score.absoluteValue
  if (value == null || value === '') {
    return null
  }

  if (typeof value === 'number') {
    return n(value, { maximumFractionDigits: 2, minimumFractionDigits: 0 })
  }

  return String(value)
})

const hasDistribution = computed(() => Boolean(props.score.distribution?.length))

const histogramOption = computed<EChartsOption | null>(() => {
  const distribution = props.score.distribution ?? []
  if (!distribution.length) {
    return null
  }

  const markPointLabel = (() => {
    const relativeValue = props.score.relativeValue
    if (relativeValue == null) {
      return null
    }

    const closest = distribution.reduce<DistributionBucket | null>((candidate, bucket) => {
      const labelNumber = Number(bucket.label)
      if (!Number.isFinite(labelNumber)) {
        return candidate
      }

      if (!candidate) {
        return bucket
      }

      const candidateDistance = Math.abs(labelNumber - relativeValue)
      const currentDistance = Math.abs(Number(candidate.label) - relativeValue)

      return candidateDistance < currentDistance ? bucket : candidate
    }, null)

    return closest?.label ?? null
  })()

  return {
    grid: { top: 20, left: 40, right: 20, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: distribution.map((bucket) => bucket.label),
      axisLabel: { rotate: 35 },
    },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        name: props.score.label,
        data: distribution.map((bucket) => bucket.value),
        itemStyle: {
          color: 'rgba(33, 150, 243, 0.75)',
        },
        markLine:
          props.score.relativeValue != null && markPointLabel
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
.impact-subscore {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
  border-radius: 22px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  min-height: 100%;
}

.impact-subscore__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.impact-subscore__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.impact-subscore__description {
  margin: 0.35rem 0 0;
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore__letter {
  font-size: 1.4rem;
  font-weight: 700;
  color: rgb(var(--v-theme-accent-supporting));
}

.impact-subscore__score {
  display: flex;
  justify-content: flex-start;
}

.impact-subscore__absolute {
  display: inline-flex;
  flex-direction: column;
  gap: 0.15rem;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 16px;
  padding: 0.6rem 1rem;
}

.impact-subscore__absolute-label {
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.impact-subscore__absolute-value {
  font-size: 1.1rem;
  font-weight: 600;
}

.impact-subscore__badge {
  display: flex;
}

.impact-subscore__energy {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-weight: 700;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #22c55e, #f97316);
  color: #ffffff;
  text-transform: uppercase;
}

.impact-subscore__chart {
  margin-top: auto;
}

.impact-subscore__echart {
  height: 220px;
}

.impact-subscore__chart-placeholder {
  height: 220px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.15);
}
</style>

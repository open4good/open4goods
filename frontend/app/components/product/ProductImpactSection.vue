<template>
  <section :id="sectionId" class="product-impact">
    <header class="product-impact__header">
      <h2 class="product-impact__title">
        {{ $t('product.impact.title') }}
      </h2>
      <p class="product-impact__subtitle">
        {{ $t('product.impact.subtitle') }}
      </p>
    </header>

    <div class="product-impact__overview">
      <div class="product-impact__radar" role="img" :aria-label="$t('product.impact.radarAria', { product: productName })">
        <ClientOnly>
          <VueECharts
            v-if="radarOption"
            :option="radarOption"
            :autoresize="true"
            class="product-impact__radar-chart"
          />
          <template #fallback>
            <div class="product-impact__radar-placeholder" />
          </template>
        </ClientOnly>
      </div>

      <div class="product-impact__ranking">
        <h3>{{ $t('product.impact.rankingTitle') }}</h3>
        <p v-if="ranking" class="product-impact__ranking-value">
          {{ $t('product.impact.rankingValue', { position: ranking.position, total: ranking.total }) }}
        </p>
        <p v-if="ranking?.globalBest" class="product-impact__ranking-link">
          <NuxtLink :to="ranking.globalBest.fullSlug">
            {{ $t('product.impact.bestProduct', { name: ranking.globalBest.bestName }) }}
          </NuxtLink>
        </p>
        <p v-if="ranking?.globalBetter" class="product-impact__ranking-link">
          <NuxtLink :to="ranking.globalBetter.fullSlug">
            {{ $t('product.impact.betterProduct', { name: ranking.globalBetter.bestName }) }}
          </NuxtLink>
        </p>

        <div v-if="country" class="product-impact__country">
          <NuxtImg
            v-if="country.flag"
            :src="country.flag"
            :alt="country.name"
            width="36"
            height="24"
            class="product-impact__country-flag"
          />
          <span>{{ country.name }}</span>
        </div>
      </div>
    </div>

    <div class="product-impact__scores">
      <v-skeleton-loader
        v-if="loading"
        type="image, article"
        class="product-impact__skeleton"
      />
      <template v-else>
        <article
          v-for="score in scores"
          :key="score.id"
          class="product-impact__score-card"
        >
          <header class="product-impact__score-header">
            <div>
              <h3 class="product-impact__score-title">{{ score.label }}</h3>
              <p v-if="score.description" class="product-impact__score-description">
                {{ score.description }}
              </p>
            </div>
            <div v-if="score.letter" class="product-impact__score-letter">
              {{ score.letter }}
            </div>
          </header>

          <div class="product-impact__score-body">
            <div class="product-impact__score-chart">
              <ClientOnly>
                <VueECharts
                  v-if="histogramOptions[score.id]"
                  :option="histogramOptions[score.id]"
                  :autoresize="true"
                  class="product-impact__histogram"
                />
                <template #fallback>
                  <div class="product-impact__histogram-placeholder" />
                </template>
              </ClientOnly>
            </div>
            <ul class="product-impact__score-facts">
              <li>
                <strong>{{ $t('product.impact.scoreValue') }}</strong>
                <span>{{ formatScore(score.relativeValue) }} / 5</span>
              </li>
              <li v-if="score.absoluteValue != null">
                <strong>{{ $t('product.impact.absoluteValue') }}</strong>
                <span>{{ score.absoluteValue }}</span>
              </li>
              <li v-if="score.percent != null">
                <strong>{{ $t('product.impact.percentile') }}</strong>
                <span>{{ score.percent }}%</span>
              </li>
              <li v-if="score.ranking">
                <strong>{{ $t('product.impact.scoreRanking') }}</strong>
                <span>{{ score.ranking }}</span>
              </li>
            </ul>
            <div v-if="score.energyLetter" class="product-impact__score-custom">
              <span class="product-impact__energy-badge">{{ score.energyLetter }}</span>
            </div>
          </div>
        </article>
      </template>
    </div>

    <div class="product-impact__table">
      <h3>{{ $t('product.impact.detailsTitle') }}</h3>
      <v-table density="compact">
        <thead>
          <tr>
            <th scope="col">{{ $t('product.impact.tableHeaders.score') }}</th>
            <th scope="col">{{ $t('product.impact.tableHeaders.value') }}</th>
            <th scope="col">{{ $t('product.impact.tableHeaders.ranking') }}</th>
            <th scope="col">{{ $t('product.impact.tableHeaders.percent') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="score in scores" :key="`${score.id}-table`">
            <th scope="row">{{ score.label }}</th>
            <td>{{ formatScore(score.relativeValue) }} / 5</td>
            <td>{{ score.ranking ?? '—' }}</td>
            <td>{{ score.percent != null ? `${score.percent}%` : '—' }}</td>
          </tr>
        </tbody>
      </v-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import VueECharts from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, PolarComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'

use([BarChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, PolarComponent, CanvasRenderer])

type DistributionBucket = {
  label: string
  value: number
}

type ScoreView = {
  id: string
  label: string
  description?: string | null
  relativeValue: number | null
  absoluteValue?: string | number | null
  percent?: number | null
  ranking?: number | string | null
  letter?: string | null
  distribution?: DistributionBucket[]
  energyLetter?: string | null
}

type RankingInfo = {
  position: number
  total: number
  globalBest?: { fullSlug: string; bestName: string }
  globalBetter?: { fullSlug: string; bestName: string }
}

type CountryInfo = {
  name: string
  flag?: string | null
}

const props = defineProps({
  sectionId: {
    type: String,
    default: 'impact',
  },
  scores: {
    type: Array as PropType<ScoreView[]>,
    default: () => [],
  },
  radarValues: {
    type: Array as PropType<Array<{ name: string; value: number }>>,
    default: () => [],
  },
  ranking: {
    type: Object as PropType<RankingInfo | null>,
    default: null,
  },
  country: {
    type: Object as PropType<CountryInfo | null>,
    default: null,
  },
  productName: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const histogramOptions = computed(() => {
  return props.scores.reduce<Record<string, EChartsOption>>((accumulator, score) => {
    const distribution = score.distribution ?? []
    if (!distribution.length) {
      return accumulator
    }

    const markPointLabel = (() => {
      const relativeValue = score.relativeValue
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

    accumulator[score.id] = {
      grid: { top: 20, left: 40, right: 20, bottom: 40 },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
      },
      xAxis: {
        type: 'category',
        data: distribution.map((bucket) => bucket.label),
        axisLabel: { rotate: 35 },
      },
      yAxis: {
        type: 'value',
      },
      series: [
        {
          type: 'bar',
          name: score.label,
          data: distribution.map((bucket) => bucket.value),
          itemStyle: {
            color: 'rgba(33, 150, 243, 0.75)',
          },
          markLine:
            score.relativeValue != null && markPointLabel
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

    return accumulator
  }, {})
})

const radarOption = computed(() => {
  if (!props.radarValues.length) {
    return null
  }

  const indicators = props.radarValues.map((entry) => ({
    name: entry.name,
    max: 5,
  }))

  return {
    tooltip: {},
    radar: {
      indicator: indicators,
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
            value: props.radarValues.map((entry) => entry.value),
            name: props.productName,
            areaStyle: {
              color: 'rgba(33, 150, 243, 0.35)',
            },
            lineStyle: {
              color: 'rgba(33, 150, 243, 0.85)',
              width: 2,
            },
            symbolSize: 6,
          },
        ],
      },
    ],
  }
})

const formatScore = (value: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return value.toFixed(1)
}
</script>

<style scoped>
.product-impact {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.product-impact__header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-impact__title {
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 700;
}

.product-impact__subtitle {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-impact__overview {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 1.5rem;
  align-items: stretch;
}

.product-impact__radar {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.product-impact__radar-placeholder {
  height: 320px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.12);
}

.product-impact__radar-chart {
  height: 320px;
}

.product-impact__ranking {
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.85), rgba(var(--v-theme-surface-glass), 0.95));
  border-radius: 24px;
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-impact__ranking h3 {
  margin: 0;
  font-size: 1.2rem;
}

.product-impact__ranking-value {
  font-size: 1.05rem;
  font-weight: 600;
}

.product-impact__ranking-link a {
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: none;
}

.product-impact__country {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  font-weight: 600;
}

.product-impact__country-flag {
  border-radius: 6px;
  width: 36px;
  height: 24px;
  object-fit: cover;
}

.product-impact__scores {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
}

.product-impact__skeleton {
  grid-column: 1 / -1;
  min-height: 320px;
}

.product-impact__score-card {
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  border-radius: 22px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-impact__score-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.product-impact__score-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.product-impact__score-description {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-impact__score-letter {
  font-size: 1.4rem;
  font-weight: 700;
  color: rgb(var(--v-theme-accent-supporting));
}

.product-impact__score-body {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.product-impact__score-chart {
  height: 220px;
}

.product-impact__histogram {
  height: 220px;
}

.product-impact__histogram-placeholder {
  height: 220px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.15);
}

.product-impact__score-facts {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.5rem;
}

.product-impact__score-facts li {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
}

.product-impact__score-facts strong {
  font-weight: 600;
}

.product-impact__score-custom {
  margin-top: 0.75rem;
}

.product-impact__energy-badge {
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

.product-impact__table {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.product-impact__table h3 {
  margin-bottom: 1rem;
}

@media (max-width: 960px) {
  .product-impact__overview {
    grid-template-columns: 1fr;
  }
}
</style>

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

    <div class="product-impact__primary">
      <ProductImpactEcoScoreCard :score="primaryScore" :vertical-home-url="verticalHomeUrl" />
    </div>

    <div class="product-impact__analysis">
      <div
        v-if="showRadar"
        class="product-impact__analysis-radar"
      >
        <ProductImpactRadarChart
          class="product-impact__analysis-radar-chart"
          :axes="radarAxes"
          :series="chartSeries"
          :product-name="productName"
        />
      </div>
      <ProductImpactDetailsTable
        class="product-impact__analysis-details"
        :class="{ 'product-impact__analysis-details--full': !showRadar }"
        :scores="detailScores"
      />
    </div>

    <div class="product-impact__subscores">
      <v-skeleton-loader
        v-if="loading"
        type="image, article"
        class="product-impact__skeleton"
      />
      <template v-else>
        <ProductImpactSubscoreCard
          v-for="score in secondaryScores"
          :key="score.id"
          :score="score"
          :product-name="productName"
        />
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductImpactEcoScoreCard from './impact/ProductImpactEcoScoreCard.vue'
import ProductImpactRadarChart from './impact/ProductImpactRadarChart.vue'
import ProductImpactDetailsTable from './impact/ProductImpactDetailsTable.vue'
import ProductImpactSubscoreCard from './impact/ProductImpactSubscoreCard.vue'
import type { ScoreView } from './impact/impact-types'

type RadarSeriesKey = 'current' | 'best' | 'worst'

interface RadarAxisEntry {
  id: string
  name: string
}

interface RadarSeriesEntry {
  key: RadarSeriesKey
  name: string
  values: Array<number | null>
}

interface RadarDataset {
  axes: RadarAxisEntry[]
  series: RadarSeriesEntry[]
}

interface ChartSeriesEntry {
  label: string
  values: Array<number | null>
  lineColor: string
  areaColor: string
  symbolColor: string
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
  radarData: {
    type: Object as PropType<RadarDataset>,
    default: () => ({ axes: [], series: [] }),
  },
  productName: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  verticalHomeUrl: {
    type: String,
    default: '',
  },
})

const radarData = toRef(props, 'radarData')
const productName = toRef(props, 'productName')
const verticalHomeUrl = toRef(props, 'verticalHomeUrl')
const { t } = useI18n()

const primaryScore = computed(() => props.scores[0] ?? null)
const secondaryScores = computed(() => props.scores.slice(1))
const detailScores = computed(() => props.scores.filter((score) => score.id !== 'ECOSCORE'))
const radarAxes = computed<RadarAxisEntry[]>(() => radarData.value.axes ?? [])
const availableSeries = computed<RadarSeriesEntry[]>(() => radarData.value.series ?? [])

const radarSeriesStyles: Record<RadarSeriesKey, { line: string; area: string; symbol: string }> = {
  current: {
    line: 'rgba(25, 118, 210, 0.85)',
    area: 'rgba(25, 118, 210, 0.25)',
    symbol: 'rgba(25, 118, 210, 1)',
  },
  best: {
    line: 'rgba(46, 125, 50, 0.85)',
    area: 'rgba(46, 125, 50, 0.25)',
    symbol: 'rgba(46, 125, 50, 1)',
  },
  worst: {
    line: 'rgba(198, 40, 40, 0.85)',
    area: 'rgba(198, 40, 40, 0.2)',
    symbol: 'rgba(198, 40, 40, 1)',
  },
}

const translationKeys: Record<RadarSeriesKey, string> = {
  current: 'product.impact.radarControls.current',
  best: 'product.impact.radarControls.best',
  worst: 'product.impact.radarControls.worst',
}

const chartSeries = computed<ChartSeriesEntry[]>(() => {
  if (!radarAxes.value.length) {
    return []
  }

  return availableSeries.value.map((entry) => {
    const style = radarSeriesStyles[entry.key]
    const values = radarAxes.value.map((_, index) => {
      const value = entry.values[index]
      return typeof value === 'number' && Number.isFinite(value) ? value : null
    })

    return {
      label: t(translationKeys[entry.key], { product: entry.name }),
      values,
      lineColor: style.line,
      areaColor: style.area,
      symbolColor: style.symbol,
    }
  })
})

const showRadar = computed(() => radarAxes.value.length >= 3 && chartSeries.value.length > 0)
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

.product-impact__primary {
  display: flex;
  flex-direction: column;
}

.product-impact__analysis {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  align-items: stretch;
}

.product-impact__analysis-radar {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-impact__analysis-radar-chart {
  flex: 1 1 auto;
}

.product-impact__analysis-details--full {
  grid-column: 1 / -1;
}

.product-impact__subscores {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

.product-impact__skeleton {
  grid-column: 1 / -1;
  min-height: 320px;
}

@media (min-width: 960px) {
  .product-impact__analysis {
    grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
  }
}

@media (min-width: 640px) {
  .product-impact__subscores {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .product-impact__subscores {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>

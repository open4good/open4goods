<template>
  <section :id="sectionId" class="product-impact">
    <header class="product-impact__header">
      <div class="product-impact__header-content">
        <h2 class="product-impact__title">
          {{ $t('product.impact.title') }}
        </h2>
        <p class="product-impact__subtitle">
          {{ $t('product.impact.subtitle', subtitleParams) }}
        </p>
      </div>
    </header>

    <div class="product-impact__primary">
      <ProductImpactEcoScoreCard
        :score="primaryScore"
        :vertical-home-url="verticalHomeUrl"
        :detail-scores="detailScores"
        :show-radar="showRadar"
        :radar-axes="radarAxes"
        :chart-series="chartSeries"
        :product-name="productName"
        :product-brand="productBrand"
        :product-model="productModel"
        :product-image="productImage"
        :vertical-title="verticalTitle"
        :expanded-score-id="expandedScoreId"
      />
    </div>

    <!-- End of Life Alert -->
    <v-alert v-if="isEndOfLife" type="warning" variant="tonal" class="mt-6">
      {{
        $t('product.impact.endOfLife', {
          brand: productBrand,
          onMarketEndDate: formattedOnMarketEndDate,
        })
      }}
    </v-alert>

    <!-- EPREL Details Table -->
    <EprelDetailsTable
      v-if="hasEprelData"
      :eprel-data="productEprelData"
      class="mt-6"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'

import ProductImpactEcoScoreCard from './impact/ProductImpactEcoScoreCard.vue'
import EprelDetailsTable from './EprelDetailsTable.vue'
import type { ScoreView } from './impact/impact-types'
import type { ProductEprelDto } from '~~/shared/api-client'

interface EprelDataWrapper {
  eprelDatas?: ProductEprelDto
}

type RadarSeriesKey = 'current' | 'best' | 'worst'

interface RadarAxisEntry {
  id: string
  name: string
  attributeValue: string | null
}

interface RadarSeriesEntry {
  key: RadarSeriesKey
  name: string
  values: Array<number | null>
  rawValues?: Array<number | null>
}

interface RadarDataset {
  axes: RadarAxisEntry[]
  series: RadarSeriesEntry[]
}

interface ChartSeriesEntry {
  label: string
  values: Array<number | null>
  rawValues?: Array<number | null>
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
  productBrand: {
    type: String,
    default: '',
  },
  productModel: {
    type: String,
    default: '',
  },
  productImage: {
    type: String,
    default: '',
  },
  verticalHomeUrl: {
    type: String,
    default: '',
  },
  verticalTitle: {
    type: String,
    default: '',
  },
  subtitleParams: {
    type: Object as PropType<Record<string, string> | undefined>,
    default: undefined,
  },
  expandedScoreId: {
    type: String as PropType<string | null>,
    default: null,
  },
  aiImpactText: {
    type: String as PropType<string | null>,
    default: null,
  },
  onMarketEndDate: {
    type: [String, Number, Date] as PropType<
      string | number | Date | null | undefined
    >,
    default: null,
  },
  eprelData: {
    type: Object as PropType<EprelDataWrapper>,
    default: null,
  },
})

const radarData = toRef(props, 'radarData')
const productName = toRef(props, 'productName')
const productBrand = toRef(props, 'productBrand')
const productModel = toRef(props, 'productModel')
const productImage = toRef(props, 'productImage')
const verticalHomeUrl = toRef(props, 'verticalHomeUrl')
const verticalTitle = toRef(props, 'verticalTitle')
const subtitleParams = toRef(props, 'subtitleParams')
const expandedScoreId = toRef(props, 'expandedScoreId')
const onMarketEndDate = toRef(props, 'onMarketEndDate')
const { t, locale } = useI18n()

const primaryScore = computed(
  () =>
    props.scores.find(score => score.id?.toUpperCase() === 'ECOSCORE') ?? null
)
const detailScores = computed(() =>
  props.scores.filter(score => score.id?.toUpperCase() !== 'ECOSCORE')
)
const radarAxes = computed<RadarAxisEntry[]>(() => radarData.value.axes ?? [])
const availableSeries = computed<RadarSeriesEntry[]>(
  () => radarData.value.series ?? []
)

const radarSeriesStyles: Record<
  RadarSeriesKey,
  { line: string; area: string; symbol: string }
> = {
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

  return availableSeries.value
    .map(entry => {
      const style = radarSeriesStyles[entry.key]
      const values = radarAxes.value.map((_, index) => {
        const value = entry.values[index]
        return typeof value === 'number' && Number.isFinite(value)
          ? value
          : null
      })

      const rawValues = 'rawValues' in entry ? entry.rawValues : undefined

      const hasRenderableValue = values.some(value => value !== null)
      if (!hasRenderableValue) {
        return null
      }

      const translationKey = translationKeys[entry.key]
      const label = translationKey
        ? t(translationKey, { product: entry.name })
        : entry.name

      return {
        label,
        values,
        rawValues,
        lineColor: style.line,
        areaColor: style.area,
        symbolColor: style.symbol,
      }
    })
    .filter(
      (seriesEntry): seriesEntry is ChartSeriesEntry => seriesEntry !== null
    )
})

const showRadar = computed(
  () => radarAxes.value.length >= 3 && chartSeries.value.length > 0
)

const isEndOfLife = computed(() => {
  const normalized = normalizeTimestamp(onMarketEndDate.value)
  if (!normalized) return false
  const date = new Date(normalized)
  // Check if valid date
  if (isNaN(date.getTime())) return false
  return date < new Date()
})

const formattedOnMarketEndDate = computed(() => {
  const normalized = normalizeTimestamp(onMarketEndDate.value)
  if (!normalized) return ''
  const date = new Date(normalized)
  if (isNaN(date.getTime())) return ''
  return format(date, 'dd MMM yyyy', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const productEprelData = toRef(props, 'eprelData')
const hasEprelData = computed(() => !!productEprelData.value?.eprelDatas)
</script>

<style scoped>
.product-impact {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.product-impact__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
}

.product-impact__header-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-width: 60ch;
}

.product-impact__header-actions {
  flex-shrink: 0;
}

.product-impact__title {
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 700;
  line-height: 1.1;
}

.product-impact__subtitle {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  line-height: 1.5;
}

.product-impact__primary {
  display: flex;
  flex-direction: column;
}

@media (max-width: 600px) {
  .product-impact__header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .product-impact__toggle {
    width: 100%;
  }
}
</style>

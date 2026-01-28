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

      <div class="product-impact__header-actions">
        <v-btn
          class="product-impact__cta"
          variant="flat"
          rounded="pill"
          size="small"
          :to="methodologyHref"
        >
          {{ $t('product.impact.methodologyLink') }}
        </v-btn>
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
        :score-min="scoreMin"
        :score-max="scoreMax"
      />
    </div>

    <!-- End of Life Card -->
    <v-card
      v-if="isEndOfLife"
      class="product-impact__end-of-life mt-6"
      color="warning"
      variant="tonal"
      data-testid="end-of-life-card"
    >
      <v-card-text class="product-impact__end-of-life-body">
        <div class="product-impact__end-of-life-layout">
          <v-icon
            class="product-impact__end-of-life-icon"
            size="56"
            icon="mdi-alert-decagram-outline"
          />
          <div class="product-impact__end-of-life-content">
            <p class="product-impact__end-of-life-title">
              {{ $t('product.impact.endOfLifeTitle') }}
            </p>
            <p class="product-impact__end-of-life-description">
              {{ endOfLifeDescription }}
            </p>
            <div class="product-impact__end-of-life-details">
              <div
                v-if="formattedSupportEndDate"
                class="product-impact__end-of-life-detail"
              >
                <span class="product-impact__end-of-life-label">
                  {{ $t('product.impact.endOfLifeSupportEnd') }}
                </span>
                <v-chip
                  size="small"
                  color="primary"
                  variant="tonal"
                  @click="scrollToTimeline"
                >
                  {{ formattedSupportEndDate }}
                </v-chip>
              </div>
              <div
                v-if="supportDuration"
                class="product-impact__end-of-life-detail"
              >
                <span class="product-impact__end-of-life-label">
                  {{ $t('product.impact.endOfLifeSupportDuration') }}
                </span>
                <v-chip size="small" color="primary" variant="tonal">
                  {{ supportDuration }}
                </v-chip>
              </div>
              <div
                v-if="supportRemaining"
                class="product-impact__end-of-life-detail"
              >
                <span class="product-impact__end-of-life-label">
                  {{ $t('product.impact.endOfLifeSupportRemaining') }}
                </span>
                <v-chip
                  size="small"
                  :color="supportRemainingChipColor"
                  variant="tonal"
                >
                  {{ supportRemaining }}
                </v-chip>
              </div>
            </div>
          </div>
        </div>
      </v-card-text>
    </v-card>

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
import { differenceInMonths, differenceInYears, format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'

import ProductImpactEcoScoreCard from './impact/ProductImpactEcoScoreCard.vue'
import EprelDetailsTable from './EprelDetailsTable.vue'
import type { ScoreView } from './impact/impact-types'
import type { ProductEprelDto } from '~~/shared/api-client'
import { normalizeTimestamp } from '~/utils/date-parsing'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

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
  scoreMin: {
    type: Number,
    default: 0,
  },
  scoreMax: {
    type: Number,
    default: 20,
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
const productEprelData = toRef(props, 'eprelData')
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

const supportStartDate = computed<Date | null>(() => {
  const normalized = normalizeTimestamp(onMarketEndDate.value)
  if (!normalized) return null
  const date = new Date(normalized)
  if (isNaN(date.getTime())) return null
  return date
})

const isEndOfLife = computed(() => {
  if (!supportStartDate.value) return false
  return supportStartDate.value < new Date()
})

const formattedSupportStartDate = computed(() => {
  if (!supportStartDate.value) return ''
  return format(supportStartDate.value, 'dd MMM yyyy', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const hasEprelData = computed(() => !!productEprelData.value?.eprelDatas)

const minGuaranteedSupportYears = computed(() => {
  const raw =
    productEprelData.value?.eprelDatas?.categorySpecificAttributes
      ?.minGuaranteedSupportYears
  return Number(raw) || 0
})

const supportEndDate = computed<Date | null>(() => {
  if (!supportStartDate.value || !minGuaranteedSupportYears.value) return null
  const supportEnd = new Date(supportStartDate.value)
  supportEnd.setFullYear(
    supportEnd.getFullYear() + minGuaranteedSupportYears.value
  )
  return supportEnd
})

const formattedSupportEndDate = computed(() => {
  if (!supportEndDate.value) return ''
  return format(supportEndDate.value, 'dd MMM yyyy', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
})

const formatDuration = (start: Date, end: Date) => {
  if (end <= start) return null
  const years = differenceInYears(end, start)
  const months = differenceInMonths(end, start) % 12
  const parts: string[] = []

  if (years > 0) parts.push(t('common.count.years', { count: years }, years))
  if (months > 0)
    parts.push(t('common.count.months', { count: months }, months))

  return parts.join(' ')
}

const supportDuration = computed(() => {
  if (!supportStartDate.value || !supportEndDate.value) return null
  return formatDuration(supportStartDate.value, supportEndDate.value)
})

const supportRemainingIsExpired = computed(() => {
  if (!supportEndDate.value) return false
  return supportEndDate.value <= new Date()
})

const supportRemaining = computed(() => {
  if (!supportEndDate.value) return null
  if (supportRemainingIsExpired.value) {
    return t('product.impact.endOfLifeSupportExpired')
  }
  return formatDuration(new Date(), supportEndDate.value)
})

const supportRemainingChipColor = computed(() => {
  if (supportRemainingIsExpired.value) return 'error'
  return 'success'
})

const endOfLifeDescription = computed(() => {
  if (!formattedSupportStartDate.value) {
    return t('product.impact.endOfLifeDescriptionFallback', {
      brand: productBrand.value,
    })
  }
  return t('product.impact.endOfLifeDescription', {
    brand: productBrand.value,
    onMarketEndDate: formattedSupportStartDate.value,
  })
})

const normalizedVerticalEcoscorePath = computed(() => {
  const raw = props.verticalHomeUrl?.trim()
  if (!raw) {
    return null
  }

  const sanitized = raw.replace(/^\/+/, '').replace(/\/+$/, '')
  if (!sanitized.length) {
    return null
  }

  return `/${sanitized}/ecoscore`
})

const methodologyHref = computed(() => {
  if (normalizedVerticalEcoscorePath.value) {
    return normalizedVerticalEcoscorePath.value
  }

  return resolveLocalizedRoutePath('impact-score', locale.value)
})

const scrollToTimeline = () => {
  const timelineSection = document.getElementById('cycle-de-vie')
  if (timelineSection) {
    timelineSection.scrollIntoView({ behavior: 'smooth' })
  }
}
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

.product-impact__end-of-life {
  border-radius: 16px;
}

.product-impact__end-of-life-body {
  padding: 1.5rem;
}

.product-impact__end-of-life-layout {
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
}

.product-impact__end-of-life-icon {
  color: rgba(var(--v-theme-warning));
}

.product-impact__end-of-life-content {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-impact__end-of-life-title {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-strong), 0.95);
}

.product-impact__end-of-life-description {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  line-height: 1.5;
}

.product-impact__end-of-life-details {
  display: grid;
  gap: 0.5rem;
}

.product-impact__end-of-life-detail {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.product-impact__end-of-life-label {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-strong), 0.85);
  min-width: 12rem;
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

  .product-impact__end-of-life-layout {
    flex-direction: column;
    align-items: flex-start;
  }

  .product-impact__end-of-life-label {
    min-width: unset;
  }
}

.product-impact__end-of-life-clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.product-impact__end-of-life-clickable:hover {
  opacity: 0.8;
}
</style>

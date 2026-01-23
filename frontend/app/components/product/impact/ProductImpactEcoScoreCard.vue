<template>
  <article v-if="score" class="impact-ecoscore">
    <div class="d-flex justify-space-between align-center flex-wrap gap-4 mb-6">
      <ImpactScore
        :score="normalizedScore"
        :max="5"
        size="xxlarge"
        mode="badge"
        badge-layout="stacked"
      />

      <v-btn
        :to="methodologyHref"
        variant="flat"
        class="text-none px-6 font-weight-bold"
        color="primary"
        rounded="pill"
        :aria-label="$t('product.impact.methodologyLinkAria')"
        append-icon="mdi-arrow-right"
      >
        {{ $t('product.impact.methodologyLink') }}
      </v-btn>
    </div>

    <div v-if="aiImpactText" class="impact-ecoscore__ai-text">
      <v-icon icon="mdi-robot" size="small" class="mr-2 text-primary" />
      <span class="text-body-2">{{ aiImpactText }}</span>
    </div>

    <v-row v-if="hasDetailContent" class="ma-n2">
      <v-col :cols="shouldDisplayRadar ? 8 : 12" class="pa-2">
        <ProductImpactDetailsTable
          v-if="detailScores.length"
          class="impact-ecoscore__analysis-details h-100"
          :scores="detailScores"
          :product-name="productName"
          :product-brand="productBrand"
          :product-model="productModel"
          :product-image="productImage"
          :vertical-title="verticalTitle"
          :expanded-score-id="expandedScoreId"
        />
      </v-col>

      <v-col v-if="shouldDisplayRadar" cols="4" class="pa-2">
        <div class="impact-ecoscore__analysis-radar">
          <ProductImpactRadarChart
            class="impact-ecoscore__analysis-radar-chart"
            :axes="radarAxes"
            :series="chartSeries"
            :product-name="productName"
          />
        </div>
      </v-col>
    </v-row>
  </article>
  <article v-else class="impact-ecoscore impact-ecoscore--empty">
    <div class="impact-ecoscore__empty-content">
      <h3 class="impact-ecoscore__empty-title">
        {{ $t('product.impact.notRated.title') }}
      </h3>
      <p class="impact-ecoscore__empty-desc">
        {{ $t('product.impact.notRated.description') }}
      </p>
      <NuxtLink
        :to="methodologyHref"
        class="impact-ecoscore__cta mt-4"
        :aria-label="$t('product.impact.methodologyLinkAria')"
      >
        <span>{{ $t('product.impact.methodologyLink') }}</span>
        <v-icon icon="mdi-arrow-top-right" size="18" />
      </NuxtLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductImpactDetailsTable from './ProductImpactDetailsTable.vue'
import ProductImpactRadarChart from './ProductImpactRadarChart.vue'
import type { ScoreView } from './impact-types'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

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
  score: ScoreView | null
  verticalHomeUrl?: string | null
  detailScores?: ScoreView[]
  showRadar?: boolean
  radarAxes?: RadarAxisEntry[]
  chartSeries?: RadarSeriesEntry[]
  productName?: string
  productBrand?: string
  productModel?: string
  productImage?: string
  verticalTitle?: string
  expandedScoreId?: string | null
  aiImpactText?: string | null
}>()

const { locale, t: $t } = useI18n()
const detailScores = computed(() => props.detailScores ?? [])
const radarAxes = computed(() => props.radarAxes ?? [])
const chartSeries = computed(() => props.chartSeries ?? [])
const productName = computed(() => props.productName ?? '')
const productBrand = computed(() => props.productBrand ?? '')
const productModel = computed(() => props.productModel ?? '')
const productImage = computed(() => props.productImage ?? '')
const verticalTitle = computed(() => props.verticalTitle ?? '')
const aiImpactText = computed(() => props.aiImpactText ?? '')
const shouldDisplayRadar = computed(() =>
  Boolean(
    props.showRadar &&
    radarAxes.value.length >= 3 &&
    chartSeries.value.length > 0
  )
)
const hasDetailContent = computed(
  () => shouldDisplayRadar.value || detailScores.value.length > 0
)

const normalizedScore = computed(() => {
  const rawValue = Number.isFinite(props.score?.value)
    ? Number(props.score?.value)
    : null

  if (rawValue == null) {
    return 0
  }

  // If value > 5, it means it's on a scale of 20. Normalize to 5.
  if (rawValue > 5) {
    return Math.max(0, Math.min(rawValue / 4, 5))
  }

  return Math.max(0, Math.min(rawValue, 5))
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

defineExpose({})
</script>

<style scoped>
.impact-ecoscore {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 26px;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-100), 0.95),
    rgba(var(--v-theme-surface-glass-strong), 0.9)
  );
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
  min-height: 100%;
}

.impact-ecoscore__header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.impact-ecoscore__header-main {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 38ch;
}

.impact-ecoscore__title {
  margin: 0;
  font-size: clamp(1.4rem, 2.5vw, 2rem);
  font-weight: 700;
}

.impact-ecoscore__description {
  margin: 0.5rem 0 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-size: 0.95rem;
}

.impact-ecoscore__score {
  display: flex;
  justify-content: flex-start;
}

.impact-ecoscore__analysis {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
  align-items: stretch;
}

.impact-ecoscore__analysis-radar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  width: 100%;
}

.impact-ecoscore__analysis-radar-chart {
  flex: 1 1 auto;
  min-height: 400px;
}

.impact-ecoscore__analysis-details--full {
  grid-column: 1 / -1;
}

.impact-ecoscore__cta {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.1rem;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-surface-default), 0.9);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.impact-ecoscore__cta:hover,
.impact-ecoscore__cta:focus-visible {
  transform: translateY(-2px);
  background: rgba(var(--v-theme-surface-default), 1);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.18);
}

.impact-ecoscore__cta:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-primary-highlight), 0.35);
}

.impact-ecoscore--empty {
  align-items: center;
  justify-content: center;
  text-align: center;
}

.impact-ecoscore__placeholder {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-ecoscore__empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 2rem;
}

.impact-ecoscore__empty-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}

.impact-ecoscore__empty-desc {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  margin: 0;
}

@media (max-width: 768px) {
  .impact-ecoscore__header-main {
    max-width: 100%;
  }

  .impact-ecoscore__cta {
    width: 100%;
    justify-content: center;
  }
}

/* Analysis grid layout removed as it's now single column usually */

@media (min-width: 640px) {
  .impact-ecoscore__analysis-radar-chart {
    min-height: 420px;
  }
}

@media (min-width: 1280px) {
  .impact-ecoscore__analysis-radar-chart {
    min-height: 460px;
  }
}
</style>

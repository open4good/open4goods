<template>
  <article v-if="score" class="impact-ecoscore">
    <header class="impact-ecoscore__header">
      <div class="impact-ecoscore__header-main">
        <span class="impact-ecoscore__eyebrow">{{
          $t('product.impact.primaryScoreLabel')
        }}</span>
        <h3 class="impact-ecoscore__title">{{ score.label }}</h3>
        <p v-if="score.description" class="impact-ecoscore__description">
          {{ score.description }}
        </p>
      </div>

      <div class="impact-ecoscore__header-actions">
        <v-checkbox
          v-model="showVirtualScores"
          :label="$t('product.impact.showVirtualScores')"
          hide-details
          density="compact"
        />

        <NuxtLink
          :to="methodologyHref"
          class="impact-ecoscore__cta"
          :aria-label="$t('product.impact.methodologyLinkAria')"
        >
          <span>{{ $t('product.impact.methodologyLink') }}</span>
          <v-icon icon="mdi-arrow-top-right" size="18" />
        </NuxtLink>
      </div>
    </header>

    <div class="impact-ecoscore__score">
      <ImpactScore :score="normalizedScore" :max="5" size="large" />
    </div>

    <div v-if="hasDetailContent" class="impact-ecoscore__analysis">
      <ProductImpactDetailsTable
        v-if="detailScores.length"
        class="impact-ecoscore__analysis-details"
        :class="{
          'impact-ecoscore__analysis-details--full': !shouldDisplayRadar,
        }"
        :scores="filteredDetailScores"
        :product-name="productName"
        :product-brand="productBrand"
        :product-model="productModel"
        :product-image="productImage"
        :vertical-title="verticalTitle"
      />
    </div>

    <div v-if="showAccordion" class="impact-ecoscore__accordion">
      <v-btn
        class="impact-ecoscore__accordion-btn"
        color="primary"
        variant="text"
        block
        :append-icon="
          isSubscoreExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'
        "
        @click="toggleSubscores"
      >
        {{
          isSubscoreExpanded
            ? $t('product.impact.hideDetails')
            : $t('product.impact.showDetails')
        }}
      </v-btn>
    </div>

    <v-expand-transition v-if="showAccordion">
      <div v-show="isSubscoreExpanded" class="impact-ecoscore__subscores">
        <div v-if="shouldDisplayRadar" class="impact-ecoscore__analysis-radar">
          <ProductImpactRadarChart
            class="impact-ecoscore__analysis-radar-chart"
            :axes="radarAxes"
            :series="chartSeries"
            :product-name="productName"
          />
        </div>

        <v-skeleton-loader
          v-if="loading"
          type="image, article"
          class="impact-ecoscore__skeleton"
        />
        <template v-else>
          <ProductImpactSubscoreCard
            v-for="subScore in secondaryScores"
            :key="subScore.id"
            :score="subScore"
            :product-name="productName"
            :product-brand="productBrand"
            :product-model="productModel"
            :product-image="productImage"
            :vertical-title="verticalTitle"
          />
        </template>
      </div>
    </v-expand-transition>
  </article>
  <article v-else class="impact-ecoscore impact-ecoscore--empty">
    <span class="impact-ecoscore__placeholder">{{
      $t('product.impact.noPrimaryScore')
    }}</span>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductImpactDetailsTable from './ProductImpactDetailsTable.vue'
import ProductImpactRadarChart from './ProductImpactRadarChart.vue'
import ProductImpactSubscoreCard from './ProductImpactSubscoreCard.vue'
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
  loading?: boolean
  secondaryScores?: ScoreView[]
  productBrand?: string
  productModel?: string
  productImage?: string
  verticalTitle?: string
}>()

const { locale, t: $t } = useI18n()
const isSubscoreExpanded = ref(false)
const showVirtualScores = ref(false)

const detailScores = computed(() => props.detailScores ?? [])
const radarAxes = computed(() => props.radarAxes ?? [])
const chartSeries = computed(() => props.chartSeries ?? [])
const productName = computed(() => props.productName ?? '')
const productBrand = computed(() => props.productBrand ?? '')
const productModel = computed(() => props.productModel ?? '')
const productImage = computed(() => props.productImage ?? '')
const verticalTitle = computed(() => props.verticalTitle ?? '')
const secondaryScores = computed(() => props.secondaryScores ?? [])
const loading = computed(() => props.loading ?? false)

const filteredDetailScores = computed(() => {
  return detailScores.value.filter(score => {
    // Virtual score filter (hide virtual if toggle is off)
    if (!showVirtualScores.value && score.virtual) {
      return false
    }

    return true
  })
})
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
const showAccordion = computed(
  () =>
    loading.value ||
    secondaryScores.value.length > 0 ||
    shouldDisplayRadar.value
)

const toggleSubscores = () => {
  isSubscoreExpanded.value = !isSubscoreExpanded.value
}

const normalizedScore = computed(() => {
  const rawValue = Number.isFinite(props.score?.value)
    ? Number(props.score?.value)
    : null

  if (rawValue == null) {
    return 0
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

defineExpose({
  showVirtualScores,
})
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

.impact-ecoscore__eyebrow {
  display: inline-flex;
  align-items: center;
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-soft), 0.85);
  margin-bottom: 0.25rem;
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
  grid-column: 1 / -1;
  width: 100%;
  margin-bottom: 2rem;
}

.impact-ecoscore__analysis-radar-chart {
  flex: 1 1 auto;
  min-height: 400px;
}

.impact-ecoscore__analysis-details--full {
  grid-column: 1 / -1;
}

.impact-ecoscore__accordion {
  padding-top: 0.5rem;
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
}

.impact-ecoscore__accordion-btn {
  justify-content: space-between;
  font-weight: 600;
}

.impact-ecoscore__subscores {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

.impact-ecoscore__skeleton {
  grid-column: 1 / -1;
  min-height: 320px;
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
  .impact-ecoscore__subscores {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .impact-ecoscore__subscores {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>

<template>
  <article class="impact-subscore">
    <ProductImpactSubscoreHeader :title="score.label" :on20="score.on20" />

    <div class="impact-subscore__value">
      <div class="impact-subscore__value-primary">
        <slot
          name="visual"
          :score="score"
          :absolute-value="absoluteValue"
          :relative-value="relativeScore"
          :product-absolute-value="productAbsoluteValue"
          :unit="absoluteUnit"
        >
          <div class="impact-subscore__value-default">
            <span class="impact-subscore__value-number">
              {{ absoluteValue ?? '—' }}
              <span v-if="absoluteUnit" class="impact-subscore__value-unit">{{
                absoluteUnit
              }}</span>
            </span>
          </div>
        </slot>
      </div>

      <ImpactCoefficientBadge
        v-if="coefficientValue != null"
        class="impact-subscore__coefficient"
        :value="coefficientValue"
        :tooltip-params="{ scoreName: score.label }"
      />
    </div>

    <div v-if="score.energyLetter" class="impact-subscore__badge">
      <span class="impact-subscore__energy">{{ score.energyLetter }}</span>
    </div>

    <ProductImpactSubscoreChart
      v-if="hasDistribution"
      :distribution="score.distribution ?? []"
      :label="score.label"
      :average-value="score.absolute?.avg ?? null"
      :current-value="score.value ?? null"
      :product-name="productName"
    />

    <v-expansion-panels
      v-if="hasDetails"
      class="impact-subscore__details"
      variant="accordion"
    >
      <v-expansion-panel elevation="0" rounded="lg">
        <v-expansion-panel-title class="impact-subscore__details-title">
          {{ $t('product.impact.subscoreDetailsToggle') }}
        </v-expansion-panel-title>
        <v-expansion-panel-text class="impact-subscore__details-content">
          <ProductImpactSubscoreExplanation
            :score="score"
            :absolute-value="absoluteValue"
            :product-name="productName"
            :product-brand="productBrand"
            :product-model="productModel"
            :vertical-title="verticalTitle"
            :importance-description="importanceDescription"
          />
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductImpactSubscoreHeader from './ProductImpactSubscoreHeader.vue'
import ProductImpactSubscoreChart from './ProductImpactSubscoreChart.vue'
import ProductImpactSubscoreExplanation from './ProductImpactSubscoreExplanation.vue'
import ImpactCoefficientBadge from '~/components/shared/ui/ImpactCoefficientBadge.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView
  productName: string
  productBrand: string
  productModel: string
  productImage: string | null
  verticalTitle: string
}>()

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

const absoluteUnit = computed(() => props.score.unit?.toString().trim() || null)

const hasDistribution = computed(() =>
  Boolean(props.score.distribution?.length)
)
const hasMetadata = computed(() =>
  Object.values(props.score.metadatas ?? {})
    .map(value => (value == null ? null : String(value).trim()))
    .some(value => (value?.length ?? 0) > 0)
)

const hasRanking = computed(() => {
  const ranking = props.score.ranking
  if (ranking == null) {
    return false
  }

  const numeric = Number(ranking)
  return Number.isFinite(numeric)
})

const hasDetails = computed(() => {
  const hasDescription =
    typeof props.score.description === 'string' &&
    props.score.description.trim().length > 0
  return (
    hasDescription ||
    Boolean(absoluteValue.value) ||
    hasRanking.value ||
    hasMetadata.value ||
    hasImportance.value
  )
})

const importanceDescription = computed(
  () => props.score.importanceDescription?.toString().trim() || ''
)
const hasImportance = computed(() => importanceDescription.value.length > 0)

const productAbsoluteValue = computed(() => {
  const absoluteValue = props.score.absolute?.value
  if (typeof absoluteValue === 'number' && Number.isFinite(absoluteValue)) {
    return absoluteValue
  }

  const directValue = props.score.value
  if (typeof directValue === 'number' && Number.isFinite(directValue)) {
    return directValue
  }

  if (
    typeof props.score.absoluteValue === 'number' &&
    Number.isFinite(props.score.absoluteValue)
  ) {
    return props.score.absoluteValue
  }

  const parsed = Number(props.score.absoluteValue)
  return Number.isFinite(parsed) ? parsed : null
})

const productBrand = computed(() => props.productBrand?.trim() ?? '')
const productModel = computed(() => props.productModel?.trim() ?? '')
const verticalTitle = computed(() => props.verticalTitle?.trim() ?? '')

const coefficientValue = computed(() => {
  const rawCoefficient = props.score.coefficient
  if (rawCoefficient == null) {
    return null
  }

  if (typeof rawCoefficient === 'number') {
    return Number.isFinite(rawCoefficient) ? rawCoefficient : null
  }

  const numeric = Number(rawCoefficient)
  return Number.isFinite(numeric) ? numeric : null
})
</script>

<style scoped>
.impact-subscore {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1.75rem 1.5rem;
  border-radius: 22px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  min-height: 100%;
}

.impact-subscore__value {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
}

.impact-subscore__value-primary {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.5rem 0.75rem;
}

.impact-subscore__value-default {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.impact-subscore__value-number {
  font-size: clamp(2.25rem, 2.2rem + 0.5vw, 2.75rem);
  font-weight: 700;
  line-height: 1.1;
  color: rgb(var(--v-theme-text-neutral-strong));
  display: inline-flex;
  align-items: baseline;
  gap: 0.35rem;
}

.impact-subscore__value-unit {
  font-size: 1rem;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.impact-subscore__coefficient {
  align-self: flex-start;
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

.impact-subscore__details {
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.impact-subscore__details-title {
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore__details-content {
  padding-top: 0;
}

.impact-subscore__details :deep(.v-expansion-panel-title__overlay) {
  display: none;
}

.impact-subscore__details :deep(.v-expansion-panel-title) {
  padding: 0.75rem 1rem;
}

.impact-subscore__details :deep(.v-expansion-panel-text__wrapper) {
  padding: 0 1rem 1rem;
}
</style>

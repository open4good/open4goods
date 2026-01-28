<template>
  <article class="impact-subscore">
    <ProductImpactSubscoreHeader
      :title="score.label"
      :on20="score.on20"
      :lifecycle="score.participateInACV ?? []"
    />

    <!-- Enhanced attribute display with icon, description, value and sourcing -->
    <div class="impact-subscore__attribute-display">
      <div class="impact-subscore__attribute-left">
        <v-icon
          v-if="scoreIcon"
          :icon="scoreIcon"
          class="impact-subscore__icon"
          size="48"
        />
        <p v-if="score.description" class="impact-subscore__description">
          {{ score.description }}
        </p>
      </div>

      <div class="impact-subscore__attribute-right">
        <div class="impact-subscore__attribute-value-row">
          <span class="impact-subscore__attribute-value">
            {{ absoluteValue ?? '—' }}
          </span>
          <span v-if="absoluteUnit" class="impact-subscore__attribute-unit">
            {{ absoluteUnit }}
          </span>
          <ProductAttributeSourcingLabel
            v-if="score.attributeSourcing"
            :sourcing="score.attributeSourcing"
            :value="absoluteValue ?? ''"
          />
        </div>
        <ImpactCoefficientBadge
          v-if="coefficientValue != null"
          class="impact-subscore__coefficient"
          :value="coefficientValue"
          :tooltip-params="{ scoreName: score.label }"
        />
      </div>
    </div>

    <div class="impact-subscore__value mt-8">
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
    </div>

    <div v-if="hasEnergyClass" class="impact-subscore__badge">
      <v-img
        v-if="energyClassImageSrc"
        :src="energyClassImageSrc"
        :alt="energyLabelAlt"
        class="impact-subscore__energy-image"
        width="140"
        height="88"
        cover
      />
      <span class="impact-subscore__energy" :aria-label="energyLabelAlt">
        {{ energyClassLabel }}
      </span>
    </div>

    <ProductImpactSubscoreChart
      v-if="hasDistribution"
      :distribution="score.distribution ?? []"
      :label="score.label"
      :average-value="score.absolute?.avg ?? null"
      :current-value="score.absolute?.value ?? null"
      :normalized-current-value="score.relativeValue ?? null"
      :normalization-method="score.scoring?.normalization?.method ?? null"
      :normalization-params="score.scoring?.normalization?.params ?? null"
      :scale="score.scoring?.scale ?? null"
      :impact-better-is="score.impactBetterIs ?? null"
      :std-dev="score.absolute?.stdDev ?? null"
      :product-name="productName"
    />

    <div v-if="hasDetails" class="impact-subscore__details">
      <ProductImpactSubscoreExplanation
        :score="score"
        :absolute-value="absoluteValue"
        :product-name="productName"
        :product-brand="productBrand"
        :product-model="productModel"
        :vertical-title="verticalTitle"
        :importance-description="importanceDescription"
      />
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductImpactSubscoreHeader from './ProductImpactSubscoreHeader.vue'
import ProductImpactSubscoreChart from './ProductImpactSubscoreChart.vue'
import ProductImpactSubscoreExplanation from './ProductImpactSubscoreExplanation.vue'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
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

const { n, t } = useI18n()

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

const scoreIcon = computed(() => {
  const icon = props.score.icon?.toString().trim() ?? ''
  return icon.length ? icon : null
})

const energyClassLabel = computed(() => {
  const display = props.score.energyClassDisplay?.trim()
  if (display?.length) {
    return display
  }

  return props.score.energyLetter?.trim() ?? ''
})

const energyClassImage = computed(
  () => props.score.energyClassImage?.trim() ?? ''
)

const energyClassImageSrc = computed(() =>
  energyClassImage.value.length ? `/images/eprel/${energyClassImage.value}` : ''
)

const hasEnergyClass = computed(
  () =>
    energyClassLabel.value.length > 0 || energyClassImageSrc.value.length > 0
)

const energyLabelAlt = computed(() =>
  t('product.impact.energyLabelAlt', {
    value: energyClassLabel.value || props.score.label || '—',
  })
)

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
  align-items: center;
  gap: 0.5rem;
  text-align: center;
}

.impact-subscore__value-primary {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: center;
  gap: 0.5rem 0.75rem;
}

.impact-subscore__value-default {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  align-self: center;
}

.impact-subscore__badge {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.impact-subscore__energy-image {
  max-width: min(100%, 180px);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
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
  padding: 1rem;
}

/* Enhanced attribute display layout */
.impact-subscore__attribute-display {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.5rem;
  padding: 1rem 0;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-subscore__attribute-left {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  flex: 1;
  min-width: 0;
}

.impact-subscore__icon {
  color: rgb(var(--v-theme-primary));
  opacity: 0.85;
}

.impact-subscore__description {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.impact-subscore__attribute-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
  flex-shrink: 0;
}

.impact-subscore__attribute-value-row {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
}

.impact-subscore__attribute-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore__attribute-unit {
  font-size: 0.95rem;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

@media (max-width: 600px) {
  .impact-subscore__attribute-display {
    flex-direction: column;
    gap: 1rem;
  }

  .impact-subscore__attribute-right {
    align-items: flex-start;
    width: 100%;
  }
}
</style>

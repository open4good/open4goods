<template>
  <article class="impact-subscore">
    <ProductImpactSubscoreHeader
      :title="score.label"
      :subtitle="score.description ?? undefined"
      :on20="score.on20"
      :percent="score.percent"
    />

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

    <ProductImpactSubscoreChart
      v-if="hasDistribution"
      :distribution="score.distribution ?? []"
      :label="score.label"
      :relative-value="score.relativeValue"
      :product-name="productName"
    />

    <ProductImpactSubscoreExplanation :score="score" :absolute-value="absoluteValue" />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import ProductImpactSubscoreHeader from './ProductImpactSubscoreHeader.vue'
import ProductImpactSubscoreChart from './ProductImpactSubscoreChart.vue'
import ProductImpactSubscoreExplanation from './ProductImpactSubscoreExplanation.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView
  productName: string
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

const hasDistribution = computed(() => Boolean(props.score.distribution?.length))
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
</style>

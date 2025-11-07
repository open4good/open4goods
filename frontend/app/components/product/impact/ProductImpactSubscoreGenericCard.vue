<template>
  <article class="impact-subscore">
    <ProductImpactSubscoreHeader
      :title="score.label"
      :on20="score.on20"
      :coefficient="score.coefficient ?? null"
    />

    <div class="impact-subscore__score">
      <ImpactScore :score="relativeScore" :max="5" show-value size="medium" />
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

    <v-expansion-panels v-if="hasDetails" class="impact-subscore__details" variant="accordion">
      <v-expansion-panel elevation="0" rounded="lg">
        <v-expansion-panel-title class="impact-subscore__details-title">
          {{ $t('product.impact.subscoreDetailsToggle') }}
        </v-expansion-panel-title>
        <v-expansion-panel-text class="impact-subscore__details-content">
          <ProductImpactSubscoreExplanation :score="score" :absolute-value="absoluteValue" />
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
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
const hasMetadata = computed(() =>
  Object.values(props.score.metadatas ?? {})
    .map((value) => (value == null ? null : String(value).trim()))
    .some((value) => (value?.length ?? 0) > 0),
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
  const hasDescription = typeof props.score.description === 'string' && props.score.description.trim().length > 0
  return hasDescription || Boolean(absoluteValue.value) || hasRanking.value || hasMetadata.value
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

.impact-subscore__score {
  display: flex;
  justify-content: flex-start;
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

<template>
  <article class="impact-details">
    <h3 class="impact-details__title">{{ $t('product.impact.detailsTitle') }}</h3>
    <v-table v-if="displayScores.length" density="compact" class="impact-details__table">
      <thead>
        <tr>
          <th scope="col">{{ $t('product.impact.tableHeaders.score') }}</th>
          <th scope="col">{{ $t('product.impact.tableHeaders.value') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="score in displayScores" :key="`${score.id}-details`">
          <th scope="row">{{ score.label }}</th>
          <td>
            <div class="impact-details__value">
              <ProductImpactSubscoreRating
                v-if="score.displayValue != null"
                :score="score.displayValue"
                :max="5"
                size="x-small"
                :show-value="false"
              />
              <span class="impact-details__value-text">{{ formatScoreLabel(score.displayValue) }}</span>
            </div>
          </td>
        </tr>
      </tbody>
    </v-table>
    <p v-else class="impact-details__empty">{{ $t('product.impact.noDetailsAvailable') }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductImpactSubscoreRating from './ProductImpactSubscoreRating.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  scores: ScoreView[]
}>()

type DetailedScore = ScoreView & { displayValue: number | null }

const resolveScoreValue = (score: ScoreView): number | null => {
  if (score.value != null && Number.isFinite(score.value)) {
    return Number(score.value)
  }

  if (score.relativeValue != null && Number.isFinite(score.relativeValue)) {
    return Number(score.relativeValue)
  }

  return null
}

const displayScores = computed<DetailedScore[]>(() =>
  props.scores
    .filter((score) => score.id !== 'ECOSCORE')
    .map((score) => ({
      ...score,
      displayValue: resolveScoreValue(score),
    })),
)

const formatScore = (value: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return value.toFixed(1)
}

const { t } = useI18n()

const formatScoreLabel = (value: number | null) => {
  const formatted = formatScore(value)
  if (formatted === '—') {
    return formatted
  }

  return t('product.impact.valueOutOf', { value: formatted, max: 5 })
}
</script>

<style scoped>
.impact-details {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.impact-details__title {
  margin: 0 0 1rem;
  font-size: 1.2rem;
  font-weight: 600;
}

.impact-details__table :deep(thead th) {
  font-weight: 600;
}

.impact-details__table :deep(tbody th) {
  font-weight: 500;
}

.impact-details__value {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.impact-details__value-text {
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-details__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  font-size: 0.95rem;
}
</style>

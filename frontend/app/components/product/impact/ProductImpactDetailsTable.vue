<template>
  <article class="impact-details">
    <h3 class="impact-details__title">{{ $t('product.impact.detailsTitle') }}</h3>
    <v-data-table
      v-if="hasRows"
      :headers="headers"
      :items="displayScores"
      :items-per-page="displayScores.length"
      class="impact-details__table"
      density="compact"
      hide-default-footer
    >
      <template #[`item.label`]="{ value }">
        <span class="impact-details__indicator">{{ value }}</span>
      </template>
      <template #[`item.displayValue`]="{ item }">
        <div class="impact-details__value">
          <ProductImpactSubscoreRating
            v-if="item.displayValue != null"
            :score="item.displayValue"
            :max="5"
            size="x-small"
            :show-value="false"
          />
          <span class="impact-details__value-text">{{ formatScoreLabel(item.displayValue) }}</span>
        </div>
      </template>
      <template #[`item.coefficient`]="{ item }">
        <ImpactCoefficientBadge
          v-if="item.coefficient != null"
          :value="item.coefficient"
          :tooltip-params="{ scoreName: item.label }"
        />
        <span v-else class="impact-details__coefficient-empty">—</span>
      </template>
    </v-data-table>
    <p v-else class="impact-details__empty">{{ $t('product.impact.noDetailsAvailable') }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactCoefficientBadge from '~/components/shared/ui/ImpactCoefficientBadge.vue'
import ProductImpactSubscoreRating from './ProductImpactSubscoreRating.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  scores: ScoreView[]
}>()

type DetailedScore = ScoreView & { displayValue: number | null; coefficient: number | null }

const resolveScoreValue = (score: ScoreView): number | null => {
  if (score.relativeValue != null && Number.isFinite(score.relativeValue)) {
    return Number(score.relativeValue)
  }

  if (score.value != null && Number.isFinite(score.value)) {
    return Number(score.value)
  }

  return null
}

const resolveCoefficientValue = (value: number | null | undefined): number | null => {
  if (value == null) {
    return null
  }

  const numeric = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numeric)) {
    return null
  }

  return Math.min(Math.max(numeric, 0), 1)
}

const { t } = useI18n()

const displayScores = computed<DetailedScore[]>(() =>
  props.scores
    .filter((score) => score.id !== 'ECOSCORE')
    .map((score) => ({
      ...score,
      displayValue: resolveScoreValue(score),
      coefficient: resolveCoefficientValue(score.coefficient ?? null),
    })),
)

const headers = computed(() => [
  { key: 'label', title: t('product.impact.tableHeaders.score'), sortable: true },
  { key: 'displayValue', title: t('product.impact.tableHeaders.value'), sortable: true },
  { key: 'coefficient', title: t('product.impact.tableHeaders.coefficient'), sortable: true },
])

const hasRows = computed(() => displayScores.value.length > 0)

const formatScore = (value: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return value.toFixed(1)
}

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

.impact-details__table {
  background: transparent;
  --v-data-table-header-background: transparent;
}

.impact-details__table :deep(.v-data-table__tr) {
  border-color: rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-details__table :deep(th) {
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.impact-details__table :deep(td) {
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-details__indicator {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
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

.impact-details__coefficient-empty {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.6);
}

.impact-details__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  font-size: 0.95rem;
}
</style>

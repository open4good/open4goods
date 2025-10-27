<template>
  <article class="impact-details">
    <h3 class="impact-details__title">{{ $t('product.impact.detailsTitle') }}</h3>
    <v-table density="compact" class="impact-details__table">
      <thead>
        <tr>
          <th scope="col">{{ $t('product.impact.tableHeaders.score') }}</th>
          <th scope="col">{{ $t('product.impact.tableHeaders.value') }}</th>
          <th scope="col">{{ $t('product.impact.tableHeaders.ranking') }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="score in scores" :key="`${score.id}-details`">
          <th scope="row">{{ score.label }}</th>
          <td>{{ formatScore(score.relativeValue) }} / 5</td>
          <td>{{ score.ranking ?? '—' }}</td>
        </tr>
      </tbody>
    </v-table>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  scores: ScoreView[]
}>()

const scores = computed(() => props.scores)

const formatScore = (value: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return value.toFixed(1)
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
</style>

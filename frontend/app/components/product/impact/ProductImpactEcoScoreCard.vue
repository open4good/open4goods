<template>
  <article v-if="score" class="impact-ecoscore">
    <header class="impact-ecoscore__header">
      <span class="impact-ecoscore__eyebrow">{{ $t('product.impact.primaryScoreLabel') }}</span>
      <h3 class="impact-ecoscore__title">{{ score.label }}</h3>
      <p v-if="score.description" class="impact-ecoscore__description">{{ score.description }}</p>
    </header>

    <div class="impact-ecoscore__score">
      <ImpactScore :score="relativeScore" :max="5" size="large" show-value />
    </div>
  </article>
  <article v-else class="impact-ecoscore impact-ecoscore--empty">
    <span class="impact-ecoscore__placeholder">{{ $t('product.impact.noPrimaryScore') }}</span>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView | null
}>()

const relativeScore = computed(() => (props.score?.relativeValue ?? 0) || 0)
</script>

<style scoped>
.impact-ecoscore {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 2rem;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-100), 0.95), rgba(var(--v-theme-surface-glass-strong), 0.9));
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
  min-height: 100%;
}

.impact-ecoscore__header {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
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

.impact-ecoscore--empty {
  align-items: center;
  justify-content: center;
  text-align: center;
}

.impact-ecoscore__placeholder {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}
</style>

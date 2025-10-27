<template>
  <article v-if="score" class="impact-ecoscore">
    <header class="impact-ecoscore__header">
      <div>
        <span class="impact-ecoscore__eyebrow">{{ $t('product.impact.primaryScoreLabel') }}</span>
        <h3 class="impact-ecoscore__title">{{ score.label }}</h3>
        <p v-if="score.description" class="impact-ecoscore__description">{{ score.description }}</p>
      </div>
      <div v-if="score.letter" class="impact-ecoscore__letter" aria-hidden="true">
        {{ score.letter }}
      </div>
    </header>

    <div class="impact-ecoscore__score">
      <ImpactScore :score="relativeScore" :max="5" size="large" show-value />
    </div>

    <div v-if="absoluteValue" class="impact-ecoscore__absolute">
      <span class="impact-ecoscore__absolute-label">{{ $t('product.impact.absoluteValue') }}</span>
      <span class="impact-ecoscore__absolute-value">{{ absoluteValue }}</span>
    </div>
  </article>
  <article v-else class="impact-ecoscore impact-ecoscore--empty">
    <span class="impact-ecoscore__placeholder">{{ $t('product.impact.noPrimaryScore') }}</span>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView | null
}>()

const { n } = useI18n()

const relativeScore = computed(() => (props.score?.relativeValue ?? 0) || 0)

const absoluteValue = computed(() => {
  const value = props.score?.absoluteValue
  if (value == null || value === '') {
    return null
  }

  if (typeof value === 'number') {
    return n(value, { maximumFractionDigits: 2, minimumFractionDigits: 0 })
  }

  return String(value)
})
</script>

<style scoped>
.impact-ecoscore {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem;
  border-radius: 26px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-100), 0.95), rgba(var(--v-theme-surface-glass-strong), 0.9));
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.12);
  min-height: 100%;
}

.impact-ecoscore__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
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

.impact-ecoscore__letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 64px;
  border-radius: 18px;
  font-size: 1.75rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-on-accent));
  background: linear-gradient(135deg, rgba(var(--v-theme-accent-supporting), 0.95), rgba(var(--v-theme-accent-primary-highlight), 0.9));
  box-shadow: 0 10px 30px rgba(var(--v-theme-accent-primary-highlight), 0.25);
}

.impact-ecoscore__score {
  display: flex;
  justify-content: flex-start;
}

.impact-ecoscore__absolute {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  padding: 0.75rem 1.25rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-glass), 0.85);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-ecoscore__absolute-label {
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.impact-ecoscore__absolute-value {
  font-size: 1.35rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
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

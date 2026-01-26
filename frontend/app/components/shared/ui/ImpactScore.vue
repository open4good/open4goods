<template>
  <div
    class="impact-score-panel"
    :class="[
      `impact-score-panel--${size}`,
      `impact-score-panel--${accentStep}`,
    ]"
    role="img"
    :aria-label="ariaLabel"
  >
    <div class="impact-score-panel__top">
      <div class="impact-score-panel__col-left">
        <div class="impact-score-panel__score">
          <span class="impact-score-panel__score-value">{{
            formattedScoreValue
          }}</span>
          <span class="impact-score-panel__score-out">/ 20</span>
        </div>
      </div>

      <div v-if="hasMeta" class="impact-score-panel__col-right">
        <v-btn
          v-if="showMethodology"
          class="impact-score-panel__cta"
          variant="text"
          density="compact"
          size="small"
          :to="'./impactscore'"
        >
          Méthodologie
        </v-btn>

        <div v-if="showRange">
          <div class="impact-score-panel__row">
            <span class="impact-score-panel__label">Min</span>
            <span class="impact-score-panel__value">{{
              n(rangeMin, {
                maximumFractionDigits: 1,
                minimumFractionDigits: 0,
              })
            }}</span>
          </div>
          <div class="impact-score-panel__row">
            <span class="impact-score-panel__label">Max</span>
            <span class="impact-score-panel__value">{{
              n(rangeMax, {
                maximumFractionDigits: 1,
                minimumFractionDigits: 0,
              })
            }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="impact-score-panel__bar" aria-hidden="true">
      <div class="impact-score-panel__track">
        <div
          class="impact-score-panel__fill"
          :style="{ width: `${Math.round(progress * 100)}%` }"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  score: {
    type: Number,
    required: true,
  },
  min: {
    type: Number,
    default: 0,
  },
  max: {
    type: Number,
    default: 20,
  },
  size: {
    type: String as PropType<'sm' | 'md' | 'lg'>,
    default: 'md',
  },
  showMethodology: {
    type: Boolean,
    default: true,
  },
  showRange: {
    type: Boolean,
    default: true,
  },
})

const { t, n } = useI18n()

const sanitizeNumber = (value: number, fallback: number) =>
  Number.isFinite(value) ? value : fallback

const rangeMin = computed(() => sanitizeNumber(props.min, 0))
const rangeMax = computed(() => sanitizeNumber(props.max, 20))
const displayScore = computed(() => sanitizeNumber(props.score, 0))

const progress = computed(() => {
  const min = rangeMin.value
  const max = rangeMax.value

  if (max <= min) {
    return 0
  }

  const clamped = Math.min(Math.max(displayScore.value, min), max)
  return (clamped - min) / (max - min)
})

// Accent step based on progress for color theming (low/mid/high)
const accentStep = computed(() => {
  if (progress.value < 1 / 3) return 'low'
  if (progress.value < 2 / 3) return 'mid'
  return 'high'
})

const formattedScoreValue = computed(() =>
  n(displayScore.value, {
    maximumFractionDigits: 1,
    minimumFractionDigits: 0,
  })
)

const ariaLabel = computed(() =>
  t('components.impactScore.svgAriaLabel', { score: formattedScoreValue.value })
)

const hasMeta = computed(() => props.showMethodology || props.showRange)
</script>

<style scoped>
/* Impact Score Panel */
.impact-score-panel {
  position: relative;
  display: grid;
  gap: 10px;
  padding: 14px 16px 12px;
  border-radius: 22px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.22);
  background: rgba(var(--v-theme-surface), 0.3);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
  overflow: hidden;
  isolation: isolate;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.impact-score-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-primary), 0.08),
    rgba(var(--v-theme-surface), 0)
  );
  opacity: 0.75;
  z-index: 0;
  pointer-events: none;
}

/* Accent color variants */
.impact-score-panel--low {
  --impact-accent: rgb(var(--v-theme-error));
}
.impact-score-panel--mid {
  --impact-accent: rgb(var(--v-theme-warning));
}
.impact-score-panel--high {
  --impact-accent: rgb(var(--v-theme-success));
}

/* Size variants */
.impact-score-panel--sm {
  padding: 12px 14px 10px;
  border-radius: 18px;
}

.impact-score-panel--sm .impact-score-panel__score-value {
  font-size: 2.4rem;
}

.impact-score-panel--md .impact-score-panel__score-value {
  font-size: 2.9rem;
}

.impact-score-panel--lg .impact-score-panel__score-value {
  font-size: 3.4rem;
}

/* Top layout */
.impact-score-panel__top {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 14px;
}

/* Score (left column) */
.impact-score-panel__col-left {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
}

.impact-score-panel__score {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-variant-numeric: tabular-nums;
  min-width: 0;
}

.impact-score-panel__score-value {
  font-size: 3.1rem;
  line-height: 1;
  font-weight: 850;
  color: var(--impact-accent);
}

.impact-score-panel__score-out {
  font-size: 1.05rem;
  font-weight: 700;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

/* Meta (right column: Methodology + Min/Max) */
.impact-score-panel__col-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  min-width: 0;
}

.impact-score-panel__row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px; /* Closer together as requested */
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.impact-score-panel__label {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  font-weight: 600;
}

.impact-score-panel__value {
  color: rgba(var(--v-theme-text-neutral-strong), 0.92);
  font-weight: 700;
}

/* CTA mini button */
.impact-score-panel__cta {
  margin-bottom: 4px;
  padding-right: 0;
  letter-spacing: 0;
  text-transform: none;
  font-weight: 600;
  justify-content: flex-end;
}

/* Progress bar */
.impact-score-panel__bar {
  position: relative;
  z-index: 1;
}

.impact-score-panel__track {
  height: 10px;
  border-radius: 999px;
  background: rgba(var(--v-theme-border-primary-strong), 0.16);
  overflow: hidden;
}

.impact-score-panel__fill {
  height: 100%;
  border-radius: 999px;
  background: var(--impact-accent);
  width: 0%;
  transition: width 0.3s ease-out;
}
</style>

<template>
  <div
    class="impact-score-panel"
    :class="[
      `impact-score-panel--${normalizedSize}`,
      `impact-score-panel--${normalizedVariant}`,
      `impact-score-panel--${accentStep}`,
      {
        'impact-score-panel--banner': banner,
        'impact-score-panel--flat': flat,
      },
    ]"
    role="img"
    :aria-label="ariaLabel"
  >
    <!-- Main Content -->
    <div class="impact-score-panel__top">
      <div v-if="!banner && !flat" class="impact-score-panel__col-cta">
        <CtaCard
          :title="t('category.filters.ecoscore.title', 'Impact Score')"
          :subtitle="
            t('category.filters.ecoscore.cta', 'Voir l\'analyse', {
              category: '',
            })
          "
          icon="mdi-leaf"
          size="small"
          :to="'/impact-score'"
          flat
          class="impact-score-panel__cta-card"
        />
      </div>
      <div class="impact-score-panel__col-left">
        <div class="impact-score-panel__score justify-center">
          <span class="impact-score-panel__score-value">{{
            formattedScoreValue
          }}</span>
          <span class="impact-score-panel__score-out">/ 20</span>
        </div>
      </div>

      <div v-if="shouldShowMeta" class="impact-score-panel__col-right">
        <div v-if="showRange" class="impact-score-panel__meta-grid">
          <span class="impact-score-panel__label">Min :</span>
          <span
            class="impact-score-panel__value"
            :class="`impact-score-panel--${getScaleStep(rangeMin)}`"
          >
            {{
              n(rangeMin, {
                maximumFractionDigits: 1,
                minimumFractionDigits: 0,
              })
            }}
          </span>

          <span class="impact-score-panel__label">Max :</span>
          <span
            class="impact-score-panel__value"
            :class="`impact-score-panel--${getScaleStep(rangeMax)}`"
          >
            {{
              n(rangeMax, {
                maximumFractionDigits: 1,
                minimumFractionDigits: 0,
              })
            }}
          </span>
        </div>
        <v-btn
          v-if="showMethodology"
          class="impact-score-panel__cta"
          variant="flat"
          density="compact"
          size="x-small"
          rounded="pill"
          :to="'./ecoscore'"
        >
          Méthodologie
        </v-btn>
      </div>
    </div>

    <div
      v-if="shouldShowBar && showProgressBar"
      class="impact-score-panel__bar"
      aria-hidden="true"
    >
      <div class="impact-score-panel__track">
        <div
          class="impact-score-panel__fill"
          :style="{ width: `${Math.round(progress * 100)}%` }"
        />
      </div>
    </div>

    <!-- Footer -->
    <div v-if="banner" class="impact-score-panel__footer">
      <!-- Left: Icon + Link -->
      <div class="impact-score-panel__footer-left">
        <v-icon
          icon="mdi-leaf"
          size="16"
          class="impact-score-panel__footer-icon"
        />
        <NuxtLink to="/ecoscore" class="impact-score-panel__footer-link">
          Impact Score
          <span v-if="category" class="impact-score-panel__footer-category">
            {{ category }}
          </span>
        </NuxtLink>
      </div>

      <div class="impact-score-panel__separator" />

      <!-- Center: Brand - Model -->
      <div class="impact-score-panel__footer-center">
        <span v-if="brand">{{ brand }}</span>
        <span v-if="brand && model" class="mx-1">-</span>
        <span v-if="model">{{ model }}</span>
      </div>

      <div class="impact-score-panel__separator" />

      <!-- Right: Generation Date -->
      <div class="impact-score-panel__footer-right">
        <span v-if="formattedDate">{{ formattedDate }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import CtaCard from '~/components/shared/CtaCard.vue'

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
    type: String as PropType<
      'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'small' | 'medium' | 'default'
    >,
    default: 'md',
  },
  variant: {
    type: String as PropType<'default' | 'corner'>,
    default: 'default',
  },
  showMethodology: {
    type: Boolean,
    default: true,
  },
  showRange: {
    type: Boolean,
    default: true,
  },
  showProgressBar: {
    type: Boolean,
    default: true,
  },
  banner: {
    type: Boolean,
    default: false,
  },
  flat: {
    type: Boolean,
    default: false,
  },
  brand: {
    type: String,
    default: '',
  },
  model: {
    type: String,
    default: '',
  },
  date: {
    type: [String, Number, Date],
    default: null,
  },
  category: {
    type: String,
    default: '',
  },
})

const { t, n, d } = useI18n()

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

// Helper to determine step for raw values relative to global scale (0-20)
// < 1/3 (6.66) -> low
// < 2/3 (13.33) -> mid
// >= 13.33 -> high
const getScaleStep = (val: number) => {
  const ratio = val / 20
  if (ratio < 1 / 3) return 'low'
  if (ratio < 2 / 3) return 'mid'
  return 'high'
}

const normalizedSize = computed(() => {
  const size = props.size

  if (size === 'small') return 'xs'
  if (size === 'medium' || size === 'default') return 'md'
  return size
})

const normalizedVariant = computed(() =>
  props.variant === 'corner' ? 'corner' : 'default'
)

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

const shouldShowMeta = computed(
  () => normalizedVariant.value === 'default' && hasMeta.value && !props.flat
)

const shouldShowBar = computed(() => normalizedVariant.value === 'default')

const formattedDate = computed(() => {
  if (!props.date) return ''
  const dateObj = props.date instanceof Date ? props.date : new Date(props.date)
  return d(dateObj, 'short')
})
</script>

<style scoped>
/* Impact Score Panel */
.impact-score-panel {
  position: relative;
  display: flex;
  flex-direction: column;
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

.impact-score-panel--banner {
  /* Ensure padding encompasses footer or reset special padding if needed */
  padding-bottom: 0; /* Footer is at bottom */
}

.impact-score-panel--flat {
  background: transparent;
  border: none;
  box-shadow: none;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
}

.impact-score-panel--flat::before {
  display: none;
  background: none;
}

/* Size variants */
.impact-score-panel--xs {
  padding: 8px 10px 8px;
  border-radius: 14px;
}

.impact-score-panel--xs .impact-score-panel__score-value {
  font-size: 1.8rem;
}

.impact-score-panel--xs .impact-score-panel__bar {
  display: none;
}

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

.impact-score-panel--xl {
  padding: 20px 24px 18px;
  border-radius: 26px;
}

.impact-score-panel--xl .impact-score-panel__score-value {
  font-size: 4.2rem;
}

.impact-score-panel--xl .impact-score-panel__bar {
  display: none;
}

/* Variant styles */
.impact-score-panel--corner {
  padding: 6px 8px;
  border-radius: 12px;
  gap: 6px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.12);
  background: rgba(var(--v-theme-surface), 0.7);
}

.impact-score-panel--corner::before {
  opacity: 0.6;
}

.impact-score-panel--corner .impact-score-panel__score {
  gap: 4px;
}

.impact-score-panel--corner .impact-score-panel__score-value {
  font-size: 1.35rem;
  line-height: 1.1;
}

.impact-score-panel--corner .impact-score-panel__score-out {
  font-size: 0.75rem;
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
.impact-score-panel__col-cta {
  display: flex;
  align-items: center;
  margin-right: auto;
  padding-right: 16px;
  border-right: 1px solid rgba(var(--v-theme-border-primary-strong), 0.1);
}

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

.impact-score-panel__meta-grid {
  display: grid;
  grid-template-columns: max-content max-content;
  column-gap: 8px;
  row-gap: 0;
  align-items: baseline;
  text-align: right;
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;
}

.impact-score-panel__label {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  font-weight: 600;
  text-align: right;
}

.impact-score-panel__value {
  color: rgba(var(--v-theme-text-neutral-strong), 0.92);
  font-weight: 700;
  text-align: left; /* Values align left */
}

/* Colorize values inside meta grid */
.impact-score-panel__value.impact-score-panel--low {
  color: rgb(var(--v-theme-error));
}
.impact-score-panel__value.impact-score-panel--mid {
  color: rgb(var(--v-theme-warning));
}
.impact-score-panel__value.impact-score-panel--high {
  color: rgb(var(--v-theme-success));
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

/* === Footer Mode Styles === */
.impact-score-panel__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  min-height: 36px;
}

.impact-score-panel__footer-left,
.impact-score-panel__footer-center,
.impact-score-panel__footer-right {
  display: flex;
  align-items: center;
}

.impact-score-panel__footer-left {
  gap: 6px;
}

.impact-score-panel__footer-icon {
  color: var(--impact-accent);
  opacity: 0.9;
}

.impact-score-panel__footer-link {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-strong), 0.9);
  text-decoration: none;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.impact-score-panel__footer-link:hover {
  text-decoration: underline;
  color: var(--impact-accent);
}

.impact-score-panel__footer-category {
  opacity: 0.85;
  font-weight: 500;
}

.impact-score-panel__footer-center {
  font-size: 0.85rem;
  font-weight: 700;
  color: rgba(var(--v-theme-text-neutral-strong), 0.8);
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.impact-score-panel__footer-right {
  font-size: 0.8rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

/* Separators */
.impact-score-panel__separator {
  width: 1px;
  height: 16px;
  background: rgba(var(--v-theme-border-primary-strong), 0.2);
  margin: 0 12px;
}
</style>

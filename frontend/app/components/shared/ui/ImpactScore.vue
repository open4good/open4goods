<template>
  <v-tooltip :text="tooltipLabel" location="top">
    <template #activator="{ props: activatorProps }">
      <!-- Stars Mode -->
      <div
        v-if="mode === 'stars' && shouldDisplayStars"
        class="impact-score"
        :class="[
          `impact-score--${size}`,
          { 'impact-score--with-value': showValue },
        ]"
        v-bind="activatorProps"
        :aria-label="tooltipLabel"
        tabindex="0"
        role="img"
      >
        <v-rating
          class="impact-score__rating"
          :model-value="normalizedScore"
          :length="length"
          :size="ratingSize"
          :color="ratingColor"
          :bg-color="ratingBackgroundColor"
          :density="ratingDensity"
          half-increments
          readonly
          aria-hidden="true"
        />
        <span v-if="showValue" class="impact-score__value">{{
          formattedScore
        }}</span>
      </div>

      <!-- SVG Mode (HTML/CSS Panel) -->
      <div
        v-else-if="mode === 'svg'"
        class="impact-score-panel"
        :class="[
          `impact-score-panel--${svgSize}`,
          `impact-score-panel--${accentStep}`,
        ]"
        v-bind="activatorProps"
        role="img"
        :aria-label="svgAriaLabel"
      >
        <div class="impact-score-panel__top">
          <div class="impact-score-panel__score">
            <span class="impact-score-panel__score-value">{{
              svgDisplayValue
            }}</span>
            <span class="impact-score-panel__score-out">/ 20</span>
          </div>
          <div v-if="showScale" class="impact-score-panel__meta">
            <div class="impact-score-panel__row">
              <span class="impact-score-panel__label">Min</span>
              <span class="impact-score-panel__value">{{
                n(svgRangeMin, {
                  maximumFractionDigits: 1,
                  minimumFractionDigits: 0,
                })
              }}</span>
            </div>
            <div class="impact-score-panel__row">
              <span class="impact-score-panel__label">Max</span>
              <span class="impact-score-panel__value">{{
                n(svgRangeMax, {
                  maximumFractionDigits: 1,
                  minimumFractionDigits: 0,
                })
              }}</span>
            </div>
            <v-btn
              class="impact-score-panel__cta"
              variant="text"
              density="compact"
              size="small"
              :to="'./impactscore'"
            >
              Méthodologie
            </v-btn>
          </div>
        </div>
        <div class="impact-score-panel__bar" aria-hidden="true">
          <div class="impact-score-panel__track">
            <div
              class="impact-score-panel__fill"
              :style="{ width: `${Math.round(svgT * 100)}%` }"
            />
          </div>
        </div>
      </div>

      <!-- Combined Mode (default) -->
      <div
        v-else-if="mode === 'combined'"
        class="impact-score-combined"
        :class="[
          `impact-score-combined--${size}`,
          `impact-score-combined--${layout}`,
        ]"
        v-bind="activatorProps"
        :aria-label="tooltipLabel"
        role="img"
      >
        <div
          v-if="shouldDisplayScore"
          class="impact-score-badge"
          :class="[
            `impact-score-badge--${size}`,
            { 'impact-score-badge--flat': flat },
          ]"
        >
          <span class="impact-score-badge__value">
            {{ formattedBadgeScore }}
          </span>
        </div>
      </div>

      <!-- Badge Mode -->
      <div
        v-else
        class="impact-score-badge"
        :class="[
          `impact-score-badge--${size}`,
          `impact-score-badge--${badgeLayout}`,
          `impact-score-badge--${badgeVariant}`,
          { 'impact-score-badge--flat': flat },
        ]"
        v-bind="activatorProps"
        :aria-label="tooltipLabel"
        role="img"
        tabindex="0"
      >
        <div v-if="badgeLayout === 'stacked'" class="impact-score-badge__stack">
          <span class="impact-score-badge__value-main">
            {{ formattedBadgeValue }}
          </span>
          <span class="impact-score-badge__value-secondary">
            {{ outOf20Label }}
          </span>
        </div>
        <span v-else class="impact-score-badge__value">
          {{ formattedBadgeScore }}
        </span>
      </div>
    </template>
  </v-tooltip>
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
    default: 5,
  },
  size: {
    type: String as PropType<
      'small' | 'medium' | 'large' | 'xlarge' | 'xxlarge'
    >,
    default: 'medium',
  },
  svgSize: {
    type: String as PropType<'sm' | 'md' | 'lg'>,
    default: 'md',
  },
  mode: {
    type: String as PropType<'badge' | 'stars' | 'combined' | 'svg'>,
    default: 'combined',
  },
  layout: {
    type: String as PropType<'horizontal' | 'vertical'>,
    default: 'horizontal',
  },
  color: {
    type: String,
    default: 'impact-score-active',
  },
  inactiveColor: {
    type: String,
    default: 'impact-score-inactive',
  },
  showValue: {
    type: Boolean,
    default: false,
  },
  showScore: {
    type: Boolean,
    default: true,
  },
  showStars: {
    type: Boolean,
    default: true,
  },
  showScale: {
    type: Boolean,
    default: true,
  },
  flat: {
    type: Boolean,
    default: false,
  },
  badgeLayout: {
    type: String as PropType<'inline' | 'stacked'>,
    default: 'inline',
  },
  badgeVariant: {
    type: String as PropType<'default' | 'corner'>,
    default: 'default',
  },
})

const { t, n } = useI18n()

const length = computed(() =>
  Math.max(1, Math.round(Number.isFinite(props.max) ? props.max : 5))
)

const normalizedScore = computed(() => {
  const safeScore = Number.isFinite(props.score) ? props.score : 0
  return Math.min(Math.max(safeScore, 0), length.value)
})

const formattedScore = computed(
  () =>
    `${n(normalizedScore.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 })} / ${length.value}`
)

// Badge logic (out of 20)
const scoreOutOf20 = computed(() => {
  if (length.value === 0) return 0
  return (normalizedScore.value / length.value) * 20
})

const formattedBadgeScore = computed(
  () =>
    `${n(scoreOutOf20.value, { maximumFractionDigits: 1, minimumFractionDigits: scoreOutOf20.value === 0 ? 0 : 1 })} / 20`
)

const formattedBadgeValue = computed(() =>
  n(scoreOutOf20.value, {
    maximumFractionDigits: 1,
    minimumFractionDigits: scoreOutOf20.value === 0 ? 0 : 1,
  })
)

const outOf20Label = computed(() => t('components.impactScore.outOf20'))

const sanitizeNumber = (value: number, fallback: number) =>
  Number.isFinite(value) ? value : fallback

const svgRangeMin = computed(() => sanitizeNumber(props.min, 0))
const svgRangeMax = computed(() => sanitizeNumber(props.max, 20))
// Display the raw score value (unclamped) while clamping only for the fill.
const svgDisplayScore = computed(() => sanitizeNumber(props.score, 0))

const svgT = computed(() => {
  const min = svgRangeMin.value
  const max = svgRangeMax.value

  if (max <= min) {
    return 0
  }

  const clamped = Math.min(Math.max(svgDisplayScore.value, min), max)
  return (clamped - min) / (max - min)
})

// Accent step based on svgT for color theming (low/mid/high)
const accentStep = computed(() => {
  if (svgT.value < 1 / 3) return 'low'
  if (svgT.value < 2 / 3) return 'mid'
  return 'high'
})

const svgDisplayValue = computed(() =>
  n(svgDisplayScore.value, {
    maximumFractionDigits: 1,
    minimumFractionDigits: 0,
  })
)

const svgAriaLabel = computed(() =>
  t('components.impactScore.svgAriaLabel', { score: svgDisplayValue.value })
)

const ratingSize = computed(() => {
  switch (props.size) {
    case 'small':
      return 22
    case 'large':
      return 40
    case 'xlarge':
      return 56
    case 'xxlarge':
      return 64
    default:
      return 28
  }
})

const ratingDensity = computed(() =>
  props.size === 'small' ? 'compact' : 'comfortable'
)

const ratingColor = computed(() => props.color)

const ratingBackgroundColor = computed(() => props.inactiveColor)

const shouldDisplayScore = computed(() => {
  if (props.mode === 'badge') return true
  if (props.mode === 'stars') return false
  return props.showScore
})

const shouldDisplayStars = computed(() => {
  if (props.mode === 'badge') return false
  if (props.mode === 'stars') return true
  return props.showStars
})

const tooltipLabel = computed(() => {
  if (props.mode === 'svg') {
    return svgAriaLabel.value
  }
  if (
    (props.mode === 'badge' || props.mode === 'combined') &&
    shouldDisplayScore.value
  ) {
    return t('components.impactScore.tooltipBadge', {
      value: n(scoreOutOf20.value, {
        maximumFractionDigits: 1,
        minimumFractionDigits: scoreOutOf20.value === 0 ? 0 : 1,
      }),
    })
  }
  if (shouldDisplayStars.value) {
    return t('components.impactScore.tooltip', {
      value: n(normalizedScore.value, {
        maximumFractionDigits: 1,
        minimumFractionDigits: normalizedScore.value === 0 ? 0 : 1,
      }),
      max: length.value,
    })
  }

  return t('components.impactScore.tooltipBadge', {
    value: n(scoreOutOf20.value, {
      maximumFractionDigits: 1,
      minimumFractionDigits: scoreOutOf20.value === 0 ? 0 : 1,
    }),
  })
})

const layout = computed(() => props.layout)
const size = computed(() => props.size)
const svgSize = computed(() => props.svgSize)
const showValue = computed(() => props.showValue)
const mode = computed(() => props.mode)
const showScale = computed(() => props.showScale)
const flat = computed(() => props.flat)
const badgeLayout = computed(() => props.badgeLayout)
const badgeVariant = computed(() => props.badgeVariant)
</script>

<style scoped>
/* Impact Score Panel (HTML/CSS replacement for SVG mode) */
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

/* Top layout (2 columns) */
.impact-score-panel__top {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

/* Score (left column) */
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

/* Meta (right column: Min/Max + CTA) */
.impact-score-panel__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}

.impact-score-panel__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
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
  align-self: flex-start;
  margin-top: 2px;
  padding-left: 0;
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

/* Responsive: stack only if very narrow */
@media (max-width: 360px) {
  .impact-score-panel__top {
    grid-template-columns: 1fr;
  }
  .impact-score-panel__meta {
    min-width: 0;
  }
}

/* Stars Style */
.impact-score {
  --impact-score-gap: 0.5rem;
  --impact-score-rating-gap: 0.375rem;
  --impact-score-value-font-size: 1rem;
  display: inline-flex;
  align-items: center;
  gap: var(--impact-score-gap);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score--small {
  --impact-score-gap: 0.375rem;
  --impact-score-rating-gap: 0.25rem;
  --impact-score-value-font-size: 0.85rem;
}

.impact-score--large {
  --impact-score-gap: 0.625rem;
  --impact-score-rating-gap: 0.5rem;
  --impact-score-value-font-size: 1.2rem;
}

.impact-score--xlarge {
  --impact-score-gap: 1rem;
  --impact-score-rating-gap: 0.8rem;
  --impact-score-value-font-size: 1.5rem;
}

.impact-score--xxlarge {
  --impact-score-gap: 1.1rem;
  --impact-score-rating-gap: 0.9rem;
  --impact-score-value-font-size: 1.75rem;
}

.impact-score__rating :deep(.v-rating__wrapper) {
  gap: var(--impact-score-rating-gap);
}

.impact-score__value {
  font-weight: 600;
  font-size: var(--impact-score-value-font-size);
  line-height: 1.1;
}

.impact-score--with-value .impact-score__rating {
  display: inline-flex;
}

/* Badge Style */
.impact-score-badge {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.55rem 0.95rem;
  border-radius: 18px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.28);
  background: rgba(var(--v-theme-surface-glass-strong), 0.95);
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 700;
  letter-spacing: 0.02em;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.12);
  overflow: hidden;
  isolation: isolate;
}

.impact-score-badge::before {
  content: '';
  position: absolute;
  top: -45%;
  right: -35%;
  width: 90%;
  height: 120%;

  transform: rotate(-10deg);
  border-radius: 999px;
  opacity: 0.8;
  z-index: 0;
}

.impact-score-badge--corner {
  background: rgba(var(--v-theme-surface-glass-strong), 0.92);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.5);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.impact-score-badge--corner::before {
  content: none;
}

.impact-score-badge--stacked {
  padding: 0.5rem 0.6rem;
}

.impact-score-badge__stack {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.1rem;
  line-height: 1;
}

.impact-score-badge--corner.impact-score-badge--stacked
  .impact-score-badge__stack {
  transform: rotate(-12deg);
}

.impact-score-badge--flat {
  border-color: transparent;
  background: rgba(var(--v-theme-surface-glass), 0.6);
  box-shadow: none;
}

.impact-score-badge__value {
  position: relative;
  z-index: 1;
  font-size: 1.05rem;
}

.impact-score-badge--small .impact-score-badge__value {
  font-size: 0.95rem;
}

.impact-score-badge__value-main {
  font-size: 1.05rem;
  font-weight: 700;
}

.impact-score-badge__value-secondary {
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.impact-score-badge--small .impact-score-badge__value-main {
  font-size: 0.95rem;
}

.impact-score-badge--small .impact-score-badge__value-secondary {
  font-size: 0.65rem;
}

.impact-score-badge--large .impact-score-badge__value {
  font-size: 1.3rem;
}

.impact-score-badge--xlarge .impact-score-badge__value {
  font-size: 1.55rem;
}

/* Large Stacked */
.impact-score-badge--large .impact-score-badge__value-main {
  font-size: 1.4rem;
}

.impact-score-badge--large .impact-score-badge__value-secondary {
  font-size: 0.8rem;
}

/* XLarge Stacked */
.impact-score-badge--xlarge .impact-score-badge__value-main {
  font-size: 1.8rem;
}

.impact-score-badge--xlarge .impact-score-badge__value-secondary {
  font-size: 0.9rem;
}

/* XXLarge Stacked */
.impact-score-badge--xxlarge .impact-score-badge__value-main {
  font-size: 2.4rem;
  line-height: 0.9;
}

.impact-score-badge--xxlarge .impact-score-badge__value-secondary {
  font-size: 1.1rem;
  font-weight: 700;
  opacity: 0.8;
}

/* General size overrides for non-stacked if needed, already present below */
.impact-score-badge--xxlarge .impact-score-badge__value {
  font-size: 1.85rem;
}

/* Combined Style */
.impact-score-combined {
  display: inline-flex;
  align-items: center;
  --impact-score-combined-gap: 0.75rem;
  --impact-score-stack-gap: 0.6rem;
  --impact-score-rating-gap: 0.375rem;
  --impact-score-value-font-size: 1rem;
  gap: var(--impact-score-combined-gap);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-combined--small {
  --impact-score-combined-gap: 0.5rem;
  --impact-score-stack-gap: 0.45rem;
  --impact-score-rating-gap: 0.25rem;
  --impact-score-value-font-size: 0.85rem;
}

.impact-score-combined--large {
  --impact-score-combined-gap: 1rem;
  --impact-score-stack-gap: 0.85rem;
  --impact-score-rating-gap: 0.5rem;
  --impact-score-value-font-size: 1.2rem;
}

.impact-score-combined--xlarge {
  --impact-score-combined-gap: 1.25rem;
  --impact-score-stack-gap: 1rem;
  --impact-score-rating-gap: 0.6rem;
  --impact-score-value-font-size: 1.4rem;
}

.impact-score-combined--xxlarge {
  --impact-score-combined-gap: 1.4rem;
  --impact-score-stack-gap: 1.2rem;
  --impact-score-rating-gap: 0.7rem;
  --impact-score-value-font-size: 1.6rem;
}

.impact-score-combined--vertical {
  flex-direction: column;
  align-items: flex-start;
  gap: var(--impact-score-stack-gap);
}

.impact-score-combined__rating {
  display: inline-flex;
  align-items: center;
  gap: var(--impact-score-rating-gap);
  color: inherit;
}

.impact-score-combined__rating .impact-score__value {
  font-size: var(--impact-score-value-font-size);
}
</style>

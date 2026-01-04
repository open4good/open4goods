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

        <div v-if="shouldDisplayStars" class="impact-score-combined__rating">
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
        <div
          v-if="badgeLayout === 'stacked'"
          class="impact-score-badge__stack"
        >
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
  mode: {
    type: String as PropType<'badge' | 'stars' | 'combined'>,
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
    `${n(scoreOutOf20.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 })} / 20`
)

const formattedBadgeValue = computed(() =>
  n(scoreOutOf20.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 })
)

const outOf20Label = computed(() => t('components.impactScore.outOf20'))

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
  if (
    (props.mode === 'badge' || props.mode === 'combined') &&
    shouldDisplayScore.value
  ) {
    return t('components.impactScore.tooltipBadge', {
      value: n(scoreOutOf20.value, {
        maximumFractionDigits: 1,
        minimumFractionDigits: 0,
      }),
    })
  }
  if (shouldDisplayStars.value) {
    return t('components.impactScore.tooltip', {
      value: n(normalizedScore.value, {
        maximumFractionDigits: 1,
        minimumFractionDigits: 0,
      }),
      max: length.value,
    })
  }

  return t('components.impactScore.tooltipBadge', {
    value: n(scoreOutOf20.value, {
      maximumFractionDigits: 1,
      minimumFractionDigits: 0,
    }),
  })
})

const layout = computed(() => props.layout)
const size = computed(() => props.size)
const showValue = computed(() => props.showValue)
const mode = computed(() => props.mode)
const flat = computed(() => props.flat)
const badgeLayout = computed(() => props.badgeLayout)
const badgeVariant = computed(() => props.badgeVariant)
</script>

<style scoped>
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
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-accent-primary-highlight), 0.45),
    rgba(var(--v-theme-accent-supporting), 0.2)
  );
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

.impact-score-badge--corner.impact-score-badge--stacked .impact-score-badge__stack {
  transform: rotate(-12deg);
}

.impact-score-badge--flat {
  border-color: transparent;
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

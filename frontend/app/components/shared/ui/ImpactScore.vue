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
        <v-chip
          v-if="shouldDisplayScore"
          class="impact-score-badge"
          :class="[`impact-score-badge--${size}`]"
          rounded="pill"
          variant="flat"
          :color="badgeColor"
        >
          <span class="impact-score-badge__value">
            {{ formattedBadgeScore }}
          </span>
        </v-chip>

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
      <v-chip
        v-else
        class="impact-score-badge"
        :class="[`impact-score-badge--${size}`]"
        v-bind="activatorProps"
        :aria-label="tooltipLabel"
        role="img"
        rounded="pill"
        variant="flat"
        :color="badgeColor"
      >
        <span class="impact-score-badge__value">
          {{ formattedBadgeScore }}
        </span>
      </v-chip>
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
    type: String as PropType<'small' | 'medium' | 'large' | 'xlarge'>,
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

const badgeColor = computed(() => {
  // Use a default color if none provided, or map specific ranges if needed.
  return undefined // heavily rely on CSS class
})

const ratingSize = computed(() => {
  switch (props.size) {
    case 'small':
      return 18
    case 'large':
      return 32
    case 'xlarge':
      return 48
    default:
      return 24
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
</script>

<style scoped>
/* Stars Style */
.impact-score {
  --impact-score-gap: 0.5rem;
  --impact-score-rating-gap: 0.375rem;
  --impact-score-value-font-size: 0.95rem;
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
  --impact-score-value-font-size: 1.05rem;
}

.impact-score--xlarge {
  --impact-score-gap: 1rem;
  --impact-score-rating-gap: 0.8rem;
  --impact-score-value-font-size: 1.6rem;
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
  background-color: rgba(var(--v-theme-surface-primary-080), 0.85) !important;
  color: rgb(var(--v-theme-text-neutral-strong)) !important;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.impact-score-badge__value {
  font-size: 0.95rem;
}

.impact-score-badge--small .impact-score-badge__value {
  font-size: 0.85rem;
}

.impact-score-badge--large .impact-score-badge__value {
  font-size: 1.05rem;
}

.impact-score-badge--xlarge .impact-score-badge__value {
  font-size: 1.2rem;
}

/* Combined Style */
.impact-score-combined {
  display: inline-flex;
  align-items: center;
  --impact-score-combined-gap: 0.75rem;
  --impact-score-stack-gap: 0.6rem;
  --impact-score-rating-gap: 0.375rem;
  --impact-score-value-font-size: 0.95rem;
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
  --impact-score-value-font-size: 1.05rem;
}

.impact-score-combined--xlarge {
  --impact-score-combined-gap: 1.25rem;
  --impact-score-stack-gap: 1rem;
  --impact-score-rating-gap: 0.6rem;
  --impact-score-value-font-size: 1.2rem;
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

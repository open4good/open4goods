<template>
  <v-tooltip :text="tooltipLabel" location="top">
    <template #activator="{ props: activatorProps }">
      <div
        class="impact-score"
        :class="[`impact-score--${size}`, { 'impact-score--with-value': showValue }]"
        v-bind="activatorProps"
        :aria-label="tooltipLabel"
        :style="rootStyle"
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
        <span v-if="showValue" class="impact-score__value">{{ formattedScore }}</span>
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
    type: String as PropType<'small' | 'medium' | 'large' | 'xlarge'>,
    default: 'medium',
  },
  starScale: {
    type: Number,
    default: 1,
    validator: (value: number) => Number.isFinite(value) && value > 0,
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
})

const { t, n } = useI18n()

const length = computed(() => Math.max(1, Math.round(Number.isFinite(props.max) ? props.max : 5)))

const normalizedScore = computed(() => {
  const safeScore = Number.isFinite(props.score) ? props.score : 0
  return Math.min(Math.max(safeScore, 0), length.value)
})

const formattedScore = computed(() =>
  `${n(normalizedScore.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 })} / ${length.value}`,
)

const ratingDensity = computed(() => (props.size === 'small' ? 'compact' : 'comfortable'))

const ratingColor = computed(() => props.color)

const ratingBackgroundColor = computed(() => props.inactiveColor)

const tooltipLabel = computed(() =>
  t('components.impactScore.tooltip', {
    value: n(normalizedScore.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 }),
    max: length.value,
  }),
)

const size = computed(() => props.size)
const showValue = computed(() => props.showValue)
const normalizedStarScale = computed(() => {
  const raw = Number(props.starScale)
  if (!Number.isFinite(raw) || raw <= 0) {
    return 1
  }

  return raw
})

const ratingSize = computed(() => {
  const scale = normalizedStarScale.value
  switch (props.size) {
    case 'small':
      return 18 * scale
    case 'large':
      return 32 * scale
    case 'xlarge':
      return 40 * scale
    default:
      return 24 * scale
  }
})

const rootStyle = computed(() => ({
  '--impact-score-scale': String(normalizedStarScale.value),
}))
</script>

<style scoped>
.impact-score {
  --impact-score-scale: 1;
  --impact-score-gap-base: 0.5rem;
  --impact-score-rating-gap-base: 0.375rem;
  --impact-score-value-font-size-base: 0.95rem;
  display: inline-flex;
  align-items: center;
  gap: calc(var(--impact-score-gap-base) * var(--impact-score-scale));
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score--small {
  --impact-score-gap-base: 0.375rem;
  --impact-score-rating-gap-base: 0.25rem;
  --impact-score-value-font-size-base: 0.85rem;
}

.impact-score--large {
  --impact-score-gap-base: 0.625rem;
  --impact-score-rating-gap-base: 0.5rem;
  --impact-score-value-font-size-base: 1.05rem;
}

.impact-score--xlarge {
  --impact-score-gap-base: 0.75rem;
  --impact-score-rating-gap-base: 0.6rem;
  --impact-score-value-font-size-base: 1.2rem;
}

.impact-score__rating :deep(.v-rating__wrapper) {
  gap: calc(var(--impact-score-rating-gap-base) * var(--impact-score-scale));
}

.impact-score__value {
  font-weight: 600;
  font-size: calc(var(--impact-score-value-font-size-base) * var(--impact-score-scale));
  line-height: 1.1;
}

.impact-score--with-value .impact-score__rating {
  display: inline-flex;
}
</style>

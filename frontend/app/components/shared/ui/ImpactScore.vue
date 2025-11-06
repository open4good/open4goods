<template>
  <v-tooltip :text="tooltipLabel" location="top">
    <template #activator="{ props: activatorProps }">
      <div
        class="impact-score"
        :class="`impact-score--${size}`"
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
          color="accent-supporting"
          :bg-color="inactiveColor"
          density="comfortable"
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
    type: String as PropType<'small' | 'medium' | 'large'>,
    default: 'medium',
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

const ratingSize = computed(() => {
  switch (props.size) {
    case 'small':
      return 18
    case 'large':
      return 32
    default:
      return 24
  }
})

const inactiveColor = computed(() => 'rgba(var(--v-theme-text-neutral-soft), 0.35)')

const tooltipLabel = computed(() =>
  t('components.impactScore.tooltip', {
    value: n(normalizedScore.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 }),
    max: length.value,
  }),
)

const size = computed(() => props.size)
const showValue = computed(() => props.showValue)
</script>

<style scoped>
.impact-score {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score__value {
  font-weight: 600;
  font-size: 0.95rem;
}

.impact-score--small .impact-score__value {
  font-size: 0.85rem;
}

.impact-score--large .impact-score__value {
  font-size: 1.05rem;
}

.impact-score__rating :deep(.v-rating__wrapper) {
  gap: 0.375rem;
}

.impact-score--small .impact-score__rating :deep(.v-rating__wrapper) {
  gap: 0.25rem;
}

.impact-score--large .impact-score__rating :deep(.v-rating__wrapper) {
  gap: 0.5rem;
}

.impact-score__rating :deep(.v-icon) {
  color: rgba(var(--v-theme-text-neutral-soft), 0.35);
}

.impact-score__rating :deep(.v-rating__item--active .v-icon),
.impact-score__rating :deep(.v-rating__item--hover .v-icon) {
  color: rgb(var(--v-theme-accent-supporting));
}
</style>

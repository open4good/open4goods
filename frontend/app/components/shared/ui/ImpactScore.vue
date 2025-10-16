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
        <div class="impact-score__stars" :style="starStyle">
          <div class="impact-score__stars-track" aria-hidden="true">
            <v-icon
              v-for="index in length"
              :key="`track-${index}`"
              icon="mdi-star"
            />
          </div>
          <div class="impact-score__stars-fill" :style="{ width: fillWidth }" aria-hidden="true">
            <v-icon
              v-for="index in length"
              :key="`fill-${index}`"
              icon="mdi-star"
            />
          </div>
        </div>
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

const starSize = computed(() => {
  switch (props.size) {
    case 'small':
      return 18
    case 'large':
      return 32
    default:
      return 24
  }
})

const starGap = computed(() => {
  switch (props.size) {
    case 'small':
      return 4
    case 'large':
      return 8
    default:
      return 6
  }
})

const fillWidth = computed(() => `${(normalizedScore.value / length.value) * 100}%`)

const starStyle = computed(() => ({
  '--impact-score-star-size': `${starSize.value}px`,
  '--impact-score-star-gap': `${starGap.value}px`,
}))

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

.impact-score__stars {
  position: relative;
  display: inline-flex;
}

.impact-score__stars-track,
.impact-score__stars-fill {
  display: inline-flex;
  gap: var(--impact-score-star-gap);
}

.impact-score__stars-track {
  color: rgba(var(--v-theme-text-neutral-soft), 0.35);
}

.impact-score__stars-fill {
  position: absolute;
  inset: 0 auto 0 0;
  display: inline-flex;
  overflow: hidden;
  color: rgb(var(--v-theme-accent-supporting));
  gap: var(--impact-score-star-gap);
}

.impact-score__stars-track :deep(.v-icon),
.impact-score__stars-fill :deep(.v-icon) {
  font-size: var(--impact-score-star-size);
  line-height: 1;
}
</style>

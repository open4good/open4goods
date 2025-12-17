<template>
  <div
    class="subscore-rating"
    :class="`subscore-rating--${size}`"
    role="img"
    :aria-label="ariaLabel"
  >
    <div class="subscore-rating__stars" :style="starStyle">
      <div class="subscore-rating__stars-track" aria-hidden="true">
        <v-icon
          v-for="index in length"
          :key="`track-${index}`"
          icon="mdi-star"
        />
      </div>
      <div
        class="subscore-rating__stars-fill"
        :style="{ width: fillWidth }"
        aria-hidden="true"
      >
        <v-icon
          v-for="index in length"
          :key="`fill-${index}`"
          icon="mdi-star"
        />
      </div>
    </div>
    <span v-if="showValue" class="subscore-rating__value">{{
      formattedScore
    }}</span>
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
  max: {
    type: Number,
    default: 5,
  },
  size: {
    type: String as PropType<'x-small' | 'small' | 'medium'>,
    default: 'small',
  },
  showValue: {
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

const fillWidth = computed(
  () => `${(normalizedScore.value / length.value) * 100}%`
)

const formattedScore = computed(
  () =>
    `${n(normalizedScore.value, { maximumFractionDigits: 1, minimumFractionDigits: 0 })} / ${length.value}`
)

const starSize = computed(() => {
  switch (props.size) {
    case 'x-small':
      return 14
    case 'medium':
      return 22
    default:
      return 18
  }
})

const starGap = computed(() => {
  switch (props.size) {
    case 'x-small':
      return 2
    case 'medium':
      return 6
    default:
      return 4
  }
})

const starStyle = computed(() => ({
  '--subscore-rating-star-size': `${starSize.value}px`,
  '--subscore-rating-star-gap': `${starGap.value}px`,
}))

const size = computed(() => props.size)
const showValue = computed(() => props.showValue)

const ariaLabel = computed(() =>
  t('components.impactScore.tooltip', {
    value: n(normalizedScore.value, {
      maximumFractionDigits: 1,
      minimumFractionDigits: 0,
    }),
    max: length.value,
  })
)
</script>

<style scoped>
.subscore-rating {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.subscore-rating__stars {
  position: relative;
  display: inline-flex;
}

.subscore-rating__stars-track,
.subscore-rating__stars-fill {
  display: inline-flex;
  gap: var(--subscore-rating-star-gap);
}

.subscore-rating__stars-track {
  color: rgba(var(--v-theme-text-neutral-soft), 0.35);
}

.subscore-rating__stars-fill {
  position: absolute;
  inset: 0 auto 0 0;
  display: inline-flex;
  overflow: hidden;
  color: rgb(var(--v-theme-accent-supporting));
  gap: var(--subscore-rating-star-gap);
}

.subscore-rating__stars-track :deep(.v-icon),
.subscore-rating__stars-fill :deep(.v-icon) {
  font-size: var(--subscore-rating-star-size);
  line-height: 1;
}

.subscore-rating__value {
  font-weight: 600;
  font-size: 0.85rem;
}

.subscore-rating--x-small .subscore-rating__value {
  font-size: 0.75rem;
}
</style>

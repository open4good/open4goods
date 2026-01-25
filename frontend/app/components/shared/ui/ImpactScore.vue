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

      <!-- SVG Mode -->
      <div
        v-else-if="mode === 'svg'"
        class="impact-score-svg"
        :class="`impact-score-svg--${svgSize}`"
        v-bind="activatorProps"
      >
        <svg
          class="scoreSvg"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 420 140"
          role="img"
          :width="svgDimensions.width"
          :height="svgDimensions.height"
          :aria-label="svgAriaLabel"
        >
          <defs>
            <clipPath :id="svgClipId">
              <path
                d="
                  M24,16
                  a16,16 0 0 1 16,-16
                  h296
                  a16,16 0 0 1 16,16
                  v18
                  l-22,0
                  a14,14 0 0 0 -14,14
                  v52
                  a16,16 0 0 1 -16,16
                  h-276
                  a16,16 0 0 1 -16,-16
                  z"
              />
            </clipPath>
          </defs>

          <g :clip-path="`url(#${svgClipId})`">
            <rect
              x="8"
              y="8"
              width="404"
              height="124"
              rx="22"
              fill="#FFFFFF"
              opacity="0.92"
            />

            <rect
              x="34"
              y="96"
              width="300"
              height="14"
              rx="7"
              :fill="svgInactiveColor"
            />
            <rect
              x="34"
              y="96"
              :width="svgBarWidth"
              height="14"
              rx="7"
              :fill="svgActiveColor"
            />

            <rect
              x="350"
              y="86"
              width="44"
              height="44"
              rx="14"
              :fill="svgActiveColor"
              opacity="0.18"
            />
            <path
              d="M364 106h16"
              :stroke="svgActiveColor"
              stroke-width="6"
              stroke-linecap="round"
            />

            <text
              x="34"
              y="64"
              font-size="54"
              font-weight="800"
              :fill="svgActiveColor"
            >
              {{ svgDisplayValue }}
              <tspan font-size="26" font-weight="700" fill="rgba(0,0,0,0.55)">
                /{{ n(svgRangeMax, { maximumFractionDigits: 1 }) }}
              </tspan>
            </text>

            <text
              v-if="showScale"
              x="34"
              y="86"
              font-size="16"
              font-weight="600"
              fill="rgba(0,0,0,0.55)"
            >
              {{ svgScaleLabel }}
            </text>
          </g>
        </svg>
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
import { computed, useId } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { vuetifyPalettes } from '~~/config/theme/palettes'

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

const svgClipId = useId()
const svgInactiveColor = vuetifyPalettes.light['impact-score-inactive']
const svgMaxColor = vuetifyPalettes.light.primary
const svgMinColor = vuetifyPalettes.light.red
const svgTrackWidth = 300

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

const svgBarWidth = computed(() => {
  const width = Math.round(svgTrackWidth * svgT.value)
  return Math.min(svgTrackWidth, Math.max(0, width))
})

const hexToRgb = (hex: string) => {
  const normalized = hex.replace('#', '').trim()
  const value = Number.parseInt(normalized, 16)
  const r = (value >> 16) & 255
  const g = (value >> 8) & 255
  const b = value & 255

  return { r, g, b }
}

const rgbToHsl = (r: number, g: number, b: number) => {
  const rNorm = r / 255
  const gNorm = g / 255
  const bNorm = b / 255
  const max = Math.max(rNorm, gNorm, bNorm)
  const min = Math.min(rNorm, gNorm, bNorm)
  const delta = max - min
  const lightness = (max + min) / 2

  if (delta === 0) {
    return { h: 0, s: 0, l: lightness }
  }

  const saturation = delta / (1 - Math.abs(2 * lightness - 1))
  let hue = 0

  switch (max) {
    case rNorm:
      hue = ((gNorm - bNorm) / delta) % 6
      break
    case gNorm:
      hue = (bNorm - rNorm) / delta + 2
      break
    default:
      hue = (rNorm - gNorm) / delta + 4
  }

  hue *= 60
  if (hue < 0) {
    hue += 360
  }

  return { h: hue, s: saturation, l: lightness }
}

const interpolateHsl = (from: string, to: string, ratio: number) => {
  const start = rgbToHsl(...Object.values(hexToRgb(from)))
  const end = rgbToHsl(...Object.values(hexToRgb(to)))
  let deltaHue = end.h - start.h

  if (Math.abs(deltaHue) > 180) {
    deltaHue -= Math.sign(deltaHue) * 360
  }

  const hue = (start.h + deltaHue * ratio + 360) % 360
  const saturation = start.s + (end.s - start.s) * ratio
  const lightness = start.l + (end.l - start.l) * ratio

  return `hsl(${Math.round(hue)}, ${Math.round(
    saturation * 100
  )}%, ${Math.round(lightness * 100)}%)`
}

const svgActiveColor = computed(() =>
  interpolateHsl(svgMinColor, svgMaxColor, svgT.value)
)

const svgDisplayValue = computed(() =>
  n(svgDisplayScore.value, {
    maximumFractionDigits: 1,
    minimumFractionDigits: 0,
  })
)

const svgScaleLabel = computed(() =>
  t('components.impactScore.scale', {
    min: n(svgRangeMin.value, { maximumFractionDigits: 1 }),
    max: n(svgRangeMax.value, { maximumFractionDigits: 1 }),
  })
)

const svgAriaLabel = computed(() =>
  t('components.impactScore.svgAriaLabel', { score: svgDisplayValue.value })
)

const svgDimensions = computed(() => {
  switch (props.svgSize) {
    case 'sm':
      return { width: 280, height: 94 }
    case 'lg':
      return { width: 420, height: 140 }
    default:
      return { width: 340, height: 113 }
  }
})

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
.impact-score-svg {
  display: inline-flex;
  align-items: center;
}

.impact-score-svg .scoreSvg {
  max-width: 100%;
  height: auto;
  font-family: 'Hanken Grotesk', 'Inter', 'Helvetica Neue', Arial, sans-serif;
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

<template>
  <div class="text-h5 font-weight-bold lh-1 text-high-emphasis">
    <span class="d-inline-block" :class="variantClass">🤘</span>
  </div>
  <div
    class="nudge-tool-animated-icon d-flex align-center justify-center fill-height"
    :style="iconStyle"
    tabindex="0"
    role="img"
    :aria-label="$t('nudge-tool.wizard.welcome')"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @focus="isHovered = true"
    @blur="isHovered = false"
  ></div>
</template>

<script setup lang="ts">
import { usePreferredReducedMotion } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useAccessibilityStore } from '~/stores/useAccessibilityStore'

type AnimatedIconVariant =
  | 'pulse'
  | 'fadeIn'
  | 'bounce'
  | 'float'
  | 'wiggle'
  | 'glow'

const props = withDefaults(
  defineProps<{
    variant?: AnimatedIconVariant
    maxScale?: number
    frequencyRange?: [number, number]
    randomizeOnMount?: boolean
    seed?: number
  }>(),
  {
    variant: 'pulse',
    maxScale: 1.25,
    frequencyRange: () => [3000, 5000],
    randomizeOnMount: true,
    seed: undefined,
  }
)

const IDLE_MIN_FALLBACK = 3000
const IDLE_MAX_FALLBACK = 5000
const PULSE_ITERATIONS_MIN = 1
const PULSE_ITERATIONS_MAX = 3
const ZOOM_DURATION_MS = 300
const ZOOM_SCALE_MIN = 1.05
const EASE_IN_OUT = 'cubic-bezier(0.42, 0, 0.58, 1)'

const interpolate = (start: number, end: number, ratio: number) =>
  start + (end - start) * ratio

const createSeededRng = (seed: number) => {
  let state = Math.abs(Math.floor(seed)) % 2147483647
  if (state === 0) {
    state = 1
  }

  return () => {
    state = (state * 16807) % 2147483647
    return state / 2147483647
  }
}

const createRng = (seed?: number) =>
  seed === undefined ? Math.random : createSeededRng(seed)

const normalizeRange = (range: [number, number]) => {
  const [start, end] = range
  const min = Math.max(0, Math.min(start ?? 0, end ?? 0))
  const max = Math.max(min, Math.max(start ?? 0, end ?? 0))

  return [min, max] as [number, number]
}

const isHovered = ref(false)
const reducedMotion = usePreferredReducedMotion()
const accessibilityStore = useAccessibilityStore()
const { prefersReducedMotionOverride } = storeToRefs(accessibilityStore)
const shouldReduceMotion = computed(
  () => prefersReducedMotionOverride.value || reducedMotion.value === 'reduce'
)
const scale = ref(1)
const easing = ref(EASE_IN_OUT)
const animationDuration = ref(0)
const animationDelay = ref(0)
const isHydrated = ref(false)

let rng = createRng(props.seed)

const isPulseVariant = computed(() => props.variant === 'pulse')
const variantClass = computed(
  () => `nudge-tool-animated-icon--${props.variant}`
)
const maxScale = computed(() => Math.max(props.maxScale, ZOOM_SCALE_MIN + 0.01))
const normalizedFrequencyRange = computed(() =>
  normalizeRange(props.frequencyRange ?? [IDLE_MIN_FALLBACK, IDLE_MAX_FALLBACK])
)

const easingForScale = (scaleValue: number) => {
  const range = maxScale.value - ZOOM_SCALE_MIN
  const ratio = range > 0 ? (scaleValue - ZOOM_SCALE_MIN) / range : 0

  // Starts at the current easing (ease-in-out) and gradually eases out more
  // aggressively as the scale increases for a softer landing.
  const x1 = interpolate(0.42, 0.25, ratio)
  const y1 = interpolate(0, 1, ratio)
  const x2 = interpolate(0.58, 0.5, ratio)
  const y2 = interpolate(1, 1, ratio)

  return `cubic-bezier(${x1.toFixed(3)}, ${y1.toFixed(3)}, ${x2.toFixed(3)}, ${y2.toFixed(3)})`
}

const randomBetween = (min: number, max: number) => min + rng() * (max - min)

const randomInt = (min: number, max: number) =>
  Math.floor(randomBetween(min, max + 1))

const randomScale = () =>
  Number(
    (ZOOM_SCALE_MIN + rng() * (maxScale.value - ZOOM_SCALE_MIN)).toFixed(3)
  )

const resolveMidpoint = (min: number, max: number) =>
  Number(((min + max) / 2).toFixed(0))

const updateAnimationTiming = () => {
  const [min, max] = normalizedFrequencyRange.value
  if (props.randomizeOnMount && isHydrated.value) {
    animationDuration.value = Math.round(randomBetween(min, max))
    animationDelay.value = Math.round(randomBetween(0, min))
    return
  }

  animationDuration.value = resolveMidpoint(min, max)
  animationDelay.value = 0
}

const resetPulseState = () => {
  scale.value = 1
  easing.value = EASE_IN_OUT
}

updateAnimationTiming()

// -- Styles --
const iconStyle = computed(() => {
  const style: Record<string, string> = {
    cursor: 'default',
    maxWidth: '100%',
    maxHeight: '100%',
    '--nudge-icon-animation-duration': `${animationDuration.value}ms`,
    '--nudge-icon-animation-delay': `${animationDelay.value}ms`,
  }

  if (isPulseVariant.value) {
    style.transform = `scale(${scale.value})`
    style.transition = `transform ${ZOOM_DURATION_MS}ms ${easing.value}`
  }

  if (shouldReduceMotion.value) {
    style.animation = 'none'
  }

  return style
})

// -- Animation Logic --
let idleTimer: ReturnType<typeof setTimeout> | null = null
let pulseTimer: ReturnType<typeof setTimeout> | null = null

const clearTimers = () => {
  if (idleTimer) clearTimeout(idleTimer)
  if (pulseTimer) clearTimeout(pulseTimer)
  idleTimer = null
  pulseTimer = null
}

const scheduleIdle = () => {
  clearTimers()
  if (shouldReduceMotion.value) {
    resetPulseState()
    return
  }

  const [min, max] = normalizedFrequencyRange.value
  const delay = props.randomizeOnMount
    ? randomInt(min, max)
    : resolveMidpoint(min, max)
  idleTimer = setTimeout(() => {
    startPulseSequence()
  }, delay)
}

const startPulseSequence = () => {
  if (shouldReduceMotion.value) {
    resetPulseState()
    return
  }

  const iterations = props.randomizeOnMount
    ? randomInt(PULSE_ITERATIONS_MIN, PULSE_ITERATIONS_MAX)
    : resolveMidpoint(PULSE_ITERATIONS_MIN, PULSE_ITERATIONS_MAX)
  runPulse(iterations)
}

const runPulse = (remaining: number) => {
  // Guard clauses
  if (isHovered.value || shouldReduceMotion.value) {
    resetPulseState()
    return
  }

  if (remaining <= 0) {
    scheduleIdle()
    return
  }

  // Zoom In
  const nextScale = props.randomizeOnMount
    ? randomScale()
    : Number(((ZOOM_SCALE_MIN + maxScale.value) / 2).toFixed(3))
  easing.value = easingForScale(nextScale)
  scale.value = nextScale

  // Schedule Zoom Out
  pulseTimer = setTimeout(() => {
    scale.value = 1

    // Schedule Next Iteration
    pulseTimer = setTimeout(() => {
      runPulse(remaining - 1)
    }, ZOOM_DURATION_MS)
  }, ZOOM_DURATION_MS)
}

// -- Lifecycle --
onMounted(() => {
  rng = createRng(props.seed)
  isHydrated.value = true
  updateAnimationTiming()
  if (isPulseVariant.value) {
    scheduleIdle()
  }
})

onBeforeUnmount(() => {
  clearTimers()
})

// -- Watchers --
watch(
  [isHovered, shouldReduceMotion, () => props.variant],
  ([hover, reduced]) => {
    if (!isPulseVariant.value) {
      clearTimers()
      resetPulseState()
      return
    }

    if (hover || reduced) {
      clearTimers()
      resetPulseState()
    } else {
      scheduleIdle()
    }
  }
)

watch(
  [
    () => props.frequencyRange,
    () => props.randomizeOnMount,
    () => props.seed,
    () => props.maxScale,
    () => props.variant,
  ],
  () => {
    rng = createRng(props.seed)
    updateAnimationTiming()
    if (isPulseVariant.value && !isHovered.value) {
      scheduleIdle()
      return
    }

    clearTimers()
    resetPulseState()
  },
  { deep: true }
)
</script>

<style scoped>
.nudge-tool-animated-icon {
  outline: none; /* remove focus ring if desired, or style it */
}

.nudge-tool-animated-icon--fadeIn,
.nudge-tool-animated-icon--bounce,
.nudge-tool-animated-icon--float,
.nudge-tool-animated-icon--wiggle,
.nudge-tool-animated-icon--glow {
  animation-duration: var(--nudge-icon-animation-duration, 3600ms);
  animation-delay: var(--nudge-icon-animation-delay, 0ms);
  animation-iteration-count: infinite;
  animation-timing-function: ease-in-out;
  animation-fill-mode: both;
}

.nudge-tool-animated-icon--fadeIn {
  animation-name: nudgeFadeIn;
  animation-iteration-count: 1;
  animation-timing-function: cubic-bezier(0.2, 0.7, 0.2, 1);
}

.nudge-tool-animated-icon--bounce {
  animation-name: nudgeBounce;
}

.nudge-tool-animated-icon--float {
  animation-name: nudgeFloat;
}

.nudge-tool-animated-icon--wiggle {
  animation-name: nudgeWiggle;
}

.nudge-tool-animated-icon--glow {
  animation-name: nudgeGlow;
}

@keyframes nudgeFadeIn {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes nudgeBounce {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8%);
  }
}

@keyframes nudgeFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5%);
  }
}

@keyframes nudgeWiggle {
  0%,
  100% {
    transform: rotate(0deg);
  }
  20% {
    transform: rotate(-3deg);
  }
  40% {
    transform: rotate(3deg);
  }
  60% {
    transform: rotate(-2deg);
  }
  80% {
    transform: rotate(2deg);
  }
}

@keyframes nudgeGlow {
  0%,
  100% {
    text-shadow: 0 0 0 rgba(255, 255, 255, 0.2);
  }
  50% {
    text-shadow: 0 0 12px rgba(255, 255, 255, 0.6);
  }
}
</style>

<template>
  <div
    class="nudge-tool-welcome-icon d-flex align-center justify-center fill-height"
    :style="iconStyle"
    tabindex="0"
    role="img"
    :aria-label="$t('nudge-tool.wizard.welcome')"
    @mouseenter="isHovered = true"
    @mouseleave="isHovered = false"
    @focus="isHovered = true"
    @blur="isHovered = false"
  >
    <div class="text-h5 font-weight-bold text-white lh-1">
      {{ $t('nudge-tool.wizard.welcome') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { usePreferredReducedMotion } from '@vueuse/core'

// -- Props & Config --
const IDLE_MIN = 3000
const IDLE_MAX = 5000
const PULSE_ITERATIONS_MIN = 1
const PULSE_ITERATIONS_MAX = 3
const ZOOM_DURATION_MS = 300
const ZOOM_SCALE_MIN = 1.05
const ZOOM_SCALE_MAX = 1.25
const EASE_IN_OUT = 'cubic-bezier(0.42, 0, 0.58, 1)'

const interpolate = (start: number, end: number, ratio: number) =>
  start + (end - start) * ratio

const easingForScale = (scaleValue: number) => {
  const ratio =
    (scaleValue - ZOOM_SCALE_MIN) / (ZOOM_SCALE_MAX - ZOOM_SCALE_MIN)

  // Starts at the current easing (ease-in-out) and gradually eases out more
  // aggressively as the scale increases for a softer landing.
  const x1 = interpolate(0.42, 0.25, ratio)
  const y1 = interpolate(0, 1, ratio)
  const x2 = interpolate(0.58, 0.5, ratio)
  const y2 = interpolate(1, 1, ratio)

  return `cubic-bezier(${x1.toFixed(3)}, ${y1.toFixed(3)}, ${x2.toFixed(3)}, ${y2.toFixed(3)})`
}

const randomScale = () =>
  Number(
    (
      ZOOM_SCALE_MIN +
      Math.random() * (ZOOM_SCALE_MAX - ZOOM_SCALE_MIN)
    ).toFixed(3)
  )

const isHovered = ref(false)
const reducedMotion = usePreferredReducedMotion()
const scale = ref(1)
const easing = ref(EASE_IN_OUT)

// -- Styles --
const iconStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transition: `transform ${ZOOM_DURATION_MS}ms ${easing.value}`,
  cursor: 'default',
  // Ensure we don't overflow parent if it's tight
  maxWidth: '100%',
  maxHeight: '100%',
}))

// -- Animation Logic --
let idleTimer: ReturnType<typeof setTimeout> | null = null
let pulseTimer: ReturnType<typeof setTimeout> | null = null

const clearTimers = () => {
  if (idleTimer) clearTimeout(idleTimer)
  if (pulseTimer) clearTimeout(pulseTimer)
  idleTimer = null
  pulseTimer = null
}

const randomInt = (min: number, max: number) =>
  Math.floor(Math.random() * (max - min + 1)) + min

const scheduleIdle = () => {
  clearTimers()
  if (reducedMotion.value === 'reduce') {
    scale.value = 1
    easing.value = EASE_IN_OUT
    return
  }

  const delay = randomInt(IDLE_MIN, IDLE_MAX)
  idleTimer = setTimeout(() => {
    startPulseSequence()
  }, delay)
}

const startPulseSequence = () => {
  if (reducedMotion.value === 'reduce') {
    scale.value = 1
    easing.value = EASE_IN_OUT
    return
  }

  const iterations = randomInt(PULSE_ITERATIONS_MIN, PULSE_ITERATIONS_MAX)
  runPulse(iterations)
}

const runPulse = (remaining: number) => {
  // Guard clauses
  if (isHovered.value || reducedMotion.value === 'reduce') {
    scale.value = 1
    easing.value = EASE_IN_OUT
    return
  }

  if (remaining <= 0) {
    scheduleIdle()
    return
  }

  // Zoom In
  const nextScale = randomScale()
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
  scheduleIdle()
})

onBeforeUnmount(() => {
  clearTimers()
})

// -- Watchers --
watch([isHovered, reducedMotion], ([hover, reduced]) => {
  if (hover || reduced === 'reduce') {
    clearTimers()
    scale.value = 1
    easing.value = EASE_IN_OUT
  } else {
    scheduleIdle()
  }
})
</script>

<style scoped>
.nudge-tool-welcome-icon {
  outline: none; /* remove focus ring if desired, or style it */
}
</style>

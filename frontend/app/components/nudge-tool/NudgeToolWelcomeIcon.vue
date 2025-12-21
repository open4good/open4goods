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
const ZOOM_SCALE = 1.15

const isHovered = ref(false)
const reducedMotion = usePreferredReducedMotion()
const scale = ref(1)

// -- Styles --
const iconStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transition: `transform ${ZOOM_DURATION_MS}ms ease-in-out`,
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
  const delay = randomInt(IDLE_MIN, IDLE_MAX)
  idleTimer = setTimeout(() => {
    startPulseSequence()
  }, delay)
}

const startPulseSequence = () => {
  const iterations = randomInt(PULSE_ITERATIONS_MIN, PULSE_ITERATIONS_MAX)
  runPulse(iterations)
}

const runPulse = (remaining: number) => {
  // Guard clauses
  if (isHovered.value || reducedMotion.value === 'reduce') {
    scale.value = 1
    return
  }

  if (remaining <= 0) {
    scheduleIdle()
    return
  }

  // Zoom In
  scale.value = ZOOM_SCALE

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

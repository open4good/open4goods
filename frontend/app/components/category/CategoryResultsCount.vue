<template>
  <p
    class="category-page__results-count"
    aria-live="polite"
    data-test="category-results-count"
  >
    {{ label }}
  </p>
</template>

<script setup lang="ts">
import { usePreferredReducedMotion } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useAccessibilityStore } from '~/stores/useAccessibilityStore'

const props = defineProps<{
  count: number
}>()

const { translatePlural } = usePluralizedTranslation()
const { locale } = useI18n()

const animatedCount = ref(Math.max(0, Math.round(props.count)))
const reducedMotionPreference = usePreferredReducedMotion()
const accessibilityStore = useAccessibilityStore()
const { prefersReducedMotionOverride } = storeToRefs(accessibilityStore)
const isReducedMotionPreferred = computed(
  () =>
    prefersReducedMotionOverride.value || reducedMotionPreference.value === 'reduce'
)
const isMounted = ref(false)
let animationFrameId: number | null = null

const numberFormatter = computed(
  () => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 })
)

const formattedCount = computed(() =>
  numberFormatter.value.format(animatedCount.value)
)

const label = computed(() =>
  translatePlural('category.products.resultsCount', animatedCount.value, {
    count: formattedCount.value,
  })
)

const cancelAnimation = () => {
  if (
    animationFrameId !== null &&
    typeof cancelAnimationFrame !== 'undefined'
  ) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

const easeOutCubic = (progress: number) => 1 - (1 - progress) ** 3

const updateCountInstantly = (target: number) => {
  animatedCount.value = Math.max(0, Math.round(target))
}

const animateCount = (from: number, to: number, duration = 600) => {
  if (import.meta.server) {
    updateCountInstantly(to)
    return
  }

  if (!isMounted.value || isReducedMotionPreferred.value) {
    updateCountInstantly(to)
    return
  }

  const startValue = Math.max(0, Math.round(from))
  const targetValue = Math.max(0, Math.round(to))

  if (startValue === targetValue) {
    animatedCount.value = targetValue
    return
  }

  const startTime = performance.now()
  const delta = targetValue - startValue

  cancelAnimation()

  const step = (timestamp: number) => {
    const elapsed = timestamp - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = easeOutCubic(progress)

    animatedCount.value = Math.round(startValue + delta * eased)

    if (progress < 1) {
      animationFrameId = requestAnimationFrame(step)
      return
    }

    animationFrameId = null
  }

  animationFrameId = requestAnimationFrame(step)
}

watch(
  () => props.count,
  (next, previous = animatedCount.value) => {
    const previousValue = Math.max(0, Math.round(previous))
    const nextValue = Math.max(0, Math.round(next))

    if (!isMounted.value) {
      updateCountInstantly(nextValue)
      return
    }

    animateCount(previousValue, nextValue)
  },
  { immediate: true }
)

watch(
  isReducedMotionPreferred,
  current => {
    if (!current) {
      return
    }

    cancelAnimation()
    updateCountInstantly(props.count)
  },
  { immediate: false }
)

onMounted(() => {
  isMounted.value = true
  updateCountInstantly(props.count)
})

onBeforeUnmount(() => {
  cancelAnimation()
})
</script>

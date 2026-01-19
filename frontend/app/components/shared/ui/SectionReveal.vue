<script setup lang="ts">
import { computed, ref, watch, useAttrs } from 'vue'
import { usePreferredReducedMotion } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { useAccessibilityStore } from '~/stores/useAccessibilityStore'

defineOptions({ inheritAttrs: false })

type TransitionName =
  | 'fade'
  | 'slide-y'
  | 'slide-y-reverse'
  | 'slide-x'
  | 'slide-x-reverse'
  | 'scale'
  | 'none'

const props = withDefaults(
  defineProps<{
    transition?: TransitionName
    tag?: string
    once?: boolean
  }>(),
  {
    transition: 'fade',
    tag: 'div',
    once: true,
  }
)

const attrs = useAttrs()
const prefersReducedMotion = usePreferredReducedMotion()
const accessibilityStore = useAccessibilityStore()
const { prefersReducedMotionOverride } = storeToRefs(accessibilityStore)

const shouldReduceMotion = computed(
  () =>
    prefersReducedMotionOverride.value ||
    prefersReducedMotion.value === 'reduce'
)

const isVisible = ref(false)

const transitionComponent = computed(() => {
  switch (props.transition) {
    case 'slide-y':
      return 'v-slide-y-transition'
    case 'slide-y-reverse':
      return 'v-slide-y-reverse-transition'
    case 'slide-x':
      return 'v-slide-x-transition'
    case 'slide-x-reverse':
      return 'v-slide-x-reverse-transition'
    case 'scale':
      return 'v-scale-transition'
    case 'none':
      return null
    case 'fade':
    default:
      return 'v-fade-transition'
  }
})

const markVisible = () => {
  isVisible.value = true
}

watch(
  shouldReduceMotion,
  value => {
    if (value) {
      markVisible()
    }
  },
  { immediate: true }
)

const handleIntersect = (
  _entries: IntersectionObserverEntry[],
  observer: IntersectionObserver,
  isIntersecting: boolean
) => {
  if (isVisible.value) {
    return
  }

  if (shouldReduceMotion.value || isIntersecting) {
    markVisible()
    if (props.once) {
      observer.disconnect()
    }
  }
}
</script>

<template>
  <component
    :is="props.tag"
    v-bind="attrs"
    v-intersect="handleIntersect"
  >
    <component
      :is="transitionComponent || 'div'"
      :disabled="shouldReduceMotion || !transitionComponent"
    >
      <div v-show="isVisible">
        <slot :reveal="isVisible" />
      </div>
    </component>
  </component>
</template>

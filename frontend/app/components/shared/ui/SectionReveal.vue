<script setup lang="ts">
/**
 * Reveals its slot with a fade/slide once it scrolls into view.
 *
 * The content is always rendered and keeps its layout box: only opacity and
 * transform animate. Hiding it until it intersects (`v-show`) made every
 * section grow from 0 px while the visitor scrolled, pushing everything below
 * it down (CLS above 1 on the home page). Transforms do not count as layout
 * shifts, so this reveal is CLS-neutral.
 */
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

const isVisible = ref(props.transition === 'none')

const contentClasses = computed(() => [
  'section-reveal__content',
  `section-reveal__content--${props.transition}`,
  {
    'section-reveal__content--hidden': !isVisible.value,
    'section-reveal__content--static': shouldReduceMotion.value,
  },
])

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
  isIntersecting: boolean,
  _entries: IntersectionObserverEntry[],
  observer: IntersectionObserver
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
  <component :is="props.tag" v-bind="attrs" v-intersect="handleIntersect">
    <div :class="contentClasses">
      <slot :reveal="isVisible" />
    </div>
  </component>
</template>

<style scoped lang="sass">
.section-reveal__content
  transition: opacity 300ms ease, transform 300ms ease
  // Lets callers stagger reveals with a transition-delay on the root element.
  transition-delay: inherit

.section-reveal__content--hidden
  opacity: 0

  &.section-reveal__content--slide-y
    transform: translateY(-16px)

  &.section-reveal__content--slide-y-reverse
    transform: translateY(16px)

  &.section-reveal__content--slide-x
    transform: translateX(-16px)

  &.section-reveal__content--slide-x-reverse
    transform: translateX(16px)

  &.section-reveal__content--scale
    transform: scale(0.96)

.section-reveal__content--static
  transition: none
</style>

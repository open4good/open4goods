<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  useWindowScroll,
  useElementBounding,
  useWindowSize,
} from '@vueuse/core'

const props = defineProps<{
  src: string
  alt?: string
}>()

const container = ref<HTMLElement | null>(null)
const svgContent = ref<string>('')

// Use VueUse for scroll and bounding
const { y: scrollY } = useWindowScroll()
const { top, height } = useElementBounding(container)
const { height: windowHeight } = useWindowSize()

// Fetch SVG content
// We need to fetch it because we want to manipulate the internal groups
// and NuxtImg or simple <img> tags don't allow CSS transforms on internal SVG structure easily
// across the shadow DOM boundary or if it's treated as image resource.
// Inline SVG is best for this.
const fetchSvg = async () => {
  try {
    const response = await fetch(props.src)
    if (response.ok) {
      let text = await response.text()
      // Strip potentially harmful scripts just in case, though these are our trusted assets
      text = text.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '')
      svgContent.value = text
    }
  } catch (e) {
    console.error('Failed to load parallax SVG', e)
  }
}

// Parallax calculation
// We want to move elements based on the scroll position relative to the element
const updateParallax = () => {
  if (!container.value) return

  // Check if element is in viewport
  // If top < windowHeight and top + height > 0
  const isInViewport =
    top.value < windowHeight.value && top.value + height.value > 0

  if (!isInViewport) return

  // Calculate progress: 0 when element enters from bottom, 1 when it leaves to top
  // Actually we usually want the center of the viewport to be the "neutral" point

  // Let's keep it simple: bind translation to scroll Y
  // We need to access the DOM elements inside the SVG
  const svgEl = container.value.querySelector('svg')
  if (!svgEl) return

  // Factor: how much to move per scroll pixel.
  // We use different factors for different layers

  // Calculate a relative scroll value centered around when the item is in middle of screen
  const viewMiddle = windowHeight.value / 2
  const elementMiddle = top.value + height.value / 2

  // distance from center of screen
  const delta = elementMiddle - viewMiddle

  const back = svgEl.getElementById('parallax-back') as SVGGraphicsElement
  const mid = svgEl.getElementById('parallax-mid') as SVGGraphicsElement
  const front = svgEl.getElementById('parallax-front') as SVGGraphicsElement

  if (back) back.style.transform = `translateY(${delta * 0.05}px)`
  if (mid) mid.style.transform = `translateY(${delta * 0.1}px)`
  if (front) front.style.transform = `translateY(${delta * 0.2}px)`
}

// Watch scroll and update
// Note: useWindowScroll is reactive, so we can watch it or use requestAnimationFrame
// Using watchEffect or computed might be too heavy, direct DOM manipulation in watcher or RAF is better

watch(scrollY, () => {
  requestAnimationFrame(updateParallax)
})

onMounted(async () => {
  await fetchSvg()
  // Initial update
  requestAnimationFrame(updateParallax)
})
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -->
  <div ref="container" class="home-parallax-visual" v-html="svgContent"></div>
</template>

<style scoped>
.home-parallax-visual {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* Ensure SVG scales correctly */
:deep(svg) {
  width: 100%;
  height: auto;
  max-height: 100%;
  display: block;
}

/* Add smooth transitions if desired, but might conflict with scroll */
:deep(#parallax-back),
:deep(#parallax-mid),
:deep(#parallax-front) {
  will-change: transform;
  /* transition: transform 0.1s linear; */
}
</style>

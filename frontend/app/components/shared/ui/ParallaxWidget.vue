<script setup lang="ts">
import { computed, ref, type CSSProperties, onMounted, onUnmounted } from 'vue'
import {
  useElementBounding,
  usePreferredReducedMotion,
  useWindowScroll,
  useWindowSize,
} from '@vueuse/core'
import { useDisplay, useTheme } from 'vuetify'

type ParallaxLayerInput =
  | string
  | {
      src?: string
      speed?: number
      blendMode?: string
    }

type ParallaxLayer = {
  src: string
  speed: number
  blendMode?: string
}

const props = withDefaults(
  defineProps<{
    backgrounds?: ParallaxLayerInput[] | ParallaxLayerInput
    backgroundLight?: ParallaxLayerInput[] | ParallaxLayerInput
    backgroundDark?: ParallaxLayerInput[] | ParallaxLayerInput
    overlayColor?: string
    overlayOpacity?: number
    minHeight?: string | null
    paddingY?: string | null
    containerPadding?: string | null
    parallaxAmount?: number
    disableParallaxBelow?: number
    maxWidth?: string
    contentAlign?: 'start' | 'center'
    enableAplats?: boolean
    aplatSvg?: string
    gapless?: boolean
    maxOffsetRatio?: number | null
    reverse?: boolean
    speedFactor?: number
    overscanRatio?: number
    overscanMinPx?: number
    revealOnView?: boolean
    revealOnce?: boolean
    revealThreshold?: number
    revealRootMargin?: string
  }>(),
  {
    backgrounds: undefined,
    backgroundLight: undefined,
    backgroundDark: undefined,
    overlayColor: 'rgb(var(--v-theme-surface-default))',
    overlayOpacity: 0.42,
    minHeight: '75vh',
    paddingY: 'clamp(3rem, 8vw, 5.5rem)',
    containerPadding: 'clamp(1.5rem, 5vw, 4rem)',
    parallaxAmount: 0.18,
    disableParallaxBelow: 960,
    maxWidth: '1180px',
    contentAlign: 'start',
    enableAplats: false,
    aplatSvg: '/images/home/parallax-aplats.svg',
    gapless: false,
    maxOffsetRatio: null,
    reverse: false,
    speedFactor: 1,
    overscanRatio: 0.1,
    overscanMinPx: 80,
    revealOnView: false,
    revealOnce: true,
    revealThreshold: 0.15,
    revealRootMargin: '0px',
  }
)

const root = ref<HTMLElement | null>(null)
const { height: windowHeight } = useWindowSize()
// We only use height from useElementBounding. Top is calculated manually relative to document.
const { height } = useElementBounding(root)
const { y: scrollY } = useWindowScroll()

const rootStyles = computed<CSSProperties>(() => ({
  minHeight: props.gapless ? 'auto' : props.minHeight || undefined,
}))

const overscanPx = computed(() => {
  const ratio =
    typeof props.overscanRatio === 'number' ? props.overscanRatio : 0.1
  const clampRatio =
    typeof props.maxOffsetRatio === 'number' ? props.maxOffsetRatio : ratio
  const minPx =
    typeof props.overscanMinPx === 'number' ? props.overscanMinPx : 80
  const px = windowHeight.value * clampRatio + 32
  return Math.ceil(Math.max(minPx, px))
})

const layerInset = computed(() => `-${overscanPx.value}px`)

const theme = useTheme()
const prefersReducedMotion = usePreferredReducedMotion()
const display = useDisplay()

const isMounted = ref(false)
const isInView = ref(!props.revealOnView)
const elementAbsoluteTop = ref(0)

const updateAbsoluteTop = () => {
  if (root.value) {
    const box = root.value.getBoundingClientRect()
    // absolute top = current scroll + viewport relative top
    elementAbsoluteTop.value = window.scrollY + box.top
  }
}

onMounted(() => {
  isMounted.value = true
  updateAbsoluteTop()
  window.addEventListener('resize', updateAbsoluteTop)

  if (!props.revealOnView) {
    isInView.value = true
    return
  }

  const el = root.value
  if (!el || typeof IntersectionObserver === 'undefined') {
    isInView.value = true
    return
  }

  const observer = new IntersectionObserver(
    entries => {
      const entry = entries[0]
      if (!entry) return

      const visible = entry.isIntersecting || entry.intersectionRatio > 0
      if (visible) {
        isInView.value = true
        if (props.revealOnce) observer.disconnect()
      } else if (!props.revealOnce) {
        isInView.value = false
      }
    },
    {
      threshold: props.revealThreshold,
      rootMargin: props.revealRootMargin,
    }
  )

  observer.observe(el)
})

onUnmounted(() => {
  if (import.meta.client) {
    window.removeEventListener('resize', updateAbsoluteTop)
  }
})

const isDark = computed(() => {
  if (!isMounted.value) return false
  return Boolean(theme.global.current.value.dark)
})

const normalizeSources = (
  value?: ParallaxLayerInput | ParallaxLayerInput[]
): ParallaxLayerInput[] => {
  if (!value) {
    return []
  }

  return (Array.isArray(value) ? value : [value]).filter(item => {
    if (typeof item === 'string') {
      return Boolean(item?.trim())
    }

    return typeof item?.src === 'string' && Boolean(item.src.trim())
  })
}

const resolveLayer = (value: ParallaxLayerInput): ParallaxLayer | null => {
  if (typeof value === 'string') {
    const src = value.trim()

    return src.length > 0
      ? { src, speed: props.parallaxAmount, blendMode: undefined }
      : null
  }

  const src = typeof value.src === 'string' ? value.src.trim() : undefined

  if (!src) {
    return null
  }

  const rawBlendMode = value.blendMode
  const blendMode =
    typeof rawBlendMode === 'string' ? rawBlendMode.trim() : undefined

  return {
    src,
    speed: typeof value.speed === 'number' ? value.speed : props.parallaxAmount,
    blendMode: blendMode?.length ? blendMode : undefined,
  }
}

const resolveLayers = (values: ParallaxLayerInput[]) =>
  values
    .map(resolveLayer)
    .filter((layer): layer is ParallaxLayer => Boolean(layer))

const resolvedBackgrounds = computed<ParallaxLayer[]>(() => {
  const themedAssets = normalizeSources(props.backgrounds)

  if (themedAssets.length > 0) {
    return resolveLayers(themedAssets)
  }

  const themedBackgrounds = isDark.value
    ? normalizeSources(props.backgroundDark)
    : normalizeSources(props.backgroundLight)

  const fallbackBackgrounds = normalizeSources(props.backgroundDark).concat(
    normalizeSources(props.backgroundLight)
  )

  if (themedBackgrounds.length > 0) {
    return resolveLayers(themedBackgrounds)
  }

  return resolveLayers(fallbackBackgrounds)
})

const isBelowBreakpoint = computed(
  () =>
    props.disableParallaxBelow > 0 &&
    display.width.value < props.disableParallaxBelow
)

const parallaxEnabled = computed(
  () =>
    import.meta.client &&
    !isBelowBreakpoint.value &&
    prefersReducedMotion.value === 'no-preference' &&
    resolvedBackgrounds.value.some(background => background.speed > 0)
)

const resolveOffset = (speed: number) => {
  if (!parallaxEnabled.value) {
    return '0px'
  }

  // Calculate current top relative to viewport dynamically
  const currentTop = elementAbsoluteTop.value - scrollY.value
  const elementCenter = currentTop + height.value / 2
  const viewportCenter = windowHeight.value / 2
  const delta = elementCenter - viewportCenter

  const rawOffset = delta * speed * props.speedFactor * (props.reverse ? -1 : 1)
  const maxOffset = props.maxOffsetRatio

  if (maxOffset && import.meta.client) {
    const viewportLimit = Math.abs(windowHeight.value * maxOffset)
    const clampedOffset = Math.min(
      viewportLimit,
      Math.max(-viewportLimit, rawOffset)
    )

    return `${clampedOffset}px`
  }

  return `${rawOffset}px`
}

const mediaStyles = computed<CSSProperties>(() => ({
  '--parallax-overlay-color': props.overlayColor,
  '--parallax-overlay-opacity': props.overlayOpacity,
}))

const contentStyles = computed<CSSProperties>(() => ({
  paddingBlock: props.gapless
    ? (props.paddingY ?? '0')
    : props.paddingY || undefined,
}))

const innerStyles = computed<CSSProperties>(() => ({
  maxWidth: props.maxWidth,
  margin: '0 auto',
}))

const contentAlignClass = computed(() =>
  props.contentAlign === 'center'
    ? 'parallax-widget__inner--center'
    : 'parallax-widget__inner--start'
)

const containerPaddingStyle = computed<CSSProperties>(() => ({
  paddingInline: props.gapless
    ? (props.containerPadding ?? '0')
    : props.containerPadding || undefined,
}))
</script>

<template>
  <div
    ref="root"
    class="parallax-widget"
    :class="{
      'parallax-widget--gapless': gapless,
      'parallax-widget--in-view': isInView,
    }"
    :style="rootStyles"
  >
    <div class="parallax-widget__media" :style="mediaStyles" aria-hidden="true">
      <div
        v-for="(background, index) in resolvedBackgrounds"
        :key="`parallax-layer-${index}-${background.src}`"
        class="parallax-widget__layer"
        :style="{
          backgroundImage: `url('${background.src}')`,
          mixBlendMode: background.blendMode,
          inset: layerInset,
          transform: `translate3d(0, ${resolveOffset(background.speed)}, 0)`,
        }"
      />
      <div class="parallax-widget__overlay" />
      <div v-if="enableAplats" class="parallax-widget__aplats">
        <slot name="aplats">
          <img
            class="parallax-widget__aplat-image"
            :src="aplatSvg"
            alt=""
            loading="lazy"
            decoding="async"
          />
        </slot>
      </div>
    </div>
    <div class="parallax-widget__content" :style="contentStyles">
      <v-container
        fluid
        class="parallax-widget__container"
        :style="containerPaddingStyle"
      >
        <div
          class="parallax-widget__inner"
          :class="contentAlignClass"
          :style="innerStyles"
        >
          <slot />
        </div>
      </v-container>
    </div>
  </div>
</template>

<style scoped lang="sass">
.parallax-widget
  position: relative
  overflow: hidden
  background-color: rgb(var(--v-theme-surface-default))
  // Ensure it fills the parent section if needed
  width: 100%
  height: 100%

.parallax-widget__media
  position: absolute
  inset: 0
  width: 100%
  height: 100%
  pointer-events: none

.parallax-widget__layer
  position: absolute
  inset: -10%
  background-size: cover
  background-repeat: no-repeat
  background-position: center center
  transform: translate3d(0, 0, 0)
  transition: transform 160ms ease-out
  filter: saturate(1.05)
  will-change: transform
  z-index: 0

.parallax-widget__overlay
  position: absolute
  inset: 0
  background: linear-gradient(180deg, color-mix(in srgb, var(--parallax-overlay-color) 12%, transparent) 0%, color-mix(in srgb, var(--parallax-overlay-color) 65%, transparent) 42%, color-mix(in srgb, var(--parallax-overlay-color) 82%, transparent) 100%)
  mix-blend-mode: multiply
  opacity: var(--parallax-overlay-opacity)
  z-index: 1

.parallax-widget__aplats
  position: absolute
  inset: 0
  display: flex
  align-items: center
  justify-content: center
  opacity: 0.45
  filter: drop-shadow(0 20px 45px rgba(var(--v-theme-shadow-primary-600), 0.22))
  z-index: 2

.parallax-widget__aplat-image
  max-width: min(100%, 1024px)
  width: 90%
  height: auto

.parallax-widget__content
  position: relative
  z-index: 3

.parallax-widget__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.parallax-widget__inner
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.5rem)

.parallax-widget__inner--center
  align-items: center
  text-align: center

.parallax-widget__inner--start
  align-items: stretch

:deep(.home-section)
  background: transparent
  box-shadow: none

:deep(.home-section__container)
  padding-inline: 0

.parallax-widget--gapless
  background-color: transparent

.parallax-widget--gapless .parallax-widget__content
  padding-block: 0 !important

.parallax-widget--gapless .parallax-widget__container
  padding-inline: 0

.parallax-widget--gapless .parallax-widget__inner
  gap: clamp(1rem, 2.5vw, 1.75rem)

@media (max-width: 959px)
  .parallax-widget__layer
    transform: none !important

  .parallax-widget__overlay
    opacity: calc(var(--parallax-overlay-opacity) + 0.08)
</style>

<script setup lang="ts">
import { computed, ref, type CSSProperties, onMounted } from 'vue'
import {
  useElementBounding,
  useIntersectionObserver,
  usePreferredReducedMotion,
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
    /**
     * Invert the parallax direction (useful for alternating hero sections).
     */
    reverse?: boolean
    /**
     * Multiplies every layer speed. 0 disables movement; 1 is default.
     */
    speedFactor?: number
    /**
     * Extra padding (overscan) used to prevent edge gaps during parallax.
     * This ratio contributes to the computed overscan in pixels.
     */
    overscanRatio?: number
    /**
     * Minimum overscan in pixels.
     */
    overscanMinPx?: number
    /**
     * Smooth transform interpolation (ms). Set to 0 for "crisp" scroll.
     */
    smoothingMs?: number
    /**
     * When enabled, adds the `parallax-widget--in-view` class once the widget
     * enters the viewport (useful to trigger SVG/CSS animations).
     */
    revealOnView?: boolean
    /**
     * If true, the in-view state stays enabled once revealed.
     */
    revealOnce?: boolean
    revealThreshold?: number
    revealRootMargin?: string
    revealDurationMs?: number
    revealDelayMs?: number
    /**
     * Caps translation in absolute px. Overrides maxOffsetRatio when set.
     */
    maxOffsetPx?: number | null
    maxOffsetRatio?: number | null
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
    overscanRatio: 0.22,
    overscanMinPx: 180,
    smoothingMs: 0,
    revealOnView: false,
    revealOnce: true,
    revealThreshold: 0.15,
    revealRootMargin: '0px 0px -10% 0px',
    revealDurationMs: 500,
    revealDelayMs: 0,
    maxOffsetPx: null,
  }
)

const root = ref<HTMLElement | null>(null)
const { height: windowHeight } = useWindowSize()
const { top, height } = useElementBounding(root)

// We want to track the parent if this widget is inside a section,
// to ensure the parallax effect covers the section bounds if needed,
// but usually the widget IS the background container.
// If it is just a div taking 100% of parent, `useElementBounding(root)` is fine.

const theme = useTheme()
const prefersReducedMotion = usePreferredReducedMotion()
const display = useDisplay()

const isMounted = ref(false)
onMounted(() => {
  isMounted.value = true
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

    return Boolean(item?.src?.trim())
  })
}

const resolveLayer = (value: ParallaxLayerInput): ParallaxLayer | null => {
  if (typeof value === 'string') {
    const src = value.trim()

    return src.length > 0
      ? { src, speed: props.parallaxAmount, blendMode: undefined }
      : null
  }

  const src = value.src?.trim()

  if (!src) {
    return null
  }

  const blendMode = value.blendMode?.trim()

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

const directionSign = computed(() => (props.reverse ? -1 : 1))
const speedFactor = computed(() =>
  Number.isFinite(props.speedFactor) ? Math.max(0, props.speedFactor) : 1
)

const maxLayerSpeed = computed(() =>
  resolvedBackgrounds.value.reduce(
    (acc, layer) => Math.max(acc, Math.abs(layer.speed)),
    0
  )
)

/**
 * Overscan is computed to cover the worst-case translation while the section is visible,
 * so we don't see "gaps" on the edges between sections.
 */
const overscanPx = computed(() => {
  const ratio = Number.isFinite(props.overscanRatio)
    ? Math.max(0, props.overscanRatio)
    : 0.22
  const minPx = Number.isFinite(props.overscanMinPx)
    ? Math.max(0, props.overscanMinPx)
    : 180
  const elementH = height.value || 0

  const maxVisibleDelta = windowHeight.value / 2 + elementH / 2
  const worstOffsetPx = maxVisibleDelta * maxLayerSpeed.value * speedFactor.value

  // Use both a height-based overscan and a worst-offset-based overscan, whichever is larger.
  const fromHeight = elementH * ratio
  const paddedWorst = worstOffsetPx * (1 + ratio)

  return Math.ceil(Math.max(minPx, fromHeight, paddedWorst))
})

const smoothingMs = computed(() => {
  if (prefersReducedMotion.value !== 'no-preference') return 0
  return Number.isFinite(props.smoothingMs) ? Math.max(0, props.smoothingMs) : 0
})

const inView = ref(!props.revealOnView)
if (typeof window !== 'undefined' && props.revealOnView) {
  const { stop } = useIntersectionObserver(
    root,
    ([entry]) => {
      const next = Boolean(entry?.isIntersecting)
      if (next) {
        inView.value = true
        if (props.revealOnce) stop()
      } else if (!props.revealOnce) {
        inView.value = false
      }
    },
    {
      threshold: props.revealThreshold,
      rootMargin: props.revealRootMargin,
    }
  )
}

const isInView = computed(() => !props.revealOnView || inView.value)

const rootStyles = computed<CSSProperties>(() => ({
  minHeight: props.gapless ? 'auto' : props.minHeight || undefined,
  '--parallax-overlay-color': props.overlayColor,
  '--parallax-overlay-opacity': props.overlayOpacity,
  '--parallax-overscan-px': `${overscanPx.value}px`,
  '--parallax-smoothing-ms': `${smoothingMs.value}ms`,
  '--parallax-reveal-duration-ms': `${props.revealDurationMs}ms`,
  '--parallax-reveal-delay-ms': `${props.revealDelayMs}ms`,
}))

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

  const elementCenter = top.value + height.value / 2
  const viewportCenter = windowHeight.value / 2
  const delta = elementCenter - viewportCenter

  const rawOffset = delta * speed * speedFactor.value * directionSign.value

  const maxOffsetPx =
    typeof props.maxOffsetPx === 'number' && props.maxOffsetPx !== 0
      ? Math.abs(props.maxOffsetPx)
      : typeof props.maxOffsetRatio === 'number' && props.maxOffsetRatio > 0
        ? Math.abs(windowHeight.value * props.maxOffsetRatio)
        : Math.max(0, overscanPx.value * 0.98)

  if (maxOffsetPx) {
    const clampedOffset = Math.min(
      maxOffsetPx,
      Math.max(-maxOffsetPx, rawOffset)
    )
    return `${clampedOffset}px`
  }

  return `${rawOffset}px`
}

const mediaStyles = computed<CSSProperties>(() => ({}))

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
  inset: calc(var(--parallax-overscan-px) * -1)
  background-size: cover
  background-repeat: no-repeat
  background-position: center center
  transform: translate3d(0, 0, 0)
  transition: transform var(--parallax-smoothing-ms) ease-out
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
  filter: drop-shadow(0 20px 45px rgba(var(--v-theme-shadow-primary-600), 0.22))
  z-index: 2
  opacity: 0
  transform: translate3d(0, 10px, 0)
  transition: opacity var(--parallax-reveal-duration-ms) ease-out, transform var(--parallax-reveal-duration-ms) ease-out
  transition-delay: var(--parallax-reveal-delay-ms)

.parallax-widget--in-view .parallax-widget__aplats
  opacity: 0.45
  transform: translate3d(0, 0, 0)

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

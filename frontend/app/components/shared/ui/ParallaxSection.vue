<script setup lang="ts">
import { computed, ref, type CSSProperties, onMounted } from 'vue'
import {
  useElementBounding,
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
    id?: string
    ariaLabel?: string
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
  }>(),
  {
    id: undefined,
    ariaLabel: undefined,
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
  }
)

const root = ref<HTMLElement | null>(null)
const { height: windowHeight } = useWindowSize()
const { top, height } = useElementBounding(root)

const theme = useTheme()
const prefersReducedMotion = usePreferredReducedMotion()
const display = useDisplay()

const isMounted = ref(false)
onMounted(() => {
  isMounted.value = true
})

// Hydration mismatch fix:
// During SSR and Client Hydration, we must match (assuming light/default).
// We only switch to dark (if detected) after mount.
const isDark = computed(() => {
  if (!isMounted.value) return false // Force light during hydration
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
    speed:
      typeof value.speed === 'number' ? value.speed : props.parallaxAmount,
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

  // Calculate position relative to viewport center
  // 0 = center of viewport
  // positive = below center
  // negative = above center
  const elementCenter = top.value + height.value / 2
  const viewportCenter = windowHeight.value / 2
  const delta = elementCenter - viewportCenter

  const rawOffset = delta * speed
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
  minHeight: props.gapless ? 'auto' : props.minHeight || undefined,
}))

const contentStyles = computed<CSSProperties>(() => ({
  paddingBlock: props.gapless
    ? props.paddingY ?? '0'
    : props.paddingY || undefined,
}))

const innerStyles = computed<CSSProperties>(() => ({
  maxWidth: props.maxWidth,
  margin: '0 auto',
}))

const contentAlignClass = computed(() =>
  props.contentAlign === 'center'
    ? 'parallax-section__inner--center'
    : 'parallax-section__inner--start'
)

const containerPaddingStyle = computed<CSSProperties>(() => ({
  paddingInline: props.gapless
    ? props.containerPadding ?? '0'
    : props.containerPadding || undefined,
}))
</script>

<template>
  <section
    :id="props.id"
    ref="root"
    class="parallax-section"
    :class="{ 'parallax-section--gapless': gapless }"
    :aria-label="ariaLabel"
  >
    <div
      class="parallax-section__media"
      :style="mediaStyles"
      aria-hidden="true"
    >
      <div
        v-for="(background, index) in resolvedBackgrounds"
        :key="`parallax-layer-${index}-${background.src}`"
        class="parallax-section__layer"
        :style="{
          backgroundImage: `url('${background.src}')`,
          mixBlendMode: background.blendMode,
          transform: `translate3d(0, ${resolveOffset(background.speed)}, 0)`,
        }"
      />
      <div class="parallax-section__overlay" />
      <div v-if="enableAplats" class="parallax-section__aplats">
        <slot name="aplats">
          <img
            class="parallax-section__aplat-image"
            :src="aplatSvg"
            alt=""
            loading="lazy"
            decoding="async"
          />
        </slot>
      </div>
    </div>
    <div class="parallax-section__content" :style="contentStyles">
      <v-container
        fluid
        class="parallax-section__container"
        :style="containerPaddingStyle"
      >
        <div
          class="parallax-section__inner"
          :class="contentAlignClass"
          :style="innerStyles"
        >
          <slot />
        </div>
      </v-container>
    </div>
  </section>
</template>

<style scoped lang="sass">
.parallax-section
  position: relative
  overflow: hidden
  background-color: rgb(var(--v-theme-surface-default))

.parallax-section__media
  position: absolute
  inset: 0
  width: 100%
  height: 100%
  pointer-events: none

.parallax-section__layer
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

.parallax-section__overlay
  position: absolute
  inset: 0
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--parallax-overlay-color) 12%, transparent) 0%,
    color-mix(in srgb, var(--parallax-overlay-color) 65%, transparent) 42%,
    color-mix(in srgb, var(--parallax-overlay-color) 82%, transparent) 100%
  )
  mix-blend-mode: multiply
  opacity: var(--parallax-overlay-opacity)
  z-index: 1

.parallax-section__aplats
  position: absolute
  inset: 0
  display: flex
  align-items: center
  justify-content: center
  opacity: 0.45
  filter: drop-shadow(0 20px 45px rgba(var(--v-theme-shadow-primary-600), 0.22))
  z-index: 2

.parallax-section__aplat-image
  max-width: min(100%, 1024px)
  width: 90%
  height: auto

.parallax-section__content
  position: relative
  z-index: 3

.parallax-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.parallax-section__inner
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.5rem)

.parallax-section__inner--center
  align-items: center
  text-align: center

.parallax-section__inner--start
  align-items: stretch

:deep(.home-section)
  background: transparent
  box-shadow: none

:deep(.home-section__container)
  padding-inline: 0

.parallax-section--gapless
  background-color: transparent

.parallax-section--gapless .parallax-section__content
  padding-block: 0 !important

.parallax-section--gapless .parallax-section__container
  padding-inline: 0

.parallax-section--gapless .parallax-section__inner
  gap: clamp(1rem, 2.5vw, 1.75rem)

@media (max-width: 959px)
  .parallax-section__layer
    transform: none !important

  .parallax-section__overlay
    opacity: calc(var(--parallax-overlay-opacity) + 0.08)
</style>

<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { usePreferredReducedMotion } from '@vueuse/core'
import { useDisplay, useTheme } from 'vuetify'

const props = withDefaults(
  defineProps<{
    id?: string
    ariaLabel?: string
    backgrounds?: string[]
    backgroundLight?: string
    backgroundDark?: string
    overlayColor?: string
    overlayOpacity?: number
    minHeight?: string | null
    paddingY?: string | null
    containerPadding?: string | null
    parallaxAmount?: number
    disableParallaxBelow?: number
    parallaxSpeed?: number
    maxWidth?: string
    contentAlign?: 'start' | 'center'
    enableAplats?: boolean
    aplatSvg?: string
    gapless?: boolean
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
    parallaxSpeed: 1,
    maxWidth: '1180px',
    contentAlign: 'start',
    enableAplats: false,
    aplatSvg: '/images/home/parallax-aplats.svg',
    gapless: false,
  }
)

const theme = useTheme()
const prefersReducedMotion = usePreferredReducedMotion()
const display = useDisplay()

const isDark = computed(() => Boolean(theme.global.current.value.dark))

const normalizeSources = (value?: string | string[]) => {
  if (!value) {
    return []
  }

  return (Array.isArray(value) ? value : [value]).filter(item =>
    Boolean(item?.trim())
  )
}

const resolvedBackgrounds = computed(() => {
  const themedAssets = normalizeSources(props.backgrounds)

  if (themedAssets.length > 0) {
    return themedAssets
  }

  const themedBackgrounds = isDark.value
    ? normalizeSources(props.backgroundDark)
    : normalizeSources(props.backgroundLight)

  const fallbackBackgrounds = normalizeSources(props.backgroundDark).concat(
    normalizeSources(props.backgroundLight)
  )

  if (themedBackgrounds.length > 0) {
    return themedBackgrounds
  }

  return fallbackBackgrounds
})

const backgroundImage = computed(() => {
  if (resolvedBackgrounds.value.length === 0) {
    return ''
  }

  return resolvedBackgrounds.value
    .map(background => `url('${background}')`)
    .join(', ')
})

const isBelowBreakpoint = computed(
  () =>
    props.disableParallaxBelow > 0 &&
    display.width.value < props.disableParallaxBelow
)

const parallaxDisabled = computed(
  () =>
    !import.meta.client ||
    isBelowBreakpoint.value ||
    prefersReducedMotion.value === 'reduce' ||
    props.parallaxAmount <= 0
)

const resolvedMinHeight = computed(() => props.minHeight || '75vh')

const primaryBackground = computed(() => resolvedBackgrounds.value[0] || '')
const stackedBackgrounds = computed(() => resolvedBackgrounds.value.slice(1))

const parallaxScale = computed(() => {
  if (parallaxDisabled.value) {
    return 1
  }

  const speed = Math.max(props.parallaxSpeed ?? 1, 0.5)
  const base = Math.max(props.parallaxAmount, 0)
  return Math.min(1 + base * speed * 2.4, 4)
})

const mediaStyles = computed<CSSProperties>(() => ({
  '--parallax-image': backgroundImage.value || 'none',
  '--parallax-overlay-color': props.overlayColor,
  '--parallax-overlay-opacity': props.overlayOpacity,
  minHeight: resolvedMinHeight.value,
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

const stackedBackgroundStyle = computed<CSSProperties>(() => ({
  backgroundImage:
    stackedBackgrounds.value.length > 0
      ? stackedBackgrounds.value.map(background => `url('${background}')`).join(', ')
      : undefined,
}))
</script>

<template>
  <section
    :id="props.id"
    class="parallax-section"
    :class="{ 'parallax-section--gapless': gapless }"
    :aria-label="ariaLabel"
  >
    <div class="parallax-section__media" :style="mediaStyles" aria-hidden="true">
      <v-parallax
        v-if="primaryBackground"
        class="parallax-section__parallax"
        :src="primaryBackground"
        :height="resolvedMinHeight"
        :scale="parallaxScale"
        :eager="true"
        transition="none"
      >
        <div v-if="stackedBackgrounds.length" class="parallax-section__stack" :style="stackedBackgroundStyle" />
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
      </v-parallax>

      <div v-else class="parallax-section__fallback">
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

.parallax-section__parallax
  height: 100%
  width: 100%

.parallax-section__stack,
.parallax-section__fallback
  position: absolute
  inset: -8%
  background-image: var(--parallax-image)
  background-size: cover
  background-repeat: no-repeat
  background-position: center center

.parallax-section__stack
  filter: saturate(1.05)

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

.parallax-section__aplats
  position: absolute
  inset: 0
  display: flex
  align-items: center
  justify-content: center
  opacity: 0.45
  filter: drop-shadow(0 20px 45px rgba(var(--v-theme-shadow-primary-600), 0.22))

.parallax-section__aplat-image
  max-width: min(100%, 1024px)
  width: 90%
  height: auto

.parallax-section__content
  position: relative
  z-index: 1

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
  .parallax-section__image
    transform: none !important

  .parallax-section__overlay
    opacity: calc(var(--parallax-overlay-opacity) + 0.08)
</style>

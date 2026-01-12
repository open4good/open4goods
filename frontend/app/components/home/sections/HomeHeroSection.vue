<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useTheme } from 'vuetify'
import {
  resolveThemedAssetUrl,
  useHeroBackgroundAsset,
} from '~~/app/composables/useThemedAsset'
import { useSeasonalEventPack } from '~~/app/composables/useSeasonalEventPack'
import { useEventPackI18n } from '~/composables/useEventPackI18n'
import { THEME_ASSETS_FALLBACK } from '~~/config/theme/assets'
import { resolveThemeName } from '~~/shared/constants/theme'

const isHeroImageLoaded = ref(false)
const heroReadyFallbackDelayMs = 900

const props = defineProps<{
  heroImageLight?: string
  heroImageDark?: string
  heroBackgroundI18nKey?: string
}>()

const theme = useTheme()
const heroBackgroundAsset = useHeroBackgroundAsset()
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)
const themeName = computed(() =>
  resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
)

const heroTitle = computed(
  () =>
    packI18n.resolveString('hero.title', {
      fallbackKeys: ['home.hero.title'],
    }) ?? ''
)

const heroTitleSubtitle = computed(
  () =>
    packI18n.resolveStringVariant('hero.titleSubtitle', {
      fallbackKeys: ['home.hero.titleSubtitle'],
      stateKey: 'home-hero-title-subtitle',
    }) ?? ''
)

const showHeroSkeleton = computed(() => !isHeroImageLoaded.value)
const heroBackgroundI18nKey = computed(
  () => props.heroBackgroundI18nKey?.trim() || 'hero.background'
)
const heroBackgroundI18nValue = computed(
  () =>
    packI18n.resolveString(heroBackgroundI18nKey.value, {
      fallbackKeys: ['home.hero.background'],
    }) ?? ''
)

const resolveHeroBackgroundSource = (value?: string): string | undefined => {
  if (!value) {
    return undefined
  }

  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }

  if (trimmed.startsWith('/') || trimmed.startsWith('http')) {
    return trimmed
  }

  return resolveThemedAssetUrl(trimmed, themeName.value, activeEventPack.value)
}

const heroBackgroundOverride = computed(() =>
  resolveHeroBackgroundSource(heroBackgroundI18nValue.value)
)

const heroBackgroundSrc = computed(() => {
  const themedAsset = heroBackgroundOverride.value?.trim()

  if (themedAsset) {
    return themedAsset
  }

  const fallbackAsset = heroBackgroundAsset.value?.trim()
  if (fallbackAsset) {
    return fallbackAsset
  }

  const isDarkMode = Boolean(theme.global.current.value.dark)
  const lightImage = props.heroImageLight?.trim()
  const darkImage = props.heroImageDark?.trim()

  return isDarkMode ? (darkImage ?? '') : (lightImage ?? '')
})

const handleHeroImageLoad = () => {
  isHeroImageLoaded.value = true
}

onMounted(() => {
  window.setTimeout(() => {
    if (!isHeroImageLoaded.value) {
      isHeroImageLoaded.value = true
    }
  }, heroReadyFallbackDelayMs)
})

useHead({
  link: [
    {
      rel: 'preload',
      as: 'image',
      href: () => heroBackgroundSrc.value,
    },
  ],
})
</script>

<template>
  <HeroSurface
    tag="section"
    class="home-hero"
    aria-labelledby="home-hero-title"
    variant="none"
    :bleed="true"
  >
    <div class="home-hero__background" aria-hidden="true">
      <v-fade-transition>
        <div v-if="showHeroSkeleton" class="home-hero__background-loader">
          <v-skeleton-loader
            type="image"
            class="home-hero__background-skeleton"
          />
        </div>
      </v-fade-transition>
      <img
        class="home-hero__background-media"
        :src="heroBackgroundSrc"
        alt=""
        fetchpriority="high"
        decoding="async"
        @load="handleHeroImageLoad"
      />
      <div class="home-hero__background-overlay" />
    </div>
    <v-container fluid class="home-hero__container">
      <div class="home-hero__inner">
        <v-row class="home-hero__layout" align="center" justify="center">
          <v-col cols="12" class="home-hero__content">
            <h1 id="home-hero-title" class="home-hero__title">
              {{ heroTitle }}
            </h1>
            <p v-if="heroTitleSubtitle" class="home-hero__title-subtitle">
              {{ heroTitleSubtitle }}
            </p>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </HeroSurface>
</template>

<style scoped lang="sass">
.home-hero
  position: relative
  overflow: hidden
  min-height: 100dvh
  box-sizing: border-box
  --home-hero-padding: clamp(2.5rem, 7vw, 1.75rem)
  padding-block: var(--home-hero-padding)
  padding-top: calc(var(--home-hero-padding) + env(safe-area-inset-top))
  padding-bottom: calc(var(--home-hero-padding) + env(safe-area-inset-bottom))

.home-hero__background
  position: absolute
  inset: 0
  z-index: 0
  pointer-events: none
  height: 100%

.home-hero__background-loader
  position: absolute
  inset: 0
  z-index: 0
  display: flex
  align-items: center
  justify-content: center
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.12), rgba(var(--v-theme-hero-gradient-end), 0.18))

.home-hero__background-skeleton
  width: 100%
  height: 100%
  opacity: 0.45

.home-hero__background-media
  position: absolute
  inset: 0
  width: 100%
  height: 100%
  opacity: 0.98
  object-fit: cover

.home-hero__background-overlay
  position: absolute
  inset: 0
  height: 100%
  background: radial-gradient(
      circle at 16% 24%,
      rgba(var(--v-theme-hero-gradient-start), 0.22),
      transparent 32%
    ), radial-gradient(
      circle at 78% 12%,
      rgba(var(--v-theme-hero-gradient-end), 0.24),
      transparent 36%
    ), linear-gradient(
      180deg,
      rgba(var(--v-theme-surface-default), 0.1) 0%,
      rgba(var(--v-theme-surface-default), 0.15) 35%,
      rgba(var(--v-theme-surface-default), 0.65) 100%
    )
  pointer-events: none

.home-hero__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)
  position: relative
  z-index: 1
  min-height: 100%
  height: 100%

.home-hero__inner
  margin: 0 auto
  min-height: 100%
  display: flex
  flex-direction: column
  justify-content: center

.home-hero__layout
  --v-gutter-x: clamp(2rem, 5vw, 3.5rem)
  --v-gutter-y: clamp(2rem, 5vw, 3.5rem)

.home-hero__content
  display: flex
  flex-direction: column
  gap: 1.5rem
  align-items: center
  text-align: center

.home-hero__title
  font-size: clamp(2.2rem, 5vw, 3.8rem)
  line-height: 1.05
  margin: 0
  color: #ffffff
  text-shadow: rgb(var(--v-theme-text-neutral-secondary)) 1px 0 10px

.home-hero__title-subtitle
  margin: clamp(0.65rem, 1.8vw, 1rem) auto 0
  max-width: 28ch
  color: rgba(var(--v-theme-surface-default), 0.94)
  font-size: clamp(1rem, 2.4vw, 1.4rem)
  line-height: 1.4

@media (max-width: 959px)
  .home-hero
    min-height: clamp(520px, 68dvh, 760px)
    padding-block: clamp(2rem, 10vw, 4rem)
</style>

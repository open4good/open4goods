<script setup lang="ts">
import { computed, onMounted, ref, toRefs } from 'vue'
import { useTheme } from 'vuetify'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import type { VerticalConfigDto } from '~~/shared/api-client'

type HeroHelperItem = {
  icon: string
  label: string
}

const isHeroImageLoaded = ref(false)
const heroReadyFallbackDelayMs = 900

const props = defineProps<{
  searchQuery: string
  minSuggestionQueryLength: number
  verticals?: VerticalConfigDto[]
  heroImageLight?: string
  heroImageDark?: string
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t, tm } = useI18n()
const theme = useTheme()

const searchQueryValue = computed(() => props.searchQuery)

const { minSuggestionQueryLength } = toRefs(props)
const wizardVerticals = computed(() => props.verticals ?? [])

const updateSearchQuery = (value: string) => {
  emit('update:searchQuery', value)
}

const normalizeHelperItems = (items: unknown): HeroHelperItem[] => {
  if (!Array.isArray(items)) {
    return []
  }

  return items
    .map((rawItem) => {
      if (typeof rawItem !== 'object' || rawItem == null) {
        return null
      }

      const { icon, label } = rawItem as { icon?: unknown; label?: unknown }
      const normalizedLabel = typeof label === 'string' ? label.trim() : ''

      if (!normalizedLabel) {
        return null
      }

      const normalizedIcon = typeof icon === 'string' && icon.trim().length > 0 ? icon.trim() : '•'

      return {
        icon: normalizedIcon,
        label: normalizedLabel,
      }
    })
    .filter((item): item is HeroHelperItem => item != null)
}

const heroHelperItems = computed<HeroHelperItem[]>(() => {
  const translatedItems = normalizeHelperItems(tm('home.hero.search.helpers'))

  if (translatedItems.length > 0) {
    return translatedItems
  }

  const fallback = String(t('home.hero.search.helper'))
  const trimmedFallback = fallback.trim()

  if (!trimmedFallback || trimmedFallback === 'home.hero.search.helper') {
    return []
  }

  return [
    {
      icon: '⚡',
      label: trimmedFallback,
    },
  ]
})

const heroIconAlt = computed(() => String(t('home.hero.iconAlt')).trim())
const heroIconSrc = '/pwa-assets/icons/android/android-launchericon-512-512.png'
const heroIconAnimationOptions = ['home-hero__icon--fade', 'home-hero__icon--scale', 'home-hero__icon--pulse'] as const
const heroIconAnimation = ref(
  heroIconAnimationOptions[Math.floor(Math.random() * heroIconAnimationOptions.length)] || heroIconAnimationOptions[0],
)
const showHeroIcon = computed(() => Boolean(heroIconAlt.value))

const showHeroSkeleton = computed(() => !isHeroImageLoaded.value)
const heroBackgroundSrc = computed(() => {
  const isDarkMode = Boolean(theme.global.current.value.dark)
  const lightImage = props.heroImageLight || '/images/home/home-hero_background.webp'
  const darkImage = props.heroImageDark || '/images/home/hero-placeholder.svg'

  return isDarkMode ? darkImage : lightImage
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

const handleSubmit = () => {
  emit('submit')
}

const handleCategorySelect = (payload: CategorySuggestionItem) => {
  emit('select-category', payload)
}

const handleProductSelect = (payload: ProductSuggestionItem) => {
  emit('select-product', payload)
}
</script>

<template>
  <HeroSurface tag="section" class="home-hero" aria-labelledby="home-hero-title" variant="aurora" :bleed="true">
    <div class="home-hero__background" aria-hidden="true">
      <v-fade-transition>
        <div v-if="showHeroSkeleton" class="home-hero__background-loader">
          <v-skeleton-loader type="image" class="home-hero__background-skeleton" />
        </div>
      </v-fade-transition>
      <v-parallax
        class="home-hero__parallax"
        :src="heroBackgroundSrc"
        height="100%"
        scale="1.08"
        eager
        @load="handleHeroImageLoad"
      >
        <div class="home-hero__background-overlay" />
      </v-parallax>
    </div>
    <v-container fluid class="home-hero__container">
      <div class="home-hero__inner">
        <v-row class="home-hero__layout" align="stretch" justify="center">
          <v-col cols="12" class="home-hero__content">
            <v-slide-y-transition mode="out-in">
              <h1 id="home-hero-title" class="home-hero__title">
                {{ t('home.hero.title') }}
              </h1>
            </v-slide-y-transition>
          </v-col>
        </v-row>
        <v-row justify="center">
          <v-col cols="12" lg="10" xl="8">
            <v-sheet class="home-hero__panel" color="transparent" elevation="0">
              <div class="home-hero__panel-grid">
                <div class="home-hero__panel-block">
                  <NudgeToolWizard :verticals="wizardVerticals" />
                </div>
                <div class="home-hero__panel-block">
                  <form class="home-hero__search" role="search" @submit.prevent="handleSubmit">
                    <SearchSuggestField
                      :model-value="searchQueryValue"
                      class="home-hero__search-input"
                      :label="t('home.hero.search.label')"
                      :placeholder="t('home.hero.search.placeholder')"
                      :aria-label="t('home.hero.search.ariaLabel')"
                      :min-chars="minSuggestionQueryLength"
                      @update:model-value="updateSearchQuery"
                      @submit="handleSubmit"
                      @select-category="handleCategorySelect"
                      @select-product="handleProductSelect"
                    >
                      <template #append-inner>
                        <v-btn
                          class="home-hero__search-submit nudger_degrade-defaut"
                          icon="mdi-arrow-right"
                          variant="flat"
                          color="primary"
                          size="small"
                          type="submit"
                          :aria-label="t('home.hero.search.cta')"
                        />
                      </template>
                    </SearchSuggestField>
                  </form>
                  <div class="home-hero__context">
                    <p class="home-hero__subtitle">{{ t('home.hero.subtitle') }}</p>
                    <div class="home-hero__helper-row">
                      <div class="home-hero__eyebrow-block">
                        <p class="home-hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
                        <div v-if="showHeroIcon" class="home-hero__icon-wrapper">
                          <img
                            :src="heroIconSrc"
                            :alt="heroIconAlt"
                            :class="['home-hero__icon', heroIconAnimation]"
                            loading="lazy"
                          />
                        </div>
                      </div>
                      <ul v-if="heroHelperItems.length" class="home-hero__helpers">
                        <li v-for="(item, index) in heroHelperItems" :key="`hero-helper-${index}`" class="home-hero__helper">
                          <span class="home-hero__helper-icon" aria-hidden="true">{{ item.icon }}</span>
                          <span class="home-hero__helper-text">{{ item.label }}</span>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </v-sheet>
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
    --home-hero-padding: clamp(2.5rem, 7vw, 4.75rem)
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

  .home-hero__parallax
    position: absolute
    inset: 0
    width: 100%
    height: 100%
    opacity: 0.98
    min-height: 100%

  .home-hero__background :deep(.v-img__img)
    width: 100%
    object-fit: cover
    min-height: 100%

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
    gap: clamp(1.75rem, 4vw, 2.75rem)

  .home-hero__layout
    --v-gutter-x: clamp(2rem, 5vw, 3.5rem)
    --v-gutter-y: clamp(2rem, 5vw, 3.5rem)

  .home-hero__content
    display: flex
    flex-direction: column
    gap: 1.5rem
    align-items: center
    text-align: center

  .home-hero__eyebrow-block
    display: inline-flex
    flex-direction: column
    gap: clamp(0.5rem, 1.5vw, 0.75rem)
    padding-inline-start: clamp(0.25rem, 1.25vw, 0.75rem)
    align-items: flex-start

  .home-hero__eyebrow
    font-weight: 600
    letter-spacing: 0.08em
    text-transform: uppercase
    color: rgba(var(--v-theme-hero-gradient-end), 0.9)
    margin: 0

  .home-hero__icon-wrapper
    width: clamp(88px, 18vw, 136px)
    height: clamp(88px, 18vw, 136px)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45)
    box-shadow: 0 12px 30px rgba(var(--v-theme-shadow-primary-600), 0.12)
    backdrop-filter: blur(8px)

  .home-hero__icon
    border-radius: inherit
    width: 100%
    height: 100%
    transition: transform 250ms ease, filter 250ms ease
    filter: drop-shadow(0 6px 14px rgba(var(--v-theme-shadow-primary-600), 0.25))

  .home-hero__icon--fade
    animation: home-hero-fade-up 900ms ease-out both

  .home-hero__icon--scale
    animation: home-hero-scale-in 800ms ease-out both

  .home-hero__icon--pulse
    animation: home-hero-pulse 1200ms ease-in-out both

  .home-hero__title
    font-size: clamp(2.2rem, 5vw, 3.8rem)
    line-height: 1.05
    margin: 0
    color: #ffffff
    text-shadow: rgb(var(--v-theme-primary)) 1px 0 10px

  .home-hero__search
    display: flex
    flex-direction: column
    gap: 0.75rem

  .home-hero__search-input
    border-radius: clamp(1.5rem, 4vw, 2rem)
    background: rgba(var(--v-theme-surface-default), 0.85)
    box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.1)

  .home-hero__search-submit
    box-shadow: none

  .home-hero__panel
    backdrop-filter: blur(10px)
    background: rgba(var(--v-theme-surface-glass), 0.82)
    border-radius: clamp(1.5rem, 4vw, 2rem)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
    box-shadow: 0 18px 40px rgba(var(--v-theme-shadow-primary-600), 0.16)
    padding: clamp(1.5rem, 4vw, 2.5rem)

  .home-hero__panel-grid
    display: grid
    gap: clamp(1.25rem, 3vw, 1.75rem)
    grid-template-columns: 1fr

  .home-hero__panel-block
    display: flex
    flex-direction: column
    gap: clamp(1.25rem, 2vw, 1.75rem)

  .home-hero__context
    display: flex
    flex-direction: column
    gap: 0.75rem
    text-align: left

  .home-hero__helper-row
    display: grid
    gap: 0.75rem
    grid-template-columns: auto 1fr
    align-items: center

  .home-hero__helpers
    margin: 0
    padding: 0
    display: grid
    gap: 0.35rem
    list-style: none

  .home-hero__helper
    display: inline-flex
    align-items: center
    gap: 0.45rem
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    font-weight: 500

  .home-hero__helper-icon
    font-size: 1.1rem

  .home-hero__helper-text
    line-height: 1.35

  .home-hero__subtitle
    margin: 0
    color: rgb(var(--v-theme-text-neutral-strong))

  .home-hero__wizard
    width: 100%

  .home-hero__media
    display: flex
    align-items: center
    justify-content: center

  .home-hero__media-sheet
    width: 100%
    padding: clamp(0.5rem, 2vw, 1rem)
    background: rgba(var(--v-theme-surface-glass), 0.85)
    overflow: hidden
    position: relative

  .home-hero__video-wrapper
    position: relative
    width: 100%
    aspect-ratio: 16 / 9
    border-radius: clamp(1.75rem, 4vw, 2.5rem)
    overflow: hidden

  .home-hero__video
    width: 100%
    height: 100%
    object-fit: cover
    transform: scale(1.3)
    transform-origin: center

  .home-hero__video-overlay
    position: absolute
    inset: 0
    background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.1), rgba(var(--v-theme-hero-gradient-end), 0.2))
    pointer-events: none

  .home-hero__media-link
    margin: 0

  .home-hero__sr-only
    position: absolute
    width: 1px
    height: 1px
    padding: 0
    margin: -1px
    overflow: hidden
    clip: rect(0, 0, 0, 0)
    white-space: nowrap
    border: 0

  @keyframes home-hero-fade-up
    from
      opacity: 0
      transform: translateY(12px) scale(0.98)
    to
      opacity: 1
      transform: translateY(0) scale(1)

  @keyframes home-hero-scale-in
    from
      opacity: 0
      transform: scale(0.9)
    55%
      opacity: 1
      transform: scale(1.02)
    to
      transform: scale(1)

  @keyframes home-hero-pulse
    0%
      transform: scale(0.92)
      opacity: 0
    35%
      transform: scale(1.04)
      opacity: 1
    70%
      transform: scale(0.98)
    100%
      transform: scale(1)

  @media (max-width: 959px)
    .home-hero
      min-height: clamp(520px, 68dvh, 760px)
      padding-block: clamp(2rem, 10vw, 4rem)

    .home-hero__panel
      padding: clamp(1.25rem, 5vw, 2rem)

    .home-hero__panel-grid
      gap: clamp(1rem, 3vw, 1.5rem)

    .home-hero__media
      order: 1

    .home-hero__content
      order: 2

    .home-hero__video-wrapper
      aspect-ratio: 4 / 5
      min-height: 320px

    .home-hero__video
      transform: scale(1.15)

  @media (min-width: 960px)
    .home-hero__panel-grid
      grid-template-columns: 1fr

    .home-hero__helper-row
      grid-template-columns: auto 1fr

  @media (min-width: 1280px)
    .home-hero__panel-grid
      grid-template-columns: 1fr

  @media (min-width: 1440px)
    .home-hero__panel
      max-width: 980px
      margin-inline: auto
</style>

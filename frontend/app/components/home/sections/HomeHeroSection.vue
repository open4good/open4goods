<script setup lang="ts">
import { computed, nextTick, onMounted, ref, toRefs, watch } from 'vue'
import { useTheme } from 'vuetify'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import type { VerticalConfigDto } from '~~/shared/api-client'
import HomeHeroHighlights from '~/components/home/sections/HomeHeroHighlights.vue'

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
  searchQuery: string
  minSuggestionQueryLength: number
  verticals?: VerticalConfigDto[]
  heroImageLight?: string
  heroImageDark?: string
  partnersCount?: number
  openDataMillions?: number
  productsCount?: number
  categoriesCount?: number
  impactScoreProductsCount?: number
  impactScoreCategoriesCount?: number
  productsWithoutVerticalCount?: number
  reviewedProductsCount?: number
  aiSummaryRemainingCredits?: number
  heroBackgroundI18nKey?: string
  shouldReduceMotion?: boolean
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const theme = useTheme()
const heroBackgroundAsset = useHeroBackgroundAsset()
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)
const themeName = computed(() =>
  resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
)

const searchQueryValue = computed(() => props.searchQuery)

const { minSuggestionQueryLength } = toRefs(props)
const shouldReduceMotion = computed(() => Boolean(props.shouldReduceMotion))
const wizardVerticals = computed(() => props.verticals ?? [])
const heroRevealReady = ref(false)
const heroRevealVisible = ref(false)
const heroRevealClasses = computed(() => ({
  'is-ready': heroRevealReady.value,
  'is-visible': heroRevealVisible.value,
}))

const updateSearchQuery = (value: string) => {
  emit('update:searchQuery', value)
}

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

  /* Hydration mismatch fix: Ensure server and client initial render match. */
  /* We assume light theme as default for SSR unless reliable cookie sync is present. */
  // If we can't guarantee theme sync, we should force a default, then switch on mount.
  // Ideally, use a client-side only guard for the dynamic theme part or strictly match server.

  const isDarkMode = Boolean(theme.global.current.value.dark)
  const lightImage = props.heroImageLight?.trim()
  const darkImage = props.heroImageDark?.trim()

  return isDarkMode ? (darkImage ?? '') : (lightImage ?? '')
})

const heroTitle = computed(
  () =>
    packI18n.resolveString('hero.title', {
      fallbackKeys: ['home.hero.title'],
    }) ?? ''
)

const handleHeroImageLoad = () => {
  isHeroImageLoaded.value = true
}

onMounted(async () => {
  // Ensure the DOM is fully mounted and initial state (opacity: 0) is rendered
  await nextTick()

  window.setTimeout(() => {
    if (!isHeroImageLoaded.value) {
      isHeroImageLoaded.value = true
    }
  }, heroReadyFallbackDelayMs)

  if (shouldReduceMotion.value) {
    heroRevealReady.value = true
    heroRevealVisible.value = true
    return
  }

  // Set ready state (transition enabled, but still hidden)
  heroRevealReady.value = true

  // Wait for the next frame to ensure the browser registers the 'ready' state
  window.requestAnimationFrame(() => {
    // And another frame to trigger the transition
    window.requestAnimationFrame(() => {
      heroRevealVisible.value = true
    })
  })
})

watch(shouldReduceMotion, value => {
  if (value) {
    heroRevealReady.value = true
    heroRevealVisible.value = true
  }
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
      <div
        class="home-hero__inner home-reveal-group"
        :class="heroRevealClasses"
      >
        <v-row class="home-hero__layout" align="stretch" justify="center">
          <v-col
            cols="12"
            class="d-flex flex-column align-center text-center ga-4"
          >
            <h1
              id="home-hero-title"
              class="mt-8 home-hero__title home-reveal-item"
              :style="{ '--reveal-delay': '0ms' }"
            >
              {{ heroTitle }}
            </h1>
            <p
              v-if="heroTitleSubtitle"
              class="home-hero__title-subtitle home-reveal-item"
              :style="{ '--reveal-delay': '90ms' }"
            >
              {{ heroTitleSubtitle }}
            </p>
          </v-col>
        </v-row>
        <v-row justify="center">
          <v-col cols="12" lg="10" xl="8">
            <v-sheet
              class="home-hero__panel home-reveal-item home-reveal-item--scale"
              color="transparent"
              elevation="0"
              :style="{ '--reveal-delay': '180ms' }"
            >
              <div class="d-flex flex-column ga-6">
                <div class="d-flex flex-column ga-4">
                  <form
                    class="home-hero__search"
                    role="search"
                    @submit.prevent="handleSubmit"
                  >
                    <SearchSuggestField
                      :model-value="searchQueryValue"
                      class="home-hero__search-input"
                      :label="
                        packI18n.resolveString('hero.search.label', {
                          fallbackKeys: ['home.hero.search.label'],
                        })
                      "
                      :placeholder="
                        packI18n.resolveList<string>(
                          'hero.search.placeholders',
                          {
                            fallbackKeys: ['home.hero.search.placeholders'],
                          }
                        )
                      "
                      :aria-label="
                        packI18n.resolveString('hero.search.ariaLabel', {
                          fallbackKeys: ['home.hero.search.ariaLabel'],
                        })
                      "
                      :min-chars="minSuggestionQueryLength"
                      :enable-scan="true"
                      :scan-mobile="true"
                      :scan-desktop="true"
                      :enable-voice="true"
                      :voice-mobile="true"
                      :voice-desktop="true"
                      @update:model-value="updateSearchQuery"
                      @submit="handleSubmit"
                      @select-category="handleCategorySelect"
                      @select-product="handleProductSelect"
                    >
                      <template #append-inner>
                        <v-btn
                          class="home-hero__search-submit"
                          icon="mdi-arrow-right"
                          variant="plain"
                          rounded="0"
                          size="small"
                          type="submit"
                          :aria-label="
                            packI18n.resolveString('hero.search.cta', {
                              fallbackKeys: ['home.hero.search.cta'],
                            })
                          "
                        />
                      </template>
                    </SearchSuggestField>
                  </form>

                  <div class="d-flex flex-column ga-4">
                    <NudgeToolWizard :verticals="wizardVerticals" />
                  </div>

                  <HomeHeroHighlights
                    :partners-count="partnersCount"
                    :open-data-millions="openDataMillions"
                    :products-count="productsCount"
                    :categories-count="categoriesCount"
                    :impact-score-products-count="impactScoreProductsCount"
                    :impact-score-categories-count="impactScoreCategoriesCount"
                    :products-without-vertical-count="
                      productsWithoutVerticalCount
                    "
                    :reviewed-products-count="reviewedProductsCount"
                    :ai-summary-remaining-credits="aiSummaryRemainingCredits"
                  />
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

// .home-hero__content styles now handled by utility classes: d-flex flex-column align-center text-center ga-4

.home-hero__eyebrow
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-end), 0.9)
  margin: 0

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

.home-hero__title-animated-subtitle
  font-size: clamp(0.95rem, 2.2vw, 1.2rem)
  color: rgba(var(--v-theme-surface-default), 0.9)
  font-size: clamp(1.2rem, 5vw, 1.8rem)
  text-shadow: rgb(var(--v-theme-text-neutral-secondary)) 1px 0 10px

// .home-hero__search styles now handled by utility classes

.home-hero__search-input
  border-radius: clamp(1.5rem, 4vw, 2rem)
  background: rgba(var(--v-theme-surface-default), 0.85)
  box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.1)

.home-hero__search-submit
  box-shadow: none

.home-hero__panel
  background: rgb(var(--v-theme-surface-default))
  border-radius: clamp(1.5rem, 4vw, 2rem)
  box-shadow: 0 4px 12px rgba(var(--v-theme-shadow-primary-600), 0.05)
  padding: clamp(2rem, 5vw, 3rem)
  margin-block-start: 1rem
  width: 100%
  max-width: clamp(56rem, 82vw, 72rem)
  margin-inline: auto

// .home-hero__panel-grid and .home-hero__panel-block styles now handled by utility classes: d-flex flex-column ga-6, ga-4

.home-hero__wizard
  width: 100%

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

@media (min-width: 960px)
  .home-hero__panel-grid
    grid-template-columns: 1fr

  .home-hero__helper-row
    grid-template-columns: auto 1fr

  .home-hero__helpers
    margin-inline-start: 1.5rem

@media (min-width: 1280px)
  .home-hero__panel-grid
    grid-template-columns: 1fr

@media (min-width: 90rem)
  .home-hero__panel
    max-width: clamp(60rem, 78vw, 74rem)
</style>

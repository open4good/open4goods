<script setup lang="ts">
import { computed, onMounted, ref, toRefs, watch } from 'vue'
import { usePreferredReducedMotion } from '@vueuse/core'
import { useTheme } from 'vuetify'
import NudgeToolWizard from '~/components/nudge-tool/NudgeToolWizard.vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'
import type { VerticalConfigDto } from '~~/shared/api-client'
import RoundedCornerCard from '~/components/shared/cards/RoundedCornerCard.vue'
import { buildRevealStyle } from '~/utils/sectionReveal'

import {
  resolveThemedAssetUrl,
  useHeroBackgroundAsset,
} from '~~/app/composables/useThemedAsset'
import { useSeasonalEventPack } from '~~/app/composables/useSeasonalEventPack'
import {
  DEFAULT_EVENT_PACK,
  EVENT_PACK_I18N_BASE_KEY,
} from '~~/config/theme/event-packs'
import { useEventPackI18n } from '~/composables/useEventPackI18n'
import { THEME_ASSETS_FALLBACK } from '~~/config/theme/assets'
import { resolveThemeName } from '~~/shared/constants/theme'

type HeroHelperSegment = {
  text: string
  to?: string
}

type HeroHelperItem = {
  icon: string
  segments: HeroHelperSegment[]
}

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
  heroBackgroundI18nKey?: string
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t, te, tm, locale } = useI18n()
const theme = useTheme()
const preferredMotion = usePreferredReducedMotion()
const heroBackgroundAsset = useHeroBackgroundAsset()
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)
const themeName = computed(() =>
  resolveThemeName(theme.global.name.value, THEME_ASSETS_FALLBACK)
)

const partnersLinkPlaceholder = '{partnersLink}'
const openDataMillionsPlaceholder = '{millions}'
const productsCountPlaceholder = '{products}'
const categoriesCountPlaceholder = '{categories}'

const isHeroVisible = ref(false)
const shouldReduceMotion = computed(
  () => preferredMotion.value === 'reduce'
)

const searchQueryValue = computed(() => props.searchQuery)

const { minSuggestionQueryLength } = toRefs(props)
const wizardVerticals = computed(() => props.verticals ?? [])

const updateSearchQuery = (value: string) => {
  emit('update:searchQuery', value)
}

const normalizeHelperSegments = (segments: unknown): HeroHelperSegment[] => {
  if (!Array.isArray(segments)) {
    return []
  }

  return segments
    .map(segment => {
      if (typeof segment !== 'object' || segment == null) {
        return null
      }

      const { text, to } = segment as { text?: unknown; to?: unknown }
      const normalizedText = typeof text === 'string' ? text.trim() : ''
      const normalizedTo =
        typeof to === 'string' && to.trim().length > 0 ? to.trim() : undefined

      if (!normalizedText) {
        return null
      }

      return {
        text: normalizedText,
        to: normalizedTo,
      }
    })
    .filter((segment): segment is HeroHelperSegment => segment != null)
}

const normalizeHelperItems = (items: unknown): HeroHelperItem[] => {
  if (!Array.isArray(items)) {
    return []
  }

  return items
    .map(rawItem => {
      if (typeof rawItem !== 'object' || rawItem == null) {
        return null
      }

      const { icon, label, segments, labelParts } = rawItem as {
        icon?: unknown
        label?: unknown
        segments?: unknown
        labelParts?: unknown
      }

      const normalizedIcon =
        typeof icon === 'string' && icon.trim().length > 0 ? icon.trim() : '•'
      const normalizedSegments = normalizeHelperSegments(segments ?? labelParts)

      if (normalizedSegments.length === 0) {
        const normalizedLabel = typeof label === 'string' ? label.trim() : ''

        if (!normalizedLabel) {
          return null
        }

        normalizedSegments.push({ text: normalizedLabel })
      }

      return {
        icon: normalizedIcon,
        segments: normalizedSegments,
      }
    })
    .filter((item): item is HeroHelperItem => item != null)
}

const heroHelperItems = computed<HeroHelperItem[]>(() => {
  const packItems = packI18n.resolveList('hero.search.helpers', {
    fallbackKeys: ['home.hero.search.helpers'],
  })

  // Robust check for array/object
  const itemsToNormalize = Array.isArray(packItems)
    ? packItems
    : Object.values(packItems ?? {})

  const translatedItems = normalizeHelperItems(itemsToNormalize)

  if (translatedItems.length > 0) {
    return applyOpenDataMillionsPlaceholder(
      applyProductsCategoriesPlaceholder(
        applyPartnerLinkPlaceholder(translatedItems)
      )
    )
  }

  // Fallback to direct translation if pack resolution failed slightly
  const tmItems = tm('home.hero.search.helpers')
  if (Array.isArray(tmItems) && tmItems.length > 0) {
    const directTranslated = normalizeHelperItems(tmItems)
    if (directTranslated.length > 0) {
      return applyOpenDataMillionsPlaceholder(
        applyProductsCategoriesPlaceholder(
          applyPartnerLinkPlaceholder(directTranslated)
        )
      )
    }
  }

  return []
})

const heroTitleSubtitle = computed(
  () =>
    packI18n.resolveStringVariant('hero.titleSubtitle', {
      fallbackKeys: ['home.hero.titleSubtitle'],
      stateKey: 'home-hero-title-subtitle',
    }) ?? ''
)

const heroContextTitle = computed(
  () =>
    packI18n.resolveString('hero.context.title', {
      fallbackKeys: ['home.hero.context.title'],
    }) ?? ''
)

const normalizedPartnersCount = computed(() => {
  const rawCount = Number(props.partnersCount ?? 0)

  if (!Number.isFinite(rawCount)) {
    return 0
  }

  return Math.max(0, Math.round(rawCount))
})

const formattedPartnersCount = computed(() => {
  const count = normalizedPartnersCount.value

  if (!count) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(count)
  } catch {
    return String(count)
  }
})

const heroPartnersLinkText = computed(() => {
  const count = normalizedPartnersCount.value
  const formattedCount = formattedPartnersCount.value
  const fallbackLabel =
    packI18n
      .resolveString('hero.search.partnerLinkFallback', {
        fallbackKeys: ['home.hero.search.partnerLinkFallback'],
      })
      ?.trim() ?? ''

  if (!count || !formattedCount) {
    return fallbackLabel || null
  }

  const translateKey = (key: string) => {
    if (!te(key)) {
      return ''
    }

    const translated = t(key, { count, formattedCount })
    const normalized =
      typeof translated === 'string'
        ? translated.trim()
        : String(translated ?? '').trim()

    return normalized && normalized !== key ? normalized : ''
  }

  const packKey = `${EVENT_PACK_I18N_BASE_KEY}.${activeEventPack.value}.hero.search.partnerLinkLabel`
  const defaultKey = `${EVENT_PACK_I18N_BASE_KEY}.${DEFAULT_EVENT_PACK}.hero.search.partnerLinkLabel`

  const translated =
    translateKey(packKey) ||
    translateKey(defaultKey) ||
    translateKey('home.hero.search.partnerLinkLabel')

  const normalizedTranslation =
    typeof translated === 'string'
      ? translated.trim()
      : String(translated ?? '').trim()

  if (
    normalizedTranslation &&
    normalizedTranslation !== 'home.hero.search.partnerLinkLabel'
  ) {
    return normalizedTranslation
  }

  if (!fallbackLabel) {
    return formattedCount
  }

  return `${formattedCount} ${fallbackLabel}`
})

const formattedOpenDataMillions = computed(() => {
  const value = props.openDataMillions

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const formattedProductsCount = computed(() => {
  const value = props.productsCount

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const formattedCategoriesCount = computed(() => {
  const value = props.categoriesCount

  if (typeof value !== 'number' || !Number.isFinite(value) || value <= 0) {
    return null
  }

  try {
    return new Intl.NumberFormat(locale.value).format(value)
  } catch {
    return String(value)
  }
})

const applyPartnerLinkPlaceholder = (items: HeroHelperItem[]) => {
  const partnerLinkText = heroPartnersLinkText.value

  if (!partnerLinkText) {
    return items
  }

  return items.map(item => {
    const segmentsWithPartnerLink = item.segments
      .map(segment => {
        if (!segment.text.includes(partnersLinkPlaceholder)) {
          return segment
        }

        const replacedText = segment.text.replaceAll(
          partnersLinkPlaceholder,
          partnerLinkText
        )

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHelperSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithPartnerLink.length > 0
          ? segmentsWithPartnerLink
          : item.segments,
    }
  })
}

const applyOpenDataMillionsPlaceholder = (items: HeroHelperItem[]) => {
  const millionsLabel = formattedOpenDataMillions.value

  if (!millionsLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithOpenDataMillions = item.segments
      .map(segment => {
        if (!segment.text.includes(openDataMillionsPlaceholder)) {
          return segment
        }

        const replacedText = segment.text.replaceAll(
          openDataMillionsPlaceholder,
          millionsLabel
        )

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHelperSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithOpenDataMillions.length > 0
          ? segmentsWithOpenDataMillions
          : item.segments,
    }
  })
}

const applyProductsCategoriesPlaceholder = (items: HeroHelperItem[]) => {
  const productsLabel = formattedProductsCount.value
  const categoriesLabel = formattedCategoriesCount.value

  if (!productsLabel || !categoriesLabel) {
    return items
  }

  return items.map(item => {
    const segmentsWithCounts = item.segments
      .map(segment => {
        if (
          !segment.text.includes(productsCountPlaceholder) &&
          !segment.text.includes(categoriesCountPlaceholder)
        ) {
          return segment
        }

        const replacedText = segment.text
          .replaceAll(productsCountPlaceholder, productsLabel)
          .replaceAll(categoriesCountPlaceholder, categoriesLabel)

        const normalizedText = replacedText.trim()

        if (!normalizedText) {
          return null
        }

        return {
          ...segment,
          text: normalizedText,
        }
      })
      .filter((segment): segment is HeroHelperSegment => segment != null)

    return {
      ...item,
      segments:
        segmentsWithCounts.length > 0 ? segmentsWithCounts : item.segments,
    }
  })
}

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

const revealHero = () => {
  if (isHeroVisible.value) {
    return
  }

  if (shouldReduceMotion.value) {
    isHeroVisible.value = true
    return
  }

  requestAnimationFrame(() => {
    isHeroVisible.value = true
  })
}

watch(shouldReduceMotion, reduceMotion => {
  if (reduceMotion) {
    isHeroVisible.value = true
  }
})

onMounted(() => {
  revealHero()
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
        <v-row class="home-hero__layout" align="stretch" justify="center">
          <v-col cols="12" class="home-hero__content">
            <h1
              id="home-hero-title"
              class="mt-8 home-hero__title home-hero__reveal-item"
              :class="{ 'home-hero__reveal-item--visible': isHeroVisible }"
              :style="buildRevealStyle(0)"
            >
              {{ heroTitle }}
            </h1>
            <p
              v-if="heroTitleSubtitle"
              class="home-hero__title-subtitle home-hero__reveal-item"
              :class="{ 'home-hero__reveal-item--visible': isHeroVisible }"
              :style="buildRevealStyle(1)"
            >
              {{ heroTitleSubtitle }}
            </p>
          </v-col>
        </v-row>
        <v-row justify="center">
          <v-col cols="12" lg="10" xl="8">
            <v-sheet class="home-hero__panel" color="transparent" elevation="0">
              <div class="home-hero__panel-grid">
                <div
                  class="home-hero__panel-block home-hero__reveal-item"
                  :class="{ 'home-hero__reveal-item--visible': isHeroVisible }"
                  :style="buildRevealStyle(2)"
                >
                  <NudgeToolWizard :verticals="wizardVerticals" />
                </div>
                <div
                  class="home-hero__panel-block home-hero__reveal-item"
                  :class="{ 'home-hero__reveal-item--visible': isHeroVisible }"
                  :style="buildRevealStyle(3)"
                >
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
                        packI18n.resolveString('hero.search.placeholder', {
                          fallbackKeys: ['home.hero.search.placeholder'],
                        })
                      "
                      :aria-label="
                        packI18n.resolveString('hero.search.ariaLabel', {
                          fallbackKeys: ['home.hero.search.ariaLabel'],
                        })
                      "
                      :min-chars="minSuggestionQueryLength"
                      :enable-scan="true"
                      :scan-mobile="true"
                      :scan-desktop="false"
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
                          class="home-hero__search-submit nudger_degrade-defaut"
                          icon="mdi-arrow-right"
                          variant="flat"
                          color="primary"
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
                  <RoundedCornerCard
                    class="home-hero__context-card"
                    surface="strong"
                    accent-corner="bottom-right"
                    corner-variant="none"
                    corner-size="lg"
                    rounded="lg"
                    :selectable="false"
                    :elevation="10"
                    :hover-elevation="14"
                    :aria-label="
                      packI18n.resolveString('hero.context.ariaLabel', {
                        fallbackKeys: ['home.hero.context.ariaLabel'],
                      })
                    "
                  >
                    <div
                      class="home-hero__context home-hero__reveal-item"
                      :class="{ 'home-hero__reveal-item--visible': isHeroVisible }"
                      :style="buildRevealStyle(4)"
                    >
                      <p class="home-hero__subtitle text-center">
                        {{ heroContextTitle }}
                      </p>

                      <v-row class="home-hero__helper-row" justify="center">
                        <v-col
                          v-if="heroHelperItems.length"
                          cols="12"
                          class="home-hero__helpers-wrapper"
                        >
                          <ul class="home-hero__helpers">
                            <li
                              v-for="(item, index) in heroHelperItems"
                              :key="`hero-helper-${index}`"
                              class="home-hero__helper home-hero__reveal-item"
                              :class="{
                                'home-hero__reveal-item--visible': isHeroVisible,
                              }"
                              :style="buildRevealStyle(index + 5)"
                            >
                              <span
                                class="home-hero__helper-icon"
                                aria-hidden="true"
                                >{{ item.icon }}</span
                              >
                              <span class="home-hero__helper-text">
                                <template
                                  v-for="(
                                    segment, segmentIndex
                                  ) in item.segments"
                                  :key="`hero-helper-segment-${index}-${segmentIndex}`"
                                >
                                  <NuxtLink
                                    v-if="segment.to"
                                    class="home-hero__helper-link"
                                    :to="segment.to"
                                  >
                                    {{
                                      segmentIndex > 0
                                        ? ` ${segment.text}`
                                        : segment.text
                                    }}
                                  </NuxtLink>
                                  <span v-else>
                                    {{
                                      segmentIndex > 0
                                        ? ` ${segment.text}`
                                        : segment.text
                                    }}
                                  </span>
                                </template>
                              </span>
                            </li>
                          </ul>
                        </v-col>
                      </v-row>
                    </div>
                  </RoundedCornerCard>
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

.home-hero__content
  display: flex
  flex-direction: column
  gap: 1.5rem
  align-items: center
  text-align: center

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
  background: rgb(var(--v-theme-surface-default))
  border-radius: clamp(1.5rem, 4vw, 2rem)
  box-shadow: 0 4px 12px rgba(var(--v-theme-shadow-primary-600), 0.05)
  padding: clamp(2rem, 5vw, 3rem)
  margin-block-start: 1rem
  width: 100%
  max-width: clamp(56rem, 82vw, 72rem)
  margin-inline: auto

.home-hero__panel-grid
  display: grid
  gap: clamp(1.5rem, 4vw, 2.5rem)
  grid-template-columns: 1fr

.home-hero__panel-block
  display: flex
  flex-direction: column
  gap: clamp(1.25rem, 2vw, 1.75rem)
  min-width: 0

.home-hero__reveal-item
  opacity: 0
  transform: translateY(18px) scale(0.985)
  transition: opacity 0.55s cubic-bezier(0.16, 1, 0.3, 1), transform 0.55s cubic-bezier(0.16, 1, 0.3, 1)
  transition-delay: var(--reveal-delay, 0ms)
  will-change: opacity, transform

.home-hero__reveal-item--visible
  opacity: 1
  transform: translateY(0) scale(1)

.home-hero__context
  flex-direction: column
  gap: 0.75rem

.home-hero__context-card
  height: 100%

.home-hero__helper-row
  display: flex
  flex-wrap: wrap
  align-items: center
  width: 100%

.home-hero__helpers-wrapper
  width: 100%

.home-hero__helpers
  margin: 0
  padding: 0
  display: grid
  gap: clamp(0.4rem, 1.5vw, 0.65rem)
  list-style: none
  width: 100%

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

.home-hero__helper-link
  color: rgb(var(--v-theme-text-neutral-strong))
  font-weight: 600
  text-decoration: underline
  text-decoration-thickness: 0.08em
  text-underline-offset: 0.1em

.home-hero__context
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-hero__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-strong))
  text-align: center
  width: 100%


.home-hero__helpers-title
  font-size: 0.875rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin-bottom: 0.5rem
  display: block
  letter-spacing: 0.01em

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

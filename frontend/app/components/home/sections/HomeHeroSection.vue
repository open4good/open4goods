<script setup lang="ts">
import { computed, toRefs } from 'vue'
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

const props = defineProps<{
  searchQuery: string
  minSuggestionQueryLength: number
  verticals?: VerticalConfigDto[]
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t, tm } = useI18n()

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
      <v-img
        class="home-hero__background-image"
        src="/images/home/home-hero_background.webp"
        alt=""
        cover
      />
    </div>
    <v-container fluid class="home-hero__container">
      <div class="home-hero__inner">
        <v-row class="home-hero__layout" align="stretch" justify="center">
          <v-col cols="12" class="home-hero__content">
            <h1 id="home-hero-title" class="home-hero__title">
              {{ t('home.hero.title') }}
            </h1>
          </v-col>
        </v-row>
        <v-row justify="center">
          <v-col cols="12" lg="6" class="home-hero__wizard">
            <NudgeToolWizard :verticals="wizardVerticals" />
          </v-col>
        </v-row>


        <v-row justify="center">
         <v-col cols="12" lg="6" class="home-hero__content">
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
          </v-col>
        </v-row>

        <v-row justify="center">
          <v-col cols="12" lg="6" class="home-hero__content">
            <div class="card__nudger">
              <p class="home-hero__subtitle">{{ t('home.hero.subtitle') }}</p>

              <v-row class="mt-5" align="center">
                <v-col cols="12" lg="4">
                  <p class="mt-4 ms-6 home-hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
                </v-col>
                <v-col cols="12" lg="8">
                  <ul v-if="heroHelperItems.length" class="ms-8 home-hero__helpers">
                    <li v-for="(item, index) in heroHelperItems" :key="`hero-helper-${index}`" class="home-hero__helper">
                      <span class="home-hero__helper-icon" aria-hidden="true">{{ item.icon }}</span>
                      <span class="home-hero__helper-text">{{ item.label }}</span>
                    </li>
                  </ul>
                </v-col>
              </v-row>
            </div>
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
    min-height: clamp(520px, 70dvh, 840px)
    padding-block: clamp(2.5rem, 7vw, 4.5rem)

  .home-hero__background
    position: absolute
    inset: 0
    z-index: 0
    pointer-events: none

  .home-hero__background-image
    height: 100%
    width: 100%
    opacity: 0.95

  .home-hero__background :deep(img)
    object-fit: cover

  .home-hero__background::after
    content: ''
    position: absolute
    inset: 0
    background: radial-gradient(
        circle at 20% 20%,
        rgba(var(--v-theme-hero-gradient-start), 0.18),
        transparent 35%
      ), radial-gradient(
        circle at 80% 10%,
        rgba(var(--v-theme-hero-gradient-end), 0.16),
        transparent 40%
      ), linear-gradient(
        180deg,
        rgba(var(--v-theme-surface-default), 0) 0%,
        rgba(var(--v-theme-surface-default), 0.15) 40%,
        rgba(var(--v-theme-surface-default), 0.65) 100%
      )
    pointer-events: none

  .home-hero__container
    padding-inline: clamp(1.5rem, 5vw, 4rem)
    position: relative
    z-index: 1

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
    text-align: center
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

  @media (max-width: 959px)
    .home-hero
      min-height: clamp(480px, 68dvh, 720px)
      padding-block: clamp(2rem, 10vw, 3.75rem)

    .home-hero__media
      order: 1

    .home-hero__content
      order: 2

    .home-hero__video-wrapper
      aspect-ratio: 4 / 5
      min-height: 320px

    .home-hero__video
      transform: scale(1.15)

</style>

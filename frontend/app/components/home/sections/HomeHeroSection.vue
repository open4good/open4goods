<script setup lang="ts">
import { computed, toRefs } from 'vue'
import HomeCategoryCarousel from '~/components/home/HomeCategoryCarousel.vue'
import SearchSuggestField, {
  type CategorySuggestionItem,
  type ProductSuggestionItem,
} from '~/components/search/SearchSuggestField.vue'

type CategoryCarouselItem = {
  id: string
  title: string
  href: string
  image?: string | null
  impactScoreHref: string
}

const props = defineProps<{
  searchQuery: string
  minSuggestionQueryLength: number
  heroVideoSrc: string
  heroVideoPoster: string
  categoryItems: CategoryCarouselItem[]
  categoriesLoading: boolean
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t } = useI18n()

const searchQueryValue = computed(() => props.searchQuery)

const { minSuggestionQueryLength, heroVideoSrc, heroVideoPoster, categoryItems, categoriesLoading } =
  toRefs(props)

const updateSearchQuery = (value: string) => {
  emit('update:searchQuery', value)
}

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
  <section class="home-hero" aria-labelledby="home-hero-title">
    <v-container fluid class="home-hero__container">
      <div class="home-hero__inner">
        <v-row class="home-hero__layout" align="stretch" justify="center">
          <v-col cols="12" lg="6" class="home-hero__content">
            <p class="home-hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
            <h1 id="home-hero-title" class="home-hero__title">
              {{ t('home.hero.title') }}
            </h1>
            <p class="home-hero__subtitle">{{ t('home.hero.subtitle') }}</p>

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
                    class="home-hero__search-submit"
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

            <p class="home-hero__helper">
              <span aria-hidden="true">⚡</span>
              {{ t('home.hero.search.helper') }}
            </p>
          </v-col>

          <v-col cols="12" lg="6" class="home-hero__media" aria-hidden="true">
            <v-sheet rounded="xl" elevation="8" class="home-hero__media-sheet">
              <div class="home-hero__video-wrapper">
                <video
                  class="home-hero__video"
                  :poster="heroVideoPoster"
                  autoplay
                  muted
                  loop
                  playsinline
                  preload="metadata"
                >
                  <source :src="heroVideoSrc" type="video/mp4" />
                </video>
                <div class="home-hero__video-overlay" />
              </div>
            </v-sheet>
          </v-col>
        </v-row>
      </div>
    </v-container>

    <div class="home-hero__categories">
      <v-container fluid class="home-hero__categories-container">
        <div class="home-hero__categories-inner">
          <HomeCategoryCarousel :items="categoryItems" :loading="categoriesLoading" />
        </div>
      </v-container>
    </div>
  </section>
</template>

<style scoped lang="sass">
.home-hero
  position: relative
  padding-block: clamp(4rem, 12vw, 8rem) clamp(6rem, 16vw, 11rem)
  background: radial-gradient(circle at top left, rgba(var(--v-theme-hero-gradient-start), 0.28), transparent 55%), radial-gradient(circle at bottom right, rgba(var(--v-theme-hero-gradient-end), 0.25), transparent 60%), rgb(var(--v-theme-surface-default))
  overflow: hidden

.home-hero__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-hero__inner
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

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

.home-hero__subtitle
  font-size: clamp(1.05rem, 2.6vw, 1.35rem)
  color: rgb(var(--v-theme-text-neutral-secondary))
  margin: 0

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

.home-hero__helper
  display: inline-flex
  align-items: center
  gap: 0.5rem
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-weight: 500

.home-hero__media
  display: flex
  align-items: center
  justify-content: center

.home-hero__media-sheet
  width: 100%
  padding: clamp(0.5rem, 2vw, 1rem)
  background: rgba(var(--v-theme-surface-glass), 0.85)
  overflow: hidden

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

.home-hero__categories
  position: relative
  z-index: 3
  margin-top: clamp(-5.25rem, -12vw, -6.25rem)

.home-hero__categories-container
  padding-inline: clamp(1rem, 5vw, 4rem)

.home-hero__categories-inner
  position: relative
  max-width: 1180px
  margin: 0 auto
  padding: clamp(0.75rem, 3vw, 1.5rem) clamp(1rem, 4vw, 2.5rem)
  border-radius: clamp(1.75rem, 4vw, 2.25rem)
  background: linear-gradient(
    180deg,
    rgba(var(--v-theme-hero-gradient-start), 0.2) 0%,
    rgba(var(--v-theme-surface-default), 0.96) 60%,
    rgb(var(--v-theme-surface-muted)) 100%
  )
  box-shadow: 0 22px 38px rgba(var(--v-theme-shadow-primary-600), 0.16)
  display: flex
  align-items: center

.home-hero__categories-inner::before
  content: ''
  position: absolute
  inset: 0
  border-radius: inherit
  background: linear-gradient(
    120deg,
    rgba(var(--v-theme-hero-gradient-start), 0.1) 0%,
    rgba(var(--v-theme-hero-gradient-end), 0.12) 45%,
    rgba(var(--v-theme-surface-default), 0) 100%
  )
  pointer-events: none
  mix-blend-mode: screen

.home-hero__categories-inner > *
  position: relative
  z-index: 1

@media (max-width: 959px)
  .home-hero
    padding-block: clamp(3.5rem, 12vw, 6rem) clamp(8rem, 20vw, 10rem)

  .home-hero__video
    transform: scale(1.15)

  .home-hero__categories
    margin-top: clamp(-4.5rem, -20vw, -5.5rem)

  .home-hero__categories-inner
    padding: clamp(0.5rem, 5vw, 1rem) clamp(0.75rem, 5vw, 1.5rem)
</style>

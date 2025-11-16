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

type HeroHelperItem = {
  icon: string
  label: string
}

const props = defineProps<{
  searchQuery: string
  minSuggestionQueryLength: number
  categoryItems: CategoryCarouselItem[]
  categoriesLoading: boolean
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  submit: []
  'select-category': [payload: CategorySuggestionItem]
  'select-product': [payload: ProductSuggestionItem]
}>()

const { t, tm } = useI18n()

const { minSuggestionQueryLength, categoryItems, categoriesLoading } = toRefs(props)

const searchQueryValue = computed(() => props.searchQuery)

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
  <HeroSurface tag="section" class="home-hero-fullscreen" aria-labelledby="home-hero-fullscreen-title" :bleed="true">
    <div class="home-hero-fullscreen__parallax" aria-hidden="true">
      <div class="home-hero-fullscreen__glow home-hero-fullscreen__glow--primary" />
      <div class="home-hero-fullscreen__glow home-hero-fullscreen__glow--secondary" />
      <div class="home-hero-fullscreen__grid" />
    </div>

    <v-container fluid class="home-hero-fullscreen__container">
      <div class="home-hero-fullscreen__inner">
        <v-row class="home-hero-fullscreen__layout" justify="center" align="center">
          <v-col cols="12" lg="10" xl="8" class="home-hero-fullscreen__content">
            <p class="home-hero-fullscreen__eyebrow">{{ t('home.hero.eyebrow') }}</p>
            <h1 id="home-hero-fullscreen-title" class="home-hero-fullscreen__title">
              {{ t('home.hero.title') }}
            </h1>
            <p class="home-hero-fullscreen__subtitle">{{ t('home.hero.subtitle') }}</p>

            <form class="home-hero-fullscreen__search" role="search" @submit.prevent="handleSubmit">
              <SearchSuggestField
                :model-value="searchQueryValue"
                class="home-hero-fullscreen__search-input"
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
                    class="home-hero-fullscreen__search-submit"
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

            <ul v-if="heroHelperItems.length" class="home-hero-fullscreen__helpers">
              <li
                v-for="(item, index) in heroHelperItems"
                :key="`hero-fullscreen-helper-${index}`"
                class="home-hero-fullscreen__helper"
              >
                <span class="home-hero-fullscreen__helper-icon" aria-hidden="true">{{ item.icon }}</span>
                <span class="home-hero-fullscreen__helper-text">{{ item.label }}</span>
              </li>
            </ul>
          </v-col>
        </v-row>
      </div>
    </v-container>

    <div class="home-hero-fullscreen__categories" :aria-label="t('home.categories.bannerAriaLabel')">
      <v-container fluid class="home-hero-fullscreen__categories-container">
        <div class="home-hero-fullscreen__categories-inner">
          <HomeCategoryCarousel :items="categoryItems" :loading="categoriesLoading" />
        </div>
      </v-container>
    </div>
  </HeroSurface>
</template>

<style scoped lang="sass">
.home-hero-fullscreen
  position: relative
  min-height: clamp(640px, 88dvh, 980px)
  padding-block: clamp(3.5rem, 10vw, 6rem)
  display: flex
  flex-direction: column
  justify-content: center
  overflow: hidden
  --hero-cat-h: var(--cat-height, 168px)
  --hero-cat-in-hero-base: calc(var(--hero-cat-h) / 2)
  --hero-cat-overlap-base: calc(var(--hero-cat-h) - var(--hero-cat-in-hero-base))
  --hero-cat-in-hero: var(--cat-in-hero, var(--hero-cat-in-hero-base))
  --hero-cat-overlap: var(--cat-overlap, var(--hero-cat-overlap-base))

.home-hero-fullscreen__parallax
  position: absolute
  inset: 0
  overflow: hidden
  pointer-events: none

.home-hero-fullscreen__glow
  position: absolute
  border-radius: 999px
  filter: blur(80px)
  opacity: 0.28
  transform: translate3d(0, 0, 0)
  animation: heroFloat 16s ease-in-out infinite alternate

.home-hero-fullscreen__glow--primary
  width: clamp(320px, 35vw, 520px)
  height: clamp(320px, 35vw, 520px)
  background: radial-gradient(circle, rgba(var(--v-theme-hero-gradient-start), 0.6), transparent 70%)
  top: 6%
  left: clamp(4%, 10vw, 12%)

.home-hero-fullscreen__glow--secondary
  width: clamp(280px, 32vw, 440px)
  height: clamp(280px, 32vw, 440px)
  background: radial-gradient(circle, rgba(var(--v-theme-hero-gradient-end), 0.55), transparent 70%)
  bottom: 8%
  right: clamp(4%, 10vw, 12%)
  animation-delay: 3s

.home-hero-fullscreen__grid
  position: absolute
  inset: -40%
  background: radial-gradient(circle at 30% 30%, rgba(var(--v-theme-hero-overlay-soft), 0.2), transparent 35%), radial-gradient(circle at 70% 60%, rgba(var(--v-theme-hero-overlay-strong), 0.16), transparent 30%), linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.08) 0%, rgba(var(--v-theme-hero-gradient-mid), 0.08) 40%, rgba(var(--v-theme-hero-gradient-end), 0.08) 100%)
  opacity: 0.6
  background-attachment: fixed
  mask: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(255, 255, 255, 0.4) 100%)
  transform: translate3d(0, 0, 0)

.home-hero-fullscreen__container
  position: relative
  z-index: 1
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-hero-fullscreen__inner
  margin: 0 auto
  max-width: 1200px
  display: flex
  flex-direction: column
  gap: clamp(1.75rem, 6vw, 2.5rem)
  align-items: center
  text-align: center

.home-hero-fullscreen__layout
  width: 100%

.home-hero-fullscreen__content
  display: flex
  flex-direction: column
  gap: clamp(1.25rem, 3vw, 1.75rem)
  align-items: center
  text-align: center

.home-hero-fullscreen__eyebrow
  margin: 0
  font-weight: 700
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-gradient-end), 0.95)

.home-hero-fullscreen__title
  margin: 0
  font-size: clamp(2.6rem, 6vw, 4rem)
  line-height: 1.05
  color: rgb(var(--v-theme-text-neutral-strong))

.home-hero-fullscreen__subtitle
  margin: 0
  max-width: min(900px, 90vw)
  font-size: clamp(1.1rem, 2.8vw, 1.35rem)
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-hero-fullscreen__search
  width: min(760px, 100%)
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-hero-fullscreen__search-input
  border-radius: clamp(1.4rem, 4vw, 1.9rem)
  background: rgba(var(--v-theme-surface-default), 0.82)
  box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.12)
  backdrop-filter: blur(8px)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.home-hero-fullscreen__search-submit
  box-shadow: none

.home-hero-fullscreen__helpers
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))
  gap: 0.6rem 1.25rem
  margin: 0
  padding: 0
  list-style: none
  width: min(820px, 100%)

.home-hero-fullscreen__helper
  display: inline-flex
  align-items: center
  gap: 0.5rem
  justify-content: center
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-weight: 500

.home-hero-fullscreen__helper-icon
  font-size: 1.1rem

.home-hero-fullscreen__helper-text
  line-height: 1.35

.home-hero-fullscreen__categories
  position: absolute
  inset-inline: 0
  bottom: calc(-1 * var(--hero-cat-overlap))
  display: flex
  justify-content: center
  z-index: 3
  pointer-events: none

.home-hero-fullscreen__categories-container
  width: 100%

.home-hero-fullscreen__categories-inner
  position: relative
  width: min(100%, 1200px)
  margin-inline: auto
  padding: clamp(0.4rem, 1.5vw, 0.8rem) clamp(0.75rem, 3vw, 1.5rem)
  border-radius: clamp(1.5rem, 4vw, 2rem)
  background: linear-gradient(
    180deg,
    rgba(var(--v-theme-hero-gradient-start), 0.18) 0%,
    rgba(var(--v-theme-surface-default), 0.96) 60%,
    rgb(var(--v-theme-surface-muted)) 100%
  )
  box-shadow: 0 22px 38px rgba(var(--v-theme-shadow-primary-600), 0.16)
  display: flex
  align-items: center
  justify-content: center
  pointer-events: auto
  height: var(--hero-cat-h)

.home-hero-fullscreen__categories-inner :deep(.home-category-carousel)
  height: 100%
  display: flex

.home-hero-fullscreen__categories-inner::before
  content: ''
  position: absolute
  inset: 0
  border-radius: inherit
  background: linear-gradient(
    120deg,
    rgba(var(--v-theme-hero-gradient-start), 0.16) 0%,
    rgba(var(--v-theme-hero-gradient-end), 0.18) 45%,
    rgba(var(--v-theme-surface-default), 0) 100%
  )
  pointer-events: none
  mix-blend-mode: screen

.home-hero-fullscreen__categories-inner > *
  position: relative
  z-index: 1

@media (max-width: 959px)
  .home-hero-fullscreen
    min-height: clamp(560px, 90dvh, 820px)
    padding-block: clamp(3rem, 18vw, 4.5rem)
    --hero-cat-h: var(--cat-height, 160px)
    --hero-cat-in-hero-base: calc(var(--hero-cat-h) / 2)

  .home-hero-fullscreen__helpers
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr))

@keyframes heroFloat
  0%
    transform: translate3d(0, 0, 0)
  100%
    transform: translate3d(0, 28px, 0)
</style>

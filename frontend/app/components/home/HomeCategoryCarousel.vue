<script setup lang="ts">
import { computed } from 'vue'
import { useDisplay } from 'vuetify'

interface CategorySlideItem {
  id: string
  title: string
  href: string
  image?: string | null
  impactScoreHref: string
}

const props = defineProps<{
  items: CategorySlideItem[]
  loading?: boolean
}>()

const { t } = useI18n()
const display = useDisplay()

const slidesPerView = computed(() => {
  if (display.lgAndUp.value) {
    return 3
  }

  if (display.mdAndUp.value) {
    return 2
  }

  return 1
})

const chunkItems = <T,>(input: readonly T[], size: number) => {
  if (size <= 0) {
    return [input.slice()]
  }

  const chunks: T[][] = []
  for (let index = 0; index < input.length; index += size) {
    chunks.push(input.slice(index, index + size))
  }

  return chunks
}

const chunkedItems = computed(() => chunkItems(props.items, slidesPerView.value))

const shouldShowArrows = computed(() => chunkedItems.value.length > 1)
const shouldCycle = computed(() => props.items.length > slidesPerView.value)

const isExternal = (url: string) => /^https?:\/\//i.test(url)
</script>

<template>
  <div class="home-category-carousel">
    <v-skeleton-loader
      v-if="loading"
      type="heading, image, paragraph"
      class="home-category-carousel__skeleton"
    />
    <v-alert
      v-else-if="!items.length"
      type="info"
      variant="tonal"
      class="home-category-carousel__empty"
      border="start"
    >
      {{ t('home.categories.emptyState') }}
    </v-alert>
    <v-carousel
      v-else
      class="home-category-carousel__carousel"
      :show-arrows="shouldShowArrows"
      :cycle="shouldCycle"
      :interval="7000"
      hide-delimiter-background
      height="auto"
      :aria-label="t('home.categories.carouselAriaLabel')"
    >
      <v-carousel-item
        v-for="(slide, index) in chunkedItems"
        :key="`category-slide-${index}`"
      >
        <v-row class="home-category-carousel__row" no-gutters>
          <v-col
            v-for="category in slide"
            :key="category.id"
            cols="12"
            md="6"
            lg="4"
          >
            <NuxtLink :to="category.href" class="home-category-carousel__link">
              <v-card
                class="home-category-carousel__card"
                variant="elevated"
                elevation="6"
              >
                <div class="home-category-carousel__media">
                  <v-img
                    v-if="typeof category.image === 'string' && category.image.length > 0"
                    :src="category.image"
                    :alt="category.title"
                    contain
                    :aspect-ratio="16 / 10"
                    class="home-category-carousel__image"
                  />
                  <div v-else class="home-category-carousel__icon" aria-hidden="true">
                    <v-icon icon="mdi-shape-outline" size="56" />
                  </div>
                </div>
                <div class="home-category-carousel__content">
                  <h3 class="home-category-carousel__title">{{ category.title }}</h3>
                  <span class="home-category-carousel__cta">
                    {{ t('home.categories.cta') }}
                  </span>
                  <component
                    :is="isExternal(category.impactScoreHref) ? 'a' : 'NuxtLink'"
                    v-if="category.impactScoreHref"
                    class="home-category-carousel__impact-link"
                    :href="isExternal(category.impactScoreHref) ? category.impactScoreHref : undefined"
                    :to="!isExternal(category.impactScoreHref) ? category.impactScoreHref : undefined"
                    :target="isExternal(category.impactScoreHref) ? '_blank' : undefined"
                    :rel="isExternal(category.impactScoreHref) ? 'noopener' : undefined"
                  >
                    {{ t('home.categories.impactLink') }}
                  </component>
                </div>
              </v-card>
            </NuxtLink>
          </v-col>
        </v-row>
      </v-carousel-item>
    </v-carousel>
  </div>
</template>

<style scoped lang="sass">
.home-category-carousel
  position: relative

  &__skeleton
    border-radius: 1.5rem
    overflow: hidden

  &__empty
    border-radius: 1.5rem

  &__carousel
    border-radius: clamp(1.5rem, 4vw, 2rem)
    overflow: hidden
    background: transparent

  &__row
    gap: 1.5rem
    padding: clamp(1.5rem, 4vw, 2rem)

  &__link
    text-decoration: none
    display: block
    height: 100%

  &__card
    height: 100%
    display: flex
    flex-direction: column
    border-radius: clamp(1.25rem, 3vw, 1.75rem)
    background: rgba(var(--v-theme-surface-default), 0.92)
    transition: transform 0.25s ease, box-shadow 0.25s ease

    &:hover
      transform: translateY(-6px)
      box-shadow: 0 24px 36px rgba(var(--v-theme-shadow-primary-600), 0.18)

  &__media
    position: relative
    overflow: hidden
    border-radius: clamp(1.25rem, 3vw, 1.75rem) clamp(1.25rem, 3vw, 1.75rem) 0 0
    display: flex
    align-items: center
    justify-content: center
    background: rgba(var(--v-theme-surface-primary-050), 0.85)
    aspect-ratio: 16 / 10
    padding: clamp(0.75rem, 2vw, 1.25rem)

  &__image
    width: 100%
    height: 100%
    object-fit: contain

  &__icon
    display: flex
    align-items: center
    justify-content: center
    width: 100%
    height: 100%
    background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.15), rgba(var(--v-theme-hero-gradient-end), 0.15))
    color: rgba(var(--v-theme-hero-gradient-start), 0.95)

  &__content
    display: flex
    flex-direction: column
    gap: 0.5rem
    padding: clamp(1.25rem, 3vw, 1.75rem)

  &__title
    margin: 0
    font-size: clamp(1.15rem, 2vw, 1.35rem)
    color: rgb(var(--v-theme-text-neutral-strong))

  &__cta
    display: inline-flex
    align-items: center
    gap: 0.35rem
    font-weight: 600
    color: rgba(var(--v-theme-hero-gradient-end), 0.95)

  &__impact-link
    margin-top: 0.25rem
    font-weight: 500
    font-size: 0.95rem
    color: rgba(var(--v-theme-hero-gradient-start), 0.9)
    text-decoration: none

    &:hover
      text-decoration: underline
</style>

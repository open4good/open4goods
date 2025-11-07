<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

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
const isExternal = (url: string) => /^https?:\/\//i.test(url)

const scrollerRef = ref<HTMLElement | null>(null)
const canScrollPrev = ref(false)
const canScrollNext = ref(false)

const detachScrollListener = () => {
  const container = scrollerRef.value
  container?.removeEventListener('scroll', updateScrollIndicators)
}

const attachScrollListener = () => {
  detachScrollListener()

  const container = scrollerRef.value
  if (!container) {
    return
  }

  container.addEventListener('scroll', updateScrollIndicators, { passive: true })
}

const updateScrollIndicators = () => {
  const container = scrollerRef.value

  if (!container) {
    canScrollPrev.value = false
    canScrollNext.value = false
    return
  }

  const tolerance = 4
  const { scrollLeft, scrollWidth, clientWidth } = container
  const maxScrollLeft = Math.max(scrollWidth - clientWidth, 0)

  canScrollPrev.value = scrollLeft > tolerance
  canScrollNext.value = scrollLeft < maxScrollLeft - tolerance
}

const getScrollAmount = () => {
  const container = scrollerRef.value

  if (!container) {
    return 280
  }

  return Math.max(container.clientWidth * 0.7, 240)
}

const scrollToPrevious = () => {
  const container = scrollerRef.value

  if (!container) {
    return
  }

  container.scrollBy({ left: -getScrollAmount(), behavior: 'smooth' })
}

const scrollToNext = () => {
  const container = scrollerRef.value

  if (!container) {
    return
  }

  container.scrollBy({ left: getScrollAmount(), behavior: 'smooth' })
}

const handleResize = () => {
  updateScrollIndicators()
}

if (import.meta.client) {
  onMounted(() => {
    nextTick(() => {
      attachScrollListener()
      updateScrollIndicators()
    })

    window.addEventListener('resize', handleResize, { passive: true })
  })

  onBeforeUnmount(() => {
    detachScrollListener()
    window.removeEventListener('resize', handleResize)
  })
}

watch(
  () => props.items,
  () => {
    if (!import.meta.client) {
      return
    }

    nextTick(() => {
      attachScrollListener()

      const container = scrollerRef.value
      if (container) {
        const { scrollLeft, scrollWidth, clientWidth } = container
        const maxScrollLeft = Math.max(scrollWidth - clientWidth, 0)

        if (scrollLeft > maxScrollLeft) {
          container.scrollTo({ left: maxScrollLeft })
        }
      }

      updateScrollIndicators()
    })
  },
  { deep: true },
)

const shouldShowNavigation = computed(() => props.items.length > 1)
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
    <nav
      v-else
      class="home-category-carousel__banner"
      :class="{ 'home-category-carousel__banner--with-controls': shouldShowNavigation }"
      :aria-label="t('home.categories.bannerAriaLabel')"
    >
      <v-btn
        v-if="shouldShowNavigation"
        class="home-category-carousel__nav home-category-carousel__nav--prev"
        icon="mdi-chevron-left"
        variant="tonal"
        size="small"
        color="primary"
        :disabled="!canScrollPrev"
        :aria-label="t('home.categories.scrollPrevious')"
        @click="scrollToPrevious"
      />

      <div
        ref="scrollerRef"
        class="home-category-carousel__scroller"
        role="list"
      >
        <div
          v-for="category in items"
          :key="category.id"
          class="home-category-carousel__item"
          role="listitem"
        >
          <NuxtLink :to="category.href" class="home-category-carousel__link">
            <div class="home-category-carousel__avatar" aria-hidden="true">
              <v-img
                v-if="typeof category.image === 'string' && category.image.length > 0"
                :src="category.image"
                :alt="''"
                role="presentation"
                class="home-category-carousel__image"
              />
              <div v-else class="home-category-carousel__placeholder">
                <v-icon icon="mdi-shape-outline" size="28" />
              </div>
            </div>
            <span class="home-category-carousel__label">{{ category.title }}</span>
          </NuxtLink>

          <component
            :is="isExternal(category.impactScoreHref) ? 'a' : 'NuxtLink'"
            v-if="category.impactScoreHref"
            class="home-category-carousel__impact-link"
            :href="isExternal(category.impactScoreHref) ? category.impactScoreHref : undefined"
            :to="!isExternal(category.impactScoreHref) ? category.impactScoreHref : undefined"
            :target="isExternal(category.impactScoreHref) ? '_blank' : undefined"
            :rel="isExternal(category.impactScoreHref) ? 'noopener' : undefined"
            :aria-label="t('home.categories.impactLinkAria', { category: category.title })"
          >
            <v-icon icon="mdi-leaf-circle" size="20" />
          </component>
        </div>
      </div>

      <v-btn
        v-if="shouldShowNavigation"
        class="home-category-carousel__nav home-category-carousel__nav--next"
        icon="mdi-chevron-right"
        variant="tonal"
        size="small"
        color="primary"
        :disabled="!canScrollNext"
        :aria-label="t('home.categories.scrollNext')"
        @click="scrollToNext"
      />
    </nav>
  </div>
</template>

<style scoped lang="sass">
.home-category-carousel
  position: relative

  &__skeleton
    border-radius: clamp(1.5rem, 4vw, 2rem)
    overflow: hidden

  &__empty
    border-radius: clamp(1.5rem, 4vw, 2rem)

  &__banner
    width: 100%
    display: grid
    grid-template-columns: minmax(0, 1fr)
    align-items: center
    gap: clamp(0.5rem, 2vw, 1rem)

  &__banner--with-controls
    grid-template-columns: auto 1fr auto

  &__nav
    border-radius: 999px
    box-shadow: 0 12px 20px rgba(var(--v-theme-shadow-primary-600), 0.12)
    backdrop-filter: blur(8px)

    &--prev,
    &--next
      min-width: 2.5rem
      min-height: 2.5rem

    &:disabled
      opacity: 0.4
      box-shadow: none

  &__scroller
    position: relative
    display: flex
    align-items: stretch
    gap: clamp(0.75rem, 2vw, 1.25rem)
    overflow-x: auto
    overflow-y: hidden
    padding: clamp(0.25rem, 1vw, 0.5rem) clamp(0.25rem, 1vw, 0.5rem)
    scroll-snap-type: x proximity
    scrollbar-width: none
    -webkit-overflow-scrolling: touch
    mask-image: linear-gradient(90deg, transparent 0%, black 6%, black 94%, transparent 100%)

    &::-webkit-scrollbar
      display: none

  &__item
    display: inline-flex
    align-items: center
    gap: clamp(0.5rem, 2vw, 0.75rem)
    scroll-snap-align: center

  &__link
    display: inline-flex
    align-items: center
    gap: clamp(0.5rem, 2vw, 0.75rem)
    padding: clamp(0.5rem, 1.8vw, 0.75rem) clamp(0.85rem, 2.8vw, 1.25rem)
    border-radius: 999px
    text-decoration: none
    background: rgba(var(--v-theme-surface-default), 0.92)
    box-shadow: 0 12px 22px rgba(var(--v-theme-shadow-primary-600), 0.14)
    color: rgb(var(--v-theme-text-neutral-strong))
    transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease

    &:hover,
    &:focus-visible
      transform: translateY(-2px)
      box-shadow: 0 18px 28px rgba(var(--v-theme-shadow-primary-600), 0.18)
      background: rgba(var(--v-theme-surface-default), 1)

  &__avatar
    width: clamp(2.5rem, 6vw, 3rem)
    height: clamp(2.5rem, 6vw, 3rem)
    border-radius: 50%
    background: rgba(var(--v-theme-surface-primary-080), 0.9)
    display: inline-flex
    align-items: center
    justify-content: center
    overflow: hidden
    box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.2)

  &__image
    width: 100%
    height: 100%
    object-fit: cover

  &__placeholder
    display: flex
    align-items: center
    justify-content: center
    width: 100%
    height: 100%
    background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.18), rgba(var(--v-theme-hero-gradient-end), 0.16))
    color: rgba(var(--v-theme-hero-gradient-start), 0.92)

  &__label
    font-weight: 600
    font-size: clamp(0.95rem, 2.2vw, 1.05rem)
    white-space: nowrap

  &__impact-link
    display: inline-flex
    align-items: center
    justify-content: center
    width: clamp(2.25rem, 5vw, 2.5rem)
    height: clamp(2.25rem, 5vw, 2.5rem)
    border-radius: 50%
    background: rgba(var(--v-theme-hero-gradient-end), 0.14)
    color: rgba(var(--v-theme-hero-gradient-end), 0.95)
    transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease
    text-decoration: none

    &:hover,
    &:focus-visible
      background: rgba(var(--v-theme-hero-gradient-end), 0.22)
      transform: translateY(-1px)
      box-shadow: 0 12px 18px rgba(var(--v-theme-shadow-primary-600), 0.18)

  &__impact-link:focus-visible
    outline: none
    box-shadow: 0 0 0 2px rgba(var(--v-theme-hero-gradient-end), 0.35)

@media (max-width: 599px)
  .home-category-carousel
    &__banner
      grid-template-columns: 1fr
      gap: clamp(0.5rem, 5vw, 0.75rem)

    &__banner--with-controls
      grid-template-columns: 1fr

    &__nav
      order: 3
      justify-self: center
      width: clamp(3rem, 22vw, 3.5rem)
      height: clamp(3rem, 22vw, 3.5rem)

    &__scroller
      order: 2
      mask-image: linear-gradient(90deg, transparent 0%, black 10%, black 90%, transparent 100%)

    &__item
      scroll-snap-align: start
</style>

<template>
  <div v-if="displayedCategories.length" class="category-badges-row">
    <v-btn
      v-if="!mobile && canScrollPrev"
      icon
      variant="text"
      size="x-small"
      class="category-badges-row__nav category-badges-row__nav--prev"
      :aria-label="t('common.navigation.previous')"
      @click="scrollBackward"
    >
      <v-icon icon="mdi-chevron-left" size="20" />
    </v-btn>

    <div ref="scrollContainer" class="category-badges-row__scroller">
      <v-chip
        v-for="category in displayedCategories"
        :key="category.id"
        :to="getCategoryUrl(category)"
        variant="flat"
        color="surface"
        size="small"
        rounded="pill"
        class="category-badges-row__chip text-secondary elevation-1"
      >
        <v-icon :icon="category.mdiIcon ?? 'mdi-tag'" start size="16" />
        {{ category.verticalHomeTitle ?? category.id }}
      </v-chip>

      <v-chip
        :to="categoriesUrl"
        variant="flat"
        color="surface"
        size="small"
        rounded="pill"
        class="category-badges-row__chip text-neutral-secondary elevation-1"
      >
        <v-icon icon="mdi-view-grid-outline" start size="16" />
        {{ t('home.hero.categoryBadges.viewAll') }}
      </v-chip>
    </div>

    <v-btn
      v-if="!mobile && canScrollNext"
      icon
      variant="text"
      size="x-small"
      class="category-badges-row__nav category-badges-row__nav--next"
      :aria-label="t('common.navigation.next')"
      @click="scrollForward"
    >
      <v-icon icon="mdi-chevron-double-right" size="18" />
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { useEventListener, useResizeObserver } from '@vueuse/core'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

const props = defineProps<{
  categories: VerticalConfigDto[]
  mobile?: boolean
}>()

const { t, locale } = useI18n()

const displayedCategories = computed(() =>
  (props.categories ?? []).filter(category => category.enabled !== false)
)

const categoriesUrl = computed(() =>
  resolveLocalizedRoutePath('categories', locale.value)
)

const getCategoryUrl = (category: VerticalConfigDto): string => {
  const url = category.verticalHomeUrl?.trim()
  if (!url) {
    return '/'
  }
  return url.startsWith('/') ? url : `/${url}`
}

const scrollContainer = ref<HTMLElement | null>(null)
const canScrollPrev = ref(false)
const canScrollNext = ref(false)

const updateNavigationState = () => {
  if (!import.meta.client) {
    canScrollPrev.value = false
    canScrollNext.value = false
    return
  }

  const element = scrollContainer.value
  if (!element) {
    canScrollPrev.value = false
    canScrollNext.value = false
    return
  }

  const maxScroll = Math.max(element.scrollWidth - element.clientWidth, 0)
  canScrollPrev.value = element.scrollLeft > 8
  canScrollNext.value = element.scrollLeft < maxScroll - 8
}

const scrollBy = (direction: 'forward' | 'backward') => {
  const element = scrollContainer.value
  if (!element) {
    return
  }

  const step = element.clientWidth * 0.6 || 200
  const nextPosition =
    direction === 'forward'
      ? element.scrollLeft + step
      : element.scrollLeft - step

  element.scrollTo({ left: nextPosition, behavior: 'smooth' })
}

const scrollForward = () => scrollBy('forward')
const scrollBackward = () => scrollBy('backward')

let stopResizeObserver: (() => void) | undefined
let stopWindowListener: (() => void) | undefined
let stopScrollListener: (() => void) | undefined

onMounted(() => {
  updateNavigationState()

  stopScrollListener = useEventListener(
    scrollContainer,
    'scroll',
    updateNavigationState
  )
  stopWindowListener = useEventListener(window, 'resize', updateNavigationState)
  const resizeObserver = useResizeObserver(
    scrollContainer,
    updateNavigationState
  )
  stopResizeObserver = resizeObserver.stop
})

onBeforeUnmount(() => {
  stopScrollListener?.()
  stopWindowListener?.()
  stopResizeObserver?.()
})

watch(displayedCategories, () => {
  nextTick(() => {
    updateNavigationState()
  })
})
</script>

<style scoped lang="sass">
.category-badges-row
  display: flex
  align-items: center
  gap: 0.75rem
  width: 100%
  padding: 0.5rem 0

  &__label
    flex: 0 0 auto
    font-size: 0.875rem
    font-weight: 500
    color: rgb(var(--v-theme-text-neutral-secondary))
    white-space: nowrap

  &__scroller
    flex: 1 1 auto
    display: flex
    align-items: center
    gap: 0.5rem
    overflow-x: auto
    scrollbar-width: none
    scroll-behavior: smooth

    &::-webkit-scrollbar
      display: none

  &__chip
    flex: 0 0 auto
    text-decoration: none
    font-weight: 500
    border-width: 1.5px

  &__nav
    flex: 0 0 auto
    color: rgb(var(--v-theme-secondary))
    min-width: 28px
    width: 28px
    height: 28px

  &__view-all
    flex: 0 0 auto
    display: inline-flex
    align-items: center
    gap: 0.25rem
    font-size: 0.85rem
    font-weight: 500
    color: rgb(var(--v-theme-secondary))
    text-decoration: none
    white-space: nowrap
    transition: opacity 0.2s ease

    &:hover
      opacity: 0.8
      text-decoration: underline

@media (max-width: 959px)
  .category-badges-row
    &__label
      display: none

    &__view-all
      font-size: 0.8rem

@media (max-width: 599px)
  .category-badges-row
    flex-wrap: wrap
    gap: 0.5rem

    &__scroller
      order: 2
      width: 100%

    &__view-all
      order: 1
      margin-left: auto
</style>

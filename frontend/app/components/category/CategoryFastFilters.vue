<template>
  <div v-if="subsets.length" class="category-fast-filters">
    <v-btn
      v-if="canScrollPrev"
      icon
      variant="text"
      color="primary"
      class="category-fast-filters__nav"
      data-testid="category-fast-filters-prev"
      :aria-label="t('category.fastFilters.navigation.previous')"
      @click="scrollBackward"
    >
      <v-icon icon="mdi-chevron-left" />
    </v-btn>

    <div ref="scrollContainer" class="category-fast-filters__scroller">
      <div class="category-fast-filters__inner">
        <article
          v-for="group in groupedSubsets"
          :key="group.key"
          class="category-fast-filters__group category-fast-filters__group--column"
          :aria-label="group.label"
        >
          <span
            class="text-caption font-weight-bold text-center w-100 text-uppercase"
          >
            {{ group.label }}
          </span>
          <v-chip-group
            class="category-fast-filters__chip-group"
            :model-value="getGroupSelection(group.key)"
            @update:model-value="
              value => onGroupSelectionChange(group.key, value)
            "
          >
            <template
              v-for="subset in group.subsets"
              :key="
                subset.id ??
                subset.caption ??
                subset.title ??
                subset.group ??
                'subset'
              "
            >
              <v-tooltip
                v-if="subset.description"
                location="bottom"
                :text="subset.description"
              >
                <template #activator="{ props: tooltipProps }">
                  <v-chip
                    v-bind="tooltipProps"
                    :value="subset.id"
                    :color="getSubsetStyle(group.key, subset).color"
                    :variant="getSubsetStyle(group.key, subset).variant"
                    rounded="lg"
                    class="category-fast-filters__chip"
                    :class="{
                      'category-fast-filters__chip--white-text': getSubsetStyle(
                        group.key,
                        subset
                      ).isDark,
                    }"
                  >
                    <span class="category-fast-filters__chip-label">
                      {{ resolveSubsetLabel(subset, group.key) }}
                    </span>
                  </v-chip>
                </template>
              </v-tooltip>

              <v-chip
                v-else
                :value="subset.id"
                :color="getSubsetStyle(group.key, subset).color"
                :variant="getSubsetStyle(group.key, subset).variant"
                rounded="lg"
                class="category-fast-filters__chip"
                :class="{
                  'category-fast-filters__chip--white-text': getSubsetStyle(
                    group.key,
                    subset
                  ).isDark,
                }"
              >
                <span class="category-fast-filters__chip-label">
                  {{ resolveSubsetLabel(subset, group.key) }}
                </span>
              </v-chip>
            </template>
          </v-chip-group>
        </article>
      </div>
    </div>

    <v-btn
      v-if="canScrollNext"
      icon
      variant="text"
      color="primary"
      class="category-fast-filters__nav"
      data-testid="category-fast-filters-next"
      :aria-label="t('category.fastFilters.navigation.next')"
      @click="scrollForward"
    >
      <v-icon icon="mdi-chevron-right" />
    </v-btn>
  </div>
</template>

<script setup lang="ts">
import { useEventListener, useResizeObserver } from '@vueuse/core'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { VerticalSubsetDto } from '~~/shared/api-client'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  subsets: VerticalSubsetDto[]
  activeSubsetIds: string[]
}>()

const emit = defineEmits<{
  'toggle-subset': [subsetId: string, active: boolean]
  reset: []
}>()

const loggedSubsets = new Set<string>()

const { t } = useI18n()

const subsets = computed(() => props.subsets ?? [])
const activeSubsetIds = computed(() => props.activeSubsetIds ?? [])

const DEFAULT_GROUP_KEY = 'ungrouped'

const subsetMap = computed(() => {
  return subsets.value.reduce<Record<string, VerticalSubsetDto>>(
    (accumulator, subset) => {
      if (subset.id) {
        accumulator[subset.id] = subset
      }
      return accumulator
    },
    {}
  )
})

const formatGroupLabel = (groupKey: string) => {
  return groupKey
    .split(/[-_\s]+/)
    .filter(Boolean)
    .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
}

const resolveGroupLabel = (groupKey: string): string => {
  if (!groupKey || groupKey === DEFAULT_GROUP_KEY) {
    return t('category.fastFilters.groupDefault')
  }

  const translationKey = `category.fastFilters.groups.${groupKey}`
  const translated = t(translationKey)

  if (translated !== translationKey) {
    return translated
  }

  return formatGroupLabel(groupKey)
}

const getGroupKey = (subset: VerticalSubsetDto | undefined | null): string => {
  return subset?.group ?? DEFAULT_GROUP_KEY
}

const groupedSubsets = computed(() => {
  const groups = new Map<
    string,
    { key: string; label: string; subsets: VerticalSubsetDto[] }
  >()

  subsets.value.forEach(subset => {
    if (!subset) {
      return
    }

    const key = getGroupKey(subset)
    if (!groups.has(key)) {
      groups.set(key, { key, label: resolveGroupLabel(key), subsets: [] })
    }

    groups.get(key)?.subsets.push(subset)
  })

  return Array.from(groups.values())
})

const isSubsetActive = (subset: VerticalSubsetDto): boolean => {
  return subset.id != null && activeSubsetIds.value.includes(subset.id)
}

const getGroupSelection = (groupKey: string): string | null => {
  const group = groupedSubsets.value.find(entry => entry.key === groupKey)
  if (!group) {
    return null
  }

  const activeSubset = group.subsets.find(
    subset => subset.id && isSubsetActive(subset)
  )
  return activeSubset?.id ?? null
}

const normalizeSelectionValue = (value: unknown): string | null => {
  if (Array.isArray(value)) {
    return normalizeSelectionValue(value[0])
  }

  if (typeof value === 'string') {
    return value.length ? value : null
  }

  if (value == null) {
    return null
  }

  return String(value)
}

const resolveSubsetLabel = (
  subset: VerticalSubsetDto,
  groupKey?: string
): string => {
  if (!subset.title && !subset.caption && import.meta.server) {
    const cacheKey = subset.id ?? subset.group ?? 'unknown'
    if (!loggedSubsets.has(cacheKey)) {
      console.error('Category subset is missing a display label.', {
        subsetId: subset.id,
        group: subset.group,
      })
      loggedSubsets.add(cacheKey)
    }
  }

  let label =
    subset.title ?? subset.caption ?? subset.id ?? subset.group ?? 'subset'

  // Special handling for Impact score labels to strip the "Impact" prefix
  if (groupKey === 'impactscore') {
    label = label.replace(/^Impact\s+/i, '')
  }

  return label
}

const getSubsetStyle = (
  groupKey: string,
  subset: VerticalSubsetDto
): {
  color: string | undefined
  variant: 'flat' | 'outlined' | 'tonal'
  isDark?: boolean
} => {
  const isActive = isSubsetActive(subset)

  if (groupKey === 'impactscore') {
    const label = resolveSubsetLabel(subset, groupKey).toLowerCase()

    // Map labels to colors: High (Red), Low (Green), Medium (Blue)
    let color = undefined
    if (label.includes('faible')) {
      color = 'success' // Green
    } else if (label.includes('élevé') || label.includes('eleve')) {
      color = 'error' // Red
    } else if (label.includes('moyen')) {
      color = 'info' // Blue
    }

    if (isActive) {
      return { color: color ?? 'primary', variant: 'flat', isDark: true }
    } else if (color) {
      // For inactive impact scores, show them with color but outlined or tonal?
      // User said "solid design" potentially implies they want them always colored?
      // "Use red / green / blue scale colors for respectiv badges."
      // Typically filters are outlined when inactive.
      // Let's use the color for the text/border (outlined) when inactive, and solid when active.
      // ACTUALLY, "solid design" + "use red/green/blue" might mean they want them to look like badges even when not selected?
      // Or maybe just when selected.
      // Standard behavior: Outlined when inactive (grey or colored border), Flat when active.
      // Let's stick to: Inactive = Outlined (default color or specific?), Active = Flat (Specific Color).
      // WAIT, "Stylise, moderniz, solid design" -> Maybe they want them always solid?
      // But standard chips usually toggle.
      // If I make them always solid, how do I show selection?
      // Maybe opacity or a checkmark?
      // Let's try: Active = Solid Color. Inactive = Outlined with that color?
      // Let's try to pass the specific color to the active state.
      // If inactive, let's keep it neutral or maybe tonal.
      // Let's assume standard behavior:
      // Active: Flat Error/Success/Info
      // Inactive: Outlined (Neutral) OR Outlined (Color).
      // Let's go with Active = Colored Flat.
      return { color: color, variant: 'outlined', isDark: false }
    }
  }

  return {
    color: isActive ? 'primary' : undefined,
    variant: isActive ? 'flat' : 'outlined',
  }
}

const emitToggle = (subset: VerticalSubsetDto, desired: boolean) => {
  if (!subset.id) {
    return
  }

  emit('toggle-subset', subset.id, desired)
}

const onGroupSelectionChange = (groupKey: string, value: unknown) => {
  const nextValue = normalizeSelectionValue(value)
  const currentValue = getGroupSelection(groupKey)

  if (nextValue == null) {
    if (currentValue) {
      const subset = subsetMap.value[currentValue]
      if (subset) {
        emitToggle(subset, false)
      }
    }
    return
  }

  if (nextValue === currentValue) {
    const subset = subsetMap.value[nextValue]
    if (subset) {
      emitToggle(subset, false)
    }
    return
  }

  const subset = subsetMap.value[nextValue]
  if (subset) {
    emitToggle(subset, true)
  }
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

  const step = element.clientWidth * 0.8 || 320
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

watch(groupedSubsets, () => {
  nextTick(() => {
    updateNavigationState()
  })
})
</script>

<style scoped lang="sass">
.category-fast-filters
  display: flex
  align-items: center
  gap: 0.75rem
  width: 100%
  background: rgb(var(--v-theme-surface-glass))
  border-radius: 999px
  padding: 0.75rem 1rem
  box-shadow: 0 14px 34px -26px rgba(var(--v-theme-shadow-primary-600), 0.32)

  &__scroller
    flex: 1 1 auto
    display: flex
    align-items: center
    overflow-x: auto
    scrollbar-width: thin
    scrollbar-color: rgba(var(--v-theme-border-primary-strong), 0.6) transparent
    scroll-behavior: smooth
    padding: 0.25rem 0

    &::-webkit-scrollbar
      height: 6px

    &::-webkit-scrollbar-track
      background: transparent

    &::-webkit-scrollbar-thumb
      background-color: rgba(var(--v-theme-border-primary-strong), 0.6)
      border-radius: 999px

  &__inner
    display: flex
    align-items: center
    gap: 1.5rem
    margin: auto

  &__group
    flex: 0 0 auto
    display: flex
    align-items: center
    gap: 0.5rem
    min-width: 0

    &--column
      flex-direction: column
      align-items: flex-start
      gap: 0.25rem

  &__chip-group
    display: flex
    flex-wrap: nowrap
    gap: 0.5rem
    min-width: 0

    :deep(.v-chip-group__container)
      display: contents

  &__chip
    flex: 0 0 auto

  &__chip-label
    font-weight: 500

  &__chip-group :deep(.v-chip--variant-flat)
    box-shadow: 0 12px 20px -14px rgba(var(--v-theme-shadow-primary-600), 0.45)
    font-weight: 600
    color: rgb(var(--v-theme-on-primary))

  &__nav
    flex: 0 0 auto
    color: rgb(var(--v-theme-text-neutral-secondary))

  &__chip--white-text
    color: #ffffff !important
    :deep(.v-chip__content)
      color: #ffffff !important

@media (max-width: 959px)
  .category-fast-filters
    flex-direction: column
    align-items: stretch
    gap: 0.75rem
    border-radius: 1rem

    &__scroller
      gap: 1rem
      padding: 0

    &__group
      flex-wrap: wrap

    &__chip-group
      flex-wrap: wrap
</style>

<template>
  <div v-if="subsets.length" class="category-fast-filters">
    <div class="category-fast-filters__groups">
      <article
        v-for="group in groupedSubsets"
        :key="group.key"
        class="category-fast-filters__group"
      >
        <h3 class="category-fast-filters__group-title">{{ group.label }}</h3>

        <div class="category-fast-filters__chip-region">
          <v-btn
            v-if="scrollState[group.key]?.left"
            class="category-fast-filters__nav"
            icon="mdi-chevron-left"
            variant="text"
            density="comfortable"
            :aria-label="t('category.fastFilters.scrollLeft', { group: group.label })"
            @click="scrollChips(group.key, -1)"
          />

          <div
            :ref="(element) => setScrollContainer(group.key, element)"
            class="category-fast-filters__chip-window"
            role="group"
            :aria-label="group.label"
            @scroll="() => onScroll(group.key)"
          >
            <v-chip-group
              class="category-fast-filters__chip-group"
              :model-value="getGroupSelection(group.key)"
              @update:model-value="(value) => onGroupSelectionChange(group.key, value)"
            >
              <template
                v-for="subset in group.subsets"
                :key="subset.id ?? subset.caption ?? subset.title ?? subset.group ?? 'subset'"
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
                      :color="isSubsetActive(subset) ? 'primary' : undefined"
                      :variant="isSubsetActive(subset) ? 'flat' : 'outlined'"
                      rounded="lg"
                    >
                      <span class="category-fast-filters__chip-label">
                        {{ resolveSubsetLabel(subset) }}
                      </span>
                    </v-chip>
                  </template>
                </v-tooltip>

                <v-chip
                  v-else
                  :value="subset.id"
                  :color="isSubsetActive(subset) ? 'primary' : undefined"
                  :variant="isSubsetActive(subset) ? 'flat' : 'outlined'"
                  rounded="lg"
                >
                  <span class="category-fast-filters__chip-label">
                    {{ resolveSubsetLabel(subset) }}
                  </span>
                </v-chip>
              </template>
            </v-chip-group>
          </div>

          <v-btn
            v-if="scrollState[group.key]?.right"
            class="category-fast-filters__nav"
            icon="mdi-chevron-right"
            variant="text"
            density="comfortable"
            :aria-label="t('category.fastFilters.scrollRight', { group: group.label })"
            @click="scrollChips(group.key, 1)"
          />
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'
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
  return subsets.value.reduce<Record<string, VerticalSubsetDto>>((accumulator, subset) => {
    if (subset.id) {
      accumulator[subset.id] = subset
    }
    return accumulator
  }, {})
})

const formatGroupLabel = (groupKey: string) => {
  return groupKey
    .split(/[-_\s]+/)
    .filter(Boolean)
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
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
  const groups = new Map<string, { key: string; label: string; subsets: VerticalSubsetDto[] }>()

  subsets.value.forEach((subset) => {
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
  const group = groupedSubsets.value.find((entry) => entry.key === groupKey)
  if (!group) {
    return null
  }

  const activeSubset = group.subsets.find((subset) => subset.id && isSubsetActive(subset))
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

const resolveSubsetLabel = (subset: VerticalSubsetDto): string => {
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

  return subset.title ?? subset.caption ?? subset.id ?? subset.group ?? 'subset'
}

const scrollContainers = new Map<string, HTMLDivElement>()
const scrollState = reactive<Record<string, { left: boolean; right: boolean }>>({})
const SCROLL_AMOUNT = 240
const SCROLL_EPSILON = 8

const updateScrollState = (groupKey: string) => {
  const container = scrollContainers.get(groupKey)
  if (!container) {
    return
  }

  const { scrollLeft, scrollWidth, clientWidth } = container
  const maxScrollLeft = Math.max(scrollWidth - clientWidth, 0)
  scrollState[groupKey] = {
    left: scrollLeft > SCROLL_EPSILON,
    right: scrollLeft < Math.max(maxScrollLeft - SCROLL_EPSILON, 0),
  }
}

const resolveScrollContainer = (
  value: Element | ComponentPublicInstance | null,
): HTMLDivElement | null => {
  if (value instanceof HTMLDivElement) {
    return value
  }

  if (value && '$el' in value) {
    const element = (value as ComponentPublicInstance).$el
    if (element instanceof HTMLDivElement) {
      return element
    }
  }

  return null
}

const setScrollContainer = (groupKey: string, element: Element | ComponentPublicInstance | null) => {
  const resolved = resolveScrollContainer(element)

  if (resolved) {
    scrollContainers.set(groupKey, resolved)
    updateScrollState(groupKey)
    return
  }

  scrollContainers.delete(groupKey)
  delete scrollState[groupKey]
}

const onScroll = (groupKey: string) => {
  updateScrollState(groupKey)
}

const scrollChips = (groupKey: string, direction: 1 | -1) => {
  const container = scrollContainers.get(groupKey)
  if (!container) {
    return
  }

  container.scrollBy({
    left: direction * SCROLL_AMOUNT,
    behavior: 'smooth',
  })
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

const refreshAllScrollStates = () => {
  groupedSubsets.value.forEach((group) => updateScrollState(group.key))
}

const handleResize = () => {
  refreshAllScrollStates()
}

onMounted(() => {
  nextTick(refreshAllScrollStates)

  if (import.meta.client) {
    window.addEventListener('resize', handleResize, { passive: true })
  }
})

onBeforeUnmount(() => {
  if (import.meta.client) {
    window.removeEventListener('resize', handleResize)
  }
})

watch(groupedSubsets, () => {
  nextTick(refreshAllScrollStates)
})
</script>

<style scoped lang="sass">
.category-fast-filters
  display: flex
  flex-direction: column
  gap: 0.75rem
  background-color: rgb(var(--v-theme-surface-glass))
  border-radius: 0.75rem
  padding: 1rem 1.25rem
  box-shadow: 0 18px 40px -28px rgba(var(--v-theme-shadow-primary-600), 0.32)
  width: 100%

  &__groups
    display: flex
    flex-direction: column
    gap: 0.75rem

  &__group
    display: flex
    align-items: center
    gap: 1rem
    min-width: 0

  &__group-title
    flex: 0 0 auto
    margin: 0
    font-size: 0.95rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))
    text-transform: uppercase
    letter-spacing: 0.04em
    white-space: nowrap

  &__chip-region
    display: flex
    align-items: center
    gap: 0.25rem
    flex: 1 1 auto
    min-width: 0

  &__chip-window
    position: relative
    flex: 1 1 auto
    overflow-x: auto
    overflow-y: hidden
    scrollbar-width: none
    -ms-overflow-style: none

    &::-webkit-scrollbar
      display: none

  &__chip-group
    display: inline-flex
    align-items: center
    gap: 0.5rem
    padding: 0.25rem 0
    min-width: 100%

    :deep(.v-chip)
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

@media (max-width: 959px)
  .category-fast-filters
    padding: 0.75rem 1rem

    &__group
      flex-direction: column
      align-items: stretch
      gap: 0.5rem

    &__group-title
      white-space: normal

    &__chip-region
      width: 100%

    &__chip-window
      width: 100%
</style>

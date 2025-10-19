<template>
  <div v-if="subsets.length" class="category-fast-filters">
    <div class="category-fast-filters__viewport">
      <v-btn
        v-if="canScrollLeft"
        class="category-fast-filters__nav category-fast-filters__nav--left"
        icon="mdi-chevron-left"
        color="primary"
        variant="flat"
        density="comfortable"
        :aria-label="t('category.fastFilters.scrollPrevious')"
        @click="scrollLeft"
      />

      <div ref="scrollContainer" class="category-fast-filters__groups">
        <article
          v-for="group in groupedSubsets"
          :key="group.key"
          class="category-fast-filters__group"
        >
          <h3 class="category-fast-filters__group-title">{{ group.label }}</h3>

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
        </article>
      </div>

      <v-btn
        v-if="canScrollRight"
        class="category-fast-filters__nav category-fast-filters__nav--right"
        icon="mdi-chevron-right"
        color="primary"
        variant="flat"
        density="comfortable"
        :aria-label="t('category.fastFilters.scrollNext')"
        @click="scrollRight"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
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

const scrollContainer = ref<HTMLDivElement | null>(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

const updateScrollState = () => {
  const element = scrollContainer.value
  if (!element) {
    canScrollLeft.value = false
    canScrollRight.value = false
    return
  }

  const tolerance = 2
  canScrollLeft.value = element.scrollLeft > tolerance
  canScrollRight.value = element.scrollLeft + element.clientWidth < element.scrollWidth - tolerance
}

const scrollByAmount = (direction: 'left' | 'right') => {
  const element = scrollContainer.value
  if (!element) {
    return
  }

  const step = Math.max(element.clientWidth * 0.75, 240)
  const offset = direction === 'left' ? -step : step
  element.scrollBy({ left: offset, behavior: 'smooth' })
}

const scrollLeft = () => scrollByAmount('left')
const scrollRight = () => scrollByAmount('right')

const handleResize = () => {
  updateScrollState()
}

onMounted(() => {
  const element = scrollContainer.value
  updateScrollState()

  if (element) {
    element.addEventListener('scroll', updateScrollState, { passive: true })
  }

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  const element = scrollContainer.value
  if (element) {
    element.removeEventListener('scroll', updateScrollState)
  }

  window.removeEventListener('resize', handleResize)
})

watch([groupedSubsets, activeSubsetIds], async () => {
  await nextTick()
  updateScrollState()
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


</script>

<style scoped lang="sass">
.category-fast-filters
  position: relative
  background-color: rgb(var(--v-theme-surface-glass))
  border-radius: 1rem
  padding: 0.75rem 2.5rem
  margin-bottom: 2rem
  box-shadow: 0 24px 48px -32px rgba(var(--v-theme-shadow-primary-600), 0.3)

  &__viewport
    position: relative
    display: flex
    align-items: center

  &__groups
    display: flex
    gap: 1rem
    overflow-x: auto
    padding: 0.25rem 0
    scroll-behavior: smooth
    scrollbar-width: none

    &::-webkit-scrollbar
      display: none

  &__group
    flex: 0 0 auto
    display: flex
    align-items: center
    gap: 0.75rem
    padding: 0.75rem 1rem
    background-color: rgba(var(--v-theme-surface-primary-080), 0.75)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5)
    border-radius: 999px
    transition: background-color 200ms ease, border-color 200ms ease, box-shadow 200ms ease
    min-width: min(320px, 100%)

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-080), 0.95)
      border-color: rgba(var(--v-theme-border-primary-strong), 0.8)
      box-shadow: 0 12px 24px -18px rgba(var(--v-theme-shadow-primary-600), 0.28)

  &__group-title
    margin: 0
    font-size: 0.85rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))
    text-transform: uppercase
    letter-spacing: 0.06em
    white-space: nowrap

  &__chip-group
    display: flex
    flex-wrap: wrap
    gap: 0.5rem
    margin: 0
    padding: 0
    flex: 1 1 auto
    min-width: 0

    :deep(.v-chip)
      margin: 0.125rem 0

  &__chip-label
    font-weight: 500

  &__chip-group :deep(.v-chip--variant-flat)
    box-shadow: 0 12px 20px -14px rgba(var(--v-theme-shadow-primary-600), 0.45)
    font-weight: 600
    color: rgb(var(--v-theme-on-primary))

  &__nav
    position: absolute
    top: 50%
    transform: translateY(-50%)
    z-index: 2
    box-shadow: 0 12px 28px -18px rgba(var(--v-theme-shadow-primary-600), 0.38)

  &__nav--left
    left: 0.25rem

  &__nav--right
    right: 0.25rem

@media (max-width: 959px)
  .category-fast-filters
    padding: 0.75rem 2.25rem

    &__group
      border-radius: 1rem

@media (max-width: 599px)
  .category-fast-filters
    padding: 0.5rem 2rem

    &__group
      padding: 0.5rem 0.75rem
</style>

<template>
    <div v-if="subsets.length" class="category-fast-filters__groups">
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
                  class="me-2 mb-2"
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
              class="me-2 mb-2"
            >
              <span class="category-fast-filters__chip-label">
                {{ resolveSubsetLabel(subset) }}
              </span>
            </v-chip>
          </template>
        </v-chip-group>
      </article>
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
  background-color: rgb(var(--v-theme-surface-glass))
  border-radius: 1rem
  padding: 1.5rem
  margin-bottom: 2rem
  box-shadow: 0 24px 48px -32px rgba(var(--v-theme-shadow-primary-600), 0.3)

  &__header
    display: flex
    align-items: center
    justify-content: space-between
    gap: 1rem
    margin-bottom: 1rem

  &__title
    margin: 0
    font-size: 1.25rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  &__reset
    text-transform: none

  &__groups
    display: grid
    gap: 1.25rem
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))

  &__group
    background-color: rgba(var(--v-theme-surface-primary-080), 0.7)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5)
    border-radius: 0.75rem
    padding: 1rem 1.25rem
    transition: background-color 200ms ease, border-color 200ms ease, box-shadow 200ms ease

    &:hover
      background-color: rgba(var(--v-theme-surface-primary-080), 0.95)
      border-color: rgba(var(--v-theme-border-primary-strong), 0.8)
      box-shadow: 0 12px 24px -18px rgba(var(--v-theme-shadow-primary-600), 0.32)

  &__group-title
    margin: 0 0 0.75rem
    font-size: 0.95rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-secondary))
    text-transform: uppercase
    letter-spacing: 0.04em

  &__chip-group
    display: flex
    flex-wrap: wrap
    margin: -0.25rem

    :deep(.v-chip)
      margin: 0.25rem

  &__chip-label
    font-weight: 500

  &__chip-group :deep(.v-chip--variant-flat)
    box-shadow: 0 12px 20px -14px rgba(var(--v-theme-shadow-primary-600), 0.45)
    font-weight: 600
    color: rgb(var(--v-theme-on-primary))

@media (max-width: 959px)
  .category-fast-filters
    padding: 1.25rem

    &__header
      align-items: flex-start
      flex-direction: column
      gap: 0.5rem

    &__reset
      align-self: flex-end

    &__groups
      grid-template-columns: minmax(0, 1fr)
</style>

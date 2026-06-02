<template>
  <div class="category-results-toolbar">
    <v-progress-linear
      v-if="loading"
      class="category-results-toolbar__loading"
      color="primary"
      indeterminate
      height="2"
      data-testid="results-toolbar-loading"
    />

    <div
      class="category-results-toolbar__section category-results-toolbar__section--left"
    >
      <v-btn
        v-if="showFiltersButton && !isDesktop"
        color="primary"
        variant="flat"
        prepend-icon="mdi-filter-variant"
        data-testid="results-toolbar-filter-button"
        :aria-label="t('category.products.openFilters')"
        @click="emit('toggle-filters')"
      >
        {{ t('category.products.openFilters') }}
        <v-badge
          v-if="filtersCount"
          :content="filtersCount"
          color="primary"
          inline
          class="ms-2"
        />
      </v-btn>

      <CategoryResultsCount v-if="showResultsCount" :count="resultsCount" />

      <v-chip
        v-if="activeFiltersCount"
        class="category-results-toolbar__active-count"
        color="primary"
        variant="tonal"
        size="small"
        prepend-icon="mdi-filter-check-outline"
        data-testid="results-toolbar-active-count"
      >
        {{ activeFiltersCount }}
      </v-chip>

      <div v-if="showSort" class="category-results-toolbar__sort">
        <div class="category-results-toolbar__sort-select">
          <v-tooltip
            location="bottom"
            :text="t('category.products.tooltips.sortField')"
          >
            <template #activator="{ props: sortSelectProps }">
              <v-select
                v-bind="sortSelectProps"
                :model-value="sortField"
                :items="sortItems"
                :label="t('category.products.sortLabel')"
                item-title="title"
                item-value="value"
                clearable
                :disabled="!hasSortItems"
                hide-details
                density="comfortable"
                data-testid="results-toolbar-sort-select"
                :menu-props="{
                  contentClass: 'category-results-toolbar__sort-menu',
                }"
                @update:model-value="emit('update:sortField', $event)"
              />
            </template>
          </v-tooltip>
        </div>
        <v-btn-toggle
          v-model="internalSortOrder"
          class="category-results-toolbar__sort-order"
          density="comfortable"
          data-testid="results-toolbar-sort-order"
        >
          <v-btn value="asc" :aria-label="t('category.products.sortOrderAsc')">
            <v-icon icon="mdi-sort-ascending" />
            <v-tooltip
              activator="parent"
              location="bottom"
              :text="t('category.products.tooltips.sortAscending')"
            />
          </v-btn>
          <v-btn
            value="desc"
            :aria-label="t('category.products.sortOrderDesc')"
          >
            <v-icon icon="mdi-sort-descending" />
            <v-tooltip
              activator="parent"
              location="bottom"
              :text="t('category.products.tooltips.sortDescending')"
            />
          </v-btn>
        </v-btn-toggle>
      </div>
    </div>

    <div
      v-if="showSearch"
      class="category-results-toolbar__section category-results-toolbar__section--center"
    >
      <div class="category-results-toolbar__search">
        <v-tooltip
          location="bottom"
          :text="t('category.products.tooltips.search')"
        >
          <template #activator="{ props: searchProps }">
            <v-text-field
              v-bind="searchProps"
              :model-value="searchTerm"
              :label="t('category.products.searchPlaceholder')"
              prepend-inner-icon="mdi-magnify"
              clearable
              hide-details
              density="comfortable"
              class="category-results-toolbar__search-input"
              data-testid="results-toolbar-search-input"
              @update:model-value="emit('update:searchTerm', $event)"
            />
          </template>
        </v-tooltip>
      </div>
    </div>

    <div
      v-if="(showViewToggle && isDesktop) || $slots.activeFilters"
      class="category-results-toolbar__section category-results-toolbar__section--right"
    >
      <v-btn-toggle
        v-if="showViewToggle && isDesktop"
        v-model="internalViewMode"
        mandatory
        class="category-results-toolbar__view-toggle"
        data-testid="results-toolbar-view-toggle"
      >
        <v-btn value="cards" :aria-label="t('category.products.viewCards')">
          <v-icon icon="mdi-view-grid" />
          <v-tooltip
            activator="parent"
            location="bottom"
            :text="t('category.products.tooltips.viewCards')"
          />
        </v-btn>
        <v-btn value="list" :aria-label="t('category.products.viewList')">
          <v-icon icon="mdi-view-list" />
          <v-tooltip
            activator="parent"
            location="bottom"
            :text="t('category.products.tooltips.viewList')"
          />
        </v-btn>
        <v-btn value="table" :aria-label="t('category.products.viewTable')">
          <v-icon icon="mdi-table" />
          <v-tooltip
            activator="parent"
            location="bottom"
            :text="t('category.products.tooltips.viewTable')"
          />
        </v-btn>
      </v-btn-toggle>

      <div
        v-if="$slots.activeFilters"
        class="category-results-toolbar__active-filters"
      >
        <slot name="activeFilters" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CategoryViewMode } from '~/utils/_category-filter-state'
import CategoryResultsCount from '~/components/category/CategoryResultsCount.vue'

type SortItem = {
  title: string
  value: string
}

type SortGroupItem =
  | {
      type: 'subheader'
      title: string
    }
  | {
      type: 'divider'
    }

type SortOption = SortItem | SortGroupItem

const props = withDefaults(
  defineProps<{
    isDesktop: boolean
    resultsCount: number
    viewMode: CategoryViewMode
    sortItems: SortOption[]
    sortField: string | null
    sortOrder: 'asc' | 'desc'
    searchTerm: string
    showSearch?: boolean
    showSort?: boolean
    showViewToggle?: boolean
    showResultsCount?: boolean
    showFiltersButton?: boolean
    filtersCount?: number
    activeFiltersCount?: number
    loading?: boolean
  }>(),
  {
    showSearch: true,
    showSort: true,
    showViewToggle: true,
    showResultsCount: true,
    showFiltersButton: false,
    filtersCount: 0,
    activeFiltersCount: 0,
    loading: false,
  }
)

const emit = defineEmits<{
  'toggle-filters': []
  'update:searchTerm': [string]
  'update:sortField': [string | null]
  'update:sortOrder': ['asc' | 'desc']
  'update:viewMode': [CategoryViewMode]
}>()

const { t } = useI18n()

const internalViewMode = computed({
  get: () => props.viewMode,
  set: value => emit('update:viewMode', value),
})

const internalSortOrder = computed({
  get: () => props.sortOrder,
  set: value => emit('update:sortOrder', value),
})

const availableSortItems = computed(() =>
  props.sortItems.filter(
    (item): item is SortItem => 'value' in item && Boolean(item.value)
  )
)

const hasSortItems = computed(() => availableSortItems.value.length > 0)
</script>

<style scoped lang="sass">
.category-results-toolbar
  position: sticky
  top: 76px
  z-index: 4
  display: flex
  flex-direction: column
  gap: 0.75rem
  margin-bottom: 0.9rem
  width: 100%
  background: rgba(var(--v-theme-surface-default), 0.96)
  padding: 0.75rem
  border-radius: 8px
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.32)
  box-shadow: 0 10px 24px -20px rgba(var(--v-theme-shadow-primary-600), 0.36)
  backdrop-filter: blur(10px)

  &__loading
    position: absolute
    inset-inline: 0
    top: 0
    border-radius: 8px 8px 0 0
    overflow: hidden

  &__section
    display: flex
    flex-wrap: wrap
    align-items: center
    gap: 0.75rem

  &__section--left
    justify-content: flex-start

  &__section--center
    justify-content: center
    flex-grow: 1

  &__section--right
    justify-content: flex-end

  &__sort
    display: flex
    flex-wrap: nowrap
    align-items: center
    gap: 0.5rem

  &__sort-select
    flex: 0 1 220px
    min-width: 0

  &__sort-order
    flex-shrink: 0
    overflow: visible
    border-radius: 8px
    border: 1px solid rgba(var(--v-theme-border-primary), 0.15)
    background: rgba(var(--v-theme-surface-default), 0.4)

  &__search
    flex: 1 1 100%
    min-width: 0
    max-width: 520px

  &__search-input
    width: 100%

  &__view-toggle
    border-radius: 8px
    border: 1px solid rgba(var(--v-theme-border-primary), 0.15)
    background: rgba(var(--v-theme-surface-default), 0.4)

  &__active-count
    font-weight: 700

  &__active-filters
    max-width: min(42vw, 520px)
    min-width: 0
    overflow: hidden

    :deep(.category-active-filters)
      gap: 0.35rem

    :deep(.category-active-filters__list)
      max-height: 2.5rem
      overflow: hidden

@media (min-width: 960px)
  .category-results-toolbar
    display: grid
    grid-template-columns: minmax(280px, auto) minmax(260px, 1fr) auto
    align-items: center
    gap: 0.85rem

  .category-results-toolbar__section
    flex-wrap: nowrap

  .category-results-toolbar__section--center
    justify-content: center

  .category-results-toolbar__section--right
    justify-content: flex-end

  .category-results-toolbar__search
    flex: 0 1 460px

@media (max-width: 959px)
  .category-results-toolbar
    position: sticky
    top: 64px
    border-radius: 0
    margin-inline: -12px
    width: calc(100% + 24px)

  .category-results-toolbar__section--center
    justify-content: center

  .category-results-toolbar__section--right
    justify-content: flex-end

  .category-results-toolbar__sort
    width: auto

  .category-results-toolbar__sort-select
    flex: 1 1 auto
    min-width: 130px

  .category-results-toolbar__active-filters
    max-width: 100%

:deep(.category-results-toolbar__sort-menu .v-list-subheader)
  font-size: 0.7rem
  font-weight: 700
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgb(var(--v-theme-text-neutral-soft))

:deep(.category-results-toolbar__sort-menu .v-divider)
  margin: 0.25rem 0
</style>

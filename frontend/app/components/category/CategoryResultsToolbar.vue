<template>
  <div class="category-results-toolbar">
    <div
      class="category-results-toolbar__section category-results-toolbar__section--left"
    >
      <v-btn
        v-if="showFiltersButton && !isDesktop"
        color="primary"
        variant="flat"
        prepend-icon="mdi-filter-variant"
        data-testid="results-toolbar-filter-button"
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
                :disabled="sortItems.length === 0"
                hide-details
                density="comfortable"
                data-testid="results-toolbar-sort-select"
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
      v-if="showViewToggle && isDesktop"
      class="category-results-toolbar__section category-results-toolbar__section--right"
    >
      <v-btn-toggle
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

const props = withDefaults(
  defineProps<{
    isDesktop: boolean
    resultsCount: number
    viewMode: CategoryViewMode
    sortItems: SortItem[]
    sortField: string | null
    sortOrder: 'asc' | 'desc'
    searchTerm: string
    showSearch?: boolean
    showSort?: boolean
    showViewToggle?: boolean
    showResultsCount?: boolean
    showFiltersButton?: boolean
    filtersCount?: number
  }>(),
  {
    showSearch: true,
    showSort: true,
    showViewToggle: true,
    showResultsCount: true,
    showFiltersButton: false,
    filtersCount: 0,
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
</script>

<style scoped lang="sass">
.category-results-toolbar
  display: flex
  flex-direction: column
  gap: 1rem
  margin-bottom: 1.5rem
  width: 100%

  &__section
    display: flex
    flex-wrap: wrap
    align-items: center
    gap: 1rem

  &__section--left
    justify-content: flex-start

  &__section--center
    justify-content: center

  &__section--right
    justify-content: flex-end

  &__sort
    display: flex
    flex-wrap: wrap
    align-items: center
    gap: 0.75rem

  &__sort-select
    flex: 1 1 220px

  &__search
    flex: 1 1 100%
    min-width: 0

  &__search-input
    width: 100%

@media (min-width: 960px)
  .category-results-toolbar
    display: grid
    grid-template-columns: auto minmax(260px, 1fr) auto
    align-items: center
    gap: 1.5rem

  .category-results-toolbar__section
    flex-wrap: nowrap

  .category-results-toolbar__section--center
    justify-content: center

  .category-results-toolbar__section--right
    justify-content: flex-end

  .category-results-toolbar__search
    flex: 0 1 420px

@media (max-width: 959px)
  .category-results-toolbar__section--center
    justify-content: center

  .category-results-toolbar__section--right
    justify-content: flex-end

  .category-results-toolbar__sort
    width: auto

  .category-results-toolbar__sort-select
    flex: 1 1 auto
    min-width: 180px
</style>

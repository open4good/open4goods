<template>
  <div class="category-page__filters-content">
    <header
      v-if="showHeader"
      class="category-page__filters-header"
      data-testid="category-filters-sidebar-header"
    >
      <div class="category-page__filters-header-copy">
        <p class="category-page__filters-heading">
          {{ t('category.filters.advanced') }}
        </p>
        <v-chip
          v-if="activeFiltersCount"
          color="primary"
          variant="tonal"
          size="small"
          prepend-icon="mdi-filter-check-outline"
          data-testid="category-filters-active-count"
        >
          {{ activeFiltersCount }}
        </v-chip>
      </div>
      <div class="category-page__filters-header-actions">
        <v-tooltip
          v-if="activeFiltersCount"
          :text="t('category.filters.clearAllTooltip')"
          location="bottom"
        >
          <template #activator="{ props: clearProps }">
            <v-btn
              v-bind="clearProps"
              icon="mdi-filter-off-outline"
              variant="text"
              density="comfortable"
              :aria-label="t('category.filters.clearAllTooltip')"
              data-testid="category-filters-clear"
              @click="emit('clear-mobile')"
            />
          </template>
        </v-tooltip>
        <v-tooltip
          v-if="showCollapseButton"
          :text="t('category.filters.toggle.hide')"
          location="bottom"
        >
          <template #activator="{ props: collapseProps }">
            <v-btn
              v-bind="collapseProps"
              icon="mdi-chevron-left"
              variant="text"
              density="comfortable"
              :aria-label="t('category.filters.toggle.hide')"
              data-testid="category-filters-collapse"
              @click="emit('collapse')"
            />
          </template>
        </v-tooltip>
      </div>
    </header>

    <v-text-field
      v-if="showFilterSearch"
      :model-value="filterSearchTerm"
      class="category-page__filters-search"
      density="compact"
      variant="outlined"
      hide-details
      clearable
      prepend-inner-icon="mdi-magnify"
      :label="t('category.filters.searchLabel')"
      data-testid="category-filters-search"
      @update:model-value="
        value => emit('update:filterSearchTerm', String(value ?? ''))
      "
    />

    <CategoryFiltersPanel
      :filter-options="props.filterOptions"
      :aggregations="props.aggregations"
      :baseline-aggregations="props.baselineAggregations"
      :filters="props.filters"
      :impact-expanded="props.impactExpanded"
      :technical-expanded="props.technicalExpanded"
      :search-term="filterSearchTerm"
      @update:filters="value => emit('update:filters', value)"
      @update:impact-expanded="value => emit('update:impactExpanded', value)"
      @update:technical-expanded="
        value => emit('update:technicalExpanded', value)
      "
    />

    <CategoryAdminFiltersPanel
      v-if="shouldShowAdminPanel"
      :fields="adminFields"
      :aggregations="props.aggregations"
      :baseline-aggregations="props.baselineAggregations"
      :filters="props.filters"
      @update:filters="value => emit('update:filters', value)"
    />

    <div v-if="$slots.extra" class="category-page__filters-extra">
      <slot name="extra" />
    </div>

    <div v-if="showMobileActions" class="category-page__filters-actions">
      <v-btn block color="primary" class="mb-2" @click="emit('apply-mobile')">
        {{ t('category.filters.mobileApply') }}
      </v-btn>
      <v-btn block variant="text" @click="emit('clear-mobile')">
        {{ t('category.filters.mobileClear') }}
      </v-btn>
    </div>

    <template v-if="hasDocumentation">
      <v-divider class="my-4" />
      <CategoryDocumentationRail
        class="category-page__documentation-block"
        :wiki-pages="wikiPages"
        :related-posts="relatedPosts"
        :vertical-home-url="props.verticalHomeUrl"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AggregationResponseDto,
  BlogPostDto,
  FieldMetadataDto,
  FilterRequestDto,
  ProductFieldOptionsResponse,
  WikiPageConfig,
} from '~~/shared/api-client'

import CategoryFiltersPanel from './CategoryFiltersPanel.vue'
import CategoryDocumentationRail from './CategoryDocumentationRail.vue'
import CategoryAdminFiltersPanel from './CategoryAdminFiltersPanel.vue'
const props = withDefaults(
  defineProps<{
    filterOptions: ProductFieldOptionsResponse | null
    aggregations: AggregationResponseDto[]
    baselineAggregations?: AggregationResponseDto[]
    filters: FilterRequestDto | null
    impactExpanded: boolean
    technicalExpanded: boolean
    showMobileActions: boolean
    hasDocumentation: boolean
    wikiPages: WikiPageConfig[]
    relatedPosts: BlogPostDto[]
    showAdminPanel?: boolean
    adminFilterFields?: FieldMetadataDto[]
    activeFiltersCount?: number
    showHeader?: boolean
    showCollapseButton?: boolean
    showFilterSearch?: boolean
    filterSearchTerm?: string
  }>(),
  {
    baselineAggregations: () => [],
    showAdminPanel: false,
    adminFilterFields: () => [],
    activeFiltersCount: 0,
    showHeader: false,
    showCollapseButton: false,
    showFilterSearch: false,
    filterSearchTerm: '',
  }
)

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'update:impactExpanded': [boolean]
  'update:technicalExpanded': [boolean]
  'update:filterSearchTerm': [string]
  'apply-mobile': []
  'clear-mobile': []
  collapse: []
}>()

const { t } = useI18n()

const wikiPages = computed(() => props.wikiPages ?? [])
const relatedPosts = computed(() => props.relatedPosts ?? [])
const hasDocumentation = computed(() => props.hasDocumentation)
const showMobileActions = computed(() => props.showMobileActions)
const filterSearchTerm = computed(() => props.filterSearchTerm ?? '')
const adminFields = computed(() => props.adminFilterFields ?? [])
const shouldShowAdminPanel = computed(
  () => props.showAdminPanel && adminFields.value.length > 0
)
</script>

<style scoped lang="sass">
.category-page__filters-content
  display: flex
  flex-direction: column
  gap: 1rem
  flex: 1 1 auto

.category-page__filters-header
  display: flex
  align-items: center
  justify-content: space-between
  gap: 0.75rem

.category-page__filters-header-copy
  display: flex
  align-items: center
  gap: 0.5rem
  min-width: 0

.category-page__filters-heading
  margin: 0
  font-size: 0.88rem
  font-weight: 700
  line-height: 1.2
  color: rgb(var(--v-theme-text-neutral-strong))

.category-page__filters-header-actions
  display: inline-flex
  align-items: center
  gap: 0.25rem
  flex: 0 0 auto

.category-page__filters-search
  width: 100%
</style>

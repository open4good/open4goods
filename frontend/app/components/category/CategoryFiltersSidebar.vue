<template>
  <div class="category-page__filters-content">
    <CategoryFiltersPanel
      :filter-options="props.filterOptions"
      :aggregations="props.aggregations"
      :filters="props.filters"
      :impact-expanded="props.impactExpanded"
      :technical-expanded="props.technicalExpanded"
      @update:filters="(value) => emit('update:filters', value)"
      @update:impact-expanded="(value) => emit('update:impactExpanded', value)"
      @update:technical-expanded="(value) => emit('update:technicalExpanded', value)"
    />

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
  FilterRequestDto,
  ProductFieldOptionsResponse,
  WikiPageConfig,
} from '~~/shared/api-client'

import CategoryDocumentationRail from './CategoryDocumentationRail.vue'
import CategoryFiltersPanel from './CategoryFiltersPanel.vue'

const props = defineProps<{
  filterOptions: ProductFieldOptionsResponse | null
  aggregations: AggregationResponseDto[]
  filters: FilterRequestDto | null
  impactExpanded: boolean
  technicalExpanded: boolean
  showMobileActions: boolean
  hasDocumentation: boolean
  wikiPages: WikiPageConfig[]
  relatedPosts: BlogPostDto[]
  verticalHomeUrl?: string | null
}>()

const emit = defineEmits<{
  'update:filters': [FilterRequestDto]
  'update:impactExpanded': [boolean]
  'update:technicalExpanded': [boolean]
  'apply-mobile': []
  'clear-mobile': []
}>()

const { t } = useI18n()

const wikiPages = computed(() => props.wikiPages ?? [])
const relatedPosts = computed(() => props.relatedPosts ?? [])
const hasDocumentation = computed(() => props.hasDocumentation)
const showMobileActions = computed(() => props.showMobileActions)
</script>

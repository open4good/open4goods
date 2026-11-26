<template>
  <nav
    class="category-pagination"
    :aria-label="t('category.pagination.ariaLabel')"
  >
    <ul class="category-pagination__list">
      <li v-if="current > 1">
        <v-btn
          :to="buildTo(current - 1)"
          rel="prev"
          variant="text"
          density="comfortable"
          icon="mdi-chevron-left"
          :aria-label="t('category.pagination.previous')"
        />
      </li>
      <li v-for="(item, index) in items" :key="`${item}-${index}`">
        <span v-if="item === null" class="category-pagination__ellipsis">
          …
        </span>
        <v-btn
          v-else
          :to="buildTo(item)"
          :variant="item === current ? 'flat' : 'text'"
          :color="item === current ? 'primary' : undefined"
          :aria-current="item === current ? 'page' : undefined"
          :aria-label="t('category.pagination.page', { page: item })"
          density="comfortable"
          min-width="40"
        >
          {{ item }}
        </v-btn>
      </li>
      <li v-if="current < length">
        <v-btn
          :to="buildTo(current + 1)"
          rel="next"
          variant="text"
          density="comfortable"
          icon="mdi-chevron-right"
          :aria-label="t('category.pagination.next')"
        />
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import { buildPaginationItems } from '~/utils/_pagination-items'

const props = defineProps<{
  length: number
  /** Current page, 1-based. */
  current: number
  /** Builds the crawlable target (`?page=N`) of a page. */
  buildTo: (page: number) => RouteLocationRaw
}>()

const { t } = useI18n()

const items = computed(() => buildPaginationItems(props.length, props.current))
</script>

<style scoped lang="sass">
.category-pagination
  display: flex
  justify-content: center

.category-pagination__list
  display: flex
  flex-wrap: wrap
  justify-content: center
  align-items: center
  gap: 4px
  list-style: none
  margin: 0
  padding: 0

.category-pagination__ellipsis
  padding-inline: 8px
</style>

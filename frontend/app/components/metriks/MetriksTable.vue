<script setup lang="ts">
/**
 * MetriksTable — Vuetify data table showing all metrics with filtering, search, and sorting.
 *
 * Clicking a row emits 'select' to add a metric to the chart.
 * Supports provider, group, and tag filtering.
 */
import type { MetrikWithTrend } from '~/types/metriks'
import { formatMetrikValue } from '~/composables/useMetriks'

const props = withDefaults(
  defineProps<{
    /** Full list of enriched metrics. */
    metriks: MetrikWithTrend[]
    /** Optional: only show metrics matching these tags. */
    filterTags?: string[]
    /** Optional: only show metrics matching these groups. */
    filterGroups?: string[]
    /** Optional: only show metrics matching these providers. */
    filterProviders?: string[]
    /** Set of currently selected metric IDs (shown in chart). */
    selectedIds?: Set<string>
  }>(),
  {
    filterTags: () => [],
    filterGroups: () => [],
    filterProviders: () => [],
    selectedIds: () => new Set<string>(),
  }
)

const emit = defineEmits<{
  select: [metrik: MetrikWithTrend]
}>()

const { t } = useI18n()

const search = ref('')

/** Filtered metrics based on search, provider, group and tag filters. */
const filteredMetriks = computed(() => {
  let result = props.metriks

  // Apply provider filter
  if (props.filterProviders.length > 0) {
    result = result.filter(m => props.filterProviders.includes(m.provider))
  }

  // Apply group filter
  if (props.filterGroups.length > 0) {
    result = result.filter(m =>
      props.filterGroups.some((grp: string) => m.groups.includes(grp))
    )
  }

  // Apply tag filter
  if (props.filterTags.length > 0) {
    result = result.filter(m =>
      props.filterTags.some((tag: string) => m.tags.includes(tag))
    )
  }

  // Apply text search
  if (search.value.trim()) {
    const q = search.value.toLowerCase()
    result = result.filter(
      m =>
        m.name.toLowerCase().includes(q) ||
        m.id.toLowerCase().includes(q) ||
        m.description.toLowerCase().includes(q) ||
        m.providerDisplayName.toLowerCase().includes(q)
    )
  }

  return result
})

const headers = computed(() => [
  { title: '', key: 'selected', sortable: false, width: '40px' },
  {
    title: t('metriks.table.provider'),
    key: 'providerDisplayName',
    sortable: true,
  },
  { title: t('metriks.table.name'), key: 'name', sortable: true },
  { title: t('metriks.table.value'), key: 'value', sortable: true },
  { title: t('metriks.table.trend'), key: 'percentChange', sortable: true },
  { title: t('metriks.table.unit'), key: 'unit', sortable: true },
  { title: t('metriks.table.status'), key: 'status', sortable: true },
  { title: '', key: 'drilldown', sortable: false, width: '40px' },
])

function onRowClick(item: MetrikWithTrend): void {
  emit('select', item)
}

function trendDisplay(m: MetrikWithTrend): string {
  if (m.percentChange === null) return '—'
  const sign = m.percentChange > 0 ? '+' : ''
  return `${sign}${m.percentChange.toFixed(1)}%`
}

function trendColor(m: MetrikWithTrend): string {
  if (m.percentChange === null) return 'grey'
  if (m.percentChange > 0) return 'success'
  if (m.percentChange < 0) return 'error'
  return 'grey'
}
</script>

<template>
  <v-card variant="outlined" rounded="lg">
    <v-card-title class="d-flex align-center ga-3 flex-wrap pa-4">
      <v-icon icon="mdi-table-large" color="primary" />
      <span>{{ t('metriks.table.title') }}</span>
      <v-spacer />
      <v-text-field
        v-model="search"
        :label="t('metriks.table.search')"
        prepend-inner-icon="mdi-magnify"
        variant="outlined"
        density="compact"
        hide-details
        clearable
        style="max-width: 300px"
      />
    </v-card-title>

    <v-data-table
      :items="filteredMetriks"
      :headers="headers"
      :search="undefined"
      density="comfortable"
      hover
      items-per-page="-1"
      class="metriks-table"
      @click:row="
        (_e: Event, { item }: { item: MetrikWithTrend }) => onRowClick(item)
      "
    >
      <!-- Selected indicator -->
      <template #[`item.selected`]="{ item }">
        <v-icon
          v-if="selectedIds.has(item.id)"
          icon="mdi-chart-line"
          color="primary"
          size="18"
        />
      </template>

      <!-- Value column -->
      <template #[`item.value`]="{ item }">
        <span
          class="font-weight-bold"
          :class="{ 'text-error': item.status === 'error' }"
        >
          {{ formatMetrikValue(item.value, item.unit) }}
        </span>
      </template>

      <!-- Trend column -->
      <template #[`item.percentChange`]="{ item }">
        <v-chip
          v-if="item.percentChange !== null"
          :color="trendColor(item)"
          size="x-small"
          variant="tonal"
        >
          {{ trendDisplay(item) }}
        </v-chip>
        <span v-else class="text-medium-emphasis">—</span>
      </template>

      <!-- Status column -->
      <template #[`item.status`]="{ item }">
        <v-icon
          :icon="item.status === 'ok' ? 'mdi-check-circle' : 'mdi-alert-circle'"
          :color="item.status === 'ok' ? 'success' : 'error'"
          size="18"
        />
      </template>

      <!-- Name column with tooltip -->
      <template #[`item.name`]="{ item }">
        <v-tooltip :text="item.description" location="top" max-width="400">
          <template #activator="{ props: tp }">
            <span
              v-bind="tp"
              class="text-truncate d-inline-block"
              style="max-width: 350px; cursor: pointer"
            >
              {{ item.name }}
            </span>
          </template>
        </v-tooltip>
      </template>

      <!-- Drilldown link column -->
      <template #[`item.drilldown`]="{ item }">
        <v-tooltip
          v-if="item.url"
          :text="t('metriks.toolbar.drilldown')"
          location="top"
        >
          <template #activator="{ props: tp }">
            <v-btn
              v-bind="tp"
              :href="item.url"
              target="_blank"
              rel="noopener noreferrer"
              icon
              size="x-small"
              variant="text"
              @click.stop
            >
              <v-icon icon="mdi-open-in-new" size="16" />
            </v-btn>
          </template>
        </v-tooltip>
      </template>
    </v-data-table>
  </v-card>
</template>

<style scoped>
.metriks-table :deep(tr) {
  cursor: pointer;
}
</style>

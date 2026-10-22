<script setup lang="ts">
/**
 * MetriksTopRows — Table of a Search Console top list (`seo.gsc.top_*`),
 * built from the event's `params.rows`.
 *
 * A `previousClicks` of `null` means the key was absent from the previous
 * window: it is shown as "new", never as a change from zero.
 */
import type { MetrikWithTrend } from '~/types/metriks'
import { parseTopRows, topRowClicksEvolution } from '~/composables/useMetriks'

const props = defineProps<{
  /** A `seo.gsc.top_queries` / `seo.gsc.top_pages` metric. */
  metrik: MetrikWithTrend
}>()

const { t, n } = useI18n()

const isError = computed(() => props.metrik.status === 'error')
const rows = computed(() => parseTopRows(props.metrik.params))
const isPages = computed(() => props.metrik.id.endsWith('top_pages'))

const headers = computed(() => [
  { title: t('metriks.top.rank'), key: 'rank', align: 'end' as const },
  {
    title: isPages.value ? t('metriks.top.page') : t('metriks.top.query'),
    key: 'key',
  },
  { title: t('metriks.top.clicks'), key: 'clicks', align: 'end' as const },
  {
    title: t('metriks.top.impressions'),
    key: 'impressions',
    align: 'end' as const,
  },
  { title: t('metriks.top.ctr'), key: 'ctr', align: 'end' as const },
  { title: t('metriks.top.position'), key: 'position', align: 'end' as const },
  {
    title: t('metriks.top.evolution'),
    key: 'evolution',
    sortable: false,
    align: 'end' as const,
  },
])

const decimals = (v: number) =>
  n(v, { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function evolutionOf(row: ReturnType<typeof parseTopRows>[number]) {
  const evo = topRowClicksEvolution(row)
  if (evo.kind === 'new') {
    return { label: t('metriks.top.new'), color: 'info' }
  }
  const sign = evo.delta > 0 ? '+' : ''
  return {
    label: `${sign}${n(evo.delta)}`,
    color: evo.delta > 0 ? 'success' : evo.delta < 0 ? 'error' : 'grey',
  }
}
</script>

<template>
  <v-card
    variant="outlined"
    rounded="lg"
    :data-testid="`metriks-top-${metrik.id}`"
  >
    <v-card-title class="d-flex align-center ga-2 text-subtitle-1">
      <v-icon icon="mdi-google" color="primary" />
      <span>{{ metrik.name }}</span>
    </v-card-title>
    <v-card-text v-if="isError">
      <v-alert type="error" variant="tonal" density="comfortable">
        {{ metrik.errorMessage || t('metriks.error.unknown') }}
      </v-alert>
    </v-card-text>
    <v-data-table
      v-else
      :items="rows"
      :headers="headers"
      density="comfortable"
      items-per-page="-1"
      hide-default-footer
      :no-data-text="t('metriks.table.noData')"
    >
      <template #[`item.rank`]="{ item }">{{ item.rank }}</template>
      <template #[`item.key`]="{ item }">
        <span class="metriks-top__key">{{ item.key }}</span>
      </template>
      <template #[`item.clicks`]="{ item }">{{ n(item.clicks) }}</template>
      <template #[`item.impressions`]="{ item }">
        {{ n(item.impressions) }}
      </template>
      <template #[`item.ctr`]="{ item }">{{ decimals(item.ctr) }}%</template>
      <template #[`item.position`]="{ item }">
        {{ decimals(item.position) }}
      </template>
      <template #[`item.evolution`]="{ item }">
        <v-chip
          :color="evolutionOf(item).color"
          size="x-small"
          variant="tonal"
          data-testid="metriks-top-evolution"
        >
          {{ evolutionOf(item).label }}
        </v-chip>
      </template>
    </v-data-table>
  </v-card>
</template>

<style scoped>
.metriks-top__key {
  display: inline-block;
  max-width: 420px;
  overflow-wrap: anywhere;
}
</style>

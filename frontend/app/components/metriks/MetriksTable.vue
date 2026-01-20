<template>
  <v-data-table
    :headers="headers"
    :items="tableRows"
    :items-per-page="itemsPerPage"
    class="metriks-table"
    density="comfortable"
    :fixed-header="true"
    height="600"
  >
    <template #[`item.metric`]="{ item }">
      <div class="metriks-table__metric">
        <div class="metriks-table__metric-text">
          <div class="metriks-table__metric-name">{{ item.name }}</div>
          <div class="metriks-table__metric-description">
            {{ item.description }}
          </div>
        </div>
        <v-btn
          v-if="item.eventUrl"
          icon
          variant="text"
          size="small"
          :aria-label="t('metriks.table.openExternal')"
          class="metriks-table__metric-link"
          @click="openExternal(item.eventUrl)"
        >
          <v-icon icon="mdi-open-in-new" size="18" />
        </v-btn>
      </div>
    </template>

    <template
      v-for="dateKey in columns"
      :key="dateKey"
      #[`item.${dateKey}`]="{ item }"
    >
      <div class="metriks-table__cell">
        <v-tooltip v-if="item.payloads[dateKey]" location="top">
          <template #activator="{ props: tooltipProps }">
            <div v-bind="tooltipProps" class="metriks-table__cell-content">
              <div class="metriks-table__cell-value">
                {{ formatValue(item.values[dateKey]) }}
                <span class="metriks-table__cell-unit">{{ item.unit }}</span>
              </div>
              <v-chip
                v-if="item.variations[dateKey]"
                size="x-small"
                class="metriks-table__variation"
                :color="item.variations[dateKey]?.color"
                variant="tonal"
              >
                {{ item.variations[dateKey]?.label }}
              </v-chip>
            </div>
          </template>
          <pre class="metriks-table__payload">{{ item.payloads[dateKey] }}</pre>
        </v-tooltip>
        <div v-else class="metriks-table__cell-content">
          <div class="metriks-table__cell-value">
            {{ formatValue(item.values[dateKey]) }}
            <span class="metriks-table__cell-unit">{{ item.unit }}</span>
          </div>
          <v-chip
            v-if="item.variations[dateKey]"
            size="x-small"
            class="metriks-table__variation"
            :color="item.variations[dateKey]?.color"
            variant="tonal"
          >
            {{ item.variations[dateKey]?.label }}
          </v-chip>
        </div>
      </div>
    </template>
  </v-data-table>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { MetriksReportDto, MetriksReportRowDto } from '~~/shared/api-client'

interface MetriksTableRow {
  id: string
  name: string
  description: string
  unit: string
  eventUrl?: string
  values: Record<string, number | null>
  variations: Record<
    string,
    { label: string; color: string } | null | undefined
  >
  payloads: Record<string, string | null>
}

const props = defineProps<{
  report: MetriksReportDto
  itemsPerPage?: number
}>()

const { t, n } = useI18n()

const columns = computed(() => props.report.columns ?? [])

const headers = computed(() => [
  {
    title: t('metriks.table.metric'),
    key: 'metric',
    sortable: false,
  },
  ...columns.value.map(column => ({
    title: column,
    key: column,
    sortable: false,
  })),
])

const tableRows = computed<MetriksTableRow[]>(() =>
  (props.report.rows ?? []).map(row => toTableRow(row, columns.value))
)

const itemsPerPage = computed(() => props.itemsPerPage ?? 50)

const formatValue = (value: number | null | undefined) => {
  if (value == null) {
    return '—'
  }

  return n(value)
}

const openExternal = (url: string) => {
  if (!url) {
    return
  }
  window.open(url, '_blank', 'noopener')
}

const toTableRow = (row: MetriksReportRowDto, columns: string[]):
  MetriksTableRow => {
  const values: Record<string, number | null> = {}
  const variations: Record<string, { label: string; color: string } | null> = {}
  const payloads: Record<string, string | null> = {}

  const valuesByDate = new Map(
    (row.values ?? []).map(value => [value.dateKey ?? '', value])
  )

  columns.forEach(column => {
    const cell = valuesByDate.get(column)
    values[column] = cell?.value ?? null
    payloads[column] = cell?.payload
      ? JSON.stringify(cell.payload, null, 2)
      : null

    if (cell?.variationPct == null) {
      variations[column] = null
      return
    }

    const variation = cell.variationPct
    const color = variation > 0 ? 'success' : variation < 0 ? 'error' : 'grey'
    const label = `${variation > 0 ? '+' : ''}${n(variation)}%`

    variations[column] = { label, color }
  })

  return {
    id: `${row.provider ?? 'unknown'}:${row.eventId ?? 'unknown'}`,
    name: row.name ?? row.eventId ?? '',
    description: row.description ?? '',
    unit: row.unit ?? '',
    eventUrl: row.eventUrl ?? undefined,
    values,
    variations,
    payloads,
  }
}
</script>

<style scoped>
.metriks-table {
  --v-data-table-header-background: transparent;
}

.metriks-table__metric {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.metriks-table__metric-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metriks-table__metric-name {
  font-weight: 600;
}

.metriks-table__metric-description {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
  font-size: 0.875rem;
}

.metriks-table__metric-link {
  align-self: flex-start;
}

.metriks-table__cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metriks-table__cell-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metriks-table__cell-value {
  font-weight: 600;
}

.metriks-table__cell-unit {
  margin-left: 4px;
  color: rgba(var(--v-theme-text-neutral-soft), 0.7);
  font-size: 0.75rem;
}

.metriks-table__variation {
  align-self: flex-start;
  font-weight: 600;
}

.metriks-table__payload {
  white-space: pre-wrap;
  max-width: 360px;
  font-size: 0.75rem;
}
</style>

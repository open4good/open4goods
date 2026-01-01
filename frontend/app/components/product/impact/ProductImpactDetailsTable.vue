<template>
  <article class="impact-details">
    <h3 class="impact-details__title">
      {{ $t('product.impact.detailsTitle') }}
    </h3>
    <v-data-table
      v-if="hasRows"
      :headers="headers"
      :items="tableItems"
      :items-per-page="itemsPerPage"
      class="impact-details__table"
      density="compact"
      hide-default-footer
    >
      <template #[`item.label`]="{ item }">
        <div
          class="impact-details__label"
          :class="{
            'impact-details__label--child': item.rowType === 'subscore',
            'impact-details__label--aggregate': item.rowType === 'aggregate',
          }"
        >
          <v-btn
            v-if="item.rowType === 'aggregate'"
            class="impact-details__toggle"
            icon
            density="comfortable"
            variant="text"
            :aria-label="
              isGroupExpanded(item.id)
                ? $t('product.impact.hideDetails')
                : $t('product.impact.subscoreDetailsToggle')
            "
            @click="toggleGroup(item.id)"
          >
            <v-icon
              :icon="
                isGroupExpanded(item.id) ? 'mdi-chevron-up' : 'mdi-chevron-down'
              "
              size="18"
            />
          </v-btn>
          <span class="impact-details__indicator">{{ item.label }}</span>
        </div>
      </template>
      <template #[`item.attributeValue`]="{ item }">
        <span
          class="impact-details__attribute"
          :class="{
            'impact-details__cell--child': item.rowType === 'subscore',
          }"
        >
          {{ item.attributeValue }}
        </span>
      </template>
      <template #[`item.displayValue`]="{ item }">
        <div
          class="impact-details__value"
          :class="{
            'impact-details__cell--child': item.rowType === 'subscore',
          }"
        >
          <ProductImpactSubscoreRating
            v-if="item.displayValue != null"
            :score="item.displayValue"
            :max="5"
            size="x-small"
            :show-value="false"
          />
          <span class="impact-details__value-text">{{
            formatScoreLabel(item.displayValue)
          }}</span>
        </div>
      </template>
      <template #[`item.coefficient`]="{ item }">
        <div
          class="impact-details__coefficient"
          :class="{
            'impact-details__cell--child': item.rowType === 'subscore',
          }"
        >
          <ImpactCoefficientBadge
            v-if="item.coefficient != null"
            :value="item.coefficient"
            :tooltip-params="{ scoreName: item.label }"
          />
          <span v-else class="impact-details__coefficient-empty">—</span>
        </div>
      </template>
      <template #[`item.lifecycle`]="{ item }">
        <div
          class="impact-details__lifecycle"
          :class="{
            'impact-details__cell--child': item.rowType === 'subscore',
          }"
        >
          <template v-if="item.lifecycle?.length">
            <v-chip
              v-for="stage in item.lifecycle"
              :key="`${item.id}-${stage}`"
              :color="lifecycleColors[stage] ?? 'surface-ice-100'"
              size="x-small"
              variant="tonal"
            >
              {{ lifecycleLabels[stage] ?? stage }}
            </v-chip>
          </template>
          <span v-else class="impact-details__coefficient-empty">—</span>
        </div>
      </template>
    </v-data-table>
    <p v-else class="impact-details__empty">
      {{ $t('product.impact.noDetailsAvailable') }}
    </p>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ImpactCoefficientBadge from '~/components/shared/ui/ImpactCoefficientBadge.vue'
import ProductImpactSubscoreRating from './ProductImpactSubscoreRating.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  scores: ScoreView[]
}>()

type DetailedScore = ScoreView & {
  displayValue: number | null
  coefficient: number | null
}
type GroupedRows = {
  groups: Array<{
    id: string
    aggregate: DetailedScore | null
    subscores: DetailedScore[]
  }>
  divers: DetailedScore[]
}
type TableRow = {
  id: string
  label: string
  attributeValue: string
  displayValue: number | null
  coefficient: number | null
  lifecycle: string[]
  rowType: 'aggregate' | 'subscore' | 'standalone'
  parentId?: string
}

const resolveScoreValue = (score: ScoreView | null): number | null => {
  if (!score) {
    return null
  }

  if (score.relativeValue != null && Number.isFinite(score.relativeValue)) {
    return Number(score.relativeValue)
  }

  if (score.value != null && Number.isFinite(score.value)) {
    return Number(score.value)
  }

  return null
}

const resolveCoefficientValue = (
  value: number | null | undefined
): number | null => {
  if (value == null) {
    return null
  }

  const numeric = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numeric)) {
    return null
  }

  return Math.min(Math.max(numeric, 0), 1)
}

const { t } = useI18n()
const expandedGroups = ref<Set<string>>(new Set())

const lifecycleLabels = computed<Record<string, string>>(() => ({
  EXTRACTION: t('product.impact.lifecycle.EXTRACTION'),
  MANUFACTURING: t('product.impact.lifecycle.MANUFACTURING'),
  TRANSPORTATION: t('product.impact.lifecycle.TRANSPORTATION'),
  USE: t('product.impact.lifecycle.USE'),
  END_OF_LIFE: t('product.impact.lifecycle.END_OF_LIFE'),
}))

const lifecycleColors: Record<string, string> = {
  EXTRACTION: 'warning',
  MANUFACTURING: 'secondary',
  TRANSPORTATION: 'info',
  USE: 'primary',
  END_OF_LIFE: 'success',
}

const DIVERS_AGGREGATE_ID = 'DIVERS'

const toFiniteNumber = (value: number | null | undefined): number | null => {
  if (value == null) {
    return null
  }

  const parsed = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const normalizeParticipations = (participations?: string[] | null): string[] =>
  (participations ?? [])
    .map(entry => entry?.toString().trim().toUpperCase())
    .filter((entry): entry is string => Boolean(entry))

const formatAttributeValue = (score: ScoreView) => {
  const raw = score.attributeValue?.toString().trim()
  if (!raw) {
    return '—'
  }

  const suffix =
    score.attributeSuffix?.toString().trim() ||
    score.unit?.toString().trim() ||
    ''
  return suffix.length ? `${raw} ${suffix}` : raw
}

const resolveAggregateLabel = (
  aggregateId: string,
  aggregateScore: DetailedScore | null
) => {
  const translationKey = `product.impact.aggregateScores.${aggregateId}`
  if (t(translationKey) !== translationKey) {
    return t(translationKey)
  }

  return aggregateScore?.label ?? aggregateId
}

const displayScores = computed<DetailedScore[]>(() =>
  props.scores
    .filter(score => score.id !== 'ECOSCORE')
    .map(score => ({
      ...score,
      displayValue: resolveScoreValue(score),
      coefficient: resolveCoefficientValue(score.coefficient ?? null),
    }))
)

const groupedScores = computed<GroupedRows>(() => {
  const scoreMap = displayScores.value.reduce<Map<string, DetailedScore>>(
    (map, score) => {
      const normalizedId = score.id?.toString().trim().toUpperCase()
      if (normalizedId) {
        map.set(normalizedId, score)
      }

      return map
    },
    new Map()
  )

  const aggregateSet = new Set<string>()
  const participationMap = new Map<string, DetailedScore[]>()
  const standalone: DetailedScore[] = []

  displayScores.value.forEach(score => {
    const participations = normalizeParticipations(score.participateInScores)

    if (!participations.length) {
      standalone.push(score)
      return
    }

    participations.forEach(aggregateId => {
      aggregateSet.add(aggregateId)

      if (!participationMap.has(aggregateId)) {
        participationMap.set(aggregateId, [])
      }

      participationMap.get(aggregateId)?.push(score)
    })
  })

  const groups = Array.from(participationMap.entries()).map(
    ([id, subscores]) => ({
      id,
      aggregate: scoreMap.get(id) ?? null,
      subscores,
    })
  )

  const filteredStandalone = standalone.filter(score => {
    const normalizedId = score.id?.toString().trim().toUpperCase()
    return normalizedId && !aggregateSet.has(normalizedId)
  })

  return { groups, divers: filteredStandalone }
})

const headers = computed(() => [
  {
    key: 'label',
    title: t('product.impact.tableHeaders.scoreName'),
    sortable: false,
  },
  {
    key: 'attributeValue',
    title: t('product.impact.tableHeaders.attributeValue'),
    sortable: false,
  },
  {
    key: 'displayValue',
    title: t('product.impact.tableHeaders.scoreValue'),
    sortable: false,
  },
  {
    key: 'coefficient',
    title: t('product.impact.tableHeaders.coefficient'),
    sortable: false,
  },
  {
    key: 'lifecycle',
    title: t('product.impact.tableHeaders.lifecycle'),
    sortable: false,
  },
])

const buildTableRow = (
  score: DetailedScore,
  rowType: TableRow['rowType'],
  parentId?: string
): TableRow => ({
  id: score.id,
  label: score.label,
  attributeValue: formatAttributeValue(score),
  displayValue: score.displayValue,
  coefficient: score.coefficient,
  lifecycle: score.participateInACV ?? [],
  rowType,
  parentId,
})

const resolveAverage = (values: number[]) =>
  values.reduce((sum, value) => sum + value, 0) / values.length

const resolveAggregateDisplayValue = (
  aggregateId: string,
  aggregateScore: DetailedScore | null,
  subscores: DetailedScore[]
): number | null => {
  const directValue = resolveScoreValue(aggregateScore)
  if (directValue != null) {
    return directValue
  }

  const aggregateValues = subscores
    .map(score => score.aggregates?.[aggregateId])
    .map(toFiniteNumber)
    .filter((value): value is number => value != null)

  if (aggregateValues.length) {
    return resolveAverage(aggregateValues)
  }

  const childValues = subscores
    .map(score => score.displayValue)
    .filter((value): value is number => value != null)

  if (childValues.length) {
    return resolveAverage(childValues)
  }

  return null
}

const buildAggregateRow = (
  aggregateId: string,
  aggregateScore: DetailedScore | null,
  subscores: DetailedScore[]
): TableRow => ({
  id: aggregateId,
  label: resolveAggregateLabel(aggregateId, aggregateScore),
  attributeValue: '—',
  displayValue: resolveAggregateDisplayValue(
    aggregateId,
    aggregateScore,
    subscores
  ),
  coefficient: aggregateScore?.coefficient ?? null,
  lifecycle: aggregateScore?.participateInACV ?? [],
  rowType: 'aggregate',
})

const tableItems = computed<TableRow[]>(() => {
  const rows: TableRow[] = []
  const expanded = new Set(expandedGroups.value)

  groupedScores.value.groups.forEach(group => {
    rows.push(buildAggregateRow(group.id, group.aggregate, group.subscores))

    if (expanded.has(group.id)) {
      rows.push(
        ...group.subscores.map(score =>
          buildTableRow(score, 'subscore', group.id)
        )
      )
    }
  })

  if (groupedScores.value.divers.length) {
    rows.push(
      buildAggregateRow(DIVERS_AGGREGATE_ID, null, groupedScores.value.divers)
    )

    if (expanded.has(DIVERS_AGGREGATE_ID)) {
      rows.push(
        ...groupedScores.value.divers.map(score =>
          buildTableRow(score, 'subscore', DIVERS_AGGREGATE_ID)
        )
      )
    }
  }

  return rows
})

const hasRows = computed(() => tableItems.value.length > 0)
const itemsPerPage = computed(() => Math.max(tableItems.value.length, 1))

const toggleGroup = (groupId: string) => {
  const next = new Set(expandedGroups.value)
  if (next.has(groupId)) {
    next.delete(groupId)
  } else {
    next.add(groupId)
  }

  expandedGroups.value = next
}

const isGroupExpanded = (groupId: string) => expandedGroups.value.has(groupId)

const formatScore = (value: number | null) => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return value.toFixed(1)
}

const formatScoreLabel = (value: number | null) => {
  const formatted = formatScore(value)
  if (formatted === '—') {
    return formatted
  }

  return t('product.impact.valueOutOf', { value: formatted, max: 5 })
}
</script>

<style scoped>
.impact-details {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.impact-details__title {
  margin: 0 0 1rem;
  font-size: 1.2rem;
  font-weight: 600;
}

.impact-details__table {
  background: transparent;
  --v-data-table-header-background: transparent;
}

.impact-details__table :deep(.v-data-table__tr) {
  border-color: rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-details__table :deep(th) {
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.impact-details__table :deep(td) {
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-details__indicator {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
}

.impact-details__label {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.impact-details__label--aggregate {
  font-weight: 700;
}

.impact-details__label--child {
  padding-left: 1.75rem;
}

.impact-details__toggle {
  margin-left: -0.25rem;
}

.impact-details__attribute {
  display: inline-flex;
  align-items: center;
}

.impact-details__value {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.impact-details__value-text {
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-details__coefficient-empty {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.6);
}

.impact-details__coefficient {
  display: inline-flex;
  align-items: center;
}

.impact-details__lifecycle {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}

.impact-details__cell--child {
  padding-left: 1.75rem;
}

.impact-details__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
  font-size: 0.95rem;
}
</style>

<template>
  <article class="impact-details">
    <h3 class="impact-details__title">{{ $t('product.impact.detailsTitle') }}</h3>
    <v-data-table
      v-if="hasRows"
      :headers="headers"
      :items="tableItems"
      :items-per-page="itemsPerPage"
      class="impact-details__table"
      density="compact"
      hide-default-footer
    >
      <template #item="{ item, columns }">
        <tr v-if="item.raw.rowType === 'aggregate'" class="impact-details__row impact-details__row--aggregate">
          <td :colspan="aggregateLabelColspan" class="impact-details__aggregate">
            <div class="impact-details__label impact-details__label--aggregate">
              <v-btn
                class="impact-details__toggle"
                icon
                density="comfortable"
                variant="text"
                :aria-label="
                  isGroupExpanded(item.raw.id)
                    ? $t('product.impact.hideDetails')
                    : $t('product.impact.subscoreDetailsToggle')
                "
                @click="toggleGroup(item.raw.id)"
              >
                <v-icon :icon="isGroupExpanded(item.raw.id) ? 'mdi-chevron-up' : 'mdi-chevron-down'" size="18" />
              </v-btn>
              <span class="impact-details__indicator">{{ item.raw.label }}</span>
            </div>
          </td>
          <td class="impact-details__value impact-details__value--aggregate">
            <ProductImpactSubscoreRating
              v-if="item.raw.displayValue != null"
              :score="item.raw.displayValue"
              :max="5"
              size="x-small"
              :show-value="false"
            />
            <span class="impact-details__value-text">{{ formatScoreLabel(item.raw.displayValue) }}</span>
          </td>
        </tr>
        <tr v-else class="impact-details__row" :class="{ 'impact-details__row--child': item.raw.rowType === 'subscore' }">
          <td v-for="column in columns" :key="column.key">
            <template v-if="column.key === 'label'">
              <div class="impact-details__label" :class="{ 'impact-details__label--child': item.raw.rowType === 'subscore' }">
                <span class="impact-details__indicator">{{ item.raw.label }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'attributeValue'">
              <ProductAttributeSourcingLabel
                class="impact-details__attribute"
                :class="{ 'impact-details__cell--child': item.raw.rowType === 'subscore' }"
                :sourcing="item.raw.attributeSourcing"
                :value="item.raw.attributeValue"
              />
            </template>
            <template v-else-if="column.key === 'displayValue'">
              <div
                class="impact-details__value"
                :class="{ 'impact-details__cell--child': item.raw.rowType === 'subscore' }"
              >
                <ProductImpactSubscoreRating
                  v-if="item.raw.displayValue != null"
                  :score="item.raw.displayValue"
                  :max="5"
                  size="x-small"
                  :show-value="false"
                />
                <span class="impact-details__value-text">{{ formatScoreLabel(item.raw.displayValue) }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'coefficient'">
              <div
                class="impact-details__coefficient"
                :class="{ 'impact-details__cell--child': item.raw.rowType === 'subscore' }"
              >
                <ImpactCoefficientBadge
                  v-if="item.raw.coefficient != null"
                  :value="item.raw.coefficient"
                  :tooltip-params="{ scoreName: item.raw.label }"
                />
                <span v-else class="impact-details__coefficient-empty">—</span>
              </div>
            </template>
            <template v-else-if="column.key === 'lifecycle'">
              <div
                class="impact-details__lifecycle"
                :class="{ 'impact-details__cell--child': item.raw.rowType === 'subscore' }"
              >
                <template v-if="item.raw.lifecycle?.length">
                  <v-chip
                    v-for="stage in item.raw.lifecycle"
                    :key="`${item.raw.id}-${stage}`"
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
          </td>
        </tr>
      </template>
    </v-data-table>
    <p v-else class="impact-details__empty">{{ $t('product.impact.noDetailsAvailable') }}</p>
  </article>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCategories } from '~/composables/categories/useCategories'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
import ImpactCoefficientBadge from '~/components/shared/ui/ImpactCoefficientBadge.vue'
import ProductImpactSubscoreRating from './ProductImpactSubscoreRating.vue'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  scores: ScoreView[]
}>()

type DetailedScore = ScoreView & { displayValue: number | null; coefficient: number | null }
type GroupedRows = {
  groups: Array<{ id: string; aggregate: DetailedScore | null; subscores: DetailedScore[] }>
  standalone: DetailedScore[]
}
type TableRow = {
  id: string
  label: string
  attributeValue: string
  attributeSourcing: ScoreView['attributeSourcing'] | null
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

const resolveCoefficientValue = (value: number | null | undefined): number | null => {
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
const { currentCategory } = useCategories()
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

const normalizeId = (value?: string | null): string => value?.toString().trim().toUpperCase() ?? ''

const normalizeParticipations = (participations?: string[] | null): string[] =>
  (participations ?? [])
    .map((entry) => entry?.toString().trim().toUpperCase())
    .filter((entry): entry is string => Boolean(entry))

const attributeLifecycleMap = computed<Map<string, string[]>>(() => {
  const configs = currentCategory.value?.attributesConfig?.configs ?? []

  return configs.reduce((map, config) => {
    const normalizedKey = normalizeId(config.key)
    if (!normalizedKey) {
      return map
    }

    const stages = normalizeParticipations(config.participateInACV ? Array.from(config.participateInACV) : [])
    if (stages.length) {
      map.set(normalizedKey, stages)
    }

    return map
  }, new Map<string, string[]>())
})

const resolveLifecycleStages = (scoreId: string, lifecycle?: string[] | null) => {
  const normalizedId = normalizeId(scoreId)
  const normalizedLifecycle = normalizeParticipations(lifecycle)

  if (normalizedLifecycle.length) {
    return normalizedLifecycle
  }

  return normalizedId ? attributeLifecycleMap.value.get(normalizedId) ?? [] : []
}

const formatAttributeValue = (score: ScoreView) => {
  const raw = score.attributeValue?.toString().trim()
  if (!raw) {
    return '—'
  }

  const suffix = score.attributeSuffix?.toString().trim() || score.unit?.toString().trim() || ''
  return suffix.length ? `${raw} ${suffix}` : raw
}

const resolveAggregateLabel = (aggregateId: string, aggregateScore: DetailedScore | null) => {
  const translationKey = `product.impact.aggregateScores.${aggregateId}`
  if (t(translationKey) !== translationKey) {
    return t(translationKey)
  }

  return aggregateScore?.label ?? aggregateId
}

const displayScores = computed<DetailedScore[]>(() =>
  props.scores
    .filter((score) => score.id !== 'ECOSCORE')
    .map((score) => ({
      ...score,
      displayValue: resolveScoreValue(score),
      coefficient: resolveCoefficientValue(score.coefficient ?? null),
    })),
)

const groupedScores = computed<GroupedRows>(() => {
  const scoreMap = displayScores.value.reduce<Map<string, DetailedScore>>((map, score) => {
    const normalizedId = normalizeId(score.id)
    if (normalizedId) {
      map.set(normalizedId, score)
    }

    return map
  }, new Map())

  const groupMap = new Map<string, { id: string; aggregate: DetailedScore | null; subscores: DetailedScore[] }>()
  const consumedSubscores = new Set<string>()

  const resolveAggregateSubscores = (score: DetailedScore): DetailedScore[] => {
    const aggregateEntries = Object.keys(score.aggregates ?? {})
      .map((aggregateId) => normalizeId(aggregateId))
      .filter((aggregateId) => aggregateId.length > 0)

    if (!aggregateEntries.length) {
      return []
    }

    const subscores = aggregateEntries
      .map((aggregateId) => scoreMap.get(aggregateId))
      .filter((entry): entry is DetailedScore => Boolean(entry))

    return subscores
  }

  const upsertGroup = (aggregateId: string, aggregateScore: DetailedScore | null, subscore: DetailedScore) => {
    const existing = groupMap.get(aggregateId)
    const currentSubscores = existing?.subscores ?? []
    const normalizedSubscoreId = normalizeId(subscore.id)

    if (!currentSubscores.some((entry) => normalizeId(entry.id) === normalizedSubscoreId)) {
      currentSubscores.push(subscore)
    }

    groupMap.set(aggregateId, {
      id: aggregateId,
      aggregate: aggregateScore ?? existing?.aggregate ?? null,
      subscores: currentSubscores,
    })
    consumedSubscores.add(normalizedSubscoreId)
  }

  displayScores.value.forEach((score) => {
    const normalizedId = normalizeId(score.id)
    if (!normalizedId) {
      return
    }

    const aggregateSubscores = resolveAggregateSubscores(score)
    if (aggregateSubscores.length) {
      aggregateSubscores.forEach((subscore) => upsertGroup(normalizedId, score, subscore))
      return
    }

    const participations = normalizeParticipations(score.participateInScores)
    participations.forEach((aggregateId) => {
      const normalizedAggregateId = normalizeId(aggregateId)
      if (!normalizedAggregateId) {
        return
      }

      const aggregateScore = scoreMap.get(normalizedAggregateId) ?? null
      upsertGroup(normalizedAggregateId, aggregateScore, score)
    })
  })

  const groups = Array.from(groupMap.values())

  const standalone = displayScores.value.filter((score) => {
    const normalizedId = normalizeId(score.id)
    return normalizedId && !consumedSubscores.has(normalizedId) && !groupMap.has(normalizedId)
  })

  return { groups, standalone }
})

const headers = computed(() => [
  { key: 'label', title: t('product.impact.tableHeaders.scoreName'), sortable: false },
  { key: 'attributeValue', title: t('product.impact.tableHeaders.attributeValue'), sortable: false },
  { key: 'displayValue', title: t('product.impact.tableHeaders.scoreValue'), sortable: false },
  { key: 'coefficient', title: t('product.impact.tableHeaders.coefficient'), sortable: false },
  { key: 'lifecycle', title: t('product.impact.tableHeaders.lifecycle'), sortable: false },
])

const aggregateLabelColspan = computed(() => Math.max(headers.value.length - 1, 1))

const buildTableRow = (
  score: DetailedScore,
  rowType: TableRow['rowType'],
  parentId?: string,
): TableRow => ({
  id: normalizeId(score.id) || score.id,
  label: score.label,
  attributeValue: formatAttributeValue(score),
  attributeSourcing: score.attributeSourcing ?? null,
  displayValue: score.displayValue,
  coefficient: score.coefficient,
  lifecycle: resolveLifecycleStages(score.id, score.participateInACV),
  rowType,
  parentId,
})

const buildAggregateRow = (aggregateId: string, aggregateScore: DetailedScore | null): TableRow => ({
  id: normalizeId(aggregateScore?.id ?? aggregateId) || aggregateId,
  label: resolveAggregateLabel(aggregateId, aggregateScore),
  attributeValue: '—',
  attributeSourcing: aggregateScore?.attributeSourcing ?? null,
  displayValue: resolveScoreValue(aggregateScore),
  coefficient: aggregateScore?.coefficient ?? null,
  lifecycle: resolveLifecycleStages(aggregateId, aggregateScore?.participateInACV),
  rowType: 'aggregate',
})

const tableItems = computed<TableRow[]>(() => {
  const rows: TableRow[] = []
  const expanded = new Set(expandedGroups.value)

  groupedScores.value.groups.forEach((group) => {
    rows.push(buildAggregateRow(group.id, group.aggregate))

    if (expanded.has(group.id)) {
      rows.push(...group.subscores.map((score) => buildTableRow(score, 'subscore', group.id)))
    }
  })

  rows.push(...groupedScores.value.standalone.map((score) => buildTableRow(score, 'standalone')))

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

.impact-details__row--aggregate {
  background: rgba(var(--v-theme-surface-primary-050), 0.5);
}

.impact-details__aggregate {
  padding-left: 0.25rem;
}

.impact-details__value--aggregate {
  white-space: nowrap;
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

.impact-details__row--child :deep(td:first-child) {
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

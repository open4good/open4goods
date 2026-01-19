<template>
  <article class="impact-details">
    <header class="impact-details__header">
      <h4 class="impact-details__title">
        {{ $t('product.impact.detailsTitle') }}
      </h4>
      <v-btn
        v-if="hasVirtualScores"
        variant="text"
        density="compact"
        class="impact-details__virtual-toggle"
        color="primary"
        :prepend-icon="
          showVirtualScores ? 'mdi-check-circle-outline' : 'mdi-circle-outline'
        "
        @click="showVirtualScores = !showVirtualScores"
      >
        {{ $t('product.impact.showVirtualScores') }}
      </v-btn>
    </header>
    <v-data-table
      v-if="hasRows"
      v-model:expanded="expandedSubscores"
      :headers="headers"
      :items="tableItems"
      :items-per-page="itemsPerPage"
      :row-props="rowProps"
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
            'cursor-pointer':
              item.rowType === 'aggregate' || item.rowType === 'subscore',
          }"
          @click="
            item.rowType === 'aggregate'
              ? toggleGroup(item.id)
              : toggleSubscoreExpansion(item)
          "
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
            @click.stop="toggleGroup(item.id)"
          >
            <v-icon
              :icon="
                isGroupExpanded(item.id) ? 'mdi-chevron-up' : 'mdi-chevron-down'
              "
              size="18"
            />
          </v-btn>

          <v-btn
            v-else-if="item.rowType === 'subscore'"
            class="impact-details__toggle"
            icon
            density="comfortable"
            variant="text"
            :aria-label="
              expandedSubscores.includes(item.id)
                ? $t('product.impact.hideDetails')
                : $t('product.impact.subscoreDetailsToggle')
            "
            @click.stop="toggleSubscoreExpansion(item)"
          >
            <v-icon
              :icon="
                expandedSubscores.includes(item.id)
                  ? 'mdi-chevron-up'
                  : 'mdi-chevron-down'
              "
              size="18"
            />
          </v-btn>
          <span class="impact-details__indicator">
            {{ item.label }}
            <v-icon
              v-if="item.virtual"
              icon="mdi-flask-outline"
              size="16"
              class="ml-2 text-disabled"
              :title="$t('product.impact.showVirtualScores')"
            />
          </span>
        </div>
      </template>
      <template #[`item.attributeValue`]="{ item }">
        <span
          class="impact-details__attribute"
          :class="{
            'impact-details__cell--child': item.rowType === 'subscore',
          }"
        >
          <ProductAttributeSourcingLabel
            v-if="item.attributeSourcing"
            :sourcing="item.attributeSourcing"
            :value="item.attributeValue"
          />
          <span v-else>{{ item.attributeValue }}</span>
        </span>
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
          <span
            v-else-if="item.rowType !== 'aggregate'"
            class="impact-details__coefficient-empty"
            >—</span
          >
        </div>
      </template>

      <template #[`item.scoreOn20`]="{ item }">
        <div
          class="impact-details__score-on-20"
          :class="{
            'impact-details__score-on-20--aggregate':
              item.rowType === 'aggregate',
          }"
        >
          <v-chip
            v-if="item.scoreOn20 != null"
            :color="
              item.scoreOn20 < 10
                ? 'error'
                : item.scoreOn20 < 15
                  ? 'warning'
                  : 'success'
            "
            variant="flat"
            size="small"
            class="font-weight-bold"
          >
            {{ item.scoreOn20.toFixed(1) }} / 20
          </v-chip>
          <span v-else class="text-caption text-disabled">—</span>
        </div>
      </template>

      <template #expanded-row="{ columns, item }">
        <tr class="impact-details__expanded-row">
          <td :colspan="columns.length" class="pa-4 bg-surface-light">
            <ProductImpactSubscoreCard
              :score="item"
              :product-name="productName"
              :product-brand="productBrand"
              :product-model="productModel"
              :product-image="productImage"
              :vertical-title="verticalTitle"
              class="mx-auto"
              style="max-width: 800px"
            />
          </td>
        </tr>
      </template>
    </v-data-table>
    <p v-else class="impact-details__empty">
      {{ $t('product.impact.noDetailsAvailable') }}
    </p>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import ProductAttributeSourcingLabel from '~/components/product/attributes/ProductAttributeSourcingLabel.vue'
import ProductImpactSubscoreCard from './ProductImpactSubscoreCard.vue'
import type { ScoreView } from './impact-types'
import {
  buildImpactAggregateAnchorId,
  buildImpactScoreGroups,
} from './impact-score-groups'

const props = defineProps<{
  scores: ScoreView[]
  productName?: string
  productBrand?: string
  productModel?: string
  productImage?: string
  verticalTitle?: string
  expandedScoreId?: string | null
}>()

const productName = computed(() => props.productName ?? '')
const productBrand = computed(() => props.productBrand ?? '')
const productModel = computed(() => props.productModel ?? '')
const productImage = computed(() => props.productImage ?? '')
const verticalTitle = computed(() => props.verticalTitle ?? '')

type DetailedScore = ScoreView & {
  displayValue: number | null
  coefficient: number | null
  scoreOn20: number | null
  virtual?: boolean
}
type TableRow = {
  id: string
  label: string
  attributeValue: string
  attributeSourcing: ScoreView['attributeSourcing']
  displayValue: number | null
  coefficient: number | null
  scoreOn20: number | null
  lifecycle: string[]
  virtual?: boolean
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
const expandedSubscores = ref<string[]>([])
const showVirtualScores = ref(false)

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

const displayScores = computed<DetailedScore[]>(() =>
  props.scores
    .filter(score => {
      if (score.id === 'ECOSCORE') return false
      if (!showVirtualScores.value && score.virtual) return false
      return true
    })
    .map(score => ({
      ...score,
      displayValue: resolveScoreValue(score),
      coefficient: resolveCoefficientValue(score.coefficient ?? null),
      scoreOn20:
        score.on20 != null && Number.isFinite(Number(score.on20))
          ? Number(score.on20)
          : null,
      virtual: score.virtual,
    }))
)

const hasVirtualScores = computed(() =>
  props.scores.some(score => score.virtual && score.id !== 'ECOSCORE')
)

const groupedScores = computed(() =>
  buildImpactScoreGroups(displayScores.value, t)
)

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
    key: 'lifecycle',
    title: t('product.impact.tableHeaders.lifecycle'),
    sortable: false,
  },
  {
    key: 'scoreOn20',
    title: t('product.impact.tableHeaders.scoreOn20'),
    align: 'end',
    sortable: false,
  },
])

const buildTableRow = (
  score: DetailedScore,
  rowType: TableRow['rowType'],
  parentId?: string
): TableRow => ({
  ...score,
  id: score.id,
  label: score.label,
  attributeValue: formatAttributeValue(score),
  attributeSourcing: score.attributeSourcing ?? null,
  displayValue: score.displayValue,
  coefficient: score.coefficient,
  scoreOn20: score.scoreOn20,
  lifecycle: score.participateInACV ?? [],
  virtual: score.virtual,
  rowType,
  parentId,
})

const resolveAverage = (values: number[]) =>
  values.reduce((sum, value) => sum + value, 0) / values.length

const resolveAggregateDisplayValue = (
  aggregateId: string,
  aggregateScore: DetailedScore | null,
  subscores: DetailedScore[]
): { displayValue: number | null; scoreOn20: number | null } => {
  const result = {
    displayValue: null as number | null,
    scoreOn20: null as number | null,
  }

  const directValue = resolveScoreValue(aggregateScore)
  if (directValue != null) {
    result.displayValue = directValue
  }

  if (
    aggregateScore?.on20 != null &&
    Number.isFinite(Number(aggregateScore.on20))
  ) {
    result.scoreOn20 = Number(aggregateScore.on20)
  }

  if (result.displayValue == null) {
    const aggregateValues = subscores
      .map(score =>
        score.aggregates?.[aggregateId] &&
        Number.isFinite(Number(score.aggregates[aggregateId]))
          ? Number(score.aggregates[aggregateId])
          : null
      )
      .filter((value): value is number => value != null)

    if (aggregateValues.length) {
      result.displayValue = resolveAverage(aggregateValues)
    } else {
      const childValues = subscores
        .map(score => score.displayValue)
        .filter((value): value is number => value != null)

      if (childValues.length) {
        result.displayValue = resolveAverage(childValues)
      }
    }
  }

  // Fallback for aggregations if not present on parent
  // We prioritize direct value, but if missing we might need to compute
  // For scoreOn20, usually it is on the aggregate score. If not, average subscores.

  if (result.scoreOn20 == null) {
    const subScoresOn20 = subscores
      .map(s => s.scoreOn20)
      .filter((v): v is number => v != null)

    if (subScoresOn20.length) {
      result.scoreOn20 = resolveAverage(subScoresOn20)
    }
  }

  return result
}

const buildAggregateRow = (
  aggregateId: string,
  label: string,
  aggregateScore: DetailedScore | null,
  subscores: DetailedScore[]
): TableRow => {
  const { displayValue, scoreOn20 } = resolveAggregateDisplayValue(
    aggregateId,
    aggregateScore,
    subscores
  )
  return {
    id: aggregateId,
    label,
    attributeValue: '',
    attributeSourcing: null,
    displayValue: displayValue,
    coefficient: aggregateScore?.coefficient ?? null,
    scoreOn20: scoreOn20,
    lifecycle: aggregateScore?.participateInACV ?? [],
    virtual: aggregateScore?.virtual,
    rowType: 'aggregate',
  }
}

const tableItems = computed<TableRow[]>(() => {
  const rows: TableRow[] = []
  const expanded = new Set(expandedGroups.value)

  groupedScores.value.groups.forEach(group => {
    rows.push(
      buildAggregateRow(group.id, group.label, group.aggregate, group.subscores)
    )

    if (expanded.has(group.id)) {
      rows.push(
        ...group.subscores.map(score =>
          buildTableRow(score, 'subscore', group.id)
        )
      )
    }
  })

  if (groupedScores.value.divers) {
    rows.push(
      buildAggregateRow(
        groupedScores.value.divers.id,
        groupedScores.value.divers.label,
        null,
        groupedScores.value.divers.subscores
      )
    )

    if (expanded.has(groupedScores.value.divers.id)) {
      rows.push(
        ...groupedScores.value.divers.subscores.map(score =>
          buildTableRow(score, 'subscore', groupedScores.value.divers?.id)
        )
      )
    }
  }

  return rows
})

const hasRows = computed(() => tableItems.value.length > 0)
const itemsPerPage = computed(() => Math.max(tableItems.value.length, 1))

const rowProps = ({ item }: { item: TableRow }) => ({
  id:
    item.rowType === 'aggregate'
      ? buildImpactAggregateAnchorId(item.id)
      : undefined,
})

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

const toggleSubscoreExpansion = (item: TableRow) => {
  if (item.rowType !== 'subscore') return

  const id = item.id
  const index = expandedSubscores.value.indexOf(id)

  if (index === -1) {
    expandedSubscores.value.push(id)
  } else {
    expandedSubscores.value.splice(index, 1)
  }
}

const expandScore = (scoreId: string) => {
  // Check if it's a group ID
  if (groupedScores.value.groups.some(g => g.id === scoreId)) {
    if (!expandedGroups.value.has(scoreId)) {
      expandedGroups.value.add(scoreId)
      // Trigger reactivity
      expandedGroups.value = new Set(expandedGroups.value)
    }
    return
  }

  // Check if it's the 'divers' group ID
  if (groupedScores.value.divers?.id === scoreId) {
    if (!expandedGroups.value.has(scoreId)) {
      expandedGroups.value.add(scoreId)
      expandedGroups.value = new Set(expandedGroups.value)
    }
    return
  }

  // Check if it's a subscore
  let foundGroup = groupedScores.value.groups.find(g =>
    g.subscores.some(s => s.id === scoreId)
  )

  if (
    !foundGroup &&
    groupedScores.value.divers?.subscores.some(s => s.id === scoreId)
  ) {
    foundGroup = groupedScores.value.divers
  }

  if (foundGroup) {
    // Expand the group
    if (!expandedGroups.value.has(foundGroup.id)) {
      expandedGroups.value.add(foundGroup.id)
      expandedGroups.value = new Set(expandedGroups.value)
    }

    // Expand the subscore (add to expandedSubscores if not present)
    if (!expandedSubscores.value.includes(scoreId)) {
      expandedSubscores.value.push(scoreId)
    }

    // Also expand all sibling subscores if the target was the group itself (via navigation anchor assumption) or if we want to be generous
    // If the input scoreId MATCHES the group ID (which matches foundGroup.id), we should expand ALL subscores
    if (scoreId === foundGroup.id) {
      foundGroup.subscores.forEach(s => {
        if (!expandedSubscores.value.includes(s.id)) {
          expandedSubscores.value.push(s.id)
        }
      })
    }
  }
}

watch(
  () => props.expandedScoreId,
  newId => {
    if (newId) {
      expandScore(newId)
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.impact-details {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  overflow-x: auto;
}

.impact-details__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.impact-details__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.impact-details__virtual-toggle {
  text-transform: none;
  font-weight: 500;
  letter-spacing: normal;
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
  font-size: 1.1rem;
}

.impact-details__table :deep(td) {
  font-weight: 500;
  color: rgb(var(--v-theme-text-neutral-strong));
  font-size: 1.1rem;
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

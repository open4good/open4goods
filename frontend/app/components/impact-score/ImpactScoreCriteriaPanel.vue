<template>
  <section class="impact-score-criteria-panel">
    <div v-if="showFilters" class="impact-score-criteria-panel__filters">
      <v-chip-group
        v-model="selectedFilter"
        class="impact-score-criteria-panel__filters-group"
        column
        selected-class="impact-score-criteria-panel__filters-chip--active"
      >
        <v-chip
          v-for="filter in filters"
          :key="filter.id"
          :value="filter.id"
          class="impact-score-criteria-panel__filters-chip"
          variant="tonal"
        >
          {{ filter.label }}
        </v-chip>
      </v-chip-group>
    </div>

    <v-table
      v-if="variant === 'table' && hasCriteria"
      class="impact-score-criteria-panel__table"
      density="comfortable"
    >
      <thead>
        <tr>
          <th scope="col" class="text-left">
            {{ t('impactScoreCriteriaPanel.table.name') }}
          </th>
          <th scope="col" class="text-left">
            {{ t('impactScoreCriteriaPanel.table.utility') }}
          </th>
          <th scope="col" class="text-left">
            {{ t('impactScoreCriteriaPanel.table.icon') }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="criterion in displayedCriteria" :key="criterion.key">
          <th scope="row">{{ criterion.name }}</th>
          <td>{{ criterion.utility || emptyFallback }}</td>
          <td>
            <v-icon v-if="criterion.icon" :icon="criterion.icon" size="20" />
            <span
              v-else
              class="impact-score-criteria-panel__icon-fallback"
              aria-hidden="true"
            >
              {{ criterion.name.charAt(0).toUpperCase() }}
            </span>
          </td>
        </tr>
      </tbody>
    </v-table>

    <v-row
      v-else-if="variant === 'cards' && hasCriteria"
      class="impact-score-criteria-panel__grid"
      align="stretch"
      dense
    >
      <v-col
        v-for="criterion in displayedCriteria"
        :key="criterion.key"
        :cols="columnLayout.cols"
        :sm="columnLayout.sm"
        :md="columnLayout.md"
        :lg="columnLayout.lg"
        :xl="columnLayout.xl"
      >
        <v-card
          class="impact-score-criteria-panel__card"
          elevation="0"
          rounded="xl"
          border
        >
          <div class="impact-score-criteria-panel__card-header">
            <v-avatar
              size="44"
              class="impact-score-criteria-panel__icon"
              color="surface-primary-120"
            >
              <v-icon
                v-if="criterion.icon"
                :icon="criterion.icon"
                size="22"
                color="primary"
              />
              <span v-else class="impact-score-criteria-panel__icon-fallback">
                {{ criterion.name.charAt(0).toUpperCase() }}
              </span>
            </v-avatar>
            <h3 class="impact-score-criteria-panel__card-title">
              {{ criterion.name }}
            </h3>
          </div>
          <p class="impact-score-criteria-panel__card-utility">
            {{ criterion.utility || emptyFallback }}
          </p>
        </v-card>
      </v-col>
    </v-row>

    <p v-else class="impact-score-criteria-panel__empty">
      {{ t('impactScoreCriteriaPanel.empty') }}
    </p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useImpactScoreCriteria,
  type ImpactScoreCriterion,
} from '~/composables/impact-score/useImpactScoreCriteria'

type CriteriaColumns = {
  cols?: number
  sm?: number
  md?: number
  lg?: number
  xl?: number
}

const props = withDefaults(
  defineProps<{
    variant?: 'cards' | 'table'
    verticalId?: string | null
    criteria?: ImpactScoreCriterion[]
    columns?: CriteriaColumns
  }>(),
  {
    variant: 'cards',
    verticalId: null,
    criteria: undefined,
    columns: () => ({}),
  }
)

const { t } = useI18n()
const {
  allCriteria,
  criteriaByVerticalId,
  verticalOptions,
  loadAllCriteria,
  loadCriteriaForVertical,
} = useImpactScoreCriteria()

const selectedFilter = ref<string>('all')

const showFilters = computed(() => !props.verticalId)
const columnLayout = computed(() => ({
  cols: 12,
  sm: 6,
  md: 4,
  ...props.columns,
}))

const filters = computed(() => [
  { id: 'all', label: t('impactScoreCriteriaPanel.filters.all') },
  ...verticalOptions.value.map(option => ({
    id: option.id,
    label: option.label,
  })),
])

const displayedCriteria = computed<ImpactScoreCriterion[]>(() => {
  if (props.criteria?.length) {
    return props.criteria
  }

  if (props.verticalId) {
    return criteriaByVerticalId.value[props.verticalId] ?? []
  }

  if (selectedFilter.value === 'all') {
    return allCriteria.value
  }

  return criteriaByVerticalId.value[selectedFilter.value] ?? []
})

const hasCriteria = computed(() => displayedCriteria.value.length > 0)
const emptyFallback = computed(() =>
  t('impactScoreCriteriaPanel.utilityFallback')
)

const loadCriteria = async () => {
  if (props.criteria?.length) {
    return
  }

  if (props.verticalId) {
    await loadCriteriaForVertical(props.verticalId)
    return
  }

  await loadAllCriteria()
}

onMounted(() => {
  loadCriteria()
})

watch(
  () => props.verticalId,
  async () => {
    if (props.verticalId) {
      selectedFilter.value = props.verticalId
    } else {
      selectedFilter.value = 'all'
    }
    await loadCriteria()
  }
)
</script>

<style scoped>
.impact-score-criteria-panel {
  display: grid;
  gap: 1.5rem;
}

.impact-score-criteria-panel__filters {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.impact-score-criteria-panel__filters-label {
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-criteria-panel__filters-group {
  gap: 0.5rem;
}

.impact-score-criteria-panel__filters-chip {
  font-weight: 600;
}

.impact-score-criteria-panel__filters-chip--active {
  color: rgb(var(--v-theme-primary));
}

.impact-score-criteria-panel__grid {
  row-gap: clamp(1rem, 2vw, 1.75rem);
}

.impact-score-criteria-panel__card {
  padding: 1.25rem;
  height: 100%;
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.2);
}

.impact-score-criteria-panel__card-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.impact-score-criteria-panel__card-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-criteria-panel__card-utility {
  margin: 0.85rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.5;
}

.impact-score-criteria-panel__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.impact-score-criteria-panel__icon-fallback {
  font-weight: 700;
  color: rgb(var(--v-theme-primary));
}

.impact-score-criteria-panel__table {
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
}

.impact-score-criteria-panel__table :deep(th),
.impact-score-criteria-panel__table :deep(td) {
  padding: 0.75rem;
  border-bottom: 1px solid rgba(var(--v-theme-border-primary-strong), 0.08);
}

.impact-score-criteria-panel__empty {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-style: italic;
}
</style>

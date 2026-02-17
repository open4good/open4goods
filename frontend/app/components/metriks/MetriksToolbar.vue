<script setup lang="ts">
/**
 * MetriksToolbar — Dashboard control bar with period selector,
 * chart type toggle, and provider/group/tag filters.
 */
import type {
  MetrikPeriodPreset,
  MetrikChartType,
} from '~/composables/useMetriks'

withDefaults(
  defineProps<{
    /** Current comparison period. */
    comparePeriod?: MetrikPeriodPreset
    /** Current chart type. */
    chartType?: MetrikChartType
    /** Available providers. */
    availableProviders: string[]
    /** Available groups. */
    availableGroups: string[]
    /** Available tags. */
    availableTags: string[]
    /** Currently selected providers. */
    selectedProviders?: string[]
    /** Currently selected groups. */
    selectedGroups?: string[]
    /** Currently selected tags. */
    selectedTags?: string[]
  }>(),
  {
    comparePeriod: '3m',
    chartType: 'bar',
    selectedProviders: () => [],
    selectedGroups: () => [],
    selectedTags: () => [],
  }
)

const emit = defineEmits<{
  'update:comparePeriod': [value: MetrikPeriodPreset]
  'update:chartType': [value: MetrikChartType]
  'update:selectedProviders': [value: string[]]
  'update:selectedGroups': [value: string[]]
  'update:selectedTags': [value: string[]]
}>()

const { t } = useI18n()

const periodOptions: MetrikPeriodPreset[] = ['7d', '3w', '3m']
const chartTypeOptions: MetrikChartType[] = ['bar', 'line']

function onPeriodChange(val: MetrikPeriodPreset): void {
  emit('update:comparePeriod', val)
}

function onChartTypeChange(val: MetrikChartType): void {
  emit('update:chartType', val)
}
</script>

<template>
  <v-card variant="outlined" rounded="lg" class="mb-4">
    <v-card-text class="pa-3 pa-md-4">
      <v-row dense align="center">
        <!-- Compare period selector -->
        <v-col cols="12" sm="6" md="auto">
          <div class="text-caption text-medium-emphasis mb-1">
            {{ t('metriks.toolbar.compareTo') }}
          </div>
          <v-btn-toggle
            :model-value="comparePeriod"
            mandatory
            density="compact"
            variant="outlined"
            divided
            color="primary"
            @update:model-value="onPeriodChange"
          >
            <v-btn v-for="p in periodOptions" :key="p" :value="p" size="small">
              {{ t(`metriks.toolbar.periods.${p}`) }}
            </v-btn>
          </v-btn-toggle>
        </v-col>

        <!-- Chart type -->
        <v-col cols="12" sm="6" md="auto">
          <div class="text-caption text-medium-emphasis mb-1">
            {{ t('metriks.toolbar.chartType') }}
          </div>
          <v-btn-toggle
            :model-value="chartType"
            mandatory
            density="compact"
            variant="outlined"
            divided
            color="primary"
            @update:model-value="onChartTypeChange"
          >
            <v-btn
              v-for="ct in chartTypeOptions"
              :key="ct"
              :value="ct"
              size="small"
            >
              <v-icon
                :icon="ct === 'bar' ? 'mdi-chart-bar' : 'mdi-chart-line'"
                size="16"
                class="mr-1"
              />
              {{ t(`metriks.toolbar.chartTypes.${ct}`) }}
            </v-btn>
          </v-btn-toggle>
        </v-col>

        <v-spacer class="d-none d-md-block" />

        <!-- Provider filter -->
        <v-col cols="12" sm="4" md="2">
          <v-select
            :model-value="selectedProviders"
            :items="availableProviders"
            :label="t('metriks.toolbar.providers')"
            multiple
            chips
            closable-chips
            variant="outlined"
            density="compact"
            hide-details
            clearable
            @update:model-value="emit('update:selectedProviders', $event)"
          />
        </v-col>

        <!-- Group filter -->
        <v-col cols="12" sm="4" md="2">
          <v-select
            :model-value="selectedGroups"
            :items="availableGroups"
            :label="t('metriks.table.filterGroups')"
            multiple
            chips
            closable-chips
            variant="outlined"
            density="compact"
            hide-details
            clearable
            @update:model-value="emit('update:selectedGroups', $event)"
          />
        </v-col>

        <!-- Tag filter -->
        <v-col cols="12" sm="4" md="2">
          <v-select
            :model-value="selectedTags"
            :items="availableTags"
            :label="t('metriks.table.filterTags')"
            multiple
            chips
            closable-chips
            variant="outlined"
            density="compact"
            hide-details
            clearable
            @update:model-value="emit('update:selectedTags', $event)"
          />
        </v-col>
      </v-row>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
/**
 * /metriks — Dashboard page showing all collected metrics
 * with interactive chart and filterable table.
 */
import type { MetrikWithTrend } from '~/types/metriks'

const { t } = useI18n()

// SEO metadata
useHead({
  title: t('metriks.seo.title'),
  meta: [
    { name: 'description', content: t('metriks.seo.description') },
    { property: 'og:title', content: t('metriks.seo.title') },
    { property: 'og:description', content: t('metriks.seo.description') },
  ],
})

const { allMetriks, loading, error, loadAll } = useMetriks()

/** Set of metric IDs currently shown on the chart. */
const selectedIds = ref(new Set<string>())

/** Ordered list of selected metrics (for the chart). */
const selectedMetriks = computed(() =>
  allMetriks.value.filter(m => selectedIds.value.has(m.id))
)

/** Top KPI metrics for the hero section (first 4 global analytics metrics). */
const heroMetriks = computed(() =>
  allMetriks.value
    .filter(m => m.status === 'ok' && m.tags.includes('global'))
    .slice(0, 4)
)

/**
 * Toggle a metric in/out of the chart selection.
 */
function onMetrikSelect(metrik: MetrikWithTrend): void {
  const ids = new Set(selectedIds.value)
  if (ids.has(metrik.id)) {
    ids.delete(metrik.id)
  } else {
    ids.add(metrik.id)
  }
  selectedIds.value = ids
}

/** Remove a metric from chart selection. */
function removeFromChart(metrikId: string): void {
  const ids = new Set(selectedIds.value)
  ids.delete(metrikId)
  selectedIds.value = ids
}

onMounted(() => {
  loadAll()
})
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <!-- Page header -->
    <v-row class="mb-4">
      <v-col>
        <h1 class="text-h4 font-weight-bold d-flex align-center ga-2">
          <v-icon
            icon="mdi-chart-box-multiple-outline"
            color="primary"
            size="36"
          />
          {{ t('metriks.pageTitle') }}
        </h1>
        <p class="text-body-1 text-medium-emphasis mt-1">
          {{ t('metriks.pageSubtitle') }}
        </p>
      </v-col>
    </v-row>

    <!-- Loading state -->
    <v-row v-if="loading" justify="center" class="py-12">
      <v-progress-circular indeterminate size="48" color="primary" />
    </v-row>

    <!-- Error state -->
    <v-alert v-else-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <template v-else>
      <!-- Hero KPI cards -->
      <v-row v-if="heroMetriks.length > 0" class="mb-6">
        <v-col v-for="m in heroMetriks" :key="m.id" cols="12" sm="6" md="3">
          <MetrikCard :metrik="m" variant="xl" />
        </v-col>
      </v-row>

      <!-- Chart section -->
      <v-row class="mb-6">
        <v-col cols="12">
          <MetriksChart :selected-metriks="selectedMetriks" />

          <!-- Selected metrics chips -->
          <div
            v-if="selectedMetriks.length > 0"
            class="d-flex flex-wrap ga-2 mt-3"
          >
            <v-chip
              v-for="m in selectedMetriks"
              :key="m.id"
              closable
              color="primary"
              variant="tonal"
              size="small"
              @click:close="removeFromChart(m.id)"
            >
              {{ m.name }}
            </v-chip>
          </div>
        </v-col>
      </v-row>

      <!-- Table section -->
      <v-row>
        <v-col cols="12">
          <MetriksTable
            :metriks="allMetriks"
            :selected-ids="selectedIds"
            @select="onMetrikSelect"
          />
        </v-col>
      </v-row>
    </template>
  </v-container>
</template>

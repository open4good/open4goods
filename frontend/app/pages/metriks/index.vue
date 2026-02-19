<script setup lang="ts">
/**
 * /metriks — Dashboard page showing all collected metrics
 * with interactive chart, filterable table, and URL-driven state.
 *
 * Query params: period, metrics, groups, tags, providers, chart
 */
import type { MetrikWithTrend } from '~/types/metriks'
import type {
  MetrikPeriodPreset,
  MetrikChartType,
} from '~/composables/useMetriks'
import { useAuth } from '~/composables/useAuth'
import { hasAdminAccess } from '~~/shared/utils/_roles'

const { t } = useI18n()
const route = useRoute()
const { isLoggedIn, roles } = useAuth()
const isAdmin = computed(() => isLoggedIn.value && hasAdminAccess(roles.value))

// SEO metadata
useHead({
  title: t('metriks.seo.title'),
  meta: [
    { name: 'description', content: t('metriks.seo.description') },
    { property: 'og:title', content: t('metriks.seo.title') },
    { property: 'og:description', content: t('metriks.seo.description') },
  ],
})

const {
  allMetriks,
  allGroups,
  allTags,
  allProviderNames,
  loading,
  error,
  comparePeriod,
  loadAll,
} = useMetriks()

// ---------- URL-driven state ----------

const validPeriods: MetrikPeriodPreset[] = ['latest', '7d', '3w', '3m']
const validChartTypes: MetrikChartType[] = ['bar', 'line']

function parseQueryArray(param: string | string[] | undefined): string[] {
  if (!param) return []
  const raw = Array.isArray(param) ? param[0] : param
  if (!raw) return []
  return raw.split(',').filter(Boolean)
}

/** Initialise state from query params. */
const initialPeriod = validPeriods.includes(
  route.query.period as MetrikPeriodPreset
)
  ? (route.query.period as MetrikPeriodPreset)
  : 'latest'

const chartType = ref<MetrikChartType>(
  validChartTypes.includes(route.query.chart as MetrikChartType)
    ? (route.query.chart as MetrikChartType)
    : 'bar'
)

const selectedIds = ref(
  new Set<string>(parseQueryArray(route.query.metrics as string | undefined))
)

const selectedProviders = ref<string[]>(
  parseQueryArray(route.query.providers as string | undefined)
)
const selectedGroups = ref<string[]>(
  parseQueryArray(route.query.groups as string | undefined)
)
const selectedTags = ref<string[]>(
  parseQueryArray(route.query.tags as string | undefined)
)

// Sync composable period
comparePeriod.value = initialPeriod

/** Ordered list of selected metrics (for the chart). */
const selectedMetriks = computed(() =>
  allMetriks.value.filter(m => selectedIds.value.has(m.id))
)

/** Top KPI metrics for the hero section (first 4 global analytics metrics). */
const heroMetriks = computed(() => {
  let pool = allMetriks.value.filter(
    m => m.status === 'ok' && m.tags.includes('global')
  )

  if (selectedProviders.value.length > 0) {
    pool = pool.filter(m => selectedProviders.value.includes(m.provider))
  }

  return pool.slice(0, 4)
})

// ---------- Event handlers ----------

function onPeriodChange(val: MetrikPeriodPreset): void {
  comparePeriod.value = val
  syncUrl()
}

function onChartTypeChange(val: MetrikChartType): void {
  chartType.value = val
  syncUrl()
}

function onProvidersChange(val: string[]): void {
  selectedProviders.value = val
  syncUrl()
}

function onGroupsChange(val: string[]): void {
  selectedGroups.value = val
  syncUrl()
}

function onTagsChange(val: string[]): void {
  selectedTags.value = val
  syncUrl()
}

function onMetrikSelect(metrik: MetrikWithTrend): void {
  const ids = new Set(selectedIds.value)
  if (ids.has(metrik.id)) {
    ids.delete(metrik.id)
  } else {
    ids.add(metrik.id)
  }
  selectedIds.value = ids
  syncUrl()
}

// ---------- URL sync ----------

function syncUrl(): void {
  if (!isAdmin.value) return

  const query: Record<string, string> = {}

  if (comparePeriod.value !== 'latest') query.period = comparePeriod.value
  if (chartType.value !== 'bar') query.chart = chartType.value
  if (selectedIds.value.size > 0)
    query.metrics = Array.from(selectedIds.value).join(',')
  if (selectedProviders.value.length > 0)
    query.providers = selectedProviders.value.join(',')
  if (selectedGroups.value.length > 0)
    query.groups = selectedGroups.value.join(',')
  if (selectedTags.value.length > 0) query.tags = selectedTags.value.join(',')

  navigateTo({ path: '/metriks', query }, { replace: true })
}

onMounted(() => {
  if (isAdmin.value) {
    loadAll()
  }
})
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div v-if="!isAdmin">
      <v-empty-state
        icon="mdi-lock"
        :headline="t('metriks.accessDenied')"
        :title="t('metriks.pageSubtitle')"
      >
        <template #actions>
          <v-btn color="primary" to="/auth/login">{{
            t('metriks.loginCta')
          }}</v-btn>
        </template>
      </v-empty-state>
    </div>

    <template v-else>
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
        <!-- Toolbar -->
        <MetriksToolbar
          :period="comparePeriod"
          :chart-type="chartType"
          :available-providers="allProviderNames"
          :available-groups="allGroups"
          :available-tags="allTags"
          :selected-providers="selectedProviders"
          :selected-groups="selectedGroups"
          :selected-tags="selectedTags"
          @update:period="onPeriodChange"
          @update:chart-type="onChartTypeChange"
          @update:selected-providers="onProvidersChange"
          @update:selected-groups="onGroupsChange"
          @update:selected-tags="onTagsChange"
        />

        <!-- Hero KPI cards -->
        <v-row v-if="heroMetriks.length > 0" class="mb-6">
          <v-col v-for="m in heroMetriks" :key="m.id" cols="12" sm="6" md="3">
            <MetrikCard :metrik="m" variant="xl" />
          </v-col>
        </v-row>

        <!-- Chart section -->
        <v-row class="mb-6">
          <v-col cols="12">
            <MetriksChart
              :selected-metriks="selectedMetriks"
              :chart-type="chartType"
            />

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
                @click:close="onMetrikSelect(m)"
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
              :filter-providers="selectedProviders"
              :filter-groups="selectedGroups"
              :filter-tags="selectedTags"
              :selected-ids="selectedIds"
              @select="onMetrikSelect"
            />
          </v-col>
        </v-row>
      </template>
    </template>
  </v-container>
</template>

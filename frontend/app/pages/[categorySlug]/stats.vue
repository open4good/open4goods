<template>
  <div class="category-stats" data-test="category-stats">
    <CategoryHero
      v-if="category"
      :title="heroTitle"
      :description="heroDescription"
      :image="heroImage"
      :breadcrumbs="heroBreadcrumbs"
    />

    <div v-if="category" class="category-stats__content">
      <v-container fluid class="py-10">
        <!-- Hero KPIs -->
        <section
          class="category-stats__section"
          aria-labelledby="stats-hero-title"
        >
          <v-skeleton-loader v-if="heroLoading" type="card" class="mb-6" />
          <DatavizHero v-else-if="heroStatsData" :stats="heroStatsData" />
        </section>

        <!-- Charts grid -->
        <section
          class="category-stats__section"
          aria-labelledby="stats-charts-title"
        >
          <header class="category-stats__section-header">
            <h2 id="stats-charts-title" class="category-stats__section-title">
              {{ t('dataviz.page.chartsTitle') }}
            </h2>
            <p class="category-stats__section-subtitle">
              {{ t('dataviz.page.chartsSubtitle') }}
            </p>
          </header>

          <v-row v-if="planData?.charts?.length" align="stretch">
            <v-col
              v-for="chart in planData.charts"
              :key="chart.id"
              cols="12"
              md="6"
            >
              <DatavizChart
                :preset="chart"
                :chart-data="chartResults[chart.id ?? ''] ?? null"
                :loading="chartLoadingStates[chart.id ?? ''] ?? false"
              />
            </v-col>
          </v-row>

          <v-skeleton-loader v-else-if="planLoading" type="card@4" />
        </section>
      </v-container>
    </div>

    <v-container v-else fluid class="py-10">
      <v-skeleton-loader type="image, article" />
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  CategoryBreadcrumbItemDto,
  DatavizChartQueryResponseDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'
import CategoryHero from '~/components/category/CategoryHero.vue'
import DatavizHero from '~/components/dataviz/DatavizHero.vue'
import DatavizChart from '~/components/dataviz/DatavizChart.vue'
import { createError, useRequestURL, useRoute, useSeoMeta } from '#imports'
import { useCategories } from '~/composables/categories/useCategories'
import { useDataviz } from '~/composables/stats/useDataviz'

definePageMeta({ lazy: true })

const route = useRoute()
const requestURL = useRequestURL()
const { t } = useI18n()

const categorySlug = computed(() => String(route.params.categorySlug ?? ''))

if (!categorySlug.value) {
  throw createError({ statusCode: 404, statusMessage: 'Category not found' })
}

// ─── Category resolution ────────────────────────────────────────────
const { selectCategoryBySlug } = useCategories()
const category = ref<VerticalConfigFullDto | null>(null)

try {
  category.value = await selectCategoryBySlug(categorySlug.value)
} catch (error) {
  if (error instanceof Error && error.name === 'CategoryNotFoundError') {
    throw createError({
      statusCode: 404,
      statusMessage: 'Category not found',
      cause: error,
    })
  }
  console.error('Failed to resolve category for stats page', error)
  throw createError({
    statusCode: 500,
    statusMessage: 'Failed to load category',
    cause: error,
  })
}

const verticalId = computed(() => category.value?.id ?? '')
const categoryLabel = computed(
  () =>
    category.value?.verticalHomeTitle ?? category.value?.verticalMetaTitle ?? ''
)

// ─── SEO ────────────────────────────────────────────────────────────
const siteName = computed(() => String(t('siteIdentity.siteName')))

useSeoMeta({
  title: () =>
    t('dataviz.page.metaTitle', {
      category: categoryLabel.value,
      siteName: siteName.value,
    }),
  description: () =>
    t('dataviz.page.metaDescription', { category: categoryLabel.value }),
  ogTitle: () =>
    t('dataviz.page.metaTitle', {
      category: categoryLabel.value,
      siteName: siteName.value,
    }),
  ogDescription: () =>
    t('dataviz.page.metaDescription', { category: categoryLabel.value }),
  ogUrl: requestURL.href,
})

// ─── Hero ───────────────────────────────────────────────────────────
const heroTitle = computed(() =>
  t('dataviz.page.heroTitle', { category: categoryLabel.value })
)
const heroDescription = computed(() =>
  t('dataviz.page.heroDescription', { category: categoryLabel.value })
)
const heroImage = computed(() => {
  if (!category.value) return null
  return (
    category.value.imageMedium ??
    category.value.imageLarge ??
    category.value.imageSmall ??
    null
  )
})
const heroBreadcrumbs = computed<CategoryBreadcrumbItemDto[]>(() => {
  const base = (category.value?.breadCrumb ?? []).map(item => ({ ...item }))
  const leafTitle = t('dataviz.page.breadcrumbLeaf')
  return leafTitle ? [...base, { title: leafTitle }] : base
})

// ─── Dataviz composable ─────────────────────────────────────────────
const {
  plan: planData,
  heroStats: heroStatsData,
  planLoading,
  heroLoading,
  fetchPlan,
  fetchHeroStats,
  fetchChartData,
} = useDataviz(verticalId.value)

const chartResults = reactive<
  Record<string, DatavizChartQueryResponseDto | null>
>({})
const chartLoadingStates = reactive<Record<string, boolean>>({})

const loadChartData = async (chartId: string, queryPreset: string) => {
  chartLoadingStates[chartId] = true
  try {
    const result = await fetchChartData({ chartId: queryPreset })
    chartResults[chartId] = result
  } finally {
    chartLoadingStates[chartId] = false
  }
}

onMounted(async () => {
  // Load plan + hero in parallel
  const [plan] = await Promise.all([fetchPlan(), fetchHeroStats()])

  // Load charts sequentially to avoid overwhelming ES
  if (plan?.charts?.length) {
    for (const chart of plan.charts) {
      if (chart.id && chart.queryPreset) {
        await loadChartData(chart.id, chart.queryPreset)
      }
    }
  }
})
</script>

<style scoped lang="scss">
.category-stats {
  &__content {
    background: rgb(var(--v-theme-surface-default));
  }

  &__section {
    margin-bottom: 48px;
  }

  &__section-header {
    margin-bottom: 24px;
  }

  &__section-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: rgb(var(--v-theme-text-neutral-strong));
    margin: 0 0 8px;
  }

  &__section-subtitle {
    font-size: 0.9rem;
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin: 0;
  }
}
</style>

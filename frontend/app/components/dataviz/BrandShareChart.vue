<template>
  <div class="brand-share-chart">
    <DatavizChart :preset="preset" :chart-data="chartData" :loading="pending" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  AggregationResponseDto,
  DatavizChartPresetDto,
  DatavizChartQueryResponseDto,
  ProductSearchResponseDto,
} from '~~/shared/api-client'
import DatavizChart from '~/components/dataviz/DatavizChart.vue'
import { useGuideContext } from '~/composables/useGuideContext'

const BRAND_FIELD = 'attributes.referentielAttributes.BRAND'

const props = withDefaults(
  defineProps<{
    vertical?: string
    type?: 'pie' | 'bar'
    metric?: 'count' | 'offers'
    title?: string
    top?: string | number
  }>(),
  {
    vertical: undefined,
    type: 'pie',
    metric: 'count',
    title: undefined,
    top: 8,
  }
)

const { t } = useI18n()
const guideContext = useGuideContext()

const normalizedVertical = computed(() =>
  `${props.vertical ?? guideContext?.verticalId ?? ''}`.trim()
)
const topCount = computed(() => {
  const parsed = Number.parseInt(`${props.top}`, 10)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 8
})

const chartType = computed(() => (props.type === 'bar' ? 'bar' : 'donut'))

const resolvedTitle = computed(
  () => props.title?.trim() || t('buyingGuide.brandShare.defaultTitle')
)

// Client-side fetch so the chartData ref transitions null -> value, which is
// what DatavizChart watches to (re)render its ECharts canvas.
const { data, pending } = await useAsyncData<AggregationResponseDto | null>(
  () => `brand-share:${normalizedVertical.value}:${topCount.value}`,
  async () => {
    if (!normalizedVertical.value) {
      return null
    }

    try {
      const response = await $fetch<ProductSearchResponseDto>(
        '/api/products/search',
        {
          method: 'POST',
          body: {
            verticalId: normalizedVertical.value,
            pageNumber: 0,
            pageSize: 0,
            aggs: {
              aggs: [
                {
                  name: BRAND_FIELD,
                  field: BRAND_FIELD,
                  type: 'terms',
                  buckets: topCount.value,
                },
              ],
            },
          },
        }
      )

      return response.aggregations?.find(agg => agg.name === BRAND_FIELD) ?? null
    } catch (error) {
      console.error('Failed to load guide brand share chart', error)
      return null
    }
  },
  {
    server: false,
    watch: [normalizedVertical, topCount],
  }
)

const buckets = computed(() => {
  const all = data.value?.buckets ?? []
  return all
    .filter(
      bucket =>
        !bucket.missing &&
        typeof bucket.key === 'string' &&
        bucket.key.trim().length > 0 &&
        (bucket.count ?? 0) > 0
    )
    .sort((a, b) => (b.count ?? 0) - (a.count ?? 0))
    .slice(0, topCount.value)
})

const chartData = computed<DatavizChartQueryResponseDto | null>(() => {
  if (!buckets.value.length) {
    return null
  }

  return {
    chartType: chartType.value,
    title: resolvedTitle.value,
    labels: buckets.value.map(bucket => bucket.key as string),
    values: buckets.value.map(bucket => bucket.count ?? 0),
  }
})

const preset = computed<DatavizChartPresetDto>(() => ({
  id: `brand-share-${normalizedVertical.value}`,
  chartType: chartType.value,
  title: resolvedTitle.value,
  description: t('buyingGuide.brandShare.description'),
}))
</script>

<style scoped lang="sass">
.brand-share-chart
  margin: 1.5rem 0
</style>

<template>
  <section :id="sectionId" class="product-price">
    <header class="product-price__header">
      <h2 id="price-history" class="product-price__title">
        {{ $t('product.price.title') }}
      </h2>
      <p class="product-price__subtitle">
        {{ $t('product.price.subtitle') }}
      </p>
    </header>

    <div class="product-price__charts">
      <article class="product-price__chart-card">
        <header>
          <h3>{{ $t('product.price.newOffers') }}</h3>
          <p v-if="newTrendLabel" class="product-price__trend">{{ newTrendLabel }}</p>
        </header>
        <ClientOnly>
          <VueECharts
            v-if="newChartOption"
            :option="newChartOption"
            :autoresize="true"
            class="product-price__chart"
          />
          <template #fallback>
            <div class="product-price__chart-placeholder" />
          </template>
        </ClientOnly>
      </article>

      <article class="product-price__chart-card">
        <header>
          <h3>{{ $t('product.price.occasionOffers') }}</h3>
        </header>
        <ClientOnly>
          <VueECharts
            v-if="occasionChartOption"
            :option="occasionChartOption"
            :autoresize="true"
            class="product-price__chart"
          />
          <template #fallback>
            <div class="product-price__chart-placeholder" />
          </template>
        </ClientOnly>
        <p v-if="!occasionChartOption" class="product-price__empty">{{ $t('product.price.noOccasionHistory') }}</p>
      </article>
    </div>

    <div class="product-price__offers">
      <h3 id="offers-list">{{ $t('product.price.offerList') }}</h3>
      <v-table density="comfortable">
        <thead>
          <tr>
            <th scope="col">{{ $t('product.price.headers.source') }}</th>
            <th scope="col">{{ $t('product.price.headers.offer') }}</th>
            <th scope="col">{{ $t('product.price.headers.price') }}</th>
            <th scope="col">{{ $t('product.price.headers.condition') }}</th>
            <th scope="col">{{ $t('product.price.headers.updated') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="offer in sortedOffers" :key="offer.id">
            <td>
              <div class="product-price__source">
                <img v-if="offer.favicon" :src="offer.favicon" :alt="offer.datasourceName" width="18" height="18">
                <span>{{ offer.datasourceName }}</span>
              </div>
            </td>
            <td>
              <a :href="offer.url" target="_blank" rel="noopener nofollow">{{ offer.offerName }}</a>
            </td>
            <td>{{ formatCurrency(offer.price, offer.currency) }}</td>
            <td>{{ offer.conditionLabel }}</td>
            <td>{{ offer.updatedLabel }}</td>
          </tr>
        </tbody>
      </v-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import VueECharts from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  DataZoomComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useI18n } from 'vue-i18n'
import { formatDistanceToNow, format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import type { ProductDto, CommercialEvent } from '~~/shared/api-client'

use([LineChart, GridComponent, TooltipComponent, DataZoomComponent, LegendComponent, TitleComponent, CanvasRenderer])

const props = defineProps({
  sectionId: {
    type: String,
    default: 'prix',
  },
  offers: {
    type: Object as PropType<ProductDto['offers']>,
    required: true,
  },
  commercialEvents: {
    type: Array as PropType<CommercialEvent[]>,
    default: () => [],
  },
})

const { locale, n, t } = useI18n()

type HistoryEntry = { timestamp: number; price: number }

const normalizeHistoryEntries = (
  entries?: Array<{ timestamp?: number | null; price?: number | null }>,
): HistoryEntry[] =>
  (entries ?? [])
    .map((entry) => ({
      timestamp: typeof entry.timestamp === 'number' ? entry.timestamp : Number(entry.timestamp ?? Number.NaN),
      price: typeof entry.price === 'number' ? entry.price : Number(entry.price ?? Number.NaN),
    }))
    .filter((entry): entry is HistoryEntry => Number.isFinite(entry.timestamp) && Number.isFinite(entry.price))

const newHistory = computed(() => normalizeHistoryEntries(props.offers?.newHistory?.entries))
const occasionHistory = computed(() => normalizeHistoryEntries(props.offers?.occasionHistory?.entries))

const resolvedEvents = computed(() =>
  props.commercialEvents
    .map((event) => ({
      label: event.label,
      start: event.startDate ? new Date(event.startDate).getTime() : Number.NaN,
      end: event.endDate ? new Date(event.endDate).getTime() : Number.NaN,
    }))
    .filter((event) => Number.isFinite(event.start) && Number.isFinite(event.end) && event.start <= event.end),
)

const newChartOption = computed(() => buildChartOption(newHistory.value, resolvedEvents.value, t('product.price.newOffers')))
const occasionChartOption = computed(() =>
  occasionHistory.value.length ? buildChartOption(occasionHistory.value, resolvedEvents.value, t('product.price.occasionOffers')) : null,
)

const newTrendLabel = computed(() => {
  const trend = props.offers?.newTrend
  if (!trend) {
    return null
  }

  if (trend.trend === 'PRICE_DECREASE') {
    return t('product.price.trend.decrease', { amount: n(Math.abs(trend.variation ?? 0), { style: 'currency', currency: props.offers?.bestPrice?.currency ?? 'EUR', maximumFractionDigits: 2 }) })
  }

  if (trend.trend === 'PRICE_INCREASE') {
    return t('product.price.trend.increase', { amount: n(Math.abs(trend.variation ?? 0), { style: 'currency', currency: props.offers?.bestPrice?.currency ?? 'EUR', maximumFractionDigits: 2 }) })
  }

  return t('product.price.trend.stable')
})

const sortedOffers = computed(() => {
  const entries = props.offers?.offersByCondition ?? {}
  const aggregated = Object.values(entries).flat().map((offer, index) => ({
    ...offer,
    id: offer.url ?? `${offer.datasourceName ?? 'source'}-${index}`,
    conditionLabel: t(
      `product.price.condition.${typeof offer.condition === 'string' ? offer.condition.toLowerCase() : 'new'}`,
    ),
    updatedLabel: formatUpdated(offer.timeStamp),
  }))

  return aggregated
    .filter((offer): offer is typeof aggregated[number] & { price: number } => typeof offer.price === 'number')
    .sort((a, b) => a.price - b.price)
})

const buildChartOption = (
  entries: HistoryEntry[],
  events: Array<{ start: number; end: number; label?: string }>,
  title: string,
) => {
  if (!entries.length) {
    return null
  }

  const data = entries.map((entry) => [entry.timestamp, entry.price])
  const markArea = events.map((event) => [
    {
      xAxis: event.start,
      itemStyle: { color: 'rgba(59, 130, 246, 0.08)' },
      label: { show: false },
    },
    {
      xAxis: event.end,
      label: {
        formatter: event.label ?? '',
        position: 'insideTop',
      },
    },
  ])

  return {
    title: { text: title, left: 'center', top: 10, textStyle: { fontSize: 14, fontWeight: 600 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 24, top: 48, bottom: 48 },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: (value: number) => {
          const numericValue = Number(value)
          if (!Number.isFinite(numericValue)) {
            return ''
          }

          return format(numericValue, 'dd MMM', { locale: locale.value.startsWith('fr') ? fr : enUS })
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        formatter: (value: number) =>
          n(Number(value), {
            style: 'currency',
            currency: props.offers?.bestPrice?.currency ?? 'EUR',
            maximumFractionDigits: 0,
          }),
      },
    },
    dataZoom: [
      {
        type: 'inside',
      },
      {
        type: 'slider',
        height: 18,
      },
    ],
    series: [
      {
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        areaStyle: { opacity: 0.08 },
        data,
        lineStyle: { color: '#2563eb' },
        markArea: markArea.length ? { data: markArea } : undefined,
      },
    ],
  }
}

const formatCurrency = (value?: number | null, currency: string = 'EUR') => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return n(value, { style: 'currency', currency, maximumFractionDigits: 2 })
}

const formatUpdated = (timestamp?: number | null) => {
  const numericTimestamp = typeof timestamp === 'number' ? timestamp : Number(timestamp)
  if (!Number.isFinite(numericTimestamp) || numericTimestamp <= 0) {
    return '—'
  }

  return formatDistanceToNow(numericTimestamp, {
    addSuffix: true,
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
}
</script>

<style scoped>
.product-price {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.product-price__header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-price__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
}

.product-price__subtitle {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-price__charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.product-price__chart-card {
  background: rgba(var(--v-theme-surface-glass), 0.92);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.06);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-price__trend {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-accent-supporting), 0.9);
}

.product-price__chart {
  height: 300px;
}

.product-price__chart-placeholder {
  height: 300px;
  border-radius: 18px;
  background: rgba(148, 163, 184, 0.14);
}

.product-price__empty {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-price__offers {
  background: rgba(var(--v-theme-surface-glass-strong), 0.94);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.product-price__source {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.product-price__source img {
  border-radius: 4px;
}

@media (max-width: 768px) {
  .product-price__chart {
    height: 240px;
  }
}
</style>

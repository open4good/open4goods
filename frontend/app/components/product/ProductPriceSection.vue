<template>
  <section :id="sectionId" class="product-price">
    <header class="product-price__header">
      <h2 class="product-price__title">
        {{ $t('product.price.title', titleParams) }}
      </h2>
      <p class="product-price__subtitle">
        {{ $t('product.price.subtitle') }}
      </p>
    </header>

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
                <img
                  v-if="offer.favicon"
                  :src="offer.favicon"
                  :alt="offer.datasourceName"
                  width="18"
                  height="18"
                />
                <span>{{ offer.datasourceName }}</span>
              </div>
            </td>
            <td>
              <a :href="offer.url" target="_blank" rel="noopener nofollow">{{
                offer.offerName
              }}</a>
            </td>
            <td>{{ formatCurrency(offer.price, offer.currency) }}</td>
            <td>{{ offer.conditionLabel }}</td>
            <td>{{ offer.updatedLabel }}</td>
          </tr>
        </tbody>
      </v-table>
    </div>

    <div class="product-price__history">
      <h3 id="price-history" class="product-price__history-title">
        {{ $t('product.price.historyTitle') }}
      </h3>
      <div
        class="product-price__charts"
        :class="{
          'product-price__charts--single': visibleChartsCount === 1,
          'product-price__charts--empty': visibleChartsCount === 0,
        }"
      >
        <article
          v-if="hasNewHistory"
          ref="newChartCardRef"
          class="product-price__chart-card"
        >
          <header class="product-price__chart-header">
            <div class="product-price__chart-heading">
              <h4>{{ $t('product.price.newOffers') }}</h4>
              <p v-if="newTrendLabel" class="product-price__trend">
                {{ newTrendLabel }}
              </p>
            </div>
          </header>
          <ClientOnly v-if="!isTestEnvironment">
            <template #default>
              <VueECharts
                v-if="newChartOption && chartVisibility.new"
                :option="newChartOption"
                :autoresize="true"
                class="product-price__chart"
              />
              <div v-else class="product-price__chart-placeholder" />
            </template>
            <template #fallback>
              <div class="product-price__chart-placeholder" />
            </template>
          </ClientOnly>
          <template v-else>
            <div
              v-if="newChartOption"
              class="echart-stub"
              :data-option="JSON.stringify(newChartOption)"
            ></div>
            <div v-else class="product-price__chart-placeholder" />
          </template>
          <footer
            v-if="newStats"
            class="product-price__metrics"
            :class="
              bestNewOffer
                ? 'product-price__metrics--with-offer'
                : 'product-price__metrics--solo'
            "
          >
            <ClientOnly>
              <NuxtLink
                v-if="bestNewOffer && bestNewOfferLink"
                :to="bestNewOfferLink"
                class="product-price__metrics-highlight product-price__metrics-offer-card product-price__metrics-offer--link"
                :aria-label="
                  $t('product.price.metrics.viewOffer', {
                    source:
                      bestNewOffer?.datasourceName ??
                      $t('product.price.metrics.unknownSource'),
                  })
                "
                @click="
                  handleOfferRedirectClick(
                    bestNewOffer,
                    'best-new-offer',
                    bestNewOfferLink
                  )
                "
              >
                <span class="product-price__metrics-label">
                  {{ $t('product.price.metrics.bestOffer') }}
                </span>
                <div class="product-price__metrics-offer">
                  <img
                    v-if="bestNewOffer?.favicon"
                    :src="bestNewOffer.favicon"
                    :alt="bestNewOffer.datasourceName ?? ''"
                    width="48"
                    height="48"
                  />
                  <div class="product-price__metrics-offer-text">
                    <p class="product-price__metrics-price">
                      {{
                        formatCurrency(
                          bestNewOffer?.price,
                          bestNewOffer?.currency
                        )
                      }}
                    </p>
                    <p class="product-price__metrics-source">
                      {{
                        bestNewOffer?.datasourceName ??
                        $t('product.price.metrics.unknownSource')
                      }}
                    </p>
                  </div>
                  <v-icon
                    icon="mdi-open-in-new"
                    size="18"
                    class="product-price__metrics-offer-icon"
                    aria-hidden="true"
                  />
                </div>
              </NuxtLink>
              <div
                v-else-if="bestNewOffer"
                class="product-price__metrics-highlight product-price__metrics-offer-card"
              >
                <span class="product-price__metrics-label">
                  {{ $t('product.price.metrics.bestOffer') }}
                </span>
                <div class="product-price__metrics-offer">
                  <img
                    v-if="bestNewOffer?.favicon"
                    :src="bestNewOffer.favicon"
                    :alt="bestNewOffer.datasourceName ?? ''"
                    width="48"
                    height="48"
                  />
                  <div class="product-price__metrics-offer-text">
                    <p class="product-price__metrics-price">
                      {{
                        formatCurrency(
                          bestNewOffer?.price,
                          bestNewOffer?.currency
                        )
                      }}
                    </p>
                    <p class="product-price__metrics-source">
                      {{
                        bestNewOffer?.datasourceName ??
                        $t('product.price.metrics.unknownSource')
                      }}
                    </p>
                  </div>
                </div>
              </div>
            </ClientOnly>
            <div class="product-price__metrics-summary">
              <dl>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-trending-down"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.lowest') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          newStats.min,
                          bestNewOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-chart-line"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.average') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          newStats.average,
                          bestNewOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-trending-up"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.highest') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          newStats.max,
                          bestNewOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
              </dl>
            </div>
          </footer>
        </article>

        <article
          v-if="hasOccasionHistory"
          ref="occasionChartCardRef"
          class="product-price__chart-card"
        >
          <header class="product-price__chart-header">
            <div class="product-price__chart-heading">
              <h4>{{ $t('product.price.occasionOffers') }}</h4>
            </div>
          </header>
          <ClientOnly v-if="!isTestEnvironment">
            <template #default>
              <VueECharts
                v-if="occasionChartOption && chartVisibility.occasion"
                :option="occasionChartOption"
                :autoresize="true"
                class="product-price__chart"
              />
              <div v-else class="product-price__chart-placeholder" />
            </template>
            <template #fallback>
              <div class="product-price__chart-placeholder" />
            </template>
          </ClientOnly>
          <template v-else>
            <div
              v-if="occasionChartOption"
              class="echart-stub"
              :data-option="JSON.stringify(occasionChartOption)"
            ></div>
            <div v-else class="product-price__chart-placeholder" />
          </template>
          <footer
            v-if="occasionStats"
            class="product-price__metrics"
            :class="
              bestOccasionOffer
                ? 'product-price__metrics--with-offer'
                : 'product-price__metrics--solo'
            "
          >
            <ClientOnly>
              <NuxtLink
                v-if="bestOccasionOffer && bestOccasionOfferLink"
                :to="bestOccasionOfferLink"
                class="product-price__metrics-highlight product-price__metrics-offer-card product-price__metrics-offer--link"
                :aria-label="
                  $t('product.price.metrics.viewOffer', {
                    source:
                      bestOccasionOffer?.datasourceName ??
                      $t('product.price.metrics.unknownSource'),
                  })
                "
                @click="
                  handleOfferRedirectClick(
                    bestOccasionOffer,
                    'best-occasion-offer',
                    bestOccasionOfferLink
                  )
                "
              >
                <span class="product-price__metrics-label">
                  {{ $t('product.price.metrics.bestOffer') }}
                </span>
                <div class="product-price__metrics-offer">
                  <img
                    v-if="bestOccasionOffer?.favicon"
                    :src="bestOccasionOffer.favicon"
                    :alt="bestOccasionOffer.datasourceName ?? ''"
                    width="48"
                    height="48"
                  />
                  <div class="product-price__metrics-offer-text">
                    <p class="product-price__metrics-price">
                      {{
                        formatCurrency(
                          bestOccasionOffer?.price,
                          bestOccasionOffer?.currency
                        )
                      }}
                    </p>
                    <p class="product-price__metrics-source">
                      {{
                        bestOccasionOffer?.datasourceName ??
                        $t('product.price.metrics.unknownSource')
                      }}
                    </p>
                  </div>
                  <v-icon
                    icon="mdi-open-in-new"
                    size="18"
                    class="product-price__metrics-offer-icon"
                    aria-hidden="true"
                  />
                </div>
              </NuxtLink>
              <div
                v-else-if="bestOccasionOffer"
                class="product-price__metrics-highlight product-price__metrics-offer-card"
              >
                <span class="product-price__metrics-label">
                  {{ $t('product.price.metrics.bestOffer') }}
                </span>
                <div class="product-price__metrics-offer">
                  <img
                    v-if="bestOccasionOffer?.favicon"
                    :src="bestOccasionOffer.favicon"
                    :alt="bestOccasionOffer.datasourceName ?? ''"
                    width="48"
                    height="48"
                  />
                  <div class="product-price__metrics-offer-text">
                    <p class="product-price__metrics-price">
                      {{
                        formatCurrency(
                          bestOccasionOffer?.price,
                          bestOccasionOffer?.currency
                        )
                      }}
                    </p>
                    <p class="product-price__metrics-source">
                      {{
                        bestOccasionOffer?.datasourceName ??
                        $t('product.price.metrics.unknownSource')
                      }}
                    </p>
                  </div>
                </div>
              </div>
            </ClientOnly>
            <div class="product-price__metrics-summary">
              <dl>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-trending-down"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.lowest') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          occasionStats.min,
                          bestOccasionOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-chart-line"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.average') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          occasionStats.average,
                          bestOccasionOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
                <div class="product-price__metrics-row">
                  <v-icon
                    icon="mdi-trending-up"
                    size="20"
                    class="product-price__metrics-stat-icon"
                    aria-hidden="true"
                  />
                  <div class="product-price__metrics-stat-text">
                    <dt>{{ $t('product.price.metrics.highest') }}</dt>
                    <dd>
                      {{
                        formatStat(
                          occasionStats.max,
                          bestOccasionOffer?.currency ??
                            props.offers?.bestPrice?.currency
                        )
                      }}
                    </dd>
                  </div>
                </div>
              </dl>
            </div>
          </footer>
        </article>

        <p
          v-if="visibleChartsCount === 0"
          class="product-price__charts-empty-message"
        >
          {{ $t('product.price.noHistory') }}
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  onBeforeUnmount,
  onMounted,
  ref,
} from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDistanceToNow, format } from 'date-fns'
import { fr, enUS } from 'date-fns/locale'
import { useAnalytics } from '~/composables/useAnalytics'
import type {
  ProductDto,
  CommercialEvent,
  ProductAggregatedPriceDto,
} from '~~/shared/api-client'
import { ensureECharts } from '~/utils/echarts-loader'

let echartsRegistered = false

const VueECharts = defineAsyncComponent(async () => {
  if (import.meta.client) {
    const echarts = await ensureECharts([
      'LineChart',
      'GridComponent',
      'TooltipComponent',
      'DataZoomComponent',
      'MarkAreaComponent',
      'CanvasRenderer',
    ])

    if (echarts && !echartsRegistered) {
      echartsRegistered = true
      const { core, modules } = echarts
      core.use(modules)
    }
  }

  const module = await import(
    /* webpackChunkName: "vendor-echarts" */ 'vue-echarts'
  )

  return module.default
})

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
  titleParams: {
    type: Object as PropType<Record<string, string> | undefined>,
    default: undefined,
  },
})

const { locale, n, t } = useI18n()
const { trackProductRedirect, extractTokenFromLink, isClientContribLink } =
  useAnalytics()

type HistoryEntry = { timestamp: number; price: number }

const newChartCardRef = ref<HTMLElement | null>(null)
const occasionChartCardRef = ref<HTMLElement | null>(null)

const isTestEnvironment = Boolean(import.meta.vitest ?? process.env.VITEST)

const chartVisibility = ref({
  new: isTestEnvironment,
  occasion: isTestEnvironment,
})
let chartObserver: IntersectionObserver | null = null

const normalizeHistoryEntries = (
  entries?: Array<{ timestamp?: number | null; price?: number | null }>
): HistoryEntry[] =>
  (entries ?? [])
    .map(entry => ({
      timestamp:
        typeof entry.timestamp === 'number'
          ? entry.timestamp
          : Number(entry.timestamp ?? Number.NaN),
      price:
        typeof entry.price === 'number'
          ? entry.price
          : Number(entry.price ?? Number.NaN),
    }))
    .filter(
      (entry): entry is HistoryEntry =>
        Number.isFinite(entry.timestamp) && Number.isFinite(entry.price)
    )

const MIN_HISTORY_POINTS = 3

const newHistory = computed(() =>
  normalizeHistoryEntries(props.offers?.newHistory?.entries)
)
const occasionHistory = computed(() =>
  normalizeHistoryEntries(props.offers?.occasionHistory?.entries)
)

const hasNewHistory = computed(
  () => newHistory.value.length >= MIN_HISTORY_POINTS
)
const hasOccasionHistory = computed(
  () => occasionHistory.value.length >= MIN_HISTORY_POINTS
)

const resolvedEvents = computed(() =>
  props.commercialEvents
    .map(event => ({
      label: event.label,
      start: event.startDate ? new Date(event.startDate).getTime() : Number.NaN,
      end: event.endDate ? new Date(event.endDate).getTime() : Number.NaN,
    }))
    .filter(
      event =>
        Number.isFinite(event.start) &&
        Number.isFinite(event.end) &&
        event.start <= event.end
    )
)

const newChartOption = computed(() =>
  hasNewHistory.value
    ? buildChartOption(newHistory.value, resolvedEvents.value)
    : null
)
const occasionChartOption = computed(() =>
  hasOccasionHistory.value
    ? buildChartOption(occasionHistory.value, resolvedEvents.value)
    : null
)

const visibleChartsCount = computed(
  () => [hasNewHistory.value, hasOccasionHistory.value].filter(Boolean).length
)

const observeCharts = () => {
  if (chartObserver || (!hasNewHistory.value && !hasOccasionHistory.value)) {
    return
  }

  if (isTestEnvironment) {
    chartVisibility.value.new = hasNewHistory.value
    chartVisibility.value.occasion = hasOccasionHistory.value
    return
  }

  if (!import.meta.client) {
    return
  }

  if (typeof IntersectionObserver === 'undefined') {
    chartVisibility.value.new = hasNewHistory.value
    chartVisibility.value.occasion = hasOccasionHistory.value
    return
  }

  chartObserver = new IntersectionObserver(
    entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting || entry.intersectionRatio < 0.3) {
          return
        }

        if (entry.target === newChartCardRef.value) {
          chartVisibility.value.new = true
          chartObserver?.unobserve(entry.target)
        }

        if (entry.target === occasionChartCardRef.value) {
          chartVisibility.value.occasion = true
          chartObserver?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.3 }
  )

  if (newChartCardRef.value) {
    chartObserver.observe(newChartCardRef.value)
  }

  if (occasionChartCardRef.value) {
    chartObserver.observe(occasionChartCardRef.value)
  }
}

onMounted(() => {
  observeCharts()
})

type HistoryStats = {
  average: number
  min: number
  max: number
}

const computeHistoryStats = (entries: HistoryEntry[]): HistoryStats | null => {
  if (!entries.length) {
    return null
  }

  const prices = entries.map(entry => entry.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const average = prices.reduce((acc, price) => acc + price, 0) / prices.length

  return { min, max, average }
}

const newStats = computed(() =>
  hasNewHistory.value ? computeHistoryStats(newHistory.value) : null
)
const occasionStats = computed(() =>
  hasOccasionHistory.value ? computeHistoryStats(occasionHistory.value) : null
)

const bestNewOffer = computed(() => props.offers?.bestNewOffer ?? null)
const bestOccasionOffer = computed(
  () => props.offers?.bestOccasionOffer ?? null
)

const resolveOfferLink = (
  offer: ProductAggregatedPriceDto | null | undefined
): string | null => {
  if (!offer || typeof offer !== 'object') {
    return null
  }

  const token = 'affiliationToken' in offer ? offer.affiliationToken : undefined
  if (token) {
    return `/contrib/${token}`
  }

  return 'url' in offer ? (offer.url ?? null) : null
}

const bestNewOfferLink = computed(() => resolveOfferLink(bestNewOffer.value))
const bestOccasionOfferLink = computed(() =>
  resolveOfferLink(bestOccasionOffer.value)
)

const handleOfferRedirectClick = (
  offer: ProductAggregatedPriceDto | null,
  placement: string,
  link?: string | null
) => {
  if (!isClientContribLink(link)) {
    return
  }

  trackProductRedirect({
    token: extractTokenFromLink(link),
    placement,
    source: offer?.datasourceName ?? null,
    url: link,
  })
}

const formatStat = (value?: number | null, currency: string = 'EUR') =>
  value == null || Number.isNaN(value)
    ? '—'
    : n(value, { style: 'currency', currency, maximumFractionDigits: 2 })

const newTrendLabel = computed(() => {
  const trend = props.offers?.newTrend
  if (!trend) {
    return null
  }

  if (trend.trend === 'PRICE_DECREASE') {
    return t('product.price.trend.decrease', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency: props.offers?.bestPrice?.currency ?? 'EUR',
        maximumFractionDigits: 2,
      }),
    })
  }

  if (trend.trend === 'PRICE_INCREASE') {
    return t('product.price.trend.increase', {
      amount: n(Math.abs(trend.variation ?? 0), {
        style: 'currency',
        currency: props.offers?.bestPrice?.currency ?? 'EUR',
        maximumFractionDigits: 2,
      }),
    })
  }

  return t('product.price.trend.stable')
})

const sortedOffers = computed(() => {
  const entries = props.offers?.offersByCondition ?? {}
  const aggregated = Object.values(entries)
    .flat()
    .map((offer, index) => ({
      ...offer,
      id: offer.url ?? `${offer.datasourceName ?? 'source'}-${index}`,
      conditionLabel: t(
        `product.price.condition.${typeof offer.condition === 'string' ? offer.condition.toLowerCase() : 'new'}`
      ),
      updatedLabel: formatUpdated(offer.timeStamp),
    }))

  return aggregated
    .filter(
      (offer): offer is (typeof aggregated)[number] & { price: number } =>
        typeof offer.price === 'number'
    )
    .sort((a, b) => a.price - b.price)
})

const buildChartOption = (
  entries: HistoryEntry[],
  events: Array<{ start: number; end: number; label?: string }>
) => {
  if (entries.length < MIN_HISTORY_POINTS) {
    return null
  }

  const data = entries.map(entry => [entry.timestamp, entry.price])
  const markArea = events.map(event => [
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
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: number | string) =>
        n(Number(value), {
          style: 'currency',
          currency: props.offers?.bestPrice?.currency ?? 'EUR',
          maximumFractionDigits: 2,
        }),
      axisPointer: { type: 'line' },
    },
    grid: { left: 40, right: 24, top: 48, bottom: 48 },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: (value: number) => {
          const numericValue = Number(value)
          if (!Number.isFinite(numericValue)) {
            return ''
          }

          return format(numericValue, 'dd MMM', {
            locale: locale.value.startsWith('fr') ? fr : enUS,
          })
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
        backgroundColor: 'transparent',
        fillerColor: 'rgba(37, 99, 235, 0)',
        borderColor: 'rgba(148, 163, 184, 0.38)',
        handleStyle: {
          color: '#ffffff',
          borderColor: '#2563eb',
          borderWidth: 2,
        },
        moveHandleStyle: {
          color: '#2563eb',
        },
        dataBackground: {
          lineStyle: {
            color: 'rgba(59, 130, 246, 0.35)',
          },
          areaStyle: {
            color: 'rgba(59, 130, 246, 0)',
          },
        },
      },
    ],
    series: [
      {
        type: 'line',
        data,
        smooth: true,
        showSymbol: false,
        lineStyle: {
          width: 3,
          color: '#2563eb',
        },
        areaStyle: {
          color: 'rgba(37, 99, 235, 0.12)',
        },
        emphasis: { focus: 'series' },
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
  const numericTimestamp =
    typeof timestamp === 'number' ? timestamp : Number(timestamp)
  if (!Number.isFinite(numericTimestamp) || numericTimestamp <= 0) {
    return '—'
  }

  return formatDistanceToNow(numericTimestamp, {
    addSuffix: true,
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
}

onBeforeUnmount(() => {
  chartObserver?.disconnect()
})
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

.product-price__history {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.product-price__history-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-price__charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.product-price__charts--single {
  grid-template-columns: 1fr;
}

.product-price__charts--empty {
  display: block;
}

.product-price__chart-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1.25rem;
}

.product-price__chart-heading {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1 1 220px;
  min-width: min(220px, 100%);
}

.product-price__chart-heading h4 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
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

.product-price__metrics {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  align-items: stretch;
}

.product-price__metrics--solo {
  align-items: center;
}

.product-price__metrics--with-offer {
  gap: 1.5rem;
}

.product-price__metrics-highlight {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.82);
  border-radius: 20px;
  padding: 1rem 1.25rem;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.12);
  width: 100%;
}

.product-price__metrics-offer-card {
  flex: 1 1 0%;
}

.product-price__metrics-summary {
  flex: 1 1 0%;
  background: rgba(var(--v-theme-surface-primary-080), 0.65);
  border-radius: 20px;
  padding: 1rem 1.25rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
}

.product-price__metrics--solo .product-price__metrics-summary {
  width: min(100%, 520px);
  margin-inline: auto;
}

.product-price__metrics-summary dl {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  margin: 0;
}

.product-price__metrics-row {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.85rem;
  align-items: center;
}

.product-price__metrics-label {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
}

.product-price__metrics-offer {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.product-price__metrics-offer--link {
  text-decoration: none;
  color: inherit;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-price__metrics-offer--link:hover,
.product-price__metrics-offer--link:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(37, 99, 235, 0.18);
}

.product-price__metrics-offer--link:focus-visible {
  outline: 2px solid rgba(var(--v-theme-accent-primary-highlight), 0.8);
  outline-offset: 4px;
}

.product-price__metrics-offer img {
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.1);
  object-fit: contain;
}

.product-price__metrics-offer-icon {
  color: rgba(var(--v-theme-accent-supporting), 0.9);
  margin-left: auto;
}

.product-price__metrics-offer-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.product-price__metrics-price {
  font-size: clamp(1.2rem, 2vw, 1.6rem);
  font-weight: 700;
  color: rgba(var(--v-theme-text-neutral-strong), 1);
}

.product-price__metrics-source {
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-price__metrics-stat-icon {
  color: rgba(var(--v-theme-accent-supporting), 0.9);
}

.product-price__metrics-stat-text {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-price__metrics-stat-text dt {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.9);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.product-price__metrics-stat-text dd {
  margin: 0;
  font-weight: 600;
  color: rgba(var(--v-theme-text-neutral-strong), 1);
}

.product-price__charts-empty-message {
  padding: 1.25rem 1.5rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-primary-050), 0.9);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.4);
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

@media (min-width: 960px) {
  .product-price__metrics--with-offer {
    flex-direction: row;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .product-price__chart-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .product-price__chart {
    height: 240px;
  }

  .product-price__metrics {
    flex-direction: column;
    align-items: stretch;
  }

  .product-price__metrics--solo {
    align-items: stretch;
  }

  .product-price__metrics-summary,
  .product-price__metrics-offer-card {
    max-width: 100%;
  }
}
</style>

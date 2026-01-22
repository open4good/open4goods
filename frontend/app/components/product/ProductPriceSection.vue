<template>
  <section :id="sectionId" class="product-price">
    <header class="product-price__header">
      <h2 class="product-price__title">
        {{ $t('product.price.title') }}
      </h2>
    </header>

    <div class="product-price__content">
      <!-- Best Offers Section -->
      <div class="product-price__section">
        <h3 class="product-price__subtitle-h3">
          {{ $t('product.price.bestOffers', 'Les meilleurs offres') }}
        </h3>

        <!-- Primary Best Offer Card -->
        <v-card
          v-if="bestNewOffer"
          class="product-price__best-offer-card my-4"
          color="primary"
          variant="flat"
        >
          <div
            class="d-flex align-center justify-space-between w-100 flex-wrap gap-4 px-2 py-1"
          >
            <NuxtLink
              v-if="bestNewOfferLink"
              :to="bestNewOfferLink"
              class="d-flex align-center gap-4 text-decoration-none text-white flex-grow-1"
              @click="
                handleOfferRedirectClick(
                  bestNewOffer,
                  'best-new-offer',
                  bestNewOfferLink
                )
              "
            >
              <v-avatar
                size="64"
                rounded="lg"
                color="white"
                class="pa-1 elevation-1"
              >
                <img
                  v-if="bestNewOffer.favicon"
                  :src="bestNewOffer.favicon"
                  :alt="bestNewOffer.datasourceName ?? ''"
                  style="object-fit: contain; width: 100%; height: 100%"
                />
                <v-icon v-else icon="mdi-store" color="grey" />
              </v-avatar>
              <div>
                <div class="text-h4 font-weight-bold product-price__best-price">
                  {{
                    formatCurrency(bestNewOffer.price, bestNewOffer.currency)
                  }}
                </div>
                <div class="text-subtitle-1 opacity-90">
                  {{ bestNewOffer.datasourceName }}
                </div>
              </div>
            </NuxtLink>
            <div v-else class="d-flex align-center gap-4 flex-grow-1">
              <v-avatar
                size="64"
                rounded="lg"
                color="white"
                class="pa-1 elevation-1"
              >
                <img
                  v-if="bestNewOffer.favicon"
                  :src="bestNewOffer.favicon"
                  :alt="bestNewOffer.datasourceName ?? ''"
                  style="object-fit: contain; width: 100%; height: 100%"
                />
                <v-icon v-else icon="mdi-store" color="grey" />
              </v-avatar>
              <div>
                <div class="text-h4 font-weight-bold product-price__best-price">
                  {{
                    formatCurrency(bestNewOffer.price, bestNewOffer.currency)
                  }}
                </div>
                <div class="text-subtitle-1 opacity-90">
                  {{ bestNewOffer.datasourceName }}
                </div>
              </div>
            </div>

            <v-btn
              v-if="bestNewOfferLink"
              :href="bestNewOfferLink"
              target="_blank"
              variant="flat"
              color="surface"
              class="product-price__cta-btn text-primary font-weight-bold px-6"
              height="48"
              @click="
                handleOfferRedirectClick(
                  bestNewOffer,
                  'best-new-offer',
                  bestNewOfferLink
                )
              "
            >
              {{ $t('product.price.viewDeal', "Voir l'offre") }}
              <v-icon end icon="mdi-open-in-new" />
            </v-btn>
          </div>
        </v-card>

        <!-- Offers Table -->
        <v-data-table
          v-if="allOffers.length"
          :headers="offersHeaders"
          :items="allOffers"
          class="mt-6 rounded-xl border product-price__offers-table"
          density="comfortable"
          :items-per-page="5"
        >
          <template #[`item.merchant`]="{ item }">
            <div class="d-flex align-center gap-2">
              <img
                v-if="item.favicon"
                :src="item.favicon"
                width="24"
                height="24"
                class="rounded"
                :alt="item.datasourceName"
              />
              <span class="font-weight-medium">{{ item.datasourceName }}</span>
            </div>
          </template>
          <template #[`item.price`]="{ item }">
            <span class="font-weight-bold text-body-1">{{
              formatCurrency(item.price, item.currency)
            }}</span>
          </template>
          <template #[`item.condition`]="{ item }">
            <v-chip
              size="small"
              :color="item.condition === 'NEW' ? 'success' : 'warning'"
              variant="tonal"
              class="font-weight-medium"
            >
              {{
                $t(`product.price.condition.${item.condition}`, item.condition)
              }}
            </v-chip>
          </template>
          <template #[`item.actions`]="{ item }">
            <v-btn
              :href="resolveOfferLink(item)"
              target="_blank"
              icon
              variant="text"
              size="small"
              color="primary"
              @click="
                handleOfferRedirectClick(
                  item,
                  'offers-table',
                  resolveOfferLink(item)
                )
              "
            >
              <v-icon icon="mdi-open-in-new" />
            </v-btn>
          </template>
        </v-data-table>
      </div>

      <!-- History Section -->
      <div class="product-price__section mt-10">
        <h3 class="product-price__subtitle-h3 mb-4">
          {{ $t('product.price.history', 'Historique') }}
        </h3>

        <div
          id="price-history"
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
                  @click="handleNewChartClick"
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
            <!-- Commercial Events Card Support -->
            <v-card
              v-if="selectedCommercialEvent"
              class="product-price__event-card"
              variant="tonal"
            >
              <div class="product-price__event-card-header">
                <div>
                  <p class="product-price__event-card-title">
                    {{ $t('product.price.events.detailsTitle') }}
                  </p>
                  <p class="product-price__event-card-label">
                    {{ selectedCommercialEvent.label }}
                  </p>
                </div>
                <v-btn
                  variant="text"
                  size="small"
                  class="product-price__event-card-clear"
                  @click="clearSelectedCommercialEvent"
                >
                  {{ $t('product.price.events.clearSelection') }}
                </v-btn>
              </div>
              <p class="product-price__event-card-dates">
                <span class="product-price__event-card-dates-label">
                  {{ $t('product.price.events.dateLabel') }}
                </span>
                {{ formatEventDateRange(selectedCommercialEvent) }}
              </p>
            </v-card>

            <!-- Metrics Footer (Stats only) -->
            <footer
              v-if="newStats"
              class="product-price__metrics product-price__metrics--solo"
            >
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
              class="product-price__metrics product-price__metrics--solo"
            >
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
import { format } from 'date-fns'
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
      'CustomChart',
      'GridComponent',
      'TooltipComponent',
      'DataZoomComponent',
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
const {
  trackProductRedirect,
  trackAffiliateClick,
  trackSectionView,
  extractTokenFromLink,
  isClientContribLink,
} = useAnalytics()

type HistoryEntry = { timestamp: number; price: number }

const newChartCardRef = ref<HTMLElement | null>(null)
const occasionChartCardRef = ref<HTMLElement | null>(null)

const isTestEnvironment = Boolean(import.meta.vitest ?? process.env.VITEST)

const chartVisibility = ref({
  new: isTestEnvironment,
  occasion: isTestEnvironment,
})
const chartTrackedVisibility = ref({
  new: false,
  occasion: false,
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

type ResolvedCommercialEvent = {
  id: string
  label: string
  start: number
  end: number
  color: string
}

type PackedCommercialEvent = ResolvedCommercialEvent & { rowIndex: number }

const EVENT_COLOR_PALETTE = ['#0ea5e9', '#14b8a6', '#f97316', '#a855f7']
const EVENT_ROW_HEIGHT = 18
const EVENT_BAND_PADDING = 6
const EVENT_BAND_GAP = 10
const EVENT_MIN_WIDTH = 2

const hashLabel = (label: string) =>
  label.split('').reduce((acc, char) => (acc * 31 + char.charCodeAt(0)) | 0, 0)

const resolveEventColor = (label: string, index: number) => {
  if (!label) {
    return EVENT_COLOR_PALETTE[index % EVENT_COLOR_PALETTE.length]
  }

  const hash = Math.abs(hashLabel(label))
  return EVENT_COLOR_PALETTE[hash % EVENT_COLOR_PALETTE.length]
}

const formatAxisLabel = (value: number) => {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    return ''
  }

  return format(numericValue, 'dd MMM', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })
}

const resolvedEvents = computed<ResolvedCommercialEvent[]>(() =>
  props.commercialEvents
    .map((event, index) => {
      const start = event.startDate
        ? new Date(event.startDate).getTime()
        : Number.NaN
      const end = event.endDate ? new Date(event.endDate).getTime() : Number.NaN
      const label = event.label?.trim() || t('product.price.events.untitled')

      if (!Number.isFinite(start) || !Number.isFinite(end) || start > end) {
        return null
      }

      return {
        id: `${label}-${start}-${end}-${index}`,
        label,
        start,
        end,
        color: resolveEventColor(label, index),
      }
    })
    .filter((event): event is ResolvedCommercialEvent => event !== null)
)

const selectedCommercialEventId = ref<string | null>(null)
const selectedCommercialEvent = computed(() =>
  resolvedEvents.value.find(
    event => event.id === selectedCommercialEventId.value
  )
)

const formatEventDate = (value: number) =>
  format(value, 'dd MMM', {
    locale: locale.value.startsWith('fr') ? fr : enUS,
  })

const formatEventDateRange = (event: ResolvedCommercialEvent) => {
  if (event.start === event.end) {
    return t('product.price.events.singleDay', {
      date: formatEventDate(event.start),
    })
  }

  return t('product.price.events.dateRange', {
    start: formatEventDate(event.start),
    end: formatEventDate(event.end),
  })
}

const clearSelectedCommercialEvent = () => {
  selectedCommercialEventId.value = null
}

const newChartOption = computed(() =>
  hasNewHistory.value
    ? buildChartOption(newHistory.value, resolvedEvents.value, {
        enableEventBand: true,
        selectedEventId: selectedCommercialEventId.value,
      })
    : null
)
const occasionChartOption = computed(() =>
  hasOccasionHistory.value
    ? buildChartOption(occasionHistory.value, [], { enableEventBand: false })
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
    if (hasNewHistory.value && !chartTrackedVisibility.value.new) {
      chartTrackedVisibility.value.new = true
      trackSectionView({
        sectionId: `${props.sectionId}-new-history`,
        page: 'product',
        label: 'new-history',
      })
    }
    if (hasOccasionHistory.value && !chartTrackedVisibility.value.occasion) {
      chartTrackedVisibility.value.occasion = true
      trackSectionView({
        sectionId: `${props.sectionId}-occasion-history`,
        page: 'product',
        label: 'occasion-history',
      })
    }
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
          if (!chartTrackedVisibility.value.new) {
            chartTrackedVisibility.value.new = true
            trackSectionView({
              sectionId: `${props.sectionId}-new-history`,
              page: 'product',
              label: 'new-history',
            })
          }
        }

        if (entry.target === occasionChartCardRef.value) {
          chartVisibility.value.occasion = true
          chartObserver?.unobserve(entry.target)
          if (!chartTrackedVisibility.value.occasion) {
            chartTrackedVisibility.value.occasion = true
            trackSectionView({
              sectionId: `${props.sectionId}-occasion-history`,
              page: 'product',
              label: 'occasion-history',
            })
          }
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

const allOffers = computed(() => {
  const byCondition = props.offers?.offersByCondition ?? {}
  const list: Array<ProductAggregatedPriceDto & { condition: string }> = []

  for (const [condition, offers] of Object.entries(byCondition)) {
    if (Array.isArray(offers)) {
      offers.forEach(offer => {
        list.push({ ...offer, condition })
      })
    }
  }

  // Sort by price ascending
  return list.sort((a, b) => (a.price ?? 0) - (b.price ?? 0))
})

const offersHeaders = [
  {
    title: t('product.price.table.merchant', 'Marchand'),
    key: 'merchant',
    sortable: true,
  },
  {
    title: t('product.price.table.price', 'Prix'),
    key: 'price',
    sortable: true,
  },
  {
    title: t('product.price.table.condition', 'Etat'),
    key: 'condition',
    sortable: true,
  },
  { title: '', key: 'actions', sortable: false, align: 'end' },
]

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

  trackAffiliateClick({
    token: extractTokenFromLink(link),
    url: link,
    partner: offer?.datasourceName ?? null,
    placement,
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

const packCommercialEvents = (events: ResolvedCommercialEvent[]) => {
  const sorted = [...events].sort((first, second) => {
    if (first.start !== second.start) {
      return first.start - second.start
    }

    const firstDuration = first.end - first.start
    const secondDuration = second.end - second.start
    return secondDuration - firstDuration
  })

  const rowEndTimes: number[] = []
  const packed: PackedCommercialEvent[] = []

  sorted.forEach(event => {
    const rowIndex = rowEndTimes.findIndex(endTime => endTime <= event.start)

    if (rowIndex === -1) {
      rowEndTimes.push(event.end)
      packed.push({ ...event, rowIndex: rowEndTimes.length - 1 })
    } else {
      rowEndTimes[rowIndex] = event.end
      packed.push({ ...event, rowIndex })
    }
  })

  return { packed, rowCount: rowEndTimes.length }
}

const buildChartOption = (
  entries: HistoryEntry[],
  events: ResolvedCommercialEvent[],
  config: { enableEventBand: boolean; selectedEventId?: string | null }
) => {
  if (entries.length < MIN_HISTORY_POINTS) {
    return null
  }

  const data = entries.map(entry => [entry.timestamp, entry.price])
  const currency = props.offers?.bestPrice?.currency ?? 'EUR'

  const baseOption = {
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value: number | string) =>
        n(Number(value), {
          style: 'currency',
          currency,
          maximumFractionDigits: 2,
        }),
      axisPointer: { type: 'line' },
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
  }

  const lineSeries = {
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
  }

  if (!config.enableEventBand || events.length === 0) {
    return {
      ...baseOption,
      grid: { left: 40, right: 24, top: 48, bottom: 48 },
      xAxis: {
        type: 'time',
        axisLabel: { formatter: formatAxisLabel },
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) =>
            n(Number(value), {
              style: 'currency',
              currency,
              maximumFractionDigits: 0,
            }),
        },
      },
      series: [lineSeries],
    }
  }

  const { packed, rowCount } = packCommercialEvents(events)
  const eventBandHeight =
    Math.max(rowCount, 1) * EVENT_ROW_HEIGHT + EVENT_BAND_PADDING * 2
  const eventBandTop = 18
  const mainChartTop = eventBandTop + eventBandHeight + EVENT_BAND_GAP

  const eventSeriesData = packed.map(event => ({
    value: [
      event.start,
      event.end,
      event.rowIndex,
      event.label,
      event.color,
      event.id,
    ],
  }))

  return {
    ...baseOption,
    grid: [
      { left: 40, right: 24, top: eventBandTop, height: eventBandHeight },
      { left: 40, right: 24, top: mainChartTop, bottom: 48 },
    ],
    xAxis: [
      {
        type: 'time',
        gridIndex: 1,
        axisLabel: { formatter: formatAxisLabel },
      },
      {
        type: 'time',
        gridIndex: 0,
        axisLabel: { show: false },
        axisTick: { show: false },
        axisLine: { show: false },
        splitLine: { show: false },
      },
    ],
    yAxis: [
      {
        type: 'value',
        gridIndex: 1,
        axisLabel: {
          formatter: (value: number) =>
            n(Number(value), {
              style: 'currency',
              currency,
              maximumFractionDigits: 0,
            }),
        },
      },
      {
        type: 'value',
        gridIndex: 0,
        min: 0,
        max: rowCount,
        inverse: true,
        axisLabel: { show: false },
        axisTick: { show: false },
        axisLine: { show: false },
        splitLine: { show: false },
      },
    ],
    dataZoom: [
      {
        type: 'inside',
        xAxisIndex: [0, 1],
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
        xAxisIndex: [0, 1],
      },
    ],
    series: [
      {
        ...lineSeries,
        xAxisIndex: 0,
        yAxisIndex: 0,
      },
      {
        type: 'custom',
        name: 'commercial-events',
        renderItem: (params, api) => {
          const start = Number(api.value(0))
          const end = Number(api.value(1))
          const rowIndex = Number(api.value(2))
          const label = String(api.value(3))
          const color = String(api.value(4))
          const eventId = String(api.value(5))
          const isSelected = eventId === config.selectedEventId

          const startCoord = api.coord([start, rowIndex + 0.5])
          const endCoord = api.coord([end, rowIndex + 0.5])
          const bandHeight = api.size([0, 1])[1]
          const rowHeight = Math.max(12, bandHeight - 6)
          const rectWidth = Math.max(
            endCoord[0] - startCoord[0],
            EVENT_MIN_WIDTH
          )
          const rectX = startCoord[0]
          const rectY = startCoord[1] - rowHeight / 2

          const clipX = Math.max(rectX, params.coordSys.x)
          const clipRight = Math.min(
            rectX + rectWidth,
            params.coordSys.x + params.coordSys.width
          )
          const clipWidth = clipRight - clipX

          if (clipWidth <= 0) {
            return null
          }

          const rectShape = {
            x: clipX,
            y: rectY,
            width: clipWidth,
            height: rowHeight,
            r: 4,
          }

          const fillOpacity = isSelected ? 0.25 : 0.15
          const borderOpacity = isSelected ? 0.95 : 0.6

          return {
            type: 'group',
            children: [
              {
                type: 'rect',
                shape: rectShape,
                style: {
                  fill: `rgba(${hexToRgb(color)}, ${fillOpacity})`,
                  stroke: `rgba(${hexToRgb(color)}, ${borderOpacity})`,
                  lineWidth: isSelected ? 2 : 1,
                },
              },
              {
                type: 'text',
                style: {
                  text: label,
                  fill: color,
                  fontSize: 11,
                  fontWeight: 600,
                  width: Math.max(0, rectShape.width - 8),
                  overflow: 'truncate',
                  ellipsis: '…',
                },
                position: [rectShape.x + 4, rectShape.y + rectShape.height / 2],
                textAlign: 'left',
                textVerticalAlign: 'middle',
              },
            ],
          }
        },
        data: eventSeriesData,
        xAxisIndex: 1,
        yAxisIndex: 1,
        tooltip: {
          trigger: 'item',
          formatter: (params: { value: Array<string | number> }) => {
            const label = String(params.value?.[3] ?? '')
            const start = Number(params.value?.[0])
            const end = Number(params.value?.[1])
            const dateRange =
              Number.isFinite(start) && Number.isFinite(end)
                ? formatEventDateRange({
                    id: '',
                    label,
                    start,
                    end,
                    color: '',
                  })
                : ''
            return `<strong>${label}</strong><br/>${dateRange}`
          },
        },
      },
    ],
  }
}

const hexToRgb = (hexColor: string) => {
  const normalized = hexColor.replace('#', '')
  const parsed = parseInt(normalized, 16)
  const r = (parsed >> 16) & 255
  const g = (parsed >> 8) & 255
  const b = parsed & 255
  return `${r}, ${g}, ${b}`
}

const handleNewChartClick = (params: {
  seriesType?: string
  value?: Array<string | number>
}) => {
  if (params.seriesType !== 'custom') {
    return
  }

  const eventId = String(params.value?.[5] ?? '')
  selectedCommercialEventId.value = eventId || null
}

const formatCurrency = (value?: number | null, currency: string = 'EUR') => {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }

  return n(value, { style: 'currency', currency, maximumFractionDigits: 2 })
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

.product-price__title {
  font-size: clamp(1.6rem, 2.4vw, 2.2rem);
  font-weight: 700;
}

.product-price__subtitle-h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-price__best-offer-card {
  border-radius: 24px;
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.product-price__best-offer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(var(--v-theme-primary), 0.25);
}

.product-price__cta-btn {
  border-radius: 12px;
}

.product-price__offers-table {
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
}

.product-price__charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}

.product-price__charts--single {
  grid-template-columns: 1fr;
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

.product-price__chart-heading h4 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
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

/* Reusing existing metric styles for history cards */
.product-price__metrics {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  align-items: center;
}

.product-price__metrics-summary {
  background: rgba(var(--v-theme-surface-primary-080), 0.65);
  border-radius: 20px;
  padding: 1rem 1.25rem;
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
  width: min(100%, 520px);
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

.product-price__event-card {
  padding: 1rem;
  background: rgba(var(--v-theme-surface-glass), 0.85);
  border-radius: 16px;
}

.product-price__event-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.product-price__event-card-title {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.8);
}

.product-price__event-card-label {
  margin: 0.2rem 0 0;
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-price__event-card-dates {
  margin: 0.75rem 0 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-size: 0.9rem;
}

.product-price__charts-empty-message {
  padding: 1.25rem 1.5rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-primary-050), 0.9);
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  border: 1px dashed rgba(var(--v-theme-border-primary-strong), 0.4);
}

@media (max-width: 768px) {
  .product-price__chart-header {
    align-items: stretch;
  }
}
</style>

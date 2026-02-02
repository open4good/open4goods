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
        <h3 v-if="allOffers.length > 1" class="product-price__subtitle-h3">
          {{ offersTitle }}
        </h3>
        <v-alert
          v-if="showNoOffersBanner"
          type="info"
          variant="tonal"
          class="product-price__no-offers"
        >
          {{ $t('product.price.noOffersBanner') }}
        </v-alert>

        <v-row align="center" justify="center">
          <v-col cols="12" md="8">
            <!-- Offers Table -->
            <v-data-table
              v-if="allOffers.length > 1"
              :headers="offersHeaders"
              :items="allOffers"
              class="rounded-xl border product-price__offers-table"
              density="comfortable"
              :items-per-page="5"
              hover
              @click:row="onRowClick"
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
                  <span class="font-weight-medium">{{
                    item.datasourceName
                  }}</span>
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
                    $t(
                      `product.price.condition.${item.condition}`,
                      item.condition
                    )
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
          </v-col>

          <v-col cols="12" md="4">
            <v-card
              v-if="allOffers.length > 2"
              class="product-price__competition-card"
              :class="`product-price__competition-card--${competitionLevel.tone}`"
              variant="flat"
            >
              <div class="product-price__competition-content">
                <div
                  class="product-price__competition-icon"
                  :class="`product-price__competition-icon--${competitionLevel.tone}`"
                >
                  <v-icon :icon="competitionLevel.icon" size="28" />
                </div>
                <div class="product-price__competition-text">
                  <p class="product-price__competition-eyebrow">
                    {{
                      $t(
                        'product.price.competition.title',
                        'Niveau de concurrence'
                      )
                    }}
                  </p>
                  <h4 class="product-price__competition-title">
                    {{
                      $t(
                        competitionLevel.labelKey,
                        competitionLevel.labelFallback
                      )
                    }}
                  </h4>
                  <p class="product-price__competition-subtitle">
                    {{
                      $t(
                        competitionLevel.descriptionKey,
                        competitionLevel.descriptionFallback
                      )
                    }}
                  </p>
                </div>
                <v-chip
                  class="product-price__competition-count"
                  variant="tonal"
                  :color="competitionLevel.color"
                  size="small"
                >
                  {{
                    $t('product.price.competition.count', '{count} offres', {
                      count: allOffers.length,
                    })
                  }}
                </v-chip>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </div>

      <!-- History Section -->
      <div class="product-price__section product-price__history-section mt-10">
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
            v-if="showNewSection"
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
              <div
                v-if="hasCommercialEvents"
                class="product-price__chart-actions"
              >
                <v-checkbox
                  v-model="showCommercialEvents"
                  density="compact"
                  hide-details
                  color="primary"
                  class="product-price__events-toggle"
                  :label="$t('product.price.events.toggleLabel')"
                />
              </div>
            </header>
            <v-row class="ma-0">
              <v-col cols="12" md="4">
                <div class="product-price__metrics h-100 pa-4">
                  <!-- Best Offer CTA -->
                  <a
                    v-if="bestNewOffer && bestNewOfferLink"
                    :href="bestNewOfferLink"
                    rel="nofollow"
                    class="product-price__history-cta"
                    @click="
                      handleOfferRedirectClick(
                        bestNewOffer,
                        'price-history-cta',
                        bestNewOfferLink
                      )
                    "
                  >
                    <img
                      v-if="bestNewOffer.favicon"
                      :src="bestNewOffer.favicon"
                      :alt="bestNewOffer.datasourceName ?? ''"
                      class="product-price__history-cta-icon"
                    />
                    <v-icon
                      v-else
                      icon="mdi-store"
                      size="24"
                      class="product-price__history-cta-icon"
                    />
                    <div class="product-price__history-cta-info">
                      <span class="product-price__history-cta-price">
                        {{
                          formatCurrency(
                            bestNewOffer.price,
                            bestNewOffer.currency
                          )
                        }}
                      </span>
                      <span class="product-price__history-cta-merchant">
                        {{ bestNewOffer.datasourceName }}
                      </span>
                    </div>
                    <v-icon icon="mdi-open-in-new" size="18" />
                  </a>
                  <ProductPriceEmptyCard
                    v-else
                    icon="mdi-tag-off-outline"
                    :label="$t('product.price.noOffers.new')"
                  />

                  <div v-if="newStats" class="product-price__metrics-summary">
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
                </div>
              </v-col>
              <v-col cols="12" md="8">
                <ClientOnly v-if="!isTestEnvironment && hasNewHistory">
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
                <div
                  v-else-if="!hasNewHistory"
                  class="d-flex align-center justify-center text-center h-100 pa-6 text-grey-darken-1"
                >
                  <div>
                    <v-icon
                      icon="mdi-chart-timeline-variant"
                      size="48"
                      class="mb-2 opacity-50"
                    />
                    <p>{{ $t('product.price.noHistory') }}</p>
                  </div>
                </div>
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
                  v-if="displaySelectedCommercialEvent"
                  class="product-price__event-card"
                  variant="tonal"
                >
                  <div class="product-price__event-card-header">
                    <div>
                      <p class="product-price__event-card-title">
                        {{ $t('product.price.events.detailsTitle') }}
                      </p>
                      <p class="product-price__event-card-label">
                        {{ displaySelectedCommercialEvent.label }}
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
                    {{ formatEventDateRange(displaySelectedCommercialEvent) }}
                  </p>
                </v-card>
              </v-col>
            </v-row>
          </article>

          <article
            v-if="showOccasionSection"
            ref="occasionChartCardRef"
            class="product-price__chart-card"
          >
            <header class="product-price__chart-header">
              <div class="product-price__chart-heading">
                <h4>{{ $t('product.price.occasionOffers') }}</h4>
              </div>
            </header>
            <v-row class="ma-0">
              <v-col cols="12" md="4">
                <div class="product-price__metrics h-100 pa-4">
                  <a
                    v-if="bestOccasionOffer && bestOccasionOfferLink"
                    :href="bestOccasionOfferLink"
                    rel="nofollow"
                    class="product-price__history-cta"
                    @click="
                      handleOfferRedirectClick(
                        bestOccasionOffer,
                        'price-history-cta',
                        bestOccasionOfferLink
                      )
                    "
                  >
                    <img
                      v-if="bestOccasionOffer.favicon"
                      :src="bestOccasionOffer.favicon"
                      :alt="bestOccasionOffer.datasourceName ?? ''"
                      class="product-price__history-cta-icon"
                    />
                    <v-icon
                      v-else
                      icon="mdi-recycle"
                      size="24"
                      class="product-price__history-cta-icon"
                    />
                    <div class="product-price__history-cta-info">
                      <span class="product-price__history-cta-price">
                        {{
                          formatCurrency(
                            bestOccasionOffer.price,
                            bestOccasionOffer.currency
                          )
                        }}
                      </span>
                      <span class="product-price__history-cta-merchant">
                        {{ bestOccasionOffer.datasourceName }}
                      </span>
                    </div>
                    <v-icon icon="mdi-open-in-new" size="18" />
                  </a>
                  <ProductPriceEmptyCard
                    v-else
                    icon="mdi-recycle"
                    :label="$t('product.price.noOffers.occasion')"
                  />
                  <div
                    v-if="occasionStats"
                    class="product-price__metrics-summary"
                  >
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
                </div>
              </v-col>
              <v-col cols="12" md="8">
                <ClientOnly v-if="!isTestEnvironment && hasOccasionHistory">
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
                <div
                  v-else-if="!hasOccasionHistory"
                  class="d-flex align-center justify-center text-center h-100 pa-6 text-grey-darken-1"
                >
                  <div>
                    <v-icon
                      icon="mdi-chart-timeline-variant"
                      size="48"
                      class="mb-2 opacity-50"
                    />
                    <p>{{ $t('product.price.noHistory') }}</p>
                  </div>
                </div>
                <template v-else>
                  <div
                    v-if="occasionChartOption"
                    class="echart-stub"
                    :data-option="JSON.stringify(occasionChartOption)"
                  ></div>
                  <div v-else class="product-price__chart-placeholder" />
                </template>
              </v-col>
            </v-row>
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
  watch,
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
import ProductPriceEmptyCard from '~/components/product/ProductPriceEmptyCard.vue'

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

const showNewSection = computed(
  () => hasNewHistory.value || Boolean(bestNewOffer.value)
)
const showOccasionSection = computed(
  () => hasOccasionHistory.value || Boolean(bestOccasionOffer.value)
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
const EVENT_LABEL_ROW_HEIGHT = 26

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

const hasCommercialEvents = computed(() => resolvedEvents.value.length > 0)
const showCommercialEvents = ref(true)
const displayCommercialEvents = computed(
  () => showCommercialEvents.value && hasCommercialEvents.value
)

const selectedCommercialEventId = ref<string | null>(null)
const selectedCommercialEvent = computed(() =>
  resolvedEvents.value.find(
    event => event.id === selectedCommercialEventId.value
  )
)

const displaySelectedCommercialEvent = computed(() =>
  displayCommercialEvents.value ? selectedCommercialEvent.value : null
)

watch(showCommercialEvents, isEnabled => {
  if (!isEnabled) {
    selectedCommercialEventId.value = null
  }
})

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
    ? buildChartOption(
        newHistory.value,
        displayCommercialEvents.value ? resolvedEvents.value : [],
        {
          enableEventBand: displayCommercialEvents.value,
          selectedEventId: selectedCommercialEventId.value,
        }
      )
    : null
)
const occasionChartOption = computed(() =>
  hasOccasionHistory.value
    ? buildChartOption(occasionHistory.value, [], { enableEventBand: false })
    : null
)

const visibleChartsCount = computed(
  () => [showNewSection.value, showOccasionSection.value].filter(Boolean).length
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
const bestOccasionOfferLink = computed(() =>
  resolveOfferLink(bestOccasionOffer.value)
)

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

const showNoOffersBanner = computed(
  () => !allOffers.value.length && !bestNewOffer.value
)

const competitionLevel = computed(() => {
  const count = allOffers.value.length

  if (count < 2) {
    return {
      tone: 'low',
      icon: 'mdi-alert-outline',
      labelKey: 'product.price.competition.low',
      labelFallback: 'Concurrence faible !',
      descriptionKey: 'product.price.competition.lowDescription',
      descriptionFallback:
        'Peu d’offres disponibles, la comparaison est limitée.',
      color: 'warning',
    }
  }

  if (count > 4) {
    return {
      tone: 'super',
      icon: 'mdi-trophy-outline',
      labelKey: 'product.price.competition.super',
      labelFallback: 'Super concurrence !',
      descriptionKey: 'product.price.competition.superDescription',
      descriptionFallback: 'Beaucoup d’offres pour décrocher le meilleur prix.',
      color: 'success',
    }
  }

  // 2-4 offers
  return {
    tone: 'correct',
    icon: 'mdi-account-multiple-check-outline',
    labelKey: 'product.price.competition.correct',
    labelFallback: 'Concurrence correcte !',
    descriptionKey: 'product.price.competition.correctDescription',
    descriptionFallback: 'Assez d’offres pour comparer en toute sérénité.',
    color: 'info',
  }
})

const offersTitle = computed(() => {
  if (!allOffers.value.length) {
    return t('product.price.bestOffers', 'Les meilleures offres')
  }

  const conditions = new Set(allOffers.value.map(o => o.condition))
  const hasNew = conditions.has('NEW')
  // If we have NEW and nothing else
  if (hasNew && conditions.size === 1) {
    return t('product.price.newOffersOnly', 'Les offres neuves')
  }
  // If we don't have NEW but have others (USED, REFURBISHED...)
  if (!hasNew && conditions.size > 0) {
    return t('product.price.usedOffersOnly', "Les offres d'occasion")
  }

  return t('product.price.bestOffers', 'Les meilleures offres')
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

const onRowClick = (
  event: PointerEvent,
  { item }: { item: ProductAggregatedPriceDto }
) => {
  const target = event.target as HTMLElement
  if (target.closest('.v-btn') || target.closest('a')) {
    return
  }

  const link = resolveOfferLink(item)
  if (link) {
    handleOfferRedirectClick(item, 'offers-table', link)
    window.open(link, '_blank')
  }
}

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
    Math.max(rowCount, 1) * EVENT_LABEL_ROW_HEIGHT + EVENT_BAND_PADDING * 2
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
        name: 'commercial-events-blocks',
        renderItem: (params, api) => {
          const start = Number(api.value(0))
          const end = Number(api.value(1))
          const rowIndex = Number(api.value(2))
          const color = String(api.value(4))
          const eventId = String(api.value(5))
          const isSelected = eventId === config.selectedEventId

          const startCoord = api.coord([start, rowIndex + 0.5])
          const endCoord = api.coord([end, rowIndex + 0.5])
          const bandHeight = api.size([0, 1])[1]
          const rowHeight = Math.max(EVENT_LABEL_ROW_HEIGHT - 6, bandHeight - 6)
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

          const fillOpacity = isSelected ? 0.32 : 0.22
          const borderOpacity = isSelected ? 0.95 : 0.6

          return {
            type: 'rect',
            shape: {
              x: clipX,
              y: rectY,
              width: clipWidth,
              height: rowHeight,
              r: 4,
            },
            style: {
              fill: `rgba(${hexToRgb(color)}, ${fillOpacity})`,
              stroke: `rgba(${hexToRgb(color)}, ${borderOpacity})`,
              lineWidth: isSelected ? 2 : 1,
            },
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

.product-price__offers-table :deep(tbody tr) {
  cursor: pointer;
}

.product-price__no-offers {
  margin-top: 1rem;
  border-radius: 16px;
}

.product-price__competition-card {
  border-radius: 24px;
  padding: 1.5rem;
  background: rgba(var(--v-theme-surface-glass), 0.96);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  box-shadow:
    0 12px 28px rgba(var(--v-theme-shadow-primary-600), 0.08),
    inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.04);
}

.product-price__competition-card--low {
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-050), 0.95),
    rgba(var(--v-theme-surface-muted), 0.9)
  );
}

.product-price__competition-card--correct {
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-080), 0.95),
    rgba(var(--v-theme-surface-glass), 0.9)
  );
}

.product-price__competition-card--super {
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-surface-primary-100), 0.95),
    rgba(var(--v-theme-surface-glass-strong), 0.92)
  );
}

.product-price__competition-content {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1.5rem;
  align-items: center;
}

.product-price__competition-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--v-theme-surface-primary-120), 0.85);
  color: rgb(var(--v-theme-text-on-accent));
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.1);
}

.product-price__competition-icon--low {
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
}

.product-price__competition-icon--correct {
  background: rgba(var(--v-theme-surface-primary-100), 0.9);
}

.product-price__competition-icon--super {
  background: rgba(var(--v-theme-surface-primary-120), 0.95);
}

.product-price__competition-text {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.product-price__competition-eyebrow {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-price__competition-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-price__competition-subtitle {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-size: 0.95rem;
}

.product-price__competition-count {
  font-weight: 600;
  border-radius: 999px;
}

.product-price__single-offer {
  border-radius: 24px;
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

.product-price__chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.product-price__chart-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.product-price__events-toggle {
  margin: 0;
}

:deep(.product-price__events-toggle .v-label) {
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
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

  .product-price__competition-content {
    grid-template-columns: 1fr;
    gap: 1rem;
    text-align: left;
  }

  .product-price__competition-count {
    justify-self: flex-start;
  }

  .product-price__charts {
    grid-template-columns: 1fr;
  }

  .product-price__chart-card {
    max-width: 100%;
  }

  .product-price__offers-table {
    overflow-x: auto;
  }

  .product-price__history-cta {
    max-width: 100%;
  }

  .product-price__history-section {
    display: none;
  }
}

/* Best Offer CTA in history section */
.product-price__history-cta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: rgba(var(--v-theme-primary), 0.1);
  border-radius: 16px;
  border: 1px solid rgba(var(--v-theme-primary), 0.2);
  text-decoration: none;
  color: inherit;
  transition:
    background 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
  width: min(100%, 520px);
}

.product-price__history-cta:hover {
  background: rgba(var(--v-theme-primary), 0.16);
  box-shadow: 0 4px 16px rgba(var(--v-theme-primary), 0.18);
  transform: translateY(-2px);
}

.product-price__history-cta-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: contain;
  flex-shrink: 0;
}

.product-price__history-cta-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-width: 0;
}

.product-price__history-cta-price {
  font-size: 1.2rem;
  font-weight: 700;
  color: rgb(var(--v-theme-primary));
}

.product-price__history-cta-merchant {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>

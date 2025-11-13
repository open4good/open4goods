<template>
  <v-card :class="['product-life-timeline', `product-life-timeline--${layout}`]" variant="flat">
    <div class="product-life-timeline__header">
      <div>
        <p class="product-life-timeline__eyebrow">
          {{ $t('product.attributes.timeline.title') }}
        </p>
        <p class="product-life-timeline__subtitle">
          {{ $t('product.attributes.timeline.subtitle') }}
        </p>
      </div>
      <v-icon icon="mdi-timeline-text-outline" class="product-life-timeline__header-icon" size="28" />
    </div>

    <div v-if="hasEvents" class="product-life-timeline__body">
      <v-timeline
        :align="timelineAlign"
        :direction="timelineDirection"
        :side="timelineSide"
        density="compact"
        truncate-line="both"
        class="product-life-timeline__timeline"
      >
        <v-timeline-item
          v-for="group in groupedEvents"
          :key="group.key"
          :dot-color="group.accentColor"
          size="small"
          fill-dot
          class="product-life-timeline__item"
        >
          <template #opposite>
            <span class="product-life-timeline__year-pill">{{ group.year }}</span>
          </template>

          <div class="product-life-timeline__year-card">
            <div class="product-life-timeline__year-heading">
              <span class="product-life-timeline__year-label">{{ group.year }}</span>
              <span class="product-life-timeline__year-separator" />
            </div>

            <ul class="product-life-timeline__event-list">
              <li
                v-for="event in group.events"
                :key="event.key"
                class="product-life-timeline__event-list-item"
              >
                <span class="product-life-timeline__event-date-chip">{{ event.formattedDate }}</span>

                <div class="product-life-timeline__event-info">
                  <div class="product-life-timeline__event-heading">
                    <v-icon
                      :icon="event.icon"
                      :color="event.color"
                      size="20"
                      class="product-life-timeline__event-icon"
                    />
                    <div class="product-life-timeline__event-title-block">
                      <p class="product-life-timeline__event-title">{{ event.label }}</p>
                      <p v-if="event.sourceLabel" class="product-life-timeline__event-source">{{ event.sourceLabel }}</p>
                    </div>
                  </div>

                  <div class="product-life-timeline__event-meta">
                    <span v-if="event.conditionLabel" class="product-life-timeline__event-chip">
                      {{ event.conditionLabel }}
                    </span>
                  </div>
                </div>
              </li>
            </ul>
          </div>
        </v-timeline-item>
      </v-timeline>
    </div>

    <p v-else class="product-life-timeline__empty">
      {{ $t('product.attributes.timeline.empty') }}
    </p>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductTimelineDto, ProductTimelineEventDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{ timeline: ProductTimelineDto | null; layout?: 'vertical' | 'horizontal'; alternate?: boolean }>(),
  {
    layout: 'vertical',
    alternate: false,
  },
)

const { t, locale } = useI18n()

type TimelineEventViewModel = {
  key: string
  label: string
  formattedDate: string
  icon: string
  color: string
  conditionLabel: string | null
  sourceLabel: string | null
}

type TimelineYearGroup = {
  key: string
  year: number
  accentColor: string
  events: TimelineEventViewModel[]
}

const eventLabelKeys: Record<string, string> = {
  PRICE_FIRST_SEEN_NEW: 'product.attributes.timeline.events.priceFirstSeenNew',
  PRICE_FIRST_SEEN_OCCASION: 'product.attributes.timeline.events.priceFirstSeenOccasion',
  PRICE_LAST_SEEN_NEW: 'product.attributes.timeline.events.priceLastSeenNew',
  PRICE_LAST_SEEN_OCCASION: 'product.attributes.timeline.events.priceLastSeenOccasion',
  EPREL_ON_MARKET_START: 'product.attributes.timeline.events.eprelOnMarketStart',
  EPREL_ON_MARKET_END: 'product.attributes.timeline.events.eprelOnMarketEnd',
  EPREL_ON_MARKET_FIRST_START: 'product.attributes.timeline.events.eprelOnMarketFirstStart',
  EPREL_FIRST_PUBLICATION: 'product.attributes.timeline.events.eprelFirstPublication',
  EPREL_LAST_PUBLICATION: 'product.attributes.timeline.events.eprelLastPublication',
  EPREL_EXPORT: 'product.attributes.timeline.events.eprelExport',
  EPREL_IMPORTED: 'product.attributes.timeline.events.eprelImported',
  EPREL_ORGANISATION_CLOSED: 'product.attributes.timeline.events.eprelOrganisationClosed',
}

const sourceLabelKeys: Record<string, string> = {
  PRICE_HISTORY: 'product.attributes.timeline.sources.priceHistory',
  EPREL: 'product.attributes.timeline.sources.eprel',
}

const conditionLabelKeys: Record<string, string> = {
  NEW: 'product.attributes.timeline.conditions.new',
  OCCASION: 'product.attributes.timeline.conditions.occasion',
}

const eventIcons: Record<string, string> = {
  PRICE_FIRST_SEEN_NEW: 'mdi-star-four-points-outline',
  PRICE_FIRST_SEEN_OCCASION: 'mdi-recycle-variant',
  PRICE_LAST_SEEN_NEW: 'mdi-history',
  PRICE_LAST_SEEN_OCCASION: 'mdi-history',
  EPREL_ON_MARKET_START: 'mdi-store-plus-outline',
  EPREL_ON_MARKET_END: 'mdi-store-minus-outline',
  EPREL_ON_MARKET_FIRST_START: 'mdi-rocket-launch-outline',
  EPREL_FIRST_PUBLICATION: 'mdi-newspaper-variant-outline',
  EPREL_LAST_PUBLICATION: 'mdi-newspaper-check',
  EPREL_EXPORT: 'mdi-cloud-upload-outline',
  EPREL_IMPORTED: 'mdi-database-import-outline',
  EPREL_ORGANISATION_CLOSED: 'mdi-domain-off',
}

const sourceColors: Record<string, string> = {
  PRICE_HISTORY: 'primary',
  EPREL: 'success',
}

const monthYearFormatter = computed(() => new Intl.DateTimeFormat(locale.value, { month: 'short', year: 'numeric' }))

const groupedEvents = computed<TimelineYearGroup[]>(() => {
  const rawEvents = props.timeline?.events ?? []
  const yearGroups = new Map<number, TimelineYearGroup>()

  rawEvents
    .filter((event): event is ProductTimelineEventDto & { timestamp: number } => typeof event?.timestamp === 'number')
    .sort((a, b) => a.timestamp - b.timestamp)
    .forEach((event, index) => {
      const type = event.type
      const source = event.source
      const key = `${type ?? 'unknown'}-${event.timestamp}-${index}`
      const labelKey = type
        ? eventLabelKeys[type] ?? 'product.attributes.timeline.events.generic'
        : 'product.attributes.timeline.events.generic'
      const label = t(labelKey)
      const date = new Date(event.timestamp)
      const formattedDate = monthYearFormatter.value.format(date)
      const icon = (type && eventIcons[type]) ?? 'mdi-timeline-clock-outline'
      const color = (source && sourceColors[source]) ?? 'primary'
      const conditionLabel = event.condition
        ? t(conditionLabelKeys[event.condition] ?? 'product.attributes.timeline.conditions.generic')
        : null
      const sourceLabel = source
        ? t(sourceLabelKeys[source] ?? 'product.attributes.timeline.sources.generic')
        : null
      const year = date.getFullYear()

      if (!yearGroups.has(year)) {
        yearGroups.set(year, {
          key: `${year}`,
          year,
          accentColor: color,
          events: [],
        })
      }

      const group = yearGroups.get(year)
      if (!group) {
        return
      }

      if (!group.accentColor) {
        group.accentColor = color
      }

      group.events.push({
        key,
        label,
        formattedDate,
        icon,
        color,
        conditionLabel,
        sourceLabel,
      })
    })

  return Array.from(yearGroups.values())
})

const hasEvents = computed(() => groupedEvents.value.length > 0)

const timelineDirection = computed(() => (props.layout === 'horizontal' ? 'horizontal' : 'vertical'))
const timelineSide = computed(() => (props.alternate ? 'both' : 'end'))
const timelineAlign = computed(() => (props.layout === 'horizontal' ? 'center' : 'start'))
</script>

<style scoped>
.product-life-timeline {
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-glass-strong), 0.96);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.5);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 14px 35px -20px rgba(15, 23, 42, 0.25);
}

.product-life-timeline__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.product-life-timeline__eyebrow {
  font-weight: 700;
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-life-timeline__subtitle {
  margin: 0.2rem 0 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  max-width: 32ch;
}

.product-life-timeline__header-icon {
  color: rgb(var(--v-theme-primary));
}

.product-life-timeline__timeline {
  padding-inline-start: 0.5rem;
}

.product-life-timeline--horizontal .product-life-timeline__body {
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.product-life-timeline__year-pill {
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.product-life-timeline__year-card {
  background: rgba(var(--v-theme-surface-primary-050), 0.9);
  border-radius: 18px;
  padding: 1rem 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
}

.product-life-timeline__year-heading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.product-life-timeline__year-label {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-life-timeline__year-separator {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(var(--v-theme-border-primary-strong), 0.4),
    rgba(var(--v-theme-border-primary-strong), 0)
  );
}

.product-life-timeline__event-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.product-life-timeline__event-list-item {
  display: flex;
  gap: 0.9rem;
  align-items: flex-start;
}

.product-life-timeline__event-date-chip {
  min-width: 82px;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  background: rgba(var(--v-theme-primary), 0.08);
  color: rgb(var(--v-theme-primary));
  border: 1px solid rgba(var(--v-theme-primary), 0.35);
  text-align: center;
}

.product-life-timeline__event-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.product-life-timeline__event-heading {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
}

.product-life-timeline__event-title {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-life-timeline__event-source {
  margin: 0.15rem 0 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
}

.product-life-timeline__event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.product-life-timeline__event-chip {
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
  background: rgba(var(--v-theme-primary), 0.08);
  color: rgb(var(--v-theme-primary));
  border: 1px solid rgba(var(--v-theme-primary), 0.35);
}

.product-life-timeline--horizontal .product-life-timeline__timeline {
  padding-inline-start: 0;
  min-width: max-content;
}

.product-life-timeline__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-style: italic;
}
</style>

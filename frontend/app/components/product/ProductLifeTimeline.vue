<template>
  <v-card
    :class="['product-life-timeline', `product-life-timeline--${layout}`]"
    variant="flat"
  >
    <div class="product-life-timeline__header">
      <div>
        <p class="product-life-timeline__eyebrow">
          {{ $t('product.attributes.timeline.title') }}
        </p>
        <p class="product-life-timeline__subtitle">
          {{ $t('product.attributes.timeline.subtitle') }}
        </p>
      </div>
      <v-icon
        icon="mdi-timeline-text-outline"
        class="product-life-timeline__header-icon"
        size="28"
      />
    </div>

    <div v-if="hasEvents" class="product-life-timeline__body">
      <div class="product-life-timeline__rail" role="list">
        <div
          v-for="group in groupedEvents"
          :key="group.key"
          class="product-life-timeline__year-group"
          role="group"
          :aria-label="
            t('product.attributes.timeline.ariaYear', { year: group.year })
          "
        >
          <div class="product-life-timeline__year-label">{{ group.year }}</div>

          <div class="product-life-timeline__year-track">
            <div class="product-life-timeline__rail-line" />

            <div
              v-for="month in group.months"
              :key="month.key"
              class="product-life-timeline__month-group"
            >
              <span class="product-life-timeline__month-label mt-4">
                {{ month.monthLabel }}
              </span>
              <div class="product-life-timeline__event-list">
                <div
                  v-for="event in month.events"
                  :key="event.key"
                  class="product-life-timeline__event"
                  role="listitem"
                >
                  <v-tooltip
                    location="top"
                    max-width="320"
                    content-class="product-life-timeline__tooltip-surface"
                  >
                    <template #activator="{ props: tooltipProps }">
                      <button
                        type="button"
                        class="product-life-timeline__event-point"
                        :style="{
                          '--timeline-event-color': `rgb(var(--v-theme-${event.color}))`,
                        }"
                        v-bind="tooltipProps"
                        :aria-label="event.ariaLabel"
                      >
                        <span class="product-life-timeline__event-dot" />
                        <v-icon
                          :icon="event.icon"
                          size="18"
                          class="product-life-timeline__event-icon"
                        />
                      </button>
                    </template>

                    <div class="product-life-timeline__tooltip">
                      <p class="product-life-timeline__tooltip-title">
                        {{ event.label }}
                      </p>
                      <p class="product-life-timeline__tooltip-description">
                        {{ event.description }}
                      </p>

                      <div class="product-life-timeline__tooltip-meta">
                        <span>
                          {{ t('product.attributes.timeline.tooltip.date') }} ·
                          {{ event.fullDateLabel }}
                        </span>
                        <span v-if="event.sourceLabel">
                          {{ t('product.attributes.timeline.tooltip.source') }}
                          ·
                          {{ event.sourceLabel }}
                        </span>
                      </div>
                    </div>
                  </v-tooltip>

                  <span class="product-life-timeline__event-title">{{
                    event.label
                  }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <p v-else class="product-life-timeline__empty">
      {{ $t('product.attributes.timeline.empty') }}
    </p>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import type {
  ProductTimelineDto,
  ProductTimelineEventDto,
} from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    timeline: ProductTimelineDto | null
    layout?: 'vertical' | 'horizontal'
    alternate?: boolean
  }>(),
  {
    layout: 'horizontal',
    alternate: false,
  }
)

const { t, locale } = useI18n()

type TimelineEventViewModel = {
  key: string
  label: string
  description: string
  timestamp: number
  monthLabel: string
  fullDateLabel: string
  icon: string
  color: string
  sourceLabel: string | null
  ariaLabel: string
}

type TimelineYearGroup = {
  key: string
  year: number
  months: TimelineMonthGroup[]
}

type TimelineMonthGroup = {
  key: string
  monthIndex: number
  monthLabel: string
  events: TimelineEventViewModel[]
}

const eventLabelKeys: Record<string, string> = {
  PRICE_FIRST_SEEN_NEW: 'product.attributes.timeline.events.priceFirstSeenNew',
  PRICE_FIRST_SEEN_OCCASION:
    'product.attributes.timeline.events.priceFirstSeenOccasion',
  PRICE_LAST_SEEN_NEW: 'product.attributes.timeline.events.priceLastSeenNew',
  PRICE_LAST_SEEN_OCCASION:
    'product.attributes.timeline.events.priceLastSeenOccasion',
  EPREL_ON_MARKET_START:
    'product.attributes.timeline.events.eprelOnMarketStart',
  EPREL_ON_MARKET_END: 'product.attributes.timeline.events.eprelOnMarketEnd',
  EPREL_ON_MARKET_FIRST_START:
    'product.attributes.timeline.events.eprelOnMarketFirstStart',
  EPREL_FIRST_PUBLICATION:
    'product.attributes.timeline.events.eprelFirstPublication',
  EPREL_LAST_PUBLICATION:
    'product.attributes.timeline.events.eprelLastPublication',
  EPREL_EXPORT: 'product.attributes.timeline.events.eprelExport',
  EPREL_SPARE_PARTS_END: 'product.attributes.timeline.events.eprelSparePartsEnd',
  EPREL_SOFTWARE_SUPPORT_END:
    'product.attributes.timeline.events.eprelSoftwareSupportEnd',
  EPREL_SUPPORT_END: 'product.attributes.timeline.events.eprelSupportEnd',
  EPREL_IMPORTED: 'product.attributes.timeline.events.eprelImported',
  EPREL_ORGANISATION_CLOSED:
    'product.attributes.timeline.events.eprelOrganisationClosed',
}

const sourceLabelKeys: Record<string, string> = {
  PRICE_HISTORY: 'product.attributes.timeline.sources.priceHistory',
  EPREL: 'product.attributes.timeline.sources.eprel',
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
  EPREL_SPARE_PARTS_END: 'mdi-cog-off-outline',
  EPREL_SOFTWARE_SUPPORT_END: 'mdi-update',
  EPREL_SUPPORT_END: 'mdi-shield-off-outline',
  EPREL_IMPORTED: 'mdi-database-import-outline',
  EPREL_ORGANISATION_CLOSED: 'mdi-domain-off',
}

const sourceColors: Record<string, string> = {
  PRICE_HISTORY: 'primary',
  EPREL: 'success',
}

const sourceOverrideByType: Record<string, string> = {
  EPREL_IMPORTED: 'PRICE_HISTORY',
}

const monthFormatter = computed(
  () => new Intl.DateTimeFormat(locale.value, { month: 'long' })
)
const fullDateFormatter = computed(
  () =>
    new Intl.DateTimeFormat(locale.value, {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    })
)
const eventDescriptionKeys: Record<string, string> = {
  PRICE_FIRST_SEEN_NEW:
    'product.attributes.timeline.descriptions.priceFirstSeenNew',
  PRICE_FIRST_SEEN_OCCASION:
    'product.attributes.timeline.descriptions.priceFirstSeenOccasion',
  PRICE_LAST_SEEN_NEW:
    'product.attributes.timeline.descriptions.priceLastSeenNew',
  PRICE_LAST_SEEN_OCCASION:
    'product.attributes.timeline.descriptions.priceLastSeenOccasion',
  EPREL_ON_MARKET_START:
    'product.attributes.timeline.descriptions.eprelOnMarketStart',
  EPREL_ON_MARKET_END:
    'product.attributes.timeline.descriptions.eprelOnMarketEnd',
  EPREL_ON_MARKET_FIRST_START:
    'product.attributes.timeline.descriptions.eprelOnMarketFirstStart',
  EPREL_FIRST_PUBLICATION:
    'product.attributes.timeline.descriptions.eprelFirstPublication',
  EPREL_LAST_PUBLICATION:
    'product.attributes.timeline.descriptions.eprelLastPublication',
  EPREL_EXPORT: 'product.attributes.timeline.descriptions.eprelExport',
  EPREL_SPARE_PARTS_END:
    'product.attributes.timeline.descriptions.eprelSparePartsEnd',
  EPREL_SOFTWARE_SUPPORT_END:
    'product.attributes.timeline.descriptions.eprelSoftwareSupportEnd',
  EPREL_SUPPORT_END: 'product.attributes.timeline.descriptions.eprelSupportEnd',
  EPREL_IMPORTED: 'product.attributes.timeline.descriptions.eprelImported',
  EPREL_ORGANISATION_CLOSED:
    'product.attributes.timeline.descriptions.eprelOrganisationClosed',
}

const groupedEvents = computed<TimelineYearGroup[]>(() => {
  const rawEvents = props.timeline?.events ?? []
  const yearGroups = new Map<
    number,
    {
      key: string
      year: number
      months: Map<number, TimelineMonthGroup>
    }
  >()

  rawEvents
    .filter(
      (event): event is ProductTimelineEventDto & { timestamp: number } =>
        typeof event?.timestamp === 'number' &&
        !['EPREL_FIRST_PUBLICATION', 'EPREL_LAST_PUBLICATION'].includes(
          event.type ?? ''
        )
    )
    .map(event => ({
      ...event,
      timestamp: normalizeTimestamp(event.timestamp) ?? 0,
    }))
    .sort((a, b) => a.timestamp - b.timestamp)
    .forEach((event, index) => {
      const type = event.type
      const source = event.source
      const key = `${type ?? 'unknown'}-${event.timestamp}-${index}`
      const labelKey = type
        ? (eventLabelKeys[type] ?? 'product.attributes.timeline.events.generic')
        : 'product.attributes.timeline.events.generic'
      const label = t(labelKey)
      const descriptionKey = type
        ? (eventDescriptionKeys[type] ??
          'product.attributes.timeline.descriptions.generic')
        : 'product.attributes.timeline.descriptions.generic'
      const description = t(descriptionKey)
      const date = new Date(event.timestamp)
      const monthLabel = monthFormatter.value.format(date)
      const fullDateLabel = fullDateFormatter.value.format(date)
      const icon = (type && eventIcons[type]) ?? 'mdi-timeline-clock-outline'
      const resolvedSource = (type && sourceOverrideByType[type]) ?? source
      const color =
        (resolvedSource && sourceColors[resolvedSource]) ?? 'primary'
      const sourceLabel = resolvedSource
        ? t(
            sourceLabelKeys[resolvedSource] ??
              'product.attributes.timeline.sources.generic'
          )
        : null
      const year = date.getFullYear()
      const monthIndex = date.getMonth()
      const monthKey = `${year}-${monthIndex}`
      const ariaLabel = t('product.attributes.timeline.tooltip.ariaLabel', {
        label,
        date: fullDateLabel,
        source: sourceLabel ?? t('product.attributes.timeline.sources.generic'),
      })

      if (!yearGroups.has(year)) {
        yearGroups.set(year, {
          key: `${year}`,
          year,
          months: new Map(),
        })
      }

      const group = yearGroups.get(year)
      if (!group) {
        return
      }

      if (!group.months.has(monthIndex)) {
        group.months.set(monthIndex, {
          key: monthKey,
          monthIndex,
          monthLabel,
          events: [],
        })
      }

      const monthGroup = group.months.get(monthIndex)
      if (!monthGroup) {
        return
      }

      monthGroup.events.push({
        key,
        label,
        description,
        timestamp: event.timestamp,
        monthLabel,
        fullDateLabel,
        icon,
        color,
        sourceLabel,
        ariaLabel,
      })
    })

  return Array.from(yearGroups.values())
    .sort((a, b) => a.year - b.year)
    .map(group => {
      const months = Array.from(group.months.values())
        .sort((a, b) => a.monthIndex - b.monthIndex)
        .map(month => ({
          ...month,
          events: month.events.sort((a, b) => a.timestamp - b.timestamp),
        }))

      return {
        key: group.key,
        year: group.year,
        months,
      }
    })
})

const hasEvents = computed(() => groupedEvents.value.length > 0)
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
  width: 100%;
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

.product-life-timeline__body {
  overflow-x: auto;
  padding-bottom: 0.25rem;
  display: flex;
  justify-content: flex-start;
  scroll-padding-inline-start: 0.5rem;
}

.product-life-timeline__rail {
  display: flex;
  gap: 1.75rem;
  align-items: flex-start;
  justify-content: flex-start;
  width: max-content;
  min-width: 100%;
  padding: 0.25rem 0.5rem;
  scroll-snap-type: x mandatory;
  margin: 0;
}

.product-life-timeline--vertical .product-life-timeline__rail {
  flex-direction: column;
  min-width: 100%;
}

.product-life-timeline__year-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 240px;
  scroll-snap-align: start;
}

.product-life-timeline__year-label {
  font-size: 1.1rem;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
  letter-spacing: 0.01em;
}

.product-life-timeline__year-track {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 1.5rem;
  padding: 0.75rem 0.25rem 0.35rem;
  justify-content: center;
  flex-wrap: wrap;
}

.product-life-timeline--vertical .product-life-timeline__year-track {
  flex-wrap: wrap;
}

.product-life-timeline__rail-line {
  position: absolute;
  top: 12px;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(
    90deg,
    rgba(var(--v-theme-border-primary-strong), 0.65),
    rgba(var(--v-theme-border-primary-strong), 0.08)
  );
  border-radius: 999px;
}

.product-life-timeline__month-group {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  min-width: 170px;
  align-items: flex-start;
}

.product-life-timeline__month-label {
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  font-weight: 700;
}

.product-life-timeline__event-list {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  align-items: flex-start;
}

.product-life-timeline__event {
  display: flex;
  flex-direction: row;
  gap: 0.5rem;
  align-items: center;
  position: relative;
  min-width: 0;
  text-align: left;
}

.product-life-timeline__event-point {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.55);
  background: rgba(var(--v-theme-surface-primary-080), 0.9);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.2s ease;
}

.product-life-timeline__event-point::after {
  content: '';
  position: absolute;
  inset: 5px;
  border-radius: 50%;
  background: var(--timeline-event-color, rgba(var(--v-theme-primary), 1));
  opacity: 0.28;
}

.product-life-timeline__event-point:hover,
.product-life-timeline__event-point:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px -18px rgba(15, 23, 42, 0.35);
}

.product-life-timeline__event-dot {
  position: absolute;
  width: 10px;
  height: 10px;
  background: var(--timeline-event-color, rgba(var(--v-theme-primary), 1));
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.75);
}

.product-life-timeline__event-icon {
  position: relative;
  color: rgb(var(--v-theme-text-on-accent));
  z-index: 1;
}

.product-life-timeline__event-title {
  font-size: 0.9rem;
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  line-height: 1.2;
  max-width: 22ch;
}

.product-life-timeline__tooltip {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 300px;
}

.product-life-timeline__tooltip-surface {
  background: rgb(var(--v-theme-surface-default)) !important;
  color: rgb(var(--v-theme-text-neutral-strong));
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  border-radius: 12px;
  padding: 0.75rem 0.9rem;
  box-shadow: 0 14px 32px -24px rgba(15, 23, 42, 0.5);
}

.product-life-timeline__tooltip-title {
  margin: 0;
  font-weight: 700;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-life-timeline__tooltip-description {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95);
  line-height: 1.4;
}

.product-life-timeline__tooltip-meta {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-soft), 0.95);
}

.product-life-timeline__empty {
  margin: 0;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  font-style: italic;
}

@media (max-width: 600px) {
  .product-life-timeline__body {
    justify-content: flex-start;
    scroll-padding-inline-start: 0.75rem;
  }

  .product-life-timeline__rail {
    justify-content: flex-start;
    margin: 0;
    padding-inline-start: 0.75rem;
  }
}
</style>

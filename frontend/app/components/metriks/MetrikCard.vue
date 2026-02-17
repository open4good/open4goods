<script setup lang="ts">
/**
 * MetrikCard — Renders a single metric with value, unit, trend, and icon.
 *
 * Supports three variants:
 * - sm: Compact inline display for table cells.
 * - lg: Medium card.
 * - xl: Large "hero card" with big MDI icon.
 */
import type { MetrikWithTrend, MetrikVariant } from '~/types/metriks'
import { formatMetrikValue, getMetrikIcon } from '~/composables/useMetriks'

const props = withDefaults(
  defineProps<{
    /** The enriched metric to display. */
    metrik: MetrikWithTrend
    /** Display variant. */
    variant?: MetrikVariant
    /** Whether to show the trend indicator. */
    showTrend?: boolean
    /** Comparison period label (e.g. 'vs 7 days'). */
    compareLabel?: string
  }>(),
  {
    variant: 'lg',
    showTrend: true,
    compareLabel: '',
  }
)

const formattedValue = computed(() =>
  formatMetrikValue(props.metrik.value, props.metrik.unit)
)

const icon = computed(() => getMetrikIcon(props.metrik.groups, props.metrik.id))

const trendIcon = computed(() => {
  if (props.metrik.percentChange === null) return null
  if (props.metrik.percentChange > 0) return 'mdi-trending-up'
  if (props.metrik.percentChange < 0) return 'mdi-trending-down'
  return 'mdi-trending-neutral'
})

const trendColor = computed(() => {
  if (props.metrik.percentChange === null) return 'grey'
  if (props.metrik.percentChange > 0) return 'success'
  if (props.metrik.percentChange < 0) return 'error'
  return 'grey'
})

const trendLabel = computed(() => {
  if (props.metrik.percentChange === null) return '—'

  const sign = props.metrik.percentChange > 0 ? '+' : ''
  const percent = `${sign}${props.metrik.percentChange.toFixed(1)}%`

  if (props.metrik.absoluteChange !== null) {
    const absSign = props.metrik.absoluteChange > 0 ? '+' : ''
    const abs = `${absSign}${formatMetrikValue(props.metrik.absoluteChange, props.metrik.unit)}`
    return `${abs} (${percent})`
  }

  return percent
})

const isError = computed(() => props.metrik.status === 'error')
</script>

<template>
  <!-- SM variant: compact inline -->
  <span
    v-if="variant === 'sm'"
    class="metrik-sm d-inline-flex align-center ga-1"
  >
    <span :class="{ 'text-error': isError }" class="font-weight-bold">
      {{ formattedValue }}
    </span>
    <v-tooltip v-if="showTrend && trendIcon" :text="trendLabel" location="top">
      <template #activator="{ props: tp }">
        <v-icon v-bind="tp" :icon="trendIcon" :color="trendColor" size="14" />
      </template>
    </v-tooltip>
    <v-tooltip :text="metrik.description" location="top" max-width="300">
      <template #activator="{ props: tp }">
        <v-icon
          v-bind="tp"
          icon="mdi-information-outline"
          size="12"
          color="grey"
        />
      </template>
    </v-tooltip>
  </span>

  <!-- LG variant: medium card -->
  <v-card
    v-else-if="variant === 'lg'"
    class="metrik-lg"
    variant="outlined"
    rounded="lg"
  >
    <v-card-text class="d-flex align-center ga-3 pa-3">
      <v-icon :icon="icon" size="28" color="primary" />
      <div class="flex-grow-1">
        <div class="text-caption text-medium-emphasis text-truncate">
          {{ metrik.name }}
        </div>
        <div
          class="text-h6 font-weight-bold"
          :class="{ 'text-error': isError }"
        >
          {{ formattedValue }}
        </div>
      </div>
      <v-chip
        v-if="showTrend && trendIcon"
        :color="trendColor"
        size="small"
        variant="tonal"
        :prepend-icon="trendIcon"
      >
        {{ trendLabel }}
      </v-chip>
      <v-tooltip :text="metrik.description" location="top" max-width="300">
        <template #activator="{ props: tp }">
          <v-icon
            v-bind="tp"
            icon="mdi-information-outline"
            size="16"
            color="grey"
          />
        </template>
      </v-tooltip>
    </v-card-text>
  </v-card>

  <!-- XL variant: hero card -->
  <v-card
    v-else
    class="metrik-xl"
    variant="elevated"
    rounded="xl"
    elevation="2"
  >
    <v-card-text class="d-flex flex-column align-center text-center pa-6">
      <v-avatar size="64" color="primary" variant="tonal" class="mb-3">
        <v-icon :icon="icon" size="36" />
      </v-avatar>
      <div class="text-body-2 text-medium-emphasis mb-1">
        {{ metrik.name }}
      </div>
      <div
        class="text-h4 font-weight-bold mb-2"
        :class="{ 'text-error': isError }"
      >
        {{ formattedValue }}
      </div>
      <v-chip
        v-if="showTrend && trendIcon"
        :color="trendColor"
        size="small"
        variant="tonal"
        :prepend-icon="trendIcon"
        class="mb-2"
      >
        {{ trendLabel }}
      </v-chip>
      <div class="text-caption text-medium-emphasis">
        {{ compareLabel }}
      </div>
    </v-card-text>
    <v-tooltip
      :text="metrik.description"
      location="top"
      max-width="300"
      activator="parent"
    />
  </v-card>
</template>

<style scoped>
.metrik-xl {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}
.metrik-xl:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(var(--v-theme-shadow-primary-600), 0.15);
}
</style>

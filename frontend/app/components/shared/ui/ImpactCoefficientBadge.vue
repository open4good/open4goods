<template>
  <v-tooltip
    v-if="badgeLabel && tooltipLabel"
    :text="tooltipLabel"
    location="top"
  >
    <template #activator="{ props: tooltipProps }">
      <v-chip
        class="impact-coefficient-badge"
        density="comfortable"
        rounded="pill"
        variant="flat"
        :data-coefficient-decimal="normalizedValue ?? undefined"
        :data-coefficient-percent="percentValue ?? undefined"
        v-bind="tooltipProps"
      >
        <span class="impact-coefficient-badge__label">{{ badgeLabel }}</span>
      </v-chip>
    </template>
  </v-tooltip>
  <v-chip
    v-else-if="badgeLabel"
    class="impact-coefficient-badge"
    density="comfortable"
    rounded="pill"
    variant="flat"
    :data-coefficient-decimal="normalizedValue ?? undefined"
    :data-coefficient-percent="percentValue ?? undefined"
  >
    <span class="impact-coefficient-badge__label">{{ badgeLabel }}</span>
  </v-chip>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  value?: number | null
  labelKey?: string
  labelParams?: Record<string, unknown>
  tooltipKey?: string
  tooltipParams?: Record<string, unknown>
}>()

const { n, t } = useI18n()

const normalizedValue = computed<number | null>(() => {
  const raw = props.value
  if (raw == null) {
    return null
  }

  const numeric = typeof raw === 'number' ? raw : Number(raw)
  if (!Number.isFinite(numeric)) {
    return null
  }

  return Math.min(Math.max(numeric, 0), 1)
})

const percentValue = computed<number | null>(() => {
  if (normalizedValue.value == null) {
    return null
  }

  return normalizedValue.value * 100
})

const percentLabel = computed<string | null>(() => {
  if (percentValue.value == null) {
    return null
  }

  return n(percentValue.value, {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0,
  })
})

const badgeLabel = computed<string | null>(() => {
  if (percentLabel.value == null) {
    return null
  }

  const key = props.labelKey?.trim().length
    ? props.labelKey
    : 'product.impact.weightChip'
  const params = {
    value: percentLabel.value,
    percent: percentLabel.value,
    ...(props.labelParams ?? {}),
  }

  return t(key as string, params)
})

const tooltipLabel = computed<string | null>(() => {
  if (percentLabel.value == null) {
    return null
  }

  const key = props.tooltipKey?.trim().length
    ? props.tooltipKey
    : 'product.impact.weightTooltip'
  const params = {
    value: percentLabel.value,
    percent: percentLabel.value,
    ...(props.tooltipParams ?? {}),
  }

  return t(key as string, params)
})
</script>

<style scoped>
.impact-coefficient-badge {
  background-color: rgba(var(--v-theme-surface-primary-080), 0.85) !important;
  color: rgb(var(--v-theme-text-neutral-strong)) !important;
  font-weight: 600;
  letter-spacing: 0.03em;
  text-transform: none;
}

.impact-coefficient-badge__label {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
</style>

<template>
  <v-chip
    v-if="badgeLabel"
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

const props = defineProps<{ value?: number | null }>()

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

const badgeLabel = computed<string | null>(() => {
  if (percentValue.value == null) {
    return null
  }

  const formatted = n(percentValue.value, {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0,
  })

  return t('product.impact.weightChip', { value: formatted })
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

<template>
  <header class="impact-subscore-header">
    <div class="impact-subscore-header__info">
      <h4 class="impact-subscore-header__title">
        {{ title }}
      </h4>
      <div v-if="lifecycle?.length" class="impact-subscore-header__chips">
        <v-chip
          v-for="stage in lifecycle"
          :key="stage"
          :color="lifecycleColors[stage] ?? 'surface-ice-100'"
          size="x-small"
          variant="tonal"
        >
          {{ lifecycleLabels[stage] ?? stage }}
        </v-chip>
      </div>
      <p v-if="subtitle" class="impact-subscore-header__subtitle">
        {{ subtitle }}
      </p>
    </div>

    <div
      v-if="on20Value"
      class="impact-subscore-header__score"
      aria-hidden="true"
    >
      <span class="impact-subscore-header__score-value">{{ on20Value }}</span>
      <span class="impact-subscore-header__score-scale">/20</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
const props = defineProps<{
  title: string
  subtitle?: string
  on20?: number | null
  percent?: number | null
  lifecycle?: string[]
}>()

const { n, t } = useI18n()
const on20Value = computed(() => {
  if (typeof props.on20 !== 'number' || Number.isNaN(props.on20)) {
    return null
  }

  return n(props.on20, { maximumFractionDigits: 1, minimumFractionDigits: 0 })
})

const lifecycleLabels = computed<Record<string, string>>(() => ({
  EXTRACTION: t('product.impact.lifecycle.EXTRACTION'),
  MANUFACTURING: t('product.impact.lifecycle.MANUFACTURING'),
  TRANSPORTATION: t('product.impact.lifecycle.TRANSPORTATION'),
  USE: t('product.impact.lifecycle.USE'),
  END_OF_LIFE: t('product.impact.lifecycle.END_OF_LIFE'),
}))

const lifecycleColors: Record<string, string> = {
  EXTRACTION: 'warning',
  MANUFACTURING: 'secondary',
  TRANSPORTATION: 'info',
  USE: 'primary',
  END_OF_LIFE: 'success',
}
</script>

<style scoped>
.impact-subscore-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.impact-subscore-header__info {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.impact-subscore-header__title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore-header__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.impact-subscore-header__subtitle {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore-header__score {
  display: flex;
  align-items: flex-end;
  gap: 0.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-accent-supporting));
}

.impact-subscore-header__score-value {
  font-size: clamp(1.8rem, 3.5vw, 2.2rem);
  line-height: 1;
}

.impact-subscore-header__score-scale {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.75);
}

@media (max-width: 600px) {
  .impact-subscore-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .impact-subscore-header__score {
    margin-top: 0.5rem;
  }
}
</style>

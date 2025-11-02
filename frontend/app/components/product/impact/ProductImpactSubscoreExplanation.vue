<template>
  <section v-if="hasContent" class="impact-subscore-explanation">
    <h5 class="impact-subscore-explanation__title">{{ $t('product.impact.explanationTitle') }}</h5>
    <p v-if="score.description" class="impact-subscore-explanation__description">{{ score.description }}</p>

    <dl v-if="infoItems.length" class="impact-subscore-explanation__list">
      <div v-for="item in infoItems" :key="item.label" class="impact-subscore-explanation__row">
        <dt class="impact-subscore-explanation__term">{{ item.label }}</dt>
        <dd class="impact-subscore-explanation__value">{{ item.value }}</dd>
      </div>
    </dl>

    <dl v-if="metadataItems.length" class="impact-subscore-explanation__list impact-subscore-explanation__list--metadata">
      <div v-for="item in metadataItems" :key="item.label" class="impact-subscore-explanation__row">
        <dt class="impact-subscore-explanation__term">{{ item.label }}</dt>
        <dd class="impact-subscore-explanation__value">{{ item.value }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ScoreView } from './impact-types'

const props = defineProps<{
  score: ScoreView
  absoluteValue: string | null
}>()

const { n, t } = useI18n()

const infoItems = computed(() => {
  const items: Array<{ label: string; value: string }> = []

  if (props.absoluteValue) {
    items.push({
      label: t('product.impact.absoluteValue'),
      value: props.absoluteValue,
    })
  }

  if (props.score.percent != null && Number.isFinite(props.score.percent)) {
    items.push({
      label: t('product.impact.percentile'),
      value: `${n(props.score.percent, { maximumFractionDigits: 0, minimumFractionDigits: 0 })}%`,
    })
  }

  if (props.score.ranking != null && Number.isFinite(Number(props.score.ranking))) {
    items.push({
      label: t('product.impact.tableHeaders.ranking'),
      value: n(Number(props.score.ranking), { maximumFractionDigits: 0, minimumFractionDigits: 0 }),
    })
  }

  return items
})

const metadataItems = computed(() => {
  const entries = Object.entries(props.score.metadatas ?? {})
    .map(([key, value]) => ({ key, value }))
    .filter((entry) => entry.value != null && String(entry.value).trim().length > 0)

  return entries.map(({ key, value }) => ({
    label: formatMetadataLabel(key),
    value: String(value),
  }))
})

const hasContent = computed(
  () => Boolean(props.score.description) || infoItems.value.length > 0 || metadataItems.value.length > 0,
)

function formatMetadataLabel(rawKey: string): string {
  const humanized = rawKey
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim()

  return humanized
    .split(' ')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
</script>

<style scoped>
.impact-subscore-explanation {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
  border-radius: 18px;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.05);
}

.impact-subscore-explanation__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-subscore-explanation__description {
  margin: 0;
  font-size: 0.9rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore-explanation__list {
  margin: 0;
  display: grid;
  gap: 0.5rem;
}

.impact-subscore-explanation__list--metadata {
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.12);
  padding-top: 0.75rem;
}

.impact-subscore-explanation__row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.impact-subscore-explanation__term {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.impact-subscore-explanation__value {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

@media (max-width: 640px) {
  .impact-subscore-explanation__row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

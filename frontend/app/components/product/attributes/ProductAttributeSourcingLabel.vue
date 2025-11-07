<template>
  <span class="product-attribute-sourcing">
    <span class="product-attribute-sourcing__value">
      <slot :display-value="displayValue">
        <span class="product-attribute-sourcing__value-text">
          {{ displayValue }}
        </span>
      </slot>
    </span>

    <v-tooltip
      v-if="hasSourcingDetails"
      location="top"
      open-delay="150"
      :close-delay="100"
      :disabled="!hasSourcingDetails"
    >
      <template #activator="{ props: tooltipProps }">
        <v-btn
          v-bind="tooltipProps"
          class="product-attribute-sourcing__info"
          variant="text"
          density="compact"
          icon
          :aria-label="tooltipAriaLabel"
        >
          <v-icon :icon="'mdi-information'" size="18" :color="iconColor" />
        </v-btn>
      </template>

      <template #default>
        <v-card class="product-attribute-sourcing__tooltip" elevation="8">
          <header class="product-attribute-sourcing__tooltip-header">
            <div class="product-attribute-sourcing__tooltip-legend">
              <p class="product-attribute-sourcing__tooltip-title">
                {{ t('product.attributes.sourcing.bestValue') }}
              </p>
              <p class="product-attribute-sourcing__tooltip-highlight">{{ displayValue }}</p>
            </div>
            <v-chip
              size="small"
              :color="chipColor"
              variant="tonal"
              class="product-attribute-sourcing__tooltip-chip"
            >
              {{ statusLabel }}
            </v-chip>
          </header>

          <v-divider class="product-attribute-sourcing__tooltip-divider" />

          <div class="product-attribute-sourcing__tooltip-body">
            <p class="product-attribute-sourcing__tooltip-description">
              {{ t('product.attributes.sourcing.description') }}
            </p>

            <v-table
              v-if="normalizedSources.length"
              density="compact"
              class="product-attribute-sourcing__sources"
            >
              <thead>
                <tr>
                  <th scope="col">{{ t('product.attributes.sourcing.columns.source') }}</th>
                  <th scope="col">{{ t('product.attributes.sourcing.columns.value') }}</th>
                  <th scope="col">{{ t('product.attributes.sourcing.columns.language') }}</th>
                  <th scope="col">{{ t('product.attributes.sourcing.columns.taxonomy') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="source in normalizedSources" :key="sourceKey(source)">
                  <td>{{ source.datasourceName ?? '—' }}</td>
                  <td>{{ formatSourceValue(source.value) }}</td>
                  <td>{{ formatLanguage(source.language) }}</td>
                  <td>{{ formatTaxonomy(source.icecatTaxonomyId) }}</td>
                </tr>
              </tbody>
            </v-table>

            <p v-else class="product-attribute-sourcing__tooltip-empty">
              {{ t('product.attributes.sourcing.empty') }}
            </p>
          </div>
        </v-card>
      </template>
    </v-tooltip>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  ProductAttributeSourceDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'

const props = defineProps({
  sourcing: {
    type: Object as PropType<ProductAttributeSourceDto | null | undefined>,
    default: null,
  },
  value: {
    type: String,
    default: '',
  },
})

const { t } = useI18n()

const bestValue = computed(() => {
  const candidate = props.sourcing?.bestValue
  if (typeof candidate !== 'string') {
    return null
  }

  const trimmed = candidate.trim()
  return trimmed.length ? trimmed : null
})

const displayValue = computed(() => bestValue.value ?? props.value)

const hasSourcingDetails = computed(() => {
  if (!props.sourcing) {
    return false
  }

  if (bestValue.value) {
    return true
  }

  const sources = props.sourcing.sources
  if (sources instanceof Set) {
    return sources.size > 0
  }

  if (Array.isArray(sources)) {
    return sources.length > 0
  }

  return false
})

const normalizedSources = computed<ProductSourcedAttributeDto[]>(() => {
  const sources = props.sourcing?.sources
  if (!sources) {
    return []
  }

  if (sources instanceof Set) {
    return Array.from(sources)
  }

  if (Array.isArray(sources)) {
    return sources
  }

  return []
})

const conflicts = computed(() => Boolean(props.sourcing?.conflicts))

const iconColor = computed(() => (conflicts.value ? 'warning' : 'primary'))

const chipColor = computed(() => (conflicts.value ? 'warning' : 'primary'))

const statusLabel = computed(() =>
  t(
    conflicts.value
      ? 'product.attributes.sourcing.status.conflicts'
      : 'product.attributes.sourcing.status.noConflicts',
  ),
)

const tooltipAriaLabel = computed(() =>
  t('product.attributes.sourcing.tooltipAriaLabel'),
)

const formatLanguage = (language?: string | null) => {
  if (!language) {
    return '—'
  }

  return language.trim().toUpperCase()
}

const formatTaxonomy = (taxonomy?: number | null) => {
  if (typeof taxonomy === 'number' && Number.isFinite(taxonomy)) {
    return taxonomy.toString()
  }

  return '—'
}

const formatSourceValue = (value?: string | null) => {
  if (typeof value !== 'string') {
    return '—'
  }

  const trimmed = value.trim()
  return trimmed.length ? trimmed : '—'
}

const sourceKey = (source: ProductSourcedAttributeDto) => {
  return [
    source.datasourceName ?? '',
    source.value ?? '',
    source.language ?? '',
    source.icecatTaxonomyId ?? '',
    source.name ?? '',
  ]
    .map((segment) => String(segment ?? ''))
    .join('|')
}
</script>

<style scoped>
.product-attribute-sourcing {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.product-attribute-sourcing__value-text {
  color: inherit;
}

.product-attribute-sourcing__info {
  --product-attribute-icon-bg: rgba(var(--v-theme-surface-primary-120), 0.8);
  --product-attribute-icon-hover: rgba(var(--v-theme-surface-primary-100), 0.95);
  border-radius: 999px;
  padding: 0.15rem;
  min-width: auto;
}

.product-attribute-sourcing__info:hover,
.product-attribute-sourcing__info:focus-visible {
  background: var(--product-attribute-icon-hover);
}

.product-attribute-sourcing__tooltip {
  padding: 1rem 1.25rem;
  max-width: min(420px, 80vw);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-attribute-sourcing__tooltip-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.product-attribute-sourcing__tooltip-legend {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.product-attribute-sourcing__tooltip-title {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
}

.product-attribute-sourcing__tooltip-highlight {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attribute-sourcing__tooltip-chip {
  flex-shrink: 0;
}

.product-attribute-sourcing__tooltip-description {
  margin: 0;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-attribute-sourcing__sources {
  border-radius: 12px;
  overflow: hidden;
}

.product-attribute-sourcing__sources thead th {
  text-align: left;
  font-weight: 600;
  font-size: 0.8rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9);
  padding: 0.5rem 0.75rem;
}

.product-attribute-sourcing__sources tbody td {
  padding: 0.45rem 0.75rem;
  font-size: 0.85rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.product-attribute-sourcing__sources tbody tr:nth-child(odd) {
  background: rgba(var(--v-theme-surface-primary-050), 0.65);
}

.product-attribute-sourcing__tooltip-empty {
  margin: 0;
  font-style: italic;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

@media (max-width: 600px) {
  .product-attribute-sourcing__tooltip {
    max-width: min(360px, 92vw);
  }
}
</style>

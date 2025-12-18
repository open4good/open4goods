<template>
  <span class="product-attribute-sourcing">
    <span v-if="isTooltipEnabled" class="product-attribute-sourcing__icon">
      <v-tooltip
        location="top"
        open-delay="150"
        :close-delay="100"
        :disabled="!isTooltipEnabled"
        :scrim="false"
        :content-class="tooltipContentClass"
        :content-style="tooltipContentStyles"
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
                <p class="product-attribute-sourcing__tooltip-highlight">
                  {{ displayValue }}
                </p>
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
              <p class="product-attribute-sourcing__tooltip-count">
                {{ sourceCountLabel }}
              </p>

              <v-table
                v-if="hasSources"
                density="compact"
                class="product-attribute-sourcing__sources"
              >
                <thead>
                  <tr>
                    <th scope="col">
                      {{ t('product.attributes.sourcing.columns.source') }}
                    </th>
                    <th scope="col">
                      {{ t('product.attributes.sourcing.columns.value') }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="source in normalizedSources"
                    :key="sourceKey(source)"
                  >
                    <td>{{ formatSourceName(source.datasourceName) }}</td>
                    <td>{{ formatSourceValue(source.value) }}</td>
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

    <span class="product-attribute-sourcing__value">
      <slot :display-value="displayValue" :display-html="sanitizedDisplayHtml">
        <!-- eslint-disable vue/no-v-html -->
        <span
          class="product-attribute-sourcing__value-text"
          v-html="sanitizedDisplayHtml || displayValue"
        />
        <!-- eslint-enable vue/no-v-html -->
      </slot>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed, toRaw, unref } from 'vue'
import type { PropType, Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'
import type {
  ProductAttributeSourceDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'
import { _sanitizeHtml } from '~~/shared/utils/sanitizer'

const props = defineProps({
  sourcing: {
    type: Object as PropType<ProductAttributeSourceDto | null | undefined>,
    default: null,
  },
  value: {
    type: String,
    default: '',
  },
  enableTooltip: {
    type: Boolean,
    default: true,
  },
})

const { t, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()

const tooltipContentClass =
  'product-attribute-sourcing__tooltip-overlay' as const
const tooltipContentStyles = Object.freeze({
  backgroundColor: 'transparent',
  boxShadow: 'none',
  padding: 0,
}) satisfies Record<string, string | number>

const bestValue = computed(() => {
  const candidate = props.sourcing?.bestValue
  if (typeof candidate !== 'string') {
    return null
  }

  const trimmed = candidate.trim()
  return trimmed.length ? trimmed : null
})

const displayValue = computed(() => {
  const candidate = bestValue.value ?? props.value ?? ''
  return typeof candidate === 'string' ? candidate : String(candidate ?? '')
})

const sanitizedDisplayHtml = computed(() => {
  const raw = displayValue.value
  if (!raw.length) {
    return ''
  }

  const { sanitizedHtml } = _sanitizeHtml(raw)
  const sanitized = sanitizedHtml.value ?? ''

  return sanitized.length ? sanitized : raw
})

const isIterable = <T,>(candidate: unknown): candidate is Iterable<T> =>
  Boolean(
    candidate &&
    typeof (candidate as Iterable<T>)[Symbol.iterator] === 'function'
  )

const isDefinedSource = (
  item: ProductSourcedAttributeDto | null | undefined
): item is ProductSourcedAttributeDto => Boolean(item)

type MaybeRef<T> = T | Ref<T>

const normalizeSources = (
  rawSources: MaybeRef<ProductAttributeSourceDto['sources'] | null | undefined>
): ProductSourcedAttributeDto[] => {
  const resolvedSources = unref(rawSources)

  if (!resolvedSources) {
    return []
  }

  const candidate =
    typeof resolvedSources === 'object' && resolvedSources !== null
      ? (toRaw(resolvedSources) as unknown)
      : (resolvedSources as unknown)

  if (Array.isArray(candidate)) {
    return candidate.filter(isDefinedSource)
  }

  if (candidate instanceof Set) {
    return Array.from(candidate.values()).filter(isDefinedSource)
  }

  if (candidate instanceof Map) {
    return Array.from(candidate.values()).filter(isDefinedSource)
  }

  if (typeof candidate === 'string') {
    return []
  }

  if (isIterable<ProductSourcedAttributeDto>(candidate)) {
    return Array.from(candidate).filter(isDefinedSource)
  }

  if (typeof candidate === 'object' && candidate !== null) {
    return Object.values(
      candidate as Record<string, ProductSourcedAttributeDto | null | undefined>
    ).filter(isDefinedSource)
  }

  return []
}

const normalizedSources = computed<ProductSourcedAttributeDto[]>(() =>
  normalizeSources(props.sourcing?.sources ?? null)
)

const hasSources = computed(() => normalizedSources.value.length > 0)

const hasSourcingDetails = computed(
  () => Boolean(bestValue.value) || hasSources.value
)

const isTooltipEnabled = computed(
  () => props.enableTooltip && hasSourcingDetails.value
)

const sourceCountLabel = computed(() => {
  const count = normalizedSources.value.length
  return translatePlural('product.attributes.sourcing.sourceCount', count, {
    count: n(count),
  })
})

const conflicts = computed(() => Boolean(props.sourcing?.conflicts))

const iconColor = computed(() => (conflicts.value ? 'warning' : 'primary'))

const chipColor = computed(() => (conflicts.value ? 'warning' : 'primary'))

const statusLabel = computed(() =>
  t(
    conflicts.value
      ? 'product.attributes.sourcing.status.conflicts'
      : 'product.attributes.sourcing.status.noConflicts'
  )
)

const tooltipAriaLabel = computed(() =>
  t('product.attributes.sourcing.tooltipAriaLabel')
)

const formatSourceName = (name?: string | null) => {
  if (typeof name !== 'string') {
    return '—'
  }

  const trimmed = name.trim()
  return trimmed.length ? trimmed : '—'
}

const formatSourceValue = (value?: string | number | boolean | null) => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return n(value)
  }

  if (typeof value === 'boolean') {
    return t(value ? 'common.boolean.true' : 'common.boolean.false')
  }

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
    .map(segment => String(segment ?? ''))
    .join('|')
}
</script>

<style scoped>
.product-attribute-sourcing {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.product-attribute-sourcing__icon {
  display: inline-flex;
  order: -1;
}

.product-attribute-sourcing__value-text {
  color: inherit;
}

.product-attribute-sourcing__info {
  --product-attribute-icon-bg: rgba(var(--v-theme-surface-primary-120), 0.8);
  --product-attribute-icon-hover: rgba(
    var(--v-theme-surface-primary-100),
    0.95
  );
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

:deep(.product-attribute-sourcing__tooltip-overlay) {
  background-color: transparent !important;
  box-shadow: none !important;
  padding: 0 !important;
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

.product-attribute-sourcing__tooltip-count {
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

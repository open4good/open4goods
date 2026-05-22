<template>
  <v-card
    v-if="verdict && verdict.level !== 'insufficient'"
    class="product-price-verdict mb-6 rounded-xl overflow-hidden border"
    :class="`product-price-verdict--${verdict.level}`"
    variant="flat"
  >
    <div class="product-price-verdict__container d-flex align-center pa-4">
      <div
        class="product-price-verdict__icon-wrapper d-flex align-center justify-center mr-4 rounded-lg"
      >
        <v-icon :icon="verdictIcon" size="28" :color="verdict.color" />
      </div>
      <div class="product-price-verdict__content flex-grow-1">
        <p
          class="product-price-verdict__eyebrow text-uppercase text-caption font-weight-bold mb-0 text-grey-darken-1"
        >
          {{ t('product.verdict.priceVerdictTitle') }}
        </p>
        <h4
          class="product-price-verdict__title font-weight-bold text-h6 mb-1"
          :style="{ color: `var(--v-theme-${verdict.color})` }"
        >
          {{ t(verdict.labelKey) }}
        </h4>
        <p
          class="product-price-verdict__description text-body-2 mb-0 text-grey-darken-3"
        >
          {{ verdictDescription }}
        </p>
        <div
          v-if="verdict.currentPrice !== null && verdict.medianPrice !== null"
          class="product-price-verdict__details text-caption text-grey mt-2 d-flex align-center gap-3"
        >
          <span
            >{{ t('product.verdict.currentPriceLabel') }}:
            <strong>{{ formatCurrency(verdict.currentPrice) }}</strong></span
          >
          <span class="text-grey-lighten-1">|</span>
          <span
            >{{ t('product.verdict.medianPriceLabel') }}:
            <strong>{{ formatCurrency(verdict.medianPrice) }}</strong></span
          >
        </div>
      </div>
      <v-chip
        v-if="verdict.deviationPercent !== null"
        class="product-price-verdict__badge ml-4 font-weight-bold"
        :color="verdict.color"
        variant="tonal"
        size="small"
      >
        {{ verdict.deviationPercent > 0 ? '+' : ''
        }}{{ verdict.deviationPercent }}%
      </v-chip>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ProductDto } from '~~/shared/api-client'
import { getPriceVerdict } from '~/utils/_product-verdict'

const props = defineProps({
  product: {
    type: Object as PropType<ProductDto>,
    required: true,
  },
})

const { t, n } = useI18n()

const verdict = computed(() => getPriceVerdict(props.product))

const verdictIcon = computed(() => {
  if (!verdict.value) return 'mdi-tag-outline'
  switch (verdict.value.level) {
    case 'good':
      return 'mdi-trending-down'
    case 'poor':
      return 'mdi-trending-up'
    case 'fair':
      return 'mdi-trending-neutral'
    case 'insufficient':
    default:
      return 'mdi-tag-outline'
  }
})

const verdictDescription = computed(() => {
  if (!verdict.value) return ''
  const percent = verdict.value.deviationPercent
  const absPercent = percent !== null ? Math.abs(percent) : null

  switch (verdict.value.level) {
    case 'good':
      return t('product.verdict.priceDescriptions.good', {
        percent: absPercent,
      })
    case 'poor':
      return t('product.verdict.priceDescriptions.poor', {
        percent: absPercent,
      })
    case 'fair':
      return t('product.verdict.priceDescriptions.fair', {
        percent: absPercent,
      })
    case 'insufficient':
    default:
      return t('product.verdict.priceDescriptions.insufficient')
  }
})

const formatCurrency = (value: number) => {
  return n(value, 'currency', {
    currency: props.product.offers?.bestPrice?.currency || 'EUR',
    maximumFractionDigits: 2,
  })
}
</script>

<style scoped>
.product-price-verdict {
  transition: all 0.25s ease;
  background: rgba(var(--v-theme-surface-variant), 0.05);
}

.product-price-verdict:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
}

.product-price-verdict--good {
  border-color: rgba(var(--v-theme-success), 0.25) !important;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-success), 0.02) 0%,
    rgba(var(--v-theme-success), 0.05) 100%
  );
}
.product-price-verdict--good .product-price-verdict__icon-wrapper {
  background: rgba(var(--v-theme-success), 0.1);
}

.product-price-verdict--fair {
  border-color: rgba(var(--v-theme-warning), 0.25) !important;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-warning), 0.02) 0%,
    rgba(var(--v-theme-warning), 0.05) 100%
  );
}
.product-price-verdict--fair .product-price-verdict__icon-wrapper {
  background: rgba(var(--v-theme-warning), 0.1);
}

.product-price-verdict--poor {
  border-color: rgba(var(--v-theme-error), 0.25) !important;
  background: linear-gradient(
    135deg,
    rgba(var(--v-theme-error), 0.02) 0%,
    rgba(var(--v-theme-error), 0.05) 100%
  );
}
.product-price-verdict--poor .product-price-verdict__icon-wrapper {
  background: rgba(var(--v-theme-error), 0.1);
}

.product-price-verdict--insufficient {
  border-color: rgba(var(--v-theme-border), 0.15) !important;
  background: rgba(var(--v-theme-surface), 0.5);
}
.product-price-verdict--insufficient .product-price-verdict__icon-wrapper {
  background: rgba(var(--v-theme-surface-variant), 0.2);
}

.gap-3 {
  gap: 0.75rem;
}
</style>

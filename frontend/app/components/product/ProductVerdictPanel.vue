<template>
  <div
    v-if="hasVerdict"
    class="product-verdict-panel w-100 pa-4 rounded-lg my-4"
  >
    <div
      class="d-flex flex-column flex-sm-row justify-space-between align-center mb-3"
    >
      <div
        class="product-verdict-panel__header d-flex align-center mb-3 mb-sm-0"
      >
        <v-icon icon="mdi-scale-balance" class="mr-2 text-primary" size="22" />
        <h2 class="text-subtitle-1 font-weight-bold text-neutral-strong ma-0">
          {{ t('product.verdict.title') }}
        </h2>
      </div>
    </div>

    <div
      class="product-verdict-panel__chips d-flex flex-wrap gap-4 justify-center justify-sm-start"
    >
      <!-- Environmental Impact Chip -->
      <ProductVerdictDimensionChip
        v-if="impactVerdict"
        :icon="impactVerdict.icon"
        :color="impactVerdict.color"
        :title="t('product.verdict.dimensions.impact')"
        :value="impactValueLabel"
        :tooltip="impactTooltip"
        @click="$emit('navigate', 'impact')"
      />

      <!-- Price Position Chip -->
      <ProductVerdictDimensionChip
        v-if="priceVerdict"
        :icon="priceVerdict.icon"
        :color="priceVerdict.color"
        :title="t('product.verdict.dimensions.price')"
        :value="priceValueLabel"
        :tooltip="priceTooltip"
        @click="$emit('navigate', 'price')"
      />

      <!-- Data Reliability Chip -->
      <ProductVerdictDimensionChip
        v-if="reliabilityVerdict"
        :icon="reliabilityVerdict.icon"
        :color="reliabilityVerdict.color"
        :title="t('product.verdict.dimensions.reliability')"
        :value="reliabilityValueLabel"
        :tooltip="reliabilityTooltip"
        @click="$emit('navigate', 'reliability')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '#imports'
import type { ProductDto } from '~~/shared/api-client'
import { useProductVerdict } from '../../composables/useProductVerdict'
import ProductVerdictDimensionChip from './ProductVerdictDimensionChip.vue'

const props = defineProps<{
  product: ProductDto
}>()

defineEmits<{
  (e: 'navigate', section: 'impact' | 'price' | 'reliability'): void
}>()

const { t } = useI18n()
const { impactVerdict, priceVerdict, reliabilityVerdict, hasVerdict } =
  useProductVerdict(computed(() => props.product))

// Impact label & tooltip helper
const impactValueLabel = computed(() => {
  if (!impactVerdict.value || impactVerdict.value.score === null) {
    return t('product.verdict.levels.insufficient')
  }
  return t(impactVerdict.value.labelKey)
})

const impactTooltip = computed(() => {
  if (!impactVerdict.value || impactVerdict.value.score === null) {
    return t('product.verdict.levels.insufficient')
  }
  return `${impactVerdict.value.score}/${impactVerdict.value.scoreOutOf} - ${t(impactVerdict.value.labelKey)}`
})

// Price label & tooltip helper
const priceValueLabel = computed(() => {
  if (!priceVerdict.value || priceVerdict.value.deviationPercent === null) {
    return t('product.verdict.priceLevels.insufficient')
  }

  const dev = priceVerdict.value.deviationPercent
  const devStr = dev > 0 ? `+${dev}%` : `${dev}%`
  return `${devStr} ${t('product.verdict.vsMedian')}`
})

const priceTooltip = computed(() => {
  if (!priceVerdict.value || priceVerdict.value.currentPrice === null) {
    return t('product.verdict.priceLevels.insufficient')
  }

  const current = priceVerdict.value.currentPrice.toFixed(2)
  if (
    priceVerdict.value.deviationPercent === null ||
    priceVerdict.value.medianPrice === null
  ) {
    return `${current} €`
  }

  const median = priceVerdict.value.medianPrice.toFixed(2)
  const key = `product.verdict.priceLevels.${priceVerdict.value.level}`
  return `${t(key)} (${current} € vs ${median} € ${t('product.verdict.vsMedian')})`
})

// Reliability label & tooltip helper
const reliabilityValueLabel = computed(() => {
  if (!reliabilityVerdict.value || reliabilityVerdict.value.score === null) {
    return t('product.verdict.reliabilityLevels.insufficient')
  }
  return t(reliabilityVerdict.value.labelKey)
})

const reliabilityTooltip = computed(() => {
  if (!reliabilityVerdict.value || reliabilityVerdict.value.score === null) {
    return t('product.verdict.reliabilityLevels.insufficient')
  }
  return `${reliabilityVerdict.value.score}/${reliabilityVerdict.value.scoreOutOf} - ${t(reliabilityVerdict.value.labelKey)}`
})
</script>

<style scoped>
.product-verdict-panel {
  background: rgba(var(--v-theme-surface-glass-strong), 0.5);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.15);
  backdrop-filter: blur(8px);
}

.product-verdict-panel__chips {
  gap: 1rem;
}
</style>

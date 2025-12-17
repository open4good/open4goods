<template>
  <ProductImpactSubscoreGenericCard v-bind="props">
    <template #visual="{ absoluteValue }">
      <div class="impact-subscore-repairability__visual">
        <NuxtImg
          v-if="repairabilityImageSrc"
          :src="repairabilityImageSrc"
          alt=""
          class="impact-subscore-repairability__image"
          width="144"
          height="144"
          loading="lazy"
        />
        <span v-else class="impact-subscore-repairability__fallback">{{
          absoluteValue ?? '—'
        }}</span>
      </div>
    </template>
  </ProductImpactSubscoreGenericCard>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ProductImpactSubscoreGenericCard from '../ProductImpactSubscoreGenericCard.vue'
import type { ScoreView } from '../impact-types'

const props = defineProps<{
  score: ScoreView
  productName: string
  productBrand: string
  productModel: string
  productImage: string
  verticalTitle: string
}>()

const MIN_ASSET_VALUE = 0.1
const MAX_ASSET_VALUE = 9.9

const numericAbsoluteValue = computed<number | null>(() => {
  const { score } = props

  const candidates = [
    score.absolute?.value,
    score.value,
    typeof score.absoluteValue === 'number'
      ? score.absoluteValue
      : parseLocalizedNumber(score.absoluteValue),
  ]

  for (const candidate of candidates) {
    if (typeof candidate === 'number' && Number.isFinite(candidate)) {
      return candidate
    }
  }

  return null
})

const repairabilityImageKey = computed<string | null>(() => {
  if (numericAbsoluteValue.value == null) {
    return null
  }

  const clamped = Math.min(
    Math.max(numericAbsoluteValue.value, MIN_ASSET_VALUE),
    MAX_ASSET_VALUE
  )
  const rounded = Math.round(clamped * 10) / 10
  return rounded.toFixed(1)
})

const repairabilityImageSrc = computed<string | null>(() => {
  if (!repairabilityImageKey.value) {
    return null
  }

  return `/images/reparability/${repairabilityImageKey.value}.svg`
})

function parseLocalizedNumber(
  value: string | number | null | undefined
): number | null {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }

  if (typeof value !== 'string') {
    return null
  }

  const normalized = value.replace(/,/g, '.').trim()
  if (!normalized.length) {
    return null
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}
</script>

<style scoped>
.impact-subscore-repairability__visual {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 144px;
}

.impact-subscore-repairability__image {
  max-width: 100%;
  height: auto;
}

.impact-subscore-repairability__fallback {
  font-size: clamp(2.25rem, 2.2rem + 0.5vw, 2.75rem);
  font-weight: 700;
  line-height: 1.1;
  color: rgb(var(--v-theme-text-neutral-strong));
}
</style>

<template>
  <component
    :is="resolvedComponent"
    :score="score"
    :product-name="productName"
    :product-brand="productBrand"
    :product-model="productModel"
    :product-image="productImage"
    :vertical-title="verticalTitle"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Component } from 'vue'
import type { ScoreView } from './impact-types'
import ProductImpactSubscoreGenericCard from './ProductImpactSubscoreGenericCard.vue'
import ProductImpactSubscorePowerConsumptionTypicalCard from './subscores/ProductImpactSubscorePowerConsumptionTypicalCard.vue'
import ProductImpactSubscoreRepairabilityIndexCard from './subscores/ProductImpactSubscoreRepairabilityIndexCard.vue'
import ProductImpactSubscorePowerConsumptionOffCard from './subscores/ProductImpactSubscorePowerConsumptionOffCard.vue'
import ProductImpactSubscoreClasseEnergyCard from './subscores/ProductImpactSubscoreClasseEnergyCard.vue'
import ProductImpactSubscoreBrandSustainabilityCard from './subscores/ProductImpactSubscoreBrandSustainabilityCard.vue'
import ProductImpactSubscoreDataQualityCard from './subscores/ProductImpactSubscoreDataQualityCard.vue'

const props = defineProps<{
  score: ScoreView
  productName: string
  productBrand: string
  productModel: string
  productImage: string
  verticalTitle: string
}>()

const specializationMap: Record<string, Component> = {
  POWER_CONSUMPTION_TYPICAL: ProductImpactSubscorePowerConsumptionTypicalCard,
  POWER_CONSUMPTION_OFF: ProductImpactSubscorePowerConsumptionOffCard,
  REPAIRABILITY_INDEX: ProductImpactSubscoreRepairabilityIndexCard,
  CLASSE_ENERGY: ProductImpactSubscoreClasseEnergyCard,
  BRAND_SUSTAINABILITY: ProductImpactSubscoreBrandSustainabilityCard,
  DATA_QUALITY: ProductImpactSubscoreDataQualityCard,
}

const resolvedComponent = computed(() => {
  const normalizedId = (props.score.id ?? '').toUpperCase()
  return specializationMap[normalizedId] ?? ProductImpactSubscoreGenericCard
})
</script>

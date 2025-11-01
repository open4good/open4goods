<template>
  <ProductImpactSubscoreGenericCard v-bind="props" />
</template>

<script setup lang="ts">
import ProductImpactSubscoreGenericCard from '../ProductImpactSubscoreGenericCard.vue'
import type { ScoreView } from '../impact-types'

const props = defineProps<{
  score: ScoreView
  productName: string
}>()
</script>

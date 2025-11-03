<template>
  <section :id="sectionId" class="product-impact">
    <header class="product-impact__header">
      <h2 class="product-impact__title">
        {{ $t('product.impact.title') }}
      </h2>
      <p class="product-impact__subtitle">
        {{ $t('product.impact.subtitle') }}
      </p>
    </header>

    <div class="product-impact__primary">
      <ProductImpactEcoScoreCard :score="primaryScore" />
    </div>

    <div class="product-impact__analysis">
      <ProductImpactRadarChart
        v-if="showRadar"
        class="product-impact__analysis-radar"
        :values="filteredRadarValues"
        :product-name="productName"
      />
      <ProductImpactDetailsTable
        class="product-impact__analysis-details"
        :class="{ 'product-impact__analysis-details--full': !showRadar }"
        :scores="detailScores"
      />
    </div>

    <div class="product-impact__subscores">
      <v-skeleton-loader
        v-if="loading"
        type="image, article"
        class="product-impact__skeleton"
      />
      <template v-else>
        <ProductImpactSubscoreCard
          v-for="score in secondaryScores"
          :key="score.id"
          :score="score"
          :product-name="productName"
        />
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, toRef } from 'vue'
import type { PropType } from 'vue'
import ProductImpactEcoScoreCard from './impact/ProductImpactEcoScoreCard.vue'
import ProductImpactRadarChart from './impact/ProductImpactRadarChart.vue'
import ProductImpactDetailsTable from './impact/ProductImpactDetailsTable.vue'
import ProductImpactSubscoreCard from './impact/ProductImpactSubscoreCard.vue'
import type { ScoreView } from './impact/impact-types'

const props = defineProps({
  sectionId: {
    type: String,
    default: 'impact',
  },
  scores: {
    type: Array as PropType<ScoreView[]>,
    default: () => [],
  },
  radarValues: {
    type: Array as PropType<Array<{ id: string; name: string; value: number }>>,
    default: () => [],
  },
  productName: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const radarValues = toRef(props, 'radarValues')
const productName = toRef(props, 'productName')

const primaryScore = computed(() => props.scores[0] ?? null)
const secondaryScores = computed(() => props.scores.slice(1))
const detailScores = computed(() => props.scores.filter((score) => score.id !== 'ECOSCORE'))
const filteredRadarValues = computed(() =>
  radarValues.value
    .filter((entry) => entry.id && entry.name && entry.name.trim().length && Number.isFinite(entry.value))
    .filter((entry) => entry.id.toUpperCase() !== 'ECOSCORE')
    .map((entry) => ({ name: entry.name, value: entry.value })),
)
const showRadar = computed(() => filteredRadarValues.value.length >= 3)
</script>

<style scoped>
.product-impact {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.product-impact__header {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.product-impact__title {
  font-size: clamp(1.6rem, 3vw, 2.4rem);
  font-weight: 700;
}

.product-impact__subtitle {
  font-size: 1rem;
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85);
}

.product-impact__primary {
  display: flex;
  flex-direction: column;
}

.product-impact__analysis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  align-items: stretch;
}

.product-impact__analysis-details--full {
  grid-column: 1 / -1;
}

.product-impact__subscores {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1.5rem;
}

.product-impact__skeleton {
  grid-column: 1 / -1;
  min-height: 320px;
}

@media (max-width: 1280px) {
  .product-impact__analysis {
    grid-template-columns: 1fr;
  }
}

@media (min-width: 640px) {
  .product-impact__subscores {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .product-impact__subscores {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>

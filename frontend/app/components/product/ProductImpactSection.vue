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

    <div class="product-impact__top">
      <ProductImpactEcoScoreCard :score="primaryScore" />
      <ProductImpactRankingCard
        v-if="ranking || country"
        :ranking="ranking"
        :country="country"
      />
    </div>

    <div class="product-impact__analysis">
      <ProductImpactRadarChart
        class="product-impact__analysis-radar"
        :values="radarValues"
        :product-name="productName"
      />
      <ProductImpactDetailsTable
        class="product-impact__analysis-details"
        :scores="scores"
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
import { computed, toRefs } from 'vue'
import type { PropType } from 'vue'
import ProductImpactEcoScoreCard from './impact/ProductImpactEcoScoreCard.vue'
import ProductImpactRankingCard from './impact/ProductImpactRankingCard.vue'
import ProductImpactRadarChart from './impact/ProductImpactRadarChart.vue'
import ProductImpactDetailsTable from './impact/ProductImpactDetailsTable.vue'
import ProductImpactSubscoreCard from './impact/ProductImpactSubscoreCard.vue'
import type { CountryInfo, RankingInfo, ScoreView } from './impact/impact-types'

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
    type: Array as PropType<Array<{ name: string; value: number }>>,
    default: () => [],
  },
  ranking: {
    type: Object as PropType<RankingInfo | null>,
    default: null,
  },
  country: {
    type: Object as PropType<CountryInfo | null>,
    default: null,
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

const { radarValues, productName } = toRefs(props)

const primaryScore = computed(() => props.scores[0] ?? null)
const secondaryScores = computed(() => props.scores.slice(1))
const scores = computed(() => props.scores)
const ranking = computed(() => props.ranking)
const country = computed(() => props.country)
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

.product-impact__top {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1.1fr);
  gap: 1.5rem;
}

.product-impact__analysis {
  display: grid;
  grid-template-columns: minmax(0, 2.2fr) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: stretch;
}

.product-impact__analysis-radar {
  grid-column: 1 / span 1;
}

.product-impact__analysis-details {
  grid-column: 2 / span 1;
}

.product-impact__subscores {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 1.5rem;
}

.product-impact__skeleton {
  grid-column: 1 / -1;
  min-height: 320px;
}

@media (max-width: 1280px) {
  .product-impact__top,
  .product-impact__analysis {
    grid-template-columns: 1fr;
  }

  .product-impact__analysis-radar,
  .product-impact__analysis-details {
    grid-column: auto;
  }
}

@media (max-width: 960px) {
  .product-impact__subscores {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  }
}
</style>

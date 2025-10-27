<template>
  <article class="impact-ranking">
    <header class="impact-ranking__header">
      <h3 class="impact-ranking__title">{{ $t('product.impact.rankingTitle') }}</h3>
      <p v-if="ranking" class="impact-ranking__value">
        {{ $t('product.impact.rankingValue', { position: ranking.position, total: ranking.total }) }}
      </p>
    </header>

    <div v-if="ranking?.globalBest" class="impact-ranking__link">
      <NuxtLink :to="ranking.globalBest.fullSlug">
        {{ $t('product.impact.bestProduct', { name: ranking.globalBest.bestName }) }}
      </NuxtLink>
    </div>

    <div v-if="ranking?.globalBetter" class="impact-ranking__link">
      <NuxtLink :to="ranking.globalBetter.fullSlug">
        {{ $t('product.impact.betterProduct', { name: ranking.globalBetter.bestName }) }}
      </NuxtLink>
    </div>

    <div v-if="country" class="impact-ranking__country">
      <NuxtImg
        v-if="country.flag"
        :src="country.flag"
        :alt="country.name"
        width="36"
        height="24"
        class="impact-ranking__country-flag"
      />
      <span>{{ country.name }}</span>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { CountryInfo, RankingInfo } from './impact-types'

defineProps<{
  ranking: RankingInfo | null
  country: CountryInfo | null
}>()
</script>

<style scoped>
.impact-ranking {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.75rem;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-glass), 0.92), rgba(var(--v-theme-surface-primary-080), 0.85));
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.08);
  min-height: 100%;
}

.impact-ranking__title {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 600;
}

.impact-ranking__value {
  margin: 0.35rem 0 0;
  font-size: 1.05rem;
  font-weight: 600;
}

.impact-ranking__link a {
  color: rgb(var(--v-theme-accent-primary-highlight));
  text-decoration: none;
  font-weight: 500;
}

.impact-ranking__link a:hover,
.impact-ranking__link a:focus {
  text-decoration: underline;
}

.impact-ranking__country {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  font-weight: 600;
}

.impact-ranking__country-flag {
  border-radius: 6px;
  width: 36px;
  height: 24px;
  object-fit: cover;
}
</style>

<template>
  <section class="impact-score-details">
    <header class="impact-score-details__header">
      <v-chip
        class="impact-score-details__eyebrow"
        color="accent-primary-highlight"
        variant="tonal"
      >
        {{ t('impactScorePage.details.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-details__title">
        {{ t('impactScorePage.details.title') }}
      </h2>
      <p class="impact-score-details__subtitle">
        {{ t('impactScorePage.details.subtitle') }}
      </p>
    </header>

    <v-row class="impact-score-details__grid" :gutter="16">
      <v-col v-for="card in detailCards" :key="card.key" cols="12" md="6">
        <v-card
          class="impact-score-details__card"
          elevation="0"
          rounded="xl"
          border
        >
          <div class="impact-score-details__card-header">
            <v-icon :icon="card.icon" size="28" color="accent-supporting" />
            <h3 class="impact-score-details__card-title">{{ card.title }}</h3>
          </div>
          <p class="impact-score-details__card-description">
            {{ card.description }}
          </p>
          <ul
            v-if="card.bullets.length"
            class="impact-score-details__card-list"
          >
            <li
              v-for="item in card.bullets"
              :key="item"
              class="impact-score-details__card-list-item"
            >
              <v-icon
                icon="mdi-check-circle"
                size="20"
                color="accent-supporting"
              />
              <span>{{ item }}</span>
            </li>
          </ul>
        </v-card>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const detailCards = computed(() => [
  {
    key: 'weighting',
    icon: 'mdi-scale-balance',
    title: t('impactScorePage.details.cards.weighting.title'),
    description: t('impactScorePage.details.cards.weighting.description'),
    bullets: [
      t('impactScorePage.details.cards.weighting.bullets.scale'),
      t('impactScorePage.details.cards.weighting.bullets.sum'),
      t('impactScorePage.details.cards.weighting.bullets.weights'),
    ],
  },
  {
    key: 'ai',
    icon: 'mdi-brain',
    title: t('impactScorePage.details.cards.ai.title'),
    description: t('impactScorePage.details.cards.ai.description'),
    bullets: [
      t('impactScorePage.details.cards.ai.bullets.verticals'),
      t('impactScorePage.details.cards.ai.bullets.opensource'),
      t('impactScorePage.details.cards.ai.bullets.discussion'),
    ],
  },
  {
    key: 'relativisation',
    icon: 'mdi-chart-bell-curve',
    title: t('impactScorePage.details.cards.relativisation.title'),
    description: t('impactScorePage.details.cards.relativisation.description'),
    bullets: [
      t('impactScorePage.details.cards.relativisation.bullets.ranking'),
    ],
  },
  {
    key: 'data-quality',
    icon: 'mdi-database-check-outline',
    title: t('impactScorePage.details.cards.dataQuality.title'),
    description: t('impactScorePage.details.cards.dataQuality.description'),
    bullets: [
      t('impactScorePage.details.cards.dataQuality.bullets.average'),
      t('impactScorePage.details.cards.dataQuality.bullets.malus'),
    ],
  },
])
</script>

<style scoped>
.impact-score-details {
  display: grid;
  gap: 1.5rem;
}

.impact-score-details__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-details__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-details__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-details__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-details__grid {
  margin-top: 0.5rem;
}

.impact-score-details__card {
  height: 100%;
  padding: 1.5rem;
  background: rgb(var(--v-theme-surface-glass));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.24);
  display: grid;
  gap: 0.9rem;
}

.impact-score-details__card-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: center;
}

.impact-score-details__card-title {
  margin: 0;
  font-size: 1.1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-details__card-description {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.6;
}

.impact-score-details__card-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.6rem;
}

.impact-score-details__card-list-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: start;
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
}
</style>

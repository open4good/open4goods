<template>
  <section class="impact-score-available-criteria">
    <header class="impact-score-available-criteria__header">
      <v-chip
        class="impact-score-available-criteria__eyebrow"
        color="accent-primary-highlight"
        variant="tonal"
      >
        {{ t('impactScorePage.availableCriteria.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-available-criteria__title">
        {{ t('impactScorePage.availableCriteria.title') }}
      </h2>
      <p class="impact-score-available-criteria__subtitle">
        {{ t('impactScorePage.availableCriteria.subtitle') }}
      </p>
    </header>

    <ImpactScoreCriteriaPanel />
  </section>
</template>

<script setup lang="ts">
import ImpactScoreCriteriaPanel from '~/components/impact-score/ImpactScoreCriteriaPanel.vue'

const { t } = useI18n()
</script>

<style scoped>
.impact-score-available-criteria {
  display: grid;
  gap: 1.5rem;
}

.impact-score-available-criteria__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-available-criteria__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-available-criteria__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-available-criteria__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}
</style>

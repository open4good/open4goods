<template>
  <section class="impact-score-summary">
    <header class="impact-score-summary__header">
      <v-chip class="impact-score-summary__eyebrow" color="accent-primary-highlight" variant="tonal">
        {{ t('impactScorePage.summary.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-summary__title">{{ t('impactScorePage.summary.title') }}</h2>
      <p class="impact-score-summary__subtitle">{{ t('impactScorePage.summary.subtitle') }}</p>
    </header>

    <v-row :gutter="16">
      <v-col
        v-for="item in summaryItems"
        :key="item"
        cols="12"
        md="6"
      >
        <v-sheet class="impact-score-summary__card" rounded="xl" elevation="0" border>
          <v-icon icon="mdi-check-circle" size="22" color="accent-supporting" />
          <p class="impact-score-summary__card-text">{{ item }}</p>
        </v-sheet>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const summaryItems = computed(() => [
  t('impactScorePage.summary.items.method'),
  t('impactScorePage.summary.items.criteria'),
  t('impactScorePage.summary.items.data'),
  t('impactScorePage.summary.items.usage'),
])
</script>

<style scoped>
.impact-score-summary {
  display: grid;
  gap: 1.5rem;
}

.impact-score-summary__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-summary__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-summary__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-summary__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-summary__card {
  height: 100%;
  padding: 1.1rem 1.3rem;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.8rem;
  align-items: center;
  background: rgba(var(--v-theme-surface-primary-080), 0.65);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.22);
}

.impact-score-summary__card-text {
  margin: 0;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
  line-height: 1.55;
}
</style>

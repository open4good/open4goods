<template>
  <section class="impact-score-approach">
    <header class="impact-score-approach__header">
      <v-chip class="impact-score-approach__eyebrow" color="accent-primary-highlight" variant="tonal">
        {{ t('impactScorePage.approach.eyebrow') }}
      </v-chip>
      <h2 class="impact-score-approach__title">{{ t('impactScorePage.approach.title') }}</h2>
      <p class="impact-score-approach__subtitle">{{ t('impactScorePage.approach.subtitle') }}</p>
    </header>

    <v-row :gutter="16" align="stretch">
      <v-col cols="12" md="7">
        <div class="impact-score-approach__pillars">
          <v-sheet
            v-for="pillar in pillars"
            :key="pillar.key"
            class="impact-score-approach__pillar"
            rounded="xl"
            elevation="0"
            border
          >
            <div class="impact-score-approach__pillar-header">
              <v-icon :icon="pillar.icon" size="26" color="accent-primary-highlight" />
              <h3 class="impact-score-approach__pillar-title">{{ pillar.title }}</h3>
            </div>
            <p class="impact-score-approach__pillar-text">{{ pillar.description }}</p>
          </v-sheet>
        </div>
      </v-col>
      <v-col cols="12" md="5">
        <v-sheet class="impact-score-approach__data" rounded="xl" elevation="0" border>
          <div class="impact-score-approach__data-header">
            <v-icon icon="mdi-database-check" size="28" color="accent-supporting" />
            <h3 class="impact-score-approach__data-title">{{ t('impactScorePage.approach.data.title') }}</h3>
          </div>
          <ul class="impact-score-approach__data-list">
            <li
              v-for="item in dataItems"
              :key="item"
              class="impact-score-approach__data-item"
            >
              <v-icon icon="mdi-check-circle" size="20" color="accent-supporting" />
              <span>{{ item }}</span>
            </li>
          </ul>
        </v-sheet>
      </v-col>
    </v-row>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const pillars = computed(() => [
  {
    key: 'transparent',
    icon: 'mdi-eye-outline',
    title: t('impactScorePage.approach.pillars.transparent.title'),
    description: t('impactScorePage.approach.pillars.transparent.description'),
  },
  {
    key: 'innovant',
    icon: 'mdi-lightbulb-on-outline',
    title: t('impactScorePage.approach.pillars.innovant.title'),
    description: t('impactScorePage.approach.pillars.innovant.description'),
  },
  {
    key: 'performant',
    icon: 'mdi-rocket-launch-outline',
    title: t('impactScorePage.approach.pillars.performant.title'),
    description: t('impactScorePage.approach.pillars.performant.description'),
  },
])

const dataItems = computed(() => [
  t('impactScorePage.approach.data.items.regulatory'),
  t('impactScorePage.approach.data.items.esg'),
  t('impactScorePage.approach.data.items.information'),
  t('impactScorePage.approach.data.items.categorySpecific'),
])
</script>

<style scoped>
.impact-score-approach {
  display: grid;
  gap: 1.5rem;
}

.impact-score-approach__header {
  display: grid;
  gap: 0.75rem;
}

.impact-score-approach__eyebrow {
  width: fit-content;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 700;
}

.impact-score-approach__title {
  margin: 0;
  font-size: clamp(1.6rem, 1.4vw + 1rem, 2rem);
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-approach__subtitle {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.6;
}

.impact-score-approach__pillars {
  display: grid;
  gap: 1rem;
}

.impact-score-approach__pillar {
  padding: 1.3rem;
  background: rgb(var(--v-theme-surface-glass));
  border-color: rgba(var(--v-theme-border-primary-strong), 0.25);
  display: grid;
  gap: 0.6rem;
}

.impact-score-approach__pillar-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: center;
}

.impact-score-approach__pillar-title {
  margin: 0;
  font-size: 1.1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-approach__pillar-text {
  margin: 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
  line-height: 1.6;
}

.impact-score-approach__data {
  height: 100%;
  padding: 1.4rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.55);
  border-color: rgba(var(--v-theme-border-primary-strong), 0.22);
  display: grid;
  gap: 1rem;
}

.impact-score-approach__data-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: center;
}

.impact-score-approach__data-title {
  margin: 0;
  font-size: 1.1rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-approach__data-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.impact-score-approach__data-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: start;
  color: rgb(var(--v-theme-text-neutral-strong));
  font-weight: 600;
  line-height: 1.6;
}
</style>

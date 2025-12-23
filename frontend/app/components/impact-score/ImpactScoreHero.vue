<template>
  <section class="impact-score-hero">
    <v-container class="py-10 py-md-14">
      <v-row class="align-stretch" :gutter="24">
        <v-col cols="12" md="7">
          <div class="impact-score-hero__intro">
            <v-chip
              class="impact-score-hero__eyebrow"
              size="large"
              color="accent-primary-highlight"
              variant="tonal"
            >
              {{ t('impactScorePage.hero.eyebrow') }}
            </v-chip>
            <h1 class="impact-score-hero__title">
              {{ t('impactScorePage.hero.title') }}
            </h1>
            <p class="impact-score-hero__subtitle">
              {{ t('impactScorePage.hero.subtitle') }}
            </p>
          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-sheet
            class="impact-score-hero__definition"
            color="surface-glass"
            elevation="0"
            rounded="xl"
            border="true"
          >
            <div class="impact-score-hero__definition-header">
              <v-icon icon="mdi-information-outline" size="28" />
              <div>
                <h2 class="impact-score-hero__definition-title">
                  {{ t('impactScorePage.hero.definition.title') }}
                </h2>
                <p class="impact-score-hero__definition-text">
                  {{ t('impactScorePage.hero.definition.description') }}
                </p>
              </div>
            </div>

            <ul class="impact-score-hero__definition-list">
              <li
                v-for="item in definitionItems"
                :key="item"
                class="impact-score-hero__definition-item"
              >
                <v-icon icon="mdi-check-circle" size="22" color="accent-supporting" />
                <span>{{ item }}</span>
              </li>
            </ul>
          </v-sheet>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const definitionItems = computed(() => [
  t('impactScorePage.hero.definition.items.scale'),
  t('impactScorePage.hero.definition.items.data'),
  t('impactScorePage.hero.definition.items.update'),
])
</script>

<style scoped>
.impact-score-hero {
  background: linear-gradient(
      120deg,
      rgba(var(--v-theme-hero-gradient-start), 0.18),
      rgba(var(--v-theme-hero-gradient-mid), 0.08)
    ),
    rgb(var(--v-theme-surface-default));
}

.impact-score-hero__intro {
  display: grid;
  gap: 1rem;
  height: 100%;
  align-content: center;
}

.impact-score-hero__eyebrow {
  width: fit-content;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.impact-score-hero__title {
  font-size: clamp(2.1rem, 2vw + 1rem, 2.8rem);
  line-height: 1.15;
  color: rgb(var(--v-theme-text-neutral-strong));
  margin: 0;
}

.impact-score-hero__subtitle {
  font-size: 1.05rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  margin: 0;
}

.impact-score-hero__definition {
  padding: 1.6rem;
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  box-shadow: 0 25px 70px rgba(15, 23, 42, 0.08);
}

.impact-score-hero__definition-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem 1rem;
  align-items: start;
}

.impact-score-hero__definition-title {
  margin: 0;
  font-size: 1.25rem;
  color: rgb(var(--v-theme-text-neutral-strong));
}

.impact-score-hero__definition-text {
  margin: 0.25rem 0 0;
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.impact-score-hero__definition-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.75rem;
}

.impact-score-hero__definition-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.6rem;
  align-items: center;
  font-weight: 600;
  color: rgb(var(--v-theme-text-neutral-strong));
}

@media (max-width: 960px) {
  .impact-score-hero__definition {
    margin-top: 1rem;
  }
}
</style>

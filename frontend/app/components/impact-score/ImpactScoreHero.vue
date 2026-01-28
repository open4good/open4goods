<template>
  <section class="impact-score-hero">
    <div
      v-if="backgroundAsset"
      class="impact-score-hero__background"
      aria-hidden="true"
    >
      <img
        :src="backgroundAsset"
        alt=""
        class="impact-score-hero__background-image"
      />
      <div class="impact-score-hero__background-overlay" />
    </div>
    <v-container class="impact-score-hero__content py-10 py-md-14">
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
            <p class="impact-score-hero__subtitle">
              {{ t('impactScorePage.hero.subtitle') }}
            </p>
            <h1 class="impact-score-hero__title">
              {{ t('impactScorePage.hero.title') }}
            </h1>
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
                <v-icon
                  icon="mdi-check-circle"
                  size="22"
                  color="accent-supporting"
                />
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
import { useThemedAsset } from '~/composables/useThemedAsset'

const { t } = useI18n()

const backgroundAsset = useThemedAsset('backgrounds/impact-score.svg')

const definitionItems = computed(() => [
  t('impactScorePage.hero.definition.items.scale'),
  t('impactScorePage.hero.definition.items.data'),
  t('impactScorePage.hero.definition.items.update'),
])
</script>

<style scoped>
.impact-score-hero {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(
      120deg,
      rgba(var(--v-theme-hero-gradient-start), 0.18),
      rgba(var(--v-theme-hero-gradient-mid), 0.08)
    ),
    rgb(var(--v-theme-surface-default));
}

.impact-score-hero__background {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.impact-score-hero__background-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.95;
}

.impact-score-hero__background-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(
      circle at 18% 30%,
      rgba(var(--v-theme-hero-overlay-strong), 0.2),
      transparent 40%
    ),
    linear-gradient(
      180deg,
      rgba(var(--v-theme-surface-default), 0.12) 0%,
      rgba(var(--v-theme-surface-default), 0.6) 100%
    );
}

.impact-score-hero__content {
  position: relative;
  z-index: 1;
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

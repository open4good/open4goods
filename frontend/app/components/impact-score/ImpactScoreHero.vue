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
          {{ t('impactScorePage.hero.intro') }}
        </p>
      </div>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { useThemedAsset } from '~/composables/useThemedAsset'

const { t } = useI18n()

const backgroundAsset = useThemedAsset('backgrounds/impact-score.svg')
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
</style>

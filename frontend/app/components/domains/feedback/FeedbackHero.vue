<template>
  <section class="feedback-hero" aria-labelledby="feedback-hero-heading">
    <div class="feedback-hero__background" aria-hidden="true" />

    <v-container class="py-16">
      <v-row class="align-center">
        <v-col cols="12" md="7">
          <div class="feedback-hero__content">
            <p class="feedback-hero__eyebrow">{{ eyebrow }}</p>
            <h1 id="feedback-hero-heading" class="feedback-hero__title">
              {{ title }}
            </h1>
            <p class="feedback-hero__subtitle">
              {{ subtitle }}
            </p>
            <p class="feedback-hero__description">
              {{ description }}
            </p>

            <div class="feedback-hero__actions" role="group" :aria-label="ctaGroupLabel">
              <v-btn
                v-if="primaryCta"
                color="primary"
                size="large"
                class="feedback-hero__cta"
                :href="primaryCta.href"
                :to="primaryCta.to"
                :aria-label="primaryCta.ariaLabel"
                :target="primaryCta.target"
                :rel="primaryCta.rel"
                :append-icon="primaryCta.icon"
                @click="primaryCta.onClick?.($event)"
              >
                {{ primaryCta.label }}
              </v-btn>

              <v-btn
                v-if="secondaryCta"
                variant="outlined"
                size="large"
                class="feedback-hero__cta"
                :href="secondaryCta.href"
                :to="secondaryCta.to"
                :aria-label="secondaryCta.ariaLabel"
                :target="secondaryCta.target"
                :rel="secondaryCta.rel"
                :append-icon="secondaryCta.icon"
                @click="secondaryCta.onClick?.($event)"
              >
                {{ secondaryCta.label }}
              </v-btn>
            </div>


          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-card class="feedback-hero__card" elevation="8" rounded="xl" role="presentation">
            <div class="feedback-hero__card-header">
              <v-icon icon="mdi-vote" size="36" color="primary" />
              <span class="feedback-hero__card-eyebrow">{{ stats.eyebrow }}</span>
            </div>
            <p class="feedback-hero__card-title">{{ stats.title }}</p>
            <p class="feedback-hero__card-description">{{ stats.description }}</p>
            <v-divider class="my-4" />
            <ul class="feedback-hero__card-list" role="list">
              <li v-for="item in stats.items" :key="item.label" class="feedback-hero__card-item">
                <v-icon :icon="item.icon" size="22" color="primary" class="me-2" />
                <span class="feedback-hero__card-item-label">{{ item.label }}</span>
              </li>
            </ul>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">

type HeroLink = {
  label: string
  ariaLabel: string
  href?: string
  to?: string
  target?: string
  rel?: string
  icon?: string
  onClick?: (event: MouseEvent) => void
}

type HeroHighlight = {
  icon: string
  title: string
  description?: string
}

type HeroStatItem = {
  icon: string
  label: string
}

type HeroStats = {
  eyebrow: string
  title: string
  description: string
  items: HeroStatItem[]
}

defineProps<{
  eyebrow: string
  title: string
  subtitle: string
  description: string
  primaryCta?: HeroLink
  secondaryCta?: HeroLink
  ctaGroupLabel: string
  highlights: HeroHighlight[]
  stats: HeroStats
}>()
</script>

<style scoped lang="scss">
.feedback-hero {
  position: relative;
  overflow: hidden;
  background: linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.88),
      rgba(var(--v-theme-hero-gradient-mid), 0.82)
    ),
    radial-gradient(circle at top right, rgba(var(--v-theme-hero-gradient-end), 0.6), transparent 45%);
  color: rgb(var(--v-theme-hero-pill-on-dark));

  &__background {
    position: absolute;
    inset: 0;
    opacity: 0.25;
    background-image: url('data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400" fill="none"%3E%3Ccircle cx="200" cy="200" r="180" stroke="white" stroke-width="1" stroke-opacity="0.2"/%3E%3Ccircle cx="200" cy="200" r="140" stroke="white" stroke-width="1" stroke-opacity="0.12" stroke-dasharray="6 12"/%3E%3C/svg%3E');
    background-size: cover;
  }

  &__content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  &__eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.16em;
    font-weight: 600;
    color: rgba(var(--v-theme-hero-pill-on-dark), 0.76);
    margin-bottom: 0.5rem;
  }

  &__title {
    font-size: clamp(2.5rem, 4vw, 3.75rem);
    font-weight: 700;
    line-height: 1.1;
    margin-bottom: 0.5rem;
  }

  &__subtitle {
    font-size: clamp(1.2rem, 2vw, 1.5rem);
    color: rgba(var(--v-theme-hero-pill-on-dark), 0.86);
    margin-bottom: 0.75rem;
  }

  &__description {
    max-width: 48rem;
    color: rgba(var(--v-theme-hero-pill-on-dark), 0.82);
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    margin-top: 1.25rem;
  }

  &__cta {
    font-weight: 600;
  }

  &__highlights {
    list-style: none;
    padding: 0;
    margin: 2rem 0 0;
    display: grid;
    gap: 1rem;
  }

  &__highlight-item {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
  }

  &__highlight-icon {
    backdrop-filter: blur(8px);
    box-shadow: 0 10px 30px rgba(var(--v-theme-shadow-primary-600), 0.18);
  }

  &__highlight-title {
    font-weight: 600;
    margin-bottom: 0.25rem;
  }

  &__highlight-description {
    margin: 0;
    color: rgba(var(--v-theme-hero-pill-on-dark), 0.72);
  }

  &__card {
    position: relative;
    z-index: 1;
    padding: clamp(1.5rem, 3vw, 2.25rem);
    background: rgba(var(--v-theme-surface-glass), 0.96);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
    color: rgb(var(--v-theme-text-neutral-strong));
    margin-top: clamp(1.5rem, 4vw, 0rem);
  }

  &__card-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.75rem;
  }

  &__card-eyebrow {
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    color: rgba(var(--v-theme-text-neutral-strong), 0.7);
  }

  &__card-title {
    font-size: 1.35rem;
    font-weight: 700;
    margin-bottom: 0.5rem;
  }

  &__card-description {
    margin: 0;
    color: rgba(var(--v-theme-text-neutral-strong), 0.75);
  }

  &__card-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 0.5rem;
  }

  &__card-item {
    display: flex;
    align-items: center;
  }

  &__card-item-label {
    font-weight: 500;
  }
}

@media (max-width: 960px) {
  .feedback-hero__card {
    margin-top: 2rem;
  }
}
</style>

<template>
  <section
    class="feedback-open-source"
    aria-labelledby="feedback-open-source-heading"
  >
    <v-container class="py-16">
      <div class="feedback-open-source__header">
        <p class="feedback-open-source__eyebrow">{{ eyebrow }}</p>
        <h2
          id="feedback-open-source-heading"
          class="feedback-open-source__title"
        >
          {{ title }}
        </h2>
        <p class="feedback-open-source__description">
          {{ description }}
        </p>
      </div>

      <v-row class="g-6 mt-6">
        <v-col v-for="card in cards" :key="card.title" cols="12" md="6">
          <v-card class="feedback-open-source__card" elevation="6" rounded="xl">
            <div class="feedback-open-source__card-header">
              <v-avatar
                size="44"
                class="feedback-open-source__card-icon"
                color="surface-primary-120"
              >
                <v-icon :icon="card.icon" size="26" color="primary" />
              </v-avatar>
              <h3 class="feedback-open-source__card-title">{{ card.title }}</h3>
            </div>
            <p class="feedback-open-source__card-text">
              {{ card.description }}
            </p>
            <v-btn
              :href="card.cta.href"
              :to="card.cta.to"
              :aria-label="card.cta.ariaLabel"
              :target="card.cta.target"
              :rel="card.cta.rel"
              color="primary"
              variant="outlined"
              append-icon="mdi-arrow-right"
            >
              {{ card.cta.label }}
            </v-btn>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
type FeedbackCardCta = {
  label: string
  ariaLabel: string
  href?: string
  to?: string
  target?: string
  rel?: string
}

type FeedbackCard = {
  icon: string
  title: string
  description: string
  cta: FeedbackCardCta
}

defineProps<{
  eyebrow: string
  title: string
  description: string
  cards: FeedbackCard[]
}>()
</script>

<style scoped lang="scss">
.feedback-open-source {
  background:
    linear-gradient(
      145deg,
      rgba(var(--v-theme-surface-ice-050), 0.95),
      rgba(var(--v-theme-surface-ice-100), 0.9)
    ),
    radial-gradient(
      circle at top left,
      rgba(var(--v-theme-hero-gradient-end), 0.18),
      transparent 55%
    );

  &__header {
    max-width: 56rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  &__eyebrow {
    text-transform: uppercase;
    letter-spacing: 0.14em;
    font-weight: 600;
    color: rgb(var(--v-theme-primary));
  }

  &__title {
    font-size: clamp(2rem, 3vw, 2.75rem);
    font-weight: 700;
    margin: 0;
  }

  &__description {
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__card {
    padding: clamp(1.5rem, 2.5vw, 2rem);
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    background: rgb(var(--v-theme-surface-glass));
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  }

  &__card-header {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  &__card-icon {
    backdrop-filter: blur(8px);
    box-shadow: 0 14px 36px rgba(var(--v-theme-shadow-primary-600), 0.12);
  }

  &__card-title {
    font-size: 1.3rem;
    font-weight: 700;
    margin: 0;
  }

  &__card-text {
    flex: 1;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>

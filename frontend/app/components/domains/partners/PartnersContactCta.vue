<template>
  <section class="partners-cta" :aria-labelledby="headingId">
    <v-container class="partners-cta__container" max-width="lg">
      <v-sheet elevation="6" class="partners-cta__sheet" rounded="xl">
        <v-row class="align-center" no-gutters>
          <v-col cols="12" md="8" class="partners-cta__content">
            <p class="text-overline text-uppercase mb-2 text-neutral-soft">
              {{ eyebrow }}
            </p>
            <h2 :id="headingId" class="text-h4 font-weight-bold mb-3">
              {{ title }}
            </h2>
            <p class="text-body-1 text-neutral-secondary mb-0">
              {{ description }}
            </p>
          </v-col>
          <v-col
            cols="12"
            md="4"
            class="partners-cta__actions d-flex justify-md-end justify-center"
          >
            <v-btn
              :to="ctaTo"
              color="accent-supporting"
              size="large"
              class="partners-cta__button"
              prepend-icon="mdi-account-multiple-plus"
            >
              {{ ctaLabel }}
            </v-btn>
          </v-col>
        </v-row>
      </v-sheet>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { useId } from 'vue'

defineProps<{
  title: string
  description: string
  ctaLabel: string
  ctaTo: string
  eyebrow: string
}>()

const headingId = useId()
</script>

<style scoped lang="scss">
.partners-cta {
  color: rgb(var(--v-theme-text-neutral-strong));

  &__container {
    padding-inline: clamp(1.25rem, 3vw, 1.75rem);
  }

  &__sheet {
    background: rgb(var(--v-theme-surface-default));
    padding: clamp(1.5rem, 2vw + 1rem, 3rem);
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  &__button {
    text-transform: none;
    font-weight: 600;
    padding-inline: 2rem;
  }
}
</style>

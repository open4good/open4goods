<script setup lang="ts">
defineProps<{
  title: string
  description: string
  ctaLabel: string
  ctaAriaLabel: string
  href: string
}>()
</script>

<template>
  <section class="opendata-oss" aria-labelledby="opendata-oss-heading">
    <v-container max-width="lg">
      <div class="opendata-oss__surface">
        <div class="opendata-oss__text">
          <h2 id="opendata-oss-heading" class="opendata-oss__title">
            {{ title }}
          </h2>
          <p class="opendata-oss__description">{{ description }}</p>
        </div>
        <v-btn
          :href="href"
          :aria-label="ctaAriaLabel"
          variant="flat"
          color="primary"
          size="large"
          append-icon="mdi-arrow-right"
        >
          {{ ctaLabel }}
        </v-btn>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-oss
  padding: clamp(2.5rem, 5vw, 4rem) 0
  background: rgba(var(--v-theme-surface-default), 1)

.opendata-oss__surface
  display: flex
  flex-wrap: wrap
  gap: 1.5rem
  align-items: center
  justify-content: space-between
  background: rgba(var(--v-theme-surface-glass), 0.9)
  border-radius: 24px
  padding: clamp(1.75rem, 4vw, 2.75rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  box-shadow: 0 12px 40px rgba(var(--v-theme-shadow-primary-600), 0.08)

.opendata-oss__text
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-oss__title
  margin: 0
  font-size: clamp(1.75rem, 3.2vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-oss__description
  margin: 0
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

@media (max-width: 959px)
  .opendata-oss__surface
    flex-direction: column
    align-items: flex-start

  .opendata-oss__surface .v-btn
    width: 100%
</style>

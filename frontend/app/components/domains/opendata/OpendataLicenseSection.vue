<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description: string
    licenseLabel: string
    licenseUrl: string
    licenseAriaLabel: string
    licenseId?: string
  }>(),
  {
    licenseId: 'opendata-odbl-license',
  },
)
</script>

<template>
  <section
    :id="licenseId"
    class="opendata-license"
    aria-labelledby="opendata-license-heading"
  >
    <v-container max-width="lg">
      <v-card class="opendata-license__card" elevation="4" rounded="xl">
        <div class="opendata-license__content">
          <div class="opendata-license__text">
            <h2 id="opendata-license-heading" class="opendata-license__title">{{ title }}</h2>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="opendata-license__description" v-html="description" />
          </div>
          <div class="opendata-license__cta">
            <v-btn
              :href="licenseUrl"
              :aria-label="licenseAriaLabel"
              target="_blank"
              rel="noopener nofollow"
              color="primary"
              variant="tonal"
              size="large"
              append-icon="mdi-open-in-new"
            >
              {{ licenseLabel }}
            </v-btn>
          </div>
        </div>
      </v-card>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-license
  padding: clamp(2.5rem, 5vw, 4rem) 0

.opendata-license__card
  background: linear-gradient(120deg, rgba(var(--v-theme-surface-callout-start), 0.92), rgba(var(--v-theme-surface-callout-end), 0.92))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
  padding: clamp(1.75rem, 4vw, 2.75rem)

.opendata-license__content
  display: flex
  flex-direction: row
  gap: clamp(1.5rem, 3vw, 2.5rem)
  align-items: center
  justify-content: space-between

.opendata-license__text
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-license__title
  margin: 0
  font-size: clamp(1.75rem, 3.5vw, 2.25rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-license__description
  margin: 0
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-license__cta
  flex-shrink: 0

@media (max-width: 959px)
  .opendata-license__content
    flex-direction: column
    align-items: flex-start

  .opendata-license__cta
    width: 100%

  .opendata-license__cta .v-btn
    width: 100%
</style>


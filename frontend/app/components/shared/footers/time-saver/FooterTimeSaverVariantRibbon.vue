<script lang="ts" setup>
import type { RouteLocationRaw } from 'vue-router'
import type { TimeSaverModel } from './FooterTimeSaverVariantFocus.vue'

const { model, learnMoreCta = undefined } = defineProps<{
  model: TimeSaverModel
  learnMoreCta?: { label: string; to: RouteLocationRaw }
}>()
</script>

<template>
  <v-sheet class="time-saver time-saver--ribbon" rounded="xl" color="surface-primary-050" border>
    <div class="time-saver__content">
      <div class="time-saver__badge-row">
        <v-avatar size="40" color="primary" variant="flat">
          <v-icon icon="mdi-timer-sand" color="on-primary" />
        </v-avatar>
        <div class="time-saver__badge-text">
          <p class="time-saver__eyebrow text-subtitle-2 mb-0">{{ model.eyebrow }}</p>
          <p class="time-saver__subtitle text-body-2 mb-0">{{ model.subtitle }}</p>
        </div>
        <v-chip class="ms-auto" color="primary" variant="tonal" size="small">{{ model.badge }}</v-chip>
      </div>

      <div class="time-saver__body">
        <h3 class="time-saver__title text-h5 mb-3">{{ model.title }}</h3>
        <div class="time-saver__helper-row">
          <v-chip
            v-for="helper in model.helpers"
            :key="helper.label"
            class="time-saver__chip"
            color="surface-primary-080"
            variant="flat"
            label
          >
            <span class="time-saver__helper-icon" aria-hidden="true">{{ helper.icon }}</span>
            {{ helper.label }}
          </v-chip>
        </div>
      </div>

      <div class="time-saver__actions">
        <v-btn :to="model.primaryCta.to" variant="flat" color="primary" class="text-none" prepend-icon="mdi-magnify">
          {{ model.primaryCta.label }}
        </v-btn>
        <v-btn :to="model.secondaryCta.to" variant="text" color="primary" class="text-none" append-icon="mdi-arrow-right">
          {{ model.secondaryCta.label }}
        </v-btn>
        <v-btn
          v-if="learnMoreCta"
          :to="learnMoreCta.to"
          variant="text"
          color="accent-supporting"
          class="text-none"
          append-icon="mdi-arrow-top-right"
        >
          {{ learnMoreCta.label }}
        </v-btn>
      </div>
    </div>
  </v-sheet>
</template>

<style scoped lang="sass">
  .time-saver
    width: 100%
    border-color: rgba(var(--v-theme-border-primary-strong), 0.4)
    background: linear-gradient(90deg, rgba(var(--v-theme-surface-primary-080), 0.7) 0%, rgba(var(--v-theme-surface-ice-100), 0.85) 100%)

  .time-saver__content
    display: flex
    flex-direction: column
    gap: 16px
    padding: clamp(18px, 3vw, 28px)

  .time-saver__badge-row
    display: flex
    align-items: center
    gap: 12px

  .time-saver__badge-text
    display: flex
    flex-direction: column
    gap: 4px

  .time-saver__eyebrow
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
    letter-spacing: 0.08em
    text-transform: uppercase

  .time-saver__subtitle
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

  .time-saver__title
    color: rgb(var(--v-theme-text-neutral-strong))

  .time-saver__helper-row
    display: flex
    flex-wrap: wrap
    gap: 10px

  .time-saver__chip
    font-weight: 600
    gap: 6px
    color: rgb(var(--v-theme-text-neutral-strong))

  .time-saver__helper-icon
    font-size: 1rem

  .time-saver__actions
    display: grid
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr))
    gap: 10px

  @media (min-width: 960px)
    .time-saver__actions
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))
</style>

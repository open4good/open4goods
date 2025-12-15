<script lang="ts" setup>
import type { TimeSaverModel } from './FooterTimeSaverVariantFocus.vue'

const { model } = defineProps<{
  model: TimeSaverModel
}>()
</script>

<template>
  <v-sheet class="time-saver time-saver--minimal" rounded="xl" border color="surface-default">
    <div class="time-saver__row">
      <div class="time-saver__copy">
        <p class="time-saver__eyebrow text-subtitle-2 mb-1">{{ model.eyebrow }}</p>
        <h3 class="time-saver__title text-h6 mb-2">{{ model.title }}</h3>
        <p class="time-saver__subtitle text-body-2 mb-0">{{ model.subtitle }}</p>
      </div>
      <div class="time-saver__helpers" role="list">
        <div v-for="helper in model.helpers" :key="helper.label" class="time-saver__helper" role="listitem">
          <span class="time-saver__helper-icon" aria-hidden="true">{{ helper.icon }}</span>
          <span class="time-saver__helper-label text-body-2">{{ helper.label }}</span>
        </div>
      </div>
      <div class="time-saver__cta-row">
        <v-btn :to="model.primaryCta.to" variant="tonal" color="primary" class="text-none" size="large">
          {{ model.primaryCta.label }}
        </v-btn>
        <v-btn :to="model.secondaryCta.to" variant="text" color="primary" class="text-none" append-icon="mdi-arrow-right">
          {{ model.secondaryCta.label }}
        </v-btn>
      </div>
    </div>
  </v-sheet>
</template>

<style scoped lang="sass">
  .time-saver
    width: 100%
    padding: clamp(16px, 3vw, 24px)
    border-color: rgba(var(--v-theme-border-primary-strong), 0.35)
    background: rgba(var(--v-theme-surface-muted), 0.6)

  .time-saver__row
    display: grid
    gap: 14px
    grid-template-columns: 1fr

  .time-saver__copy
    display: flex
    flex-direction: column
    gap: 6px

  .time-saver__eyebrow
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
    letter-spacing: 0.06em
    text-transform: uppercase

  .time-saver__title
    color: rgb(var(--v-theme-text-neutral-strong))

  .time-saver__subtitle
    color: rgba(var(--v-theme-text-neutral-secondary), 0.92)
    line-height: 1.5

  .time-saver__helpers
    display: grid
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr))
    gap: 8px

  .time-saver__helper
    display: inline-flex
    align-items: center
    gap: 8px
    padding: 8px 10px
    border-radius: 12px
    background: rgba(var(--v-theme-surface-primary-080), 0.9)

  .time-saver__helper-icon
    font-size: 1rem

  .time-saver__cta-row
    display: flex
    flex-wrap: wrap
    gap: 10px
  
  .text-none
    text-transform: none
</style>

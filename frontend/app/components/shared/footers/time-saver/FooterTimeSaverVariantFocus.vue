<script lang="ts" setup>
import type { RouteLocationRaw } from 'vue-router'

export type TimeSaverHelper = {
  icon: string
  label: string
}

export type TimeSaverModel = {
  eyebrow: string
  title: string
  subtitle: string
  badge: string
  helpers: TimeSaverHelper[]
  primaryCta: { label: string; to: RouteLocationRaw }
  secondaryCta: { label: string; to: RouteLocationRaw }
}

const { model } = defineProps<{
  model: TimeSaverModel
}>()
</script>

<template>
  <v-sheet
    class="time-saver time-saver--focus"
    elevation="8"
    rounded="xl"
    color="surface-glass-strong"
  >
    <div class="time-saver__header">
      <v-chip
        class="time-saver__pill"
        color="accent-supporting"
        label
        size="small"
        variant="flat"
      >
        {{ model.badge }}
      </v-chip>
      <p class="time-saver__eyebrow text-subtitle-2 mb-1">
        {{ model.eyebrow }}
      </p>
      <h3 class="time-saver__title text-h5 mb-2">{{ model.title }}</h3>
      <p class="time-saver__subtitle text-body-1 mb-0">{{ model.subtitle }}</p>
    </div>

    <v-divider class="my-4" />

    <div class="time-saver__actions d-flex flex-column flex-sm-row ga-3">
      <v-btn
        :to="model.primaryCta.to"
        color="primary"
        class="time-saver__cta"
        size="large"
        prepend-icon="mdi-flash"
      >
        {{ model.primaryCta.label }}
      </v-btn>
      <v-btn
        :to="model.secondaryCta.to"
        variant="outlined"
        color="primary"
        class="time-saver__cta"
        size="large"
        append-icon="mdi-arrow-right"
      >
        {{ model.secondaryCta.label }}
      </v-btn>
    </div>

    <div class="time-saver__helpers" role="list">
      <div
        v-for="helper in model.helpers"
        :key="helper.label"
        class="time-saver__helper"
        role="listitem"
      >
        <span class="time-saver__helper-icon" aria-hidden="true">{{
          helper.icon
        }}</span>
        <span class="time-saver__helper-label text-body-2">{{
          helper.label
        }}</span>
      </div>
    </div>
  </v-sheet>
</template>

<style scoped lang="sass">
.time-saver
  width: 100%
  background: linear-gradient(120deg, rgba(var(--v-theme-surface-primary-080), 0.85) 0%, rgba(var(--v-theme-surface-glass), 0.95) 100%)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  box-shadow: 0 18px 42px -18px rgba(var(--v-theme-shadow-primary-600), 0.35)
  padding: clamp(20px, 3vw, 28px)

.time-saver__pill
  align-self: flex-start
  font-weight: 700
  letter-spacing: 0.04em

.time-saver__eyebrow
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  text-transform: uppercase
  letter-spacing: 0.08em

.time-saver__title
  color: rgb(var(--v-theme-text-neutral-strong))

.time-saver__subtitle
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
  line-height: 1.6

.time-saver__actions
  width: 100%

.time-saver__cta
  flex: 1 1 auto
  text-transform: none
  font-weight: 700

.time-saver__helpers
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr))
  gap: 12px
  margin-top: 20px

.time-saver__helper
  display: inline-flex
  align-items: center
  gap: 8px
  padding: 12px
  border-radius: 12px
  background: rgba(var(--v-theme-surface-primary-050), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)

.time-saver__helper-icon
  font-size: 1.15rem
</style>

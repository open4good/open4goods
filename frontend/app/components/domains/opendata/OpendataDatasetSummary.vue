<script setup lang="ts">
interface SummaryItem {
  label: string
  value: string | null | undefined
  icon?: string
}

defineProps<{
  title: string
  items: SummaryItem[]
}>()
</script>

<template>
  <section class="dataset-summary" aria-labelledby="dataset-summary-heading">
    <v-container max-width="lg">
      <v-card class="dataset-summary__card" rounded="xl" elevation="2">
        <div class="dataset-summary__header">
          <h2 id="dataset-summary-heading" class="dataset-summary__title">
            {{ title }}
          </h2>
        </div>
        <v-divider class="my-4" />
        <div class="dataset-summary__grid">
          <div
            v-for="item in items"
            :key="item.label"
            class="dataset-summary__item"
          >
            <div class="dataset-summary__item-icon" aria-hidden="true">
              <v-icon
                :icon="item.icon ?? 'mdi-information-outline'"
                size="28"
              />
            </div>
            <div class="dataset-summary__item-content">
              <p class="dataset-summary__item-label">{{ item.label }}</p>
              <p class="dataset-summary__item-value">{{ item.value ?? '—' }}</p>
            </div>
          </div>
        </div>
      </v-card>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.dataset-summary
  padding: clamp(2.5rem, 5vw, 4rem) 0

.dataset-summary__card
  padding: clamp(1.75rem, 4vw, 2.75rem)
  background: rgba(var(--v-theme-surface-default), 1)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.dataset-summary__title
  margin: 0
  font-size: clamp(1.75rem, 3vw, 2.2rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.dataset-summary__grid
  display: grid
  gap: clamp(1rem, 2vw, 1.5rem)
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))

.dataset-summary__item
  display: flex
  gap: 1rem
  align-items: center
  padding: 1rem
  border-radius: 16px
  background: rgba(var(--v-theme-surface-primary-080), 0.65)

.dataset-summary__item-icon
  width: 48px
  height: 48px
  display: inline-flex
  align-items: center
  justify-content: center
  border-radius: 14px
  background: rgba(var(--v-theme-surface-primary-120), 0.85)
  color: rgba(var(--v-theme-primary), 1)

.dataset-summary__item-label
  margin: 0
  font-size: 0.9rem
  text-transform: uppercase
  letter-spacing: 0.05em
  color: rgba(var(--v-theme-text-neutral-soft), 0.95)

.dataset-summary__item-value
  margin: 0
  font-size: 1.15rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-strong), 1)

@media (max-width: 600px)
  .dataset-summary__item
    flex-direction: column
    align-items: flex-start

  .dataset-summary__item-icon
    margin-bottom: 0.5rem

  .dataset-summary__item-value
    font-size: 1.05rem
</style>

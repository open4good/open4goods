<script setup lang="ts">
interface StatItem {
  icon: string
  label: string
  value: string
  description?: string
}

defineProps<{ stats: StatItem[] }>()
</script>

<template>
  <section class="opendata-stats" aria-labelledby="opendata-stats-title">
    <v-container max-width="lg">
      <div class="opendata-stats__surface">
        <h2 id="opendata-stats-title" class="visually-hidden">
          {{ $t('opendata.stats.accessibilityTitle') }}
        </h2>
        <ul class="opendata-stats__list">
          <li
            v-for="stat in stats"
            :key="stat.label"
            class="opendata-stats__item"
          >
            <div class="opendata-stats__icon" aria-hidden="true">
              <v-icon :icon="stat.icon" size="32" />
            </div>
            <div class="opendata-stats__content">
              <p class="opendata-stats__value">{{ stat.value }}</p>
              <p class="opendata-stats__label">{{ stat.label }}</p>
              <p v-if="stat.description" class="opendata-stats__description">
                {{ stat.description }}
              </p>
            </div>
          </li>
        </ul>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-stats
  position: relative
  background: rgba(var(--v-theme-surface-muted), 1)
  padding: 3rem 0

.opendata-stats__surface
  border-radius: 24px
  background: rgba(var(--v-theme-surface-default), 0.88)
  box-shadow: 0 20px 60px rgba(var(--v-theme-shadow-primary-600), 0.08)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
  padding: clamp(1.5rem, 4vw, 2.75rem)

.opendata-stats__list
  list-style: none
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))
  gap: clamp(1rem, 3vw, 2rem)
  margin: 0
  padding: 0

.opendata-stats__item
  display: flex
  gap: 1rem
  align-items: flex-start

.opendata-stats__icon
  display: inline-flex
  width: 48px
  height: 48px
  border-radius: 16px
  justify-content: center
  align-items: center
  background: rgba(var(--v-theme-surface-primary-100), 0.9)
  color: rgba(var(--v-theme-primary), 1)
  flex-shrink: 0

.opendata-stats__content
  display: flex
  flex-direction: column
  gap: 0.25rem

.opendata-stats__value
  margin: 0
  font-size: 1.625rem
  font-weight: 700
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-stats__label
  margin: 0
  font-size: 0.95rem
  font-weight: 500
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-stats__description
  margin: 0
  font-size: 0.875rem
  color: rgba(var(--v-theme-text-neutral-soft), 0.9)

.visually-hidden
  position: absolute
  width: 1px
  height: 1px
  padding: 0
  margin: -1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)
  white-space: nowrap
  border: 0
</style>

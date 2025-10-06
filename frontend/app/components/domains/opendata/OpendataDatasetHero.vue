<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface HeroStat {
  label: string
  value: string
  icon?: string
}

defineProps<{
  eyebrow: string
  title: string
  descriptionBlocId: string
  stats: HeroStat[]
}>()
</script>

<template>
  <section class="dataset-hero" aria-labelledby="dataset-hero-heading">
    <v-container max-width="lg" class="py-12">
      <div class="dataset-hero__surface">
        <div class="dataset-hero__text">
          <span class="dataset-hero__eyebrow">{{ eyebrow }}</span>
          <h1 id="dataset-hero-heading" class="dataset-hero__title">{{ title }}</h1>
          <TextContent :bloc-id="descriptionBlocId" :ipsum-length="220" />
        </div>
        <div class="dataset-hero__stats" role="list">
          <div v-for="stat in stats" :key="stat.label" class="dataset-hero__stat" role="listitem">
            <v-icon v-if="stat.icon" :icon="stat.icon" class="dataset-hero__stat-icon" size="28" />
            <p class="dataset-hero__stat-value">{{ stat.value }}</p>
            <p class="dataset-hero__stat-label">{{ stat.label }}</p>
          </div>
        </div>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.dataset-hero
  position: relative
  background: radial-gradient(circle at top left, rgba(var(--v-theme-hero-gradient-start), 0.9), rgba(var(--v-theme-hero-gradient-end), 0.8))
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

.dataset-hero__surface
  display: grid
  gap: clamp(1.75rem, 4vw, 3rem)
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr))

.dataset-hero__text
  display: flex
  flex-direction: column
  gap: 1rem

.dataset-hero__eyebrow
  display: inline-flex
  align-self: flex-start
  padding: 0.35rem 0.9rem
  border-radius: 999px
  background: rgba(var(--v-theme-hero-overlay-soft), 0.18)
  font-size: 0.85rem
  letter-spacing: 0.1em
  text-transform: uppercase

.dataset-hero__title
  margin: 0
  font-size: clamp(2.25rem, 4.5vw, 3.25rem)
  line-height: 1.15

.dataset-hero__stats
  display: grid
  gap: clamp(1rem, 2vw, 1.5rem)
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr))

.dataset-hero__stat
  padding: 1.5rem
  border-radius: 18px
  background: rgba(var(--v-theme-hero-overlay-soft), 0.12)
  border: 1px solid rgba(var(--v-theme-hero-overlay-soft), 0.25)
  display: flex
  flex-direction: column
  gap: 0.5rem

.dataset-hero__stat-icon
  color: rgba(var(--v-theme-hero-overlay-soft), 0.95)

.dataset-hero__stat-value
  margin: 0
  font-size: 1.5rem
  font-weight: 700

.dataset-hero__stat-label
  margin: 0
  font-size: 0.95rem
  color: rgba(var(--v-theme-hero-overlay-soft), 0.85)
</style>


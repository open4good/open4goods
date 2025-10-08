<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

defineProps<{
  eyebrow: string
  title: string
  descriptionBlocId: string
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
  display: flex
  flex-direction: column
  align-items: center
  gap: clamp(1.5rem, 3vw, 2.5rem)

.dataset-hero__text
  display: flex
  flex-direction: column
  gap: 1rem
  width: 100%
  max-width: 760px

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
</style>


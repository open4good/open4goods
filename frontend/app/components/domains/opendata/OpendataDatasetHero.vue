<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

defineProps<{
  eyebrow: string
  title: string
  descriptionBlocId: string
  breadcrumb?: {
    label: string
    href: string
    ariaLabel?: string
  }
}>()
</script>

<template>
  <section class="dataset-hero" aria-labelledby="dataset-hero-heading">
    <v-container max-width="lg" class="py-12">
      <div class="dataset-hero__surface">
        <div class="dataset-hero__text">
          <div class="dataset-hero__meta-row">
            <NuxtLink
              v-if="breadcrumb"
              :to="breadcrumb.href"
              :aria-label="breadcrumb.ariaLabel ?? breadcrumb.label"
              class="dataset-hero__breadcrumb"
            >
              {{ breadcrumb.label }}
            </NuxtLink>
            <span class="dataset-hero__eyebrow">{{ eyebrow }}</span>
          </div>
          <h1 id="dataset-hero-heading" class="dataset-hero__title">
            {{ title }}
          </h1>
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
  align-items: stretch
  gap: clamp(1.5rem, 3vw, 2.5rem)

.dataset-hero__text
  display: flex
  flex-direction: column
  gap: 1rem
  width: 100%

.dataset-hero__meta-row
  display: flex
  gap: 0.5rem
  align-items: center
  flex-wrap: wrap

.dataset-hero__breadcrumb
  display: inline-flex
  align-items: center
  padding: 0.35rem 0.9rem
  border-radius: 999px
  background: rgba(var(--v-theme-hero-overlay-soft), 0.32)
  font-size: 0.85rem
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-pill-on-dark), 1)
  text-decoration: none
  transition: background-color 0.2s ease, color 0.2s ease

.dataset-hero__breadcrumb:hover,
.dataset-hero__breadcrumb:focus-visible
  background: rgba(var(--v-theme-hero-overlay-soft), 0.48)
  color: rgba(var(--v-theme-hero-pill-on-dark), 1)

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

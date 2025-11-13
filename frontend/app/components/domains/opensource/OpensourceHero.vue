<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface HeroCta {
  label: string
  href: string
  ariaLabel: string
  icon?: string
  color?: string
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  target?: string
  rel?: string
}

interface HeroInfoCardItem {
  icon?: string
  text: string
}

interface HeroInfoCard {
  icon: string
  highlight?: string
  description?: string
  items: HeroInfoCardItem[]
}

withDefaults(
  defineProps<{
    title: string
    subtitle: string
    descriptionBlocId: string
    ctas?: HeroCta[]
    ctaGroupLabel?: string
    infoCard?: HeroInfoCard
  }>(),
  {
    ctas: () => [],
    ctaGroupLabel: undefined,
    infoCard: undefined,
  },
)
</script>

<template>
  <HeroSurface class="opensource-hero" aria-labelledby="opensource-hero-title" variant="prism">
    <v-container class="py-16 position-relative">
        <v-row class="align-center" :no-gutters="false" justify="space-between">
          <v-col cols="12" md="7" class="d-flex flex-column gap-4">

            <div class="hero-headline">
              <h1 id="opensource-hero-title" class="hero-title">
                <span class="fw-light">{{ title }}</span>
              </h1>
              <p class="hero-subtitle">{{ subtitle }}</p>
            </div>

            <TextContent :bloc-id="descriptionBlocId" :ipsum-length="220" />

            <div class="hero-ctas" role="group" :aria-label="ctaGroupLabel">
              <v-btn
                v-for="cta in ctas"
                :key="cta.href"
                :href="cta.href"
                :aria-label="cta.ariaLabel"
                :color="cta.color ?? 'primary'"
                :variant="cta.variant ?? 'flat'"
                :target="cta.target"
                :rel="cta.rel"
                size="large"
                class="me-3 mb-3"
                append-icon="mdi-arrow-top-right"
              >
                <template v-if="cta.icon" #prepend>
                  <v-icon :icon="cta.icon" aria-hidden="true" />
                </template>
                {{ cta.label }}
              </v-btn>
            </div>
          </v-col>

          <v-col cols="12" md="5" class="mt-10 mt-md-0">
            <v-card v-if="infoCard" class="hero-card" elevation="12" rounded="xl" aria-hidden="true">
              <div class="hero-card-content">
                <v-icon :icon="infoCard.icon" class="hero-card-icon" size="64" />
                <p class="hero-card-text">
                  <span v-if="infoCard.highlight" class="fw-medium">{{ infoCard.highlight }}</span>
                  <span v-if="infoCard.description">{{ infoCard.description }}</span>
                </p>
                <v-divider class="my-4" />
                <ul class="hero-card-list">
                  <li v-for="(item, index) in infoCard.items" :key="index">
                    <v-icon v-if="item.icon" :icon="item.icon" size="small" />
                    <span>{{ item.text }}</span>
                  </li>
                </ul>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
  </HeroSurface>
</template>

<style scoped lang="sass">
.opensource-hero
  position: relative
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)
  overflow: hidden

.hero-chip
  align-self: flex-start
  font-weight: 600
  letter-spacing: 0.08em
  text-transform: uppercase

.hero-headline
  display: flex
  flex-direction: column
  gap: 0.5rem

.hero-title
  font-size: clamp(2.5rem, 5vw, 3.75rem)
  margin: 0
  line-height: 1.1

.hero-subtitle
  font-size: 1.2rem
  opacity: 0.9
  margin: 0

.hero-ctas
  display: flex
  flex-wrap: wrap
  gap: 0.75rem

.hero-card
  background: rgba(var(--v-theme-surface-glass), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
  backdrop-filter: blur(18px)

.hero-card-content
  padding: clamp(1.5rem, 3vw, 2.5rem)
  display: flex
  flex-direction: column
  gap: 1.5rem
  color: rgba(var(--v-theme-text-neutral-strong), 0.95)

.hero-card-icon
  align-self: flex-start
  color: rgba(var(--v-theme-accent-primary-highlight), 0.85)

.hero-card-text
  font-size: 1rem
  margin: 0

.hero-card-text .fw-medium
  margin-right: 0.25rem
  
.hero-card-list
  list-style: none
  padding: 0
  margin: 0
  display: flex
  flex-direction: column
  gap: 0.75rem

.hero-card-list li
  display: flex
  align-items: center
  gap: 0.75rem
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.hero-card-list li .v-icon
  color: rgba(var(--v-theme-accent-supporting), 0.9)
</style>

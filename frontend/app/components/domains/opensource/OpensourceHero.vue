<script setup lang="ts">
import HeroEducationCard from '~/components/shared/ui/HeroEducationCard.vue'

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
  title: string
  bodyHtml?: string
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
            <h1 id="opensource-hero-title" class="hero-title mb-3">
              <span class="fw-light">{{ title }}</span>
            </h1>
            <p class="hero-subtitle">{{ subtitle }}</p>
          </div>

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
              class="me-3 mb-3 mt-5"
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
          <HeroEducationCard
            v-if="infoCard"
            :icon="infoCard.icon"
            :title="infoCard.title"
            :body-html="infoCard.bodyHtml"
            :items="infoCard.items"
          />
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
</style>

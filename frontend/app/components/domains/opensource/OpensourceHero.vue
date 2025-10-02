<script setup lang="ts">
import TextContent from '~/components/domains/content/TextContent.vue'

interface HeroStat {
  value: string
  label: string
}

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

withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    subtitle: string
    descriptionBlocId: string
    stats?: HeroStat[]
    ctas?: HeroCta[]
  }>(),
  {
    stats: () => [],
    ctas: () => [],
  },
)
</script>

<template>
  <section class="opensource-hero" aria-labelledby="opensource-hero-title">
    <div class="hero-surface">
      <v-container class="py-16 position-relative">
        <v-row class="align-center" :no-gutters="false" justify="space-between">
          <v-col cols="12" md="7" class="d-flex flex-column gap-4">
            <v-chip
              v-if="eyebrow"
              label
              color="accent-supporting"
              class="hero-chip"
              variant="flat"
              size="small"
            >
              {{ eyebrow }}
            </v-chip>

            <div class="hero-headline">
              <h1 id="opensource-hero-title" class="hero-title">
                <span class="fw-light">{{ title }}</span>
              </h1>
              <p class="hero-subtitle">{{ subtitle }}</p>
            </div>

            <TextContent :bloc-id="descriptionBlocId" :ipsum-length="220" />

            <div class="hero-ctas" role="group" aria-label="Hero call to actions">
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

            <v-divider v-if="stats.length" class="my-4" color="accent-supporting" />

            <div v-if="stats.length" class="hero-stats" role="list">
              <div v-for="stat in stats" :key="stat.label" class="hero-stat" role="listitem">
                <span class="hero-stat-value">{{ stat.value }}</span>
                <span class="hero-stat-label">{{ stat.label }}</span>
              </div>
            </div>
          </v-col>

          <v-col cols="12" md="5" class="mt-10 mt-md-0">
            <v-card class="hero-card" elevation="12" rounded="xl" aria-hidden="true">
              <div class="hero-card-content">
                <v-icon icon="mdi-source-branch" class="hero-card-icon" size="64" />
                <p class="hero-card-text">
                  <span class="fw-medium">Open4goods</span> est construit en commun. Chaque pull request, issue ou retour
                  utilisateur façonne une plateforme plus transparente.
                </p>
                <v-divider class="my-4" />
                <ul class="hero-card-list">
                  <li>
                    <v-icon icon="mdi-checkbox-marked-circle-outline" size="small" />
                    <span>Code et données publiés sous licences ouvertes</span>
                  </li>
                  <li>
                    <v-icon icon="mdi-checkbox-marked-circle-outline" size="small" />
                    <span>Revue collaborative des contributions</span>
                  </li>
                  <li>
                    <v-icon icon="mdi-checkbox-marked-circle-outline" size="small" />
                    <span>Gouvernance partagée et documentation vivante</span>
                  </li>
                </ul>
              </div>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </div>
  </section>
</template>

<style scoped lang="sass">
.opensource-hero
  position: relative
  background: radial-gradient(circle at top left, rgba(var(--v-theme-hero-gradient-start), 0.65), rgba(var(--v-theme-hero-gradient-end), 0.85))
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)
  overflow: hidden

.hero-surface
  position: relative
  isolation: isolate

.hero-surface::after
  content: ''
  position: absolute
  inset: 0
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-overlay-soft), 0.08) 0%, rgba(var(--v-theme-hero-overlay-soft), 0.2) 50%, rgba(var(--v-theme-hero-overlay-soft), 0.1) 100%)
  z-index: 0

.hero-surface > .v-container
  position: relative
  z-index: 1

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

.hero-stats
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr))
  gap: 1.5rem

.hero-stat
  display: flex
  flex-direction: column
  gap: 0.25rem

.hero-stat-value
  font-size: 1.75rem
  font-weight: 700

.hero-stat-label
  font-size: 0.95rem
  opacity: 0.85

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

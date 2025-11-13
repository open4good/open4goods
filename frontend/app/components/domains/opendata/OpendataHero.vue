<script setup lang="ts">
interface HeroCta {
  label: string
  href: string
  ariaLabel: string
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  color?: string
  appendIcon?: string
}

const props = withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    subtitle: string
    primaryCta?: HeroCta
  }>(),
  {
    eyebrow: undefined,
    primaryCta: undefined,
  },
)

const handlePrimaryClick = (event: MouseEvent) => {
  const href = props.primaryCta?.href
  if (!href || !href.startsWith('#')) {
    return
  }

  if (import.meta.client) {
    event.preventDefault()
    const target = document.querySelector(href)
    if (target instanceof HTMLElement) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}
</script>

<template>
  <HeroSurface class="opendata-hero" aria-labelledby="opendata-hero-heading" variant="orbit">
    <v-container class="py-16" max-width="lg">
      <v-row align="center" class="g-8">
        <v-col cols="12" md="7" class="d-flex flex-column gap-6">
          <div class="opendata-hero__header">
            <span v-if="eyebrow" class="opendata-hero__eyebrow" role="text">{{ eyebrow }}</span>
            <h1 id="opendata-hero-heading" class="opendata-hero__title">{{ title }}</h1>
            <p class="opendata-hero__subtitle">{{ subtitle }}</p>
          </div>


            <div class="mt-4 opendata-hero__actions" role="group">
              <v-btn
                v-if="primaryCta"
                :href="primaryCta.href"
                :aria-label="primaryCta.ariaLabel"
                :variant="primaryCta.variant ?? 'flat'"
                :color="primaryCta.color ?? 'primary'"
                size="large"
                class="opendata-hero__cta"
                :append-icon="primaryCta.appendIcon ?? 'mdi-arrow-right'"
                @click="handlePrimaryClick"
              >
                {{ primaryCta.label }}
              </v-btn>
            </div>
        </v-col>

        <v-col cols="12" md="5" class="opendata-hero__visual">
          <div class="opendata-hero__glow" aria-hidden="true">
            <div class="opendata-hero__glow-ring" />
            <div class="opendata-hero__glow-ring opendata-hero__glow-ring--secondary" />
            <div class="opendata-hero__glow-dots">
              <span v-for="index in 6" :key="index" class="opendata-hero__glow-dot" />
            </div>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </HeroSurface>
</template>

<style scoped lang="sass">
.opendata-hero
  position: relative
  overflow: hidden
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

.opendata-hero__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-hero__eyebrow
  align-self: flex-start
  padding: 0.375rem 0.75rem
  border-radius: 999px
  background-color: rgba(var(--v-theme-hero-overlay-strong), 0.12)
  font-size: 0.875rem
  letter-spacing: 0.08em
  text-transform: uppercase

.opendata-hero__title
  font-size: clamp(2.5rem, 5vw, 3.5rem)
  margin: 0
  line-height: 1.1

.opendata-hero__subtitle
  margin: 0
  font-size: 1.2rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

.opendata-hero__actions
  display: flex
  flex-wrap: wrap
  gap: 0.75rem

.opendata-hero__cta
  font-weight: 600

.opendata-hero__visual
  display: flex
  justify-content: center
  align-items: center

.opendata-hero__glow
  position: relative
  width: min(320px, 100%)
  aspect-ratio: 1

.opendata-hero__glow-ring
  position: absolute
  inset: 10%
  border-radius: 50%
  border: 1px solid rgba(var(--v-theme-hero-overlay-strong), 0.35)
  box-shadow: 0 0 60px rgba(var(--v-theme-accent-primary-highlight), 0.25)

.opendata-hero__glow-ring--secondary
  inset: 20%
  border-color: rgba(var(--v-theme-accent-supporting), 0.3)
  box-shadow: 0 0 40px rgba(var(--v-theme-accent-supporting), 0.22)

.opendata-hero__glow-dots
  position: absolute
  inset: 0
  display: grid
  place-items: center
  grid-template-columns: repeat(3, 1fr)
  grid-template-rows: repeat(3, 1fr)

.opendata-hero__glow-dot
  width: 10px
  height: 10px
  border-radius: 50%
  background: rgba(var(--v-theme-hero-overlay-strong), 0.6)
  box-shadow: 0 0 18px rgba(var(--v-theme-hero-overlay-strong), 0.45)

@media (max-width: 959px)
  .opendata-hero__visual
    margin-top: 2rem

  .opendata-hero__glow
    width: 240px
</style>


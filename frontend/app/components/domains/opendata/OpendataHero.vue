<script setup lang="ts">
import HeroEducationCard from '~/components/shared/ui/HeroEducationCard.vue'

interface HeroCta {
  label: string
  href: string
  ariaLabel: string
  variant?: 'flat' | 'outlined' | 'tonal' | 'text' | 'plain'
  color?: string
  appendIcon?: string
}

interface HeroEducationCardItem {
  icon?: string
  text: string
}

interface HeroEducationCardProps {
  icon: string
  title: string
  bodyHtml?: string
  items?: HeroEducationCardItem[]
}

const props = withDefaults(
  defineProps<{
    eyebrow?: string
    title: string
    subtitle: string
    primaryCta?: HeroCta
    educationCard?: HeroEducationCardProps
  }>(),
  {
    eyebrow: undefined,
    primaryCta: undefined,
    educationCard: undefined,
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

const handleSubtitleClick = (event: MouseEvent) => {
  if (!import.meta.client) {
    return
  }

  const anchor = (event.target as HTMLElement | null)?.closest('[data-scroll-target]')
  const targetSelector = anchor?.getAttribute('data-scroll-target')

  if (anchor && targetSelector) {
    const target = document.querySelector(targetSelector)
    if (target instanceof HTMLElement) {
      event.preventDefault()
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
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="opendata-hero__subtitle" @click="handleSubtitleClick" v-html="subtitle" />
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
              <span v-for="index in 6" :key="index" />
          </div>

          <HeroEducationCard
            v-if="educationCard"
            class="opendata-hero__education-card"
            :icon="educationCard.icon"
            :title="educationCard.title"
            :body-html="educationCard.bodyHtml"
            :items="educationCard.items"
          />
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
  position: relative

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

.opendata-hero__education-card
  width: 100%
  max-width: 420px
  position: relative
  z-index: 1
  margin-top: clamp(1rem, 3vw, 2rem)
  box-shadow: 0 20px 60px rgba(var(--v-theme-shadow-primary-600), 0.18)


@media (max-width: 959px)
  .opendata-hero__visual
    margin-top: 2rem
    align-items: stretch

  .opendata-hero__glow
    width: 240px
    margin-inline: auto

  .opendata-hero__education-card
    max-width: none
</style>


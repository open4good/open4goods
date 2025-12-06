<script setup lang="ts">
import { computed, useId } from 'vue'
import TextContent from '~/components/domains/content/TextContent.vue'

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
    description?: string
    descriptionBlocId?: string
    primaryCta?: HeroCta
    headingId?: string
  }>(),
  {
    eyebrow: undefined,
    description: undefined,
    descriptionBlocId: undefined,
    primaryCta: undefined,
    headingId: undefined,
  }
)

const generatedHeadingId = useId()
const headingLabelId = computed(() => props.headingId ?? generatedHeadingId)

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
  <HeroSurface
    class="partners-hero"
    :aria-labelledby="headingLabelId"
    variant="orbit"
  >
    <v-container class="py-16" max-width="lg">
      <v-row align="center" class="g-8">
        <v-col cols="12" md="7" class="d-flex flex-column gap-6">
          <div class="partners-hero__header">
            <span v-if="eyebrow" class="partners-hero__eyebrow" role="text">{{
              eyebrow
            }}</span>
            <h1 :id="headingLabelId" class="partners-hero__title">
              {{ title }}
            </h1>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p class="partners-hero__subtitle" v-html="subtitle" />
          </div>

          <div class="partners-hero__body">
            <p
              v-if="description && !descriptionBlocId"
              class="partners-hero__description"
            >
              {{ description }}
            </p>
            <TextContent
              v-else-if="descriptionBlocId"
              class="partners-hero__cms"
              :bloc-id="descriptionBlocId"
              :fallback-text="description"
            />
          </div>

          <div class="partners-hero__actions" role="group">
            <v-btn
              v-if="primaryCta"
              :href="primaryCta.href"
              :aria-label="primaryCta.ariaLabel"
              :variant="primaryCta.variant ?? 'flat'"
              :color="primaryCta.color ?? 'primary'"
              size="large"
              class="partners-hero__cta"
              :append-icon="primaryCta.appendIcon ?? 'mdi-arrow-right'"
              @click="handlePrimaryClick"
            >
              {{ primaryCta.label }}
            </v-btn>
          </div>
        </v-col>

        <v-col cols="12" md="5" class="partners-hero__visual">
          <div class="partners-hero__glow" aria-hidden="true">
            <div class="partners-hero__glow-ring" />
            <div
              class="partners-hero__glow-ring partners-hero__glow-ring--secondary"
            />
            <span v-for="index in 6" :key="index" />
          </div>
        </v-col>
      </v-row>
    </v-container>
  </HeroSurface>
</template>

<style scoped lang="sass">
.partners-hero
  position: relative
  overflow: hidden
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

.partners-hero__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.partners-hero__eyebrow
  align-self: flex-start
  padding: 0.375rem 0.75rem
  border-radius: 999px
  background-color: rgba(var(--v-theme-hero-overlay-strong), 0.12)
  font-size: 0.875rem
  letter-spacing: 0.08em
  text-transform: uppercase

.partners-hero__title
  font-size: clamp(2.5rem, 5vw, 3.5rem)
  margin: 0
  line-height: 1.1

.partners-hero__subtitle
  margin: 0
  font-size: 1.2rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

.partners-hero__body
  display: flex
  flex-direction: column
  gap: 0.75rem

.partners-hero__description
  margin: 0
  font-size: 1.05rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.85)
  line-height: 1.6

.partners-hero__cms
  padding: 0

.partners-hero__actions
  display: flex
  flex-wrap: wrap
  gap: 0.75rem

.partners-hero__cta
  font-weight: 600

.partners-hero__visual
  display: flex
  justify-content: center
  align-items: center

.partners-hero__glow
  position: relative
  width: min(320px, 100%)
  aspect-ratio: 1

.partners-hero__glow-ring
  position: absolute
  inset: 10%
  border-radius: 50%
  border: 1px solid rgba(var(--v-theme-hero-overlay-strong), 0.35)
  box-shadow: 0 0 60px rgba(var(--v-theme-accent-primary-highlight), 0.25)

.partners-hero__glow-ring--secondary
  inset: 20%
  border-color: rgba(var(--v-theme-accent-supporting), 0.3)
  box-shadow: 0 0 40px rgba(var(--v-theme-accent-supporting), 0.22)

@media (max-width: 959px)
  .partners-hero__visual
    margin-top: 2rem

  .partners-hero__glow
    width: 240px
</style>

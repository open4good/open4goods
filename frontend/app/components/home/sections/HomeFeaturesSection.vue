<script setup lang="ts">
import ParallaxSection from '~/components/shared/ui/ParallaxSection.vue'

type FeatureCard = {
  icon: string
  title: string
  description: string
}

const props = defineProps<{
  features: FeatureCard[]
}>()

const { t } = useI18n()
</script>

<template>
  <ParallaxSection
    class="home-features"
    aria-label="home-features-title"
    :background-light="'/assets/themes/light/parallax/parallax-background-bubbles-transparent-1.svg'"
    :background-dark="'/assets/themes/dark/parallax/parallax-background-bubbles-1.svg'"
    :parallax-amount="0.12"
  >
    <div class="home-features__inner">
      <header class="home-section__header">
        <h2 id="home-features-title">{{ t('home.features.title') }}</h2>
        <!-- eslint-disable-next-line vue/no-v-html -->
        <p
          class="home-section__subtitle subtitle-text"
          v-html="t('home.features.subtitle')"
        />
      </header>
      <v-row class="home-features__grid" align="stretch">
        <v-col
          v-for="feature in props.features"
          :key="feature.title"
          cols="12"
          sm="6"
          lg="4"
        >
          <v-card class="home-features__card" variant="flat">
            <v-icon
              class="home-features__icon"
              :icon="feature.icon"
              size="36"
            />
            <h3 class="home-features__card-title">{{ feature.title }}</h3>
            <p class="home-features__card-description">
              {{ feature.description }}
            </p>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </ParallaxSection>
</template>

<style scoped lang="sass">
// .home-features
  // background managed by ParallaxSection

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

.home-section__header
  max-width: 760px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0

.home-features__grid
  --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
  --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

.home-features__card
  height: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.75rem, 4vw, 2.25rem)
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-glass-strong), 0.9)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  box-shadow: 0 16px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-features__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.9)

.home-features__card-title
  margin: 0
  font-size: 1.2rem
  font-weight: 600

.home-features__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

</style>



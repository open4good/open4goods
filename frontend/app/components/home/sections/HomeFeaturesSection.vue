<script setup lang="ts">
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
  <section class="home-section home-features" aria-labelledby="home-features-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <p id="home-features-title" class="home-hero__subtitle">{{ t('home.features.title') }}</p>
        <p class="home-section__subtitle text-center">{{ t('home.features.subtitle') }}</p>
        <v-row class="home-features__grid mt-5" align="stretch" justify="center">
          <v-col
            v-for="feature in props.features"
            :key="feature.title"
            cols="12"
            sm="6"
            lg="4"
          >
            <v-card class="home-features__card card__nudger" variant="flat">
              <div class="text-center">
                <v-icon class="home-features__icon" :icon="feature.icon" size="100" />
              </div>
              <h3 class="home-hero__subtitle home-features__card-title">{{ feature.title }}</h3>
              <p class="home-features__card-description text-center mt-auto">{{ feature.description }}</p>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.25rem);

.home-section__header
  max-width: 760px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-features__grid
  --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
  --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

.home-features__card
  height: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  //padding: clamp(1.75rem, 4vw, 2.25rem)
  //border-radius: clamp(1.25rem, 3vw, 1.75rem)
  //background: rgba(var(--v-theme-surface-glass-strong), 0.9)
  //border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  box-shadow: 0 16px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-features__icon
  color: rgb(var(--v-theme-secondary))
  text-align: center

.home-features__card-title
  margin: 0
  font-size: 1.2rem
  font-weight: 600

.home-features__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
</style>

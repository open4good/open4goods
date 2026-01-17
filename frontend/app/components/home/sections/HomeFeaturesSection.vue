<script setup lang="ts">
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'
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
  <section class="home-features" aria-labelledby="home-features-title">
    <div class="home-features__inner home-section__inner">
      <header class="home-section__header">
        <h2 id="home-features-title">{{ t('home.features.title') }}</h2>
        <!-- eslint-disable vue/no-v-html -->
        <p
          class="home-section__subtitle subtitle-text"
          v-html="t('home.features.subtitle')"
        />
        <!-- eslint-enable vue/no-v-html -->
      </header>
      <v-row class="home-features__grid" align="stretch" justify="center">
        <v-col
          v-for="feature in props.features"
          :key="feature.title"
          cols="12"
          sm="6"
          lg="4"
        >
          <NudgerCard class="home-features__card">
            <div class="text-center">
              <v-icon
                class="home-features__icon"
                :icon="feature.icon"
                size="100"
              />
            </div>
            <h3 class="home-hero__subtitle home-features__card-title">
              {{ feature.title }}
            </h3>
            <p class="home-features__card-description">
              {{ feature.description }}
            </p>
          </NudgerCard>
        </v-col>
      </v-row>
    </div>
  </section>
</template>

<style scoped lang="sass">
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
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-shadow: 0 16px 28px rgba(var(--v-theme-shadow-primary-600), 0.12);

.home-features__icon
  color: rgb(var(--v-theme-secondary));
  text-align: center;

.home-features__card-title
  margin: 0
  font-size: 1.2rem
  font-weight: 600

.home-features__card-description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
</style>

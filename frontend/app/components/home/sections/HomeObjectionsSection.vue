<script setup lang="ts">
type ObjectionItem = {
  icon: string
  question: string
  answer: string
}

const props = defineProps<{
  items: ObjectionItem[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="home-section home-objections" aria-labelledby="home-objections-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <header class="home-section__header">
          <h2 id="home-objections-title">{{ t('home.objections.title') }}</h2>
          <!-- eslint-disable-next-line vue/no-v-html -->
          <p class="home-section__subtitle subtitle-text" v-html="t('home.objections.subtitle')" />
        </header>
        <v-row class="home-objections__grid">
          <v-col v-for="item in props.items" :key="item.question" cols="12" md="4">
            <v-card class="home-objections__card" variant="outlined">
              <div class="home-objections__card-header">
                <v-icon class="home-objections__icon" :icon="item.icon" size="28" />
                <h3 class="home-objections__question">{{ item.question }}</h3>
              </div>
              <p class="home-objections__answer">{{ item.answer }}</p>
            </v-card>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(3rem, 6vw, 5rem)
  background: rgba(var(--v-theme-surface-default), 0.98)

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

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

.home-objections__grid
  --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
  --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

.home-objections__card
  height: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.75rem, 4vw, 2.25rem)
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.32)
  background: rgba(var(--v-theme-surface-glass-strong), 0.9)
  box-shadow: 0 16px 26px rgba(var(--v-theme-shadow-primary-600), 0.12)

.home-objections__card-header
  display: flex
  gap: 0.75rem
  align-items: center

.home-objections__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.95)

.home-objections__question
  margin: 0
  font-size: 1.1rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.home-objections__answer
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
</style>

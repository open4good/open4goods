<script setup lang="ts">
type SolutionBenefit = {
  emoji: string
  label: string
}

const props = defineProps<{
  benefits: SolutionBenefit[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="home-section home-solution" aria-labelledby="home-solution-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <v-row class="home-solution__content" align="center" justify="space-between">
          <v-col cols="12" md="6" class="home-solution__col home-solution__col--copy">
            <header class="home-section__header">
              <h2 id="home-solution-title">{{ t('home.solution.title') }}</h2>
              <p class="home-section__subtitle">{{ t('home.solution.description') }}</p>
            </header>
            <ul class="home-solution__list">
              <li v-for="item in props.benefits" :key="item.label" class="home-solution__item">
                <span class="home-solution__icon" aria-hidden="true">{{ item.emoji }}</span>
                <p class="home-solution__label">{{ item.label }}</p>
              </li>
            </ul>
          </v-col>
          <v-col cols="12" md="6" class="home-solution__col home-solution__col--visual">
            <div class="home-solution__visual" role="presentation">
              <NuxtImg
                src="/images/home/nudgder-pain.png"
                :alt="t('home.solution.title')"
                class="home-solution__image"
                loading="lazy"
              />
            </div>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(3rem, 6vw, 5.5rem)
  background: rgba(var(--v-theme-surface-primary-050), 0.75)

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3.25rem)

.home-solution__content
  row-gap: clamp(2rem, 5vw, 3rem)

.home-section__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-solution__col--copy
  display: flex
  flex-direction: column
  gap: clamp(1.75rem, 4vw, 2.5rem)

.home-solution__list
  margin: 0
  padding: 0
  display: grid
  gap: clamp(1.25rem, 3vw, 1.75rem)

.home-solution__item
  list-style: none
  display: flex
  align-items: flex-start
  gap: clamp(1rem, 2.5vw, 1.5rem)
  padding: clamp(1.1rem, 3vw, 1.65rem)
  border-radius: clamp(1.1rem, 3vw, 1.5rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.72), rgba(var(--v-theme-surface-default), 0.95))
  box-shadow: 0 10px 24px rgba(var(--v-theme-shadow-primary-600), 0.12)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)

.home-solution__icon
  display: inline-flex
  align-items: center
  justify-content: center
  flex-shrink: 0
  width: clamp(3.25rem, 8vw, 3.75rem)
  aspect-ratio: 1
  border-radius: 50%
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-120), 0.95), rgba(var(--v-theme-surface-primary-100), 0.85))
  color: rgb(var(--v-theme-text-on-accent))
  font-size: clamp(1.75rem, 5vw, 2rem)
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35), 0 10px 18px rgba(var(--v-theme-shadow-primary-600), 0.16)
  line-height: 1

.home-solution__label
  margin: 0
  font-size: clamp(1.1rem, 2.4vw, 1.35rem)
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__col--visual
  display: flex
  justify-content: center

.home-solution__visual
  position: relative
  width: min(100%, 480px)
  padding: clamp(1.5rem, 4vw, 2.5rem)
  background: linear-gradient(150deg, rgba(var(--v-theme-surface-primary-100), 0.95), rgba(var(--v-theme-surface-primary-050), 0.85))
  border-radius: clamp(1.75rem, 5vw, 2.75rem)
  box-shadow: 0 26px 45px rgba(var(--v-theme-shadow-primary-600), 0.2)
  overflow: hidden

.home-solution__visual::after
  content: ''
  position: absolute
  inset: 10%
  border-radius: clamp(1.25rem, 4vw, 2rem)
  background: radial-gradient(circle at 20% 20%, rgba(var(--v-theme-accent-primary-highlight), 0.18), transparent 55%)
  pointer-events: none

.home-solution__image
  position: relative
  z-index: 1
  width: 100%
  height: auto
  display: block

@media (max-width: 959px)
  .home-solution__col--visual
    order: -1

@media (max-width: 599px)
  .home-solution__item
    flex-direction: column
    text-align: center
    align-items: center
  .home-solution__icon
    margin-bottom: 0.5rem
</style>

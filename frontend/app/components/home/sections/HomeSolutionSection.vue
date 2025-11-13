<script setup lang="ts">
type SolutionBenefit = {
  emoji: string
  label: string
  description: string
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
            <v-row class="home-solution__list" dense>
              <v-col
                v-for="item in props.benefits"
                :key="item.label"
                cols="12"
                sm="6"
                class="home-solution__list-col"
              >
                <v-sheet class="home-solution__item" rounded="xl" elevation="0">
                  <v-avatar class="home-solution__icon" size="60" color="surface">
                    <span aria-hidden="true">{{ item.emoji }}</span>
                  </v-avatar>
                  <div class="home-solution__texts">
                    <p class="home-solution__label">{{ item.label }}</p>
                    <p class="home-solution__description">{{ item.description }}</p>
                  </div>
                </v-sheet>
              </v-col>
            </v-row>
          </v-col>
          <v-col cols="12" md="6" class="home-solution__col home-solution__col--visual">
            <div class="home-solution__visual" role="presentation">
              <NuxtImg
                src="/images/home/nudger-screaming.webp"
                :alt="t('home.solution.title')"
                class="home-solution__image"
                sizes="(min-width: 960px) 306px, 60vw"
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
  background: rgba(var(--v-theme-surface-primary-050), 0.6)

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
  row-gap: clamp(1rem, 2.5vw, 1.5rem)

.home-solution__list-col
  display: flex

.home-solution__item
  width: 100%
  display: flex
  gap: 1rem
  align-items: flex-start
  padding: clamp(1.25rem, 3vw, 1.5rem)
  background: rgba(var(--v-theme-surface-default), 0.9)
  border-radius: clamp(1rem, 3vw, 1.5rem)
  box-shadow: none

.home-solution__item::after
  display: none

.home-solution__icon
  font-size: clamp(1.65rem, 5vw, 2rem)
  background: rgba(var(--v-theme-surface-primary-080), 0.6)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__texts
  display: flex
  flex-direction: column
  gap: 0.35rem

.home-solution__description
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.95rem

.home-solution__label
  margin: 0
  font-size: clamp(1.05rem, 2.4vw, 1.25rem)
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__col--visual
  display: flex
  justify-content: center

.home-solution__visual
  position: relative
  width: min(100%, 460px)
  display: flex
  justify-content: center
  align-items: center

.home-solution__image
  position: relative
  z-index: 1
  width: min(66%, 306px)
  height: auto
  display: block
  margin-inline: auto

@media (max-width: 959px)
  .home-solution__col--visual
    order: -1

@media (max-width: 599px)
  .home-solution__item
    flex-direction: column
    text-align: center
    align-items: center
  .home-solution__texts
    align-items: center
</style>

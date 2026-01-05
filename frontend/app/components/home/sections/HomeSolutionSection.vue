<script setup lang="ts">
import { computed } from 'vue'
import HomeParallaxVisual from '../HomeParallaxVisual.vue'

type SolutionBenefit = {
  emoji: string
  label: string
  description: string
}

const props = defineProps<{
  benefits: SolutionBenefit[]
}>()

const { t } = useI18n()

const sectionTitle = computed(() => t('home.solution.title'))
const sectionDescription = computed(() => t('home.solution.description'))
</script>

<template>
  <section
    id="home-solution"
    class="home-solution"
    aria-labelledby="home-solution-title"
  >
    <div class="home-solution__inner">
      <v-row
        class="home-solution__layout"
        align="center"
        justify="space-between"
      >
        <v-col cols="12" md="6" class="home-solution__copy">
          <header class="home-section__header">
            <h2 id="home-solution-title">{{ sectionTitle }}</h2>
            <p class="home-section__subtitle subtitle-text">
              {{ sectionDescription }}
            </p>
          </header>
          <v-row class="home-solution__list" dense>
            <v-col
              v-for="item in props.benefits"
              :key="item.label"
              cols="12"
              class="home-solution__list-col card__nudger card__nudger--border card__nudger--radius_top-right_0 card__nudger--radius_bottom-right_50px"
            >
              <v-sheet class="home-solution__item" rounded="xl" elevation="0">
                <v-avatar class="home-solution__icon" size="60" color="surface">
                  <span aria-hidden="true">{{ item.emoji }}</span>
                </v-avatar>
                <div class="home-solution__texts">
                  <p class="home-solution__label">{{ item.label }}</p>
                  <p class="home-solution__description">
                    {{ item.description }}
                  </p>
                </div>
              </v-sheet>
            </v-col>
          </v-row>
        </v-col>
        <v-col cols="12" md="6" class="home-solution__visual">
          <div class="home-solution__image-wrapper">
            <HomeParallaxVisual
              src="/images/parallax/solution.svg"
              :alt="sectionTitle"
            />
          </div>
        </v-col>
      </v-row>
    </div>
  </section>
</template>

<style scoped lang="sass">
.home-solution__inner
  max-width: 1180px
  margin: 0 auto

.home-solution__layout
  row-gap: clamp(2rem, 5vw, 3rem)

.home-solution__copy
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.5rem)

.home-section__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-solution__visual
  display: flex
  justify-content: center
  position: relative

.home-solution__image-wrapper
  width: min(100%, 460px)
  display: flex
  justify-content: center
  align-items: center
  position: relative

.home-solution__image
  width: min(66%, 320px)
  height: auto
  display: block
  filter: drop-shadow(0 20px 40px rgba(var(--v-theme-shadow-primary-600), 0.15))

.home-solution__next-btn
  position: absolute
  bottom: 0
  right: 1rem
  z-index: 2
  box-shadow: 0 4px 6px rgba(0,0,0,0.1)

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

.home-solution__item::after
  display: none

.home-solution__icon
  font-size: clamp(1.65rem, 5vw, 2rem)
  border: 1px solid rgb(var(--v-theme-secondary))
  color: rgb(var(--v-theme-secondary))

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

@media (max-width: 959px)
  .home-solution__visual
    order: -1
    margin-bottom: 1rem

  .home-solution__next-btn
    right: 0

@media (max-width: 599px)
  .home-solution__item
    flex-direction: column
    text-align: center
    align-items: center
  .home-solution__texts
    align-items: center
</style>

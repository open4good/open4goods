<script setup lang="ts">
import { computed } from 'vue'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'

const solutionImageSrc = '/homepage/gain/nudger-screaming.webp'

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
        <v-col cols="12" md="6" class="home-solution__col home-solution__col--copy">
          <header class="home-section__header">
            <h2 id="home-solution-title">{{ sectionTitle }}</h2>
            <p class="home-section__subtitle subtitle-text">
              {{ sectionDescription }}
            </p>
          </header>
          <v-row class="home-solution__list" dense>
            <v-col v-for="item in props.benefits" :key="item.label" cols="12" md="6">
              <NudgerCard
                class="home-solution__item"
                border
                :flat-corners="['top-left']"
                :accent-corners="['bottom-left']"
              >
                <v-avatar class="home-solution__icon" size="60" color="surface">
                  <span aria-hidden="true">{{ item.emoji }}</span>
                </v-avatar>
                <div class="home-solution__texts">
                  <p class="home-solution__label">{{ item.label }}</p>
                  <p class="home-solution__description">
                    {{ item.description }}
                  </p>
                </div>
              </NudgerCard>
            </v-col>
          </v-row>
        </v-col>
        <v-col cols="12" md="6" class="home-solution__visual">
          <div class="home-solution__image-wrapper">
            <img
              :src="solutionImageSrc"
              :alt="sectionTitle"
              class="home-solution__image"
              loading="lazy"
              decoding="async"
            />
          </div>
        </v-col>
      </v-row>
    </div>
  </section>
</template>

<style scoped lang="sass">
  .home-solution__col
    display: flex;
    flex-direction: column;
    gap: clamp(0.875rem, 2vw, 1.25rem);

  .home-section
    padding-block: clamp(1.5rem, 3vw, 2.75rem)
    background: rgb(var(--v-theme-surface-default))

  .home-solution__list
    margin: 0
    padding: 0
    row-gap: clamp(1rem, 2.5vw, 1.5rem)


  .home-solution__item
    width: 100%;
    display: flex;
    gap: 1rem;
    align-items: center;

  .home-solution__item::after
    display: none

  .home-solution__icon
    font-size: clamp(1.65rem, 5vw, 2rem)
    //background: rgba(var(--v-theme-surface-primary-080), 0.6)
    border: 1px solid rgb(var(--v-theme-secondary))
    color: rgb(var(--v-theme-secondary))

  .home-solution
    display: flex;
    flex-direction: column;
    gap: clamp(0.875rem, 2vw, 1.25rem);

  .home-section__header
    display: flex;
    flex-direction: column;
    gap: 0.75rem;

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

  .home-solution__image
    position: relative
    z-index: 1
    width: min(66%, 320px)
    height: auto
    display: block
    margin-inline: auto

  @media (max-width: 599px)
    .home-solution__item
      flex-direction: column
      text-align: center
      align-items: center
    .home-solution__texts
      align-items: center
</style>

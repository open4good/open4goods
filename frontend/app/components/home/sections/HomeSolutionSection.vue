<script setup lang="ts">
import { computed } from 'vue'
import HomeSplitSection from './HomeSplitSection.vue'

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
  <HomeSplitSection
    id="home-solution"
    class="home-solution"
    :title="sectionTitle"
    :description="sectionDescription"
    :image="{
      src: '/images/home/nudger-screaming.webp',
      alt: sectionTitle,
      sizes: '(min-width: 960px) 306px, 60vw',
      width: 1024,
      height: 1536,
    }"
    visual-position="right"
  >
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
            <p class="home-solution__description">{{ item.description }}</p>
          </div>
        </v-sheet>
      </v-col>
    </v-row>
  </HomeSplitSection>
</template>

<style scoped lang="sass">
  .home-section
    padding-block: clamp(1.5rem, 3vw, 2.75rem)
    background: rgb(var(--v-theme-surface-default))

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
    //background: rgba(var(--v-theme-surface-primary-080), 0.6)
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

  @media (max-width: 599px)
    .home-solution__item
      flex-direction: column
      text-align: center
      align-items: center
    .home-solution__texts
      align-items: center
</style>

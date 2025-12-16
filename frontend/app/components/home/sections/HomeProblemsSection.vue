<script setup lang="ts">
import { computed } from 'vue'
import HomeSplitSection from './HomeSplitSection.vue'

type ProblemItem = {
  icon: string
  text: string
}

const props = defineProps<{
  items: ProblemItem[]
}>()

const { t } = useI18n()

const sectionTitle = computed(() => t('home.problems.title'))
const sectionDescription = computed(() => t('home.problems.description'))
const visualImage = computed(() => ({
  src: '/images/home/nudger-problem.webp',
  alt: sectionTitle.value,
  sizes: '(min-width: 960px) 360px, 70vw',
  width: 1024,
  height: 1536,
}))
</script>

<template>
  <HomeSplitSection
    id="home-problems"
    class="home-problems"
    :title="sectionTitle"
    :description="sectionDescription"
    :image="visualImage"
    visual-position="left"
  >
    <v-row class="home-problems__list" dense>
      <v-col
        v-for="item in props.items"
        :key="item.text"
        cols="12"
        class="home-problems__list-item card__nudger card__nudger--border card__nudger--radius_top-right_0 card__nudger--radius_bottom-right_50px"
      >
        <v-sheet class="home-problems__card" rounded="xl" elevation="0">
          <v-avatar class="home-problems__icon" color="surface" size="64">
            <v-icon :icon="item.icon" size="32" />
          </v-avatar>
          <p class="home-problems__text">{{ item.text }}</p>
        </v-sheet>
      </v-col>
    </v-row>
  </HomeSplitSection>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-split__content
  position: relative

.home-problems
  //background: rgba(var(--v-theme-surface-default), 0.98)

  .card__nudger
    margin-bottom: 1rem

    &:last-of-type
      margin-bottom: 0

.home-problems__list
  --v-gutter-y: clamp(1rem, 3vw, 1.5rem)

.home-problems__list-item
  display: flex

.home-problems__card
  width: 100%
  display: flex
  gap: 1rem
  align-items: center
  background: transparent!important

.home-problems__icon
  border: 1px solid rgb(var(--v-theme-secondary))
  color: rgb(var(--v-theme-secondary))

.home-problems__text
  margin: 0
  font-size: 1.05rem
  line-height: 1.5
  color: rgb(var(--v-theme-text-neutral-strong))

@media (min-width: 960px)
  .home-problems__list
    --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)

@media (max-width: 599px)
  .home-problems__card
    flex-direction: column
    text-align: center
</style>

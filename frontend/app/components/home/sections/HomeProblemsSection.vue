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
        sm="6"
        class="home-problems__list-item"
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
.home-problems
  background: rgba(var(--v-theme-surface-default), 0.98)

.home-problems__list
  --v-gutter-y: clamp(1rem, 3vw, 1.5rem)

.home-problems__list-item
  display: flex

.home-problems__card
  width: 100%
  display: flex
  gap: 1rem
  align-items: center
  padding: clamp(1.25rem, 3vw, 1.75rem)
  background: rgba(var(--v-theme-surface-default), 1)
  border-radius: clamp(1.25rem, 3vw, 1.75rem)
  box-shadow: 0 18px 28px rgba(var(--v-theme-shadow-primary-600), 0.08)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.25)

.home-problems__icon
  background: rgba(var(--v-theme-surface-primary-080), 0.7)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  color: rgba(var(--v-theme-hero-gradient-start), 0.95)

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

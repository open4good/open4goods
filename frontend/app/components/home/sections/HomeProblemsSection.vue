<script setup lang="ts">
import { computed } from 'vue'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'
import HomeSplitSection from './HomeSplitSection.vue'

type ProblemItem = {
  icon: string
  text: string
}

const props = defineProps<{
  items: ProblemItem[]
  reveal?: boolean
}>()

const { t } = useI18n()

const sectionTitle = computed(() => t('home.problems.title'))
const sectionDescription = computed(() => t('home.problems.description'))
const isVisible = computed(() => Boolean(props.reveal))
</script>

<template>
  <HomeSplitSection
    id="home-problems"
    class="home-problems"
    :title="sectionTitle"
    :description="sectionDescription"
    visual-position="left"
  >
    <v-row
      class="home-problems__list home-reveal-group"
      :class="{ 'is-ready': true, 'is-visible': isVisible }"
      dense
    >
      <v-col v-for="(item, index) in props.items" :key="item.text" cols="12">
        <NudgerCard
          class="home-problems__card home-hover-card home-reveal-item d-flex align-center ga-4 w-100"
          base-radius="24px"
          background="rgb(var(--v-theme-surface))"
          :shadow="true"
          :style="{
            '--reveal-delay': `${index * 90}ms`,
            border: '1px solid rgba(var(--v-theme-secondary), 0.2)',
          }"
        >
          <v-avatar class="home-problems__icon" color="surface" size="64">
            <v-icon :icon="item.icon" size="32" />
          </v-avatar>
          <p class="home-problems__text ma-0">{{ item.text }}</p>
        </NudgerCard>
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

.home-problems__list
  --v-gutter-y: clamp(1rem, 3vw, 1.5rem)

.home-problems__card
  // Layout now handled by utility classes: d-flex align-center ga-4 w-100

.home-problems__icon
  border: 1px solid rgb(var(--v-theme-secondary))
  color: rgb(var(--v-theme-secondary))

.home-problems__text
  // margin now handled by utility class: ma-0
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

<script setup lang="ts">
import { computed, ref } from 'vue'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'

const solutionImageSrc = '/homepage/gain/nudger-screaming.webp'

type SolutionBenefit = {
  emoji: string
  label: string
  description: string
}

const props = defineProps<{
  benefits: SolutionBenefit[]
  reveal?: boolean
}>()

const { t } = useI18n()

const sectionTitle = computed(() => t('home.solution.title'))
const sectionDescription = computed(() => t('home.solution.description'))
const isVisible = computed(() => Boolean(props.reveal))
const isImageTiltUnlocked = ref(false)

const unlockImageTilt = () => {
  if (isImageTiltUnlocked.value) {
    return
  }

  isImageTiltUnlocked.value = true
}
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
        <v-col cols="12" md="6" class="d-flex flex-column ga-3">
          <header class="d-flex flex-column ga-2">
            <h2 id="home-solution-title">{{ sectionTitle }}</h2>
            <p class="home-section__subtitle subtitle-text ma-0">
              {{ sectionDescription }}
            </p>
          </header>
          <v-row
            class="home-solution__list home-reveal-group"
            :class="{ 'is-ready': true, 'is-visible': isVisible }"
            dense
          >
            <v-col
              v-for="(item, index) in props.benefits"
              :key="item.label"
              cols="12"
              md="6"
            >
              <NudgerCard
                class="home-solution__item home-hover-card home-reveal-item d-flex align-center ga-4 w-100"
                border
                :flat-corners="['top-left']"
                :accent-corners="['bottom-left']"
                :style="{ '--reveal-delay': `${index * 90}ms` }"
              >
                <v-avatar class="home-solution__icon" size="60" color="surface">
                  <span aria-hidden="true">{{ item.emoji }}</span>
                </v-avatar>
                <div class="d-flex flex-column ga-1">
                  <p class="home-solution__label ma-0 font-weight-bold">
                    {{ item.label }}
                  </p>
                  <p class="home-solution__description ma-0">
                    {{ item.description }}
                  </p>
                </div>
              </NudgerCard>
            </v-col>
          </v-row>
        </v-col>
        <v-col cols="12" md="6" class="home-solution__visual">
          <div
            class="home-solution__image-wrapper"
            @pointerenter="unlockImageTilt"
          >
            <img
              :src="solutionImageSrc"
              :alt="sectionTitle"
              class="home-solution__image home-tilt-lock"
              :class="{ 'home-tilt-lock--unlocked': isImageTiltUnlocked }"
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
// .home-solution__col styles now handled by utility classes: d-flex flex-column ga-3

.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-solution__list
  margin: 0
  padding: 0
  row-gap: clamp(1rem, 2.5vw, 1.5rem)



.home-solution__item
  // d-flex align-center ga-4 w-100 now handled by utility classes
  background: transparent !important;

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

// .home-section__header styles now handled by utility classes: d-flex flex-column ga-2

// .home-solution__texts styles now handled by utility classes: d-flex flex-column ga-1

.home-solution__description
  // margin now handled by utility class: ma-0
  color: rgb(var(--v-theme-text-neutral-secondary))
  font-size: 0.95rem

.home-solution__label
  // margin, font-weight now handled by utility classes: ma-0 font-weight-bold
  font-size: clamp(1.05rem, 2.4vw, 1.25rem)
  color: rgb(var(--v-theme-text-neutral-strong))

.home-solution__image
  position: relative
  z-index: 1
  width: min(66%, 320px)
  height: auto
  display: block
  margin-inline: auto
  --tilt-default-angle: 7deg

@media (max-width: 599px)
  .home-solution__item

    flex-direction: column
    text-align: center
    align-items: center
  .home-solution__texts
    align-items: center
</style>

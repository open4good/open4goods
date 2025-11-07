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
        <header class="home-section__header">
          <h2 id="home-solution-title">{{ t('home.solution.title') }}</h2>
          <p class="home-section__subtitle">{{ t('home.solution.description') }}</p>
        </header>
        <v-row class="home-solution__list" tag="ul" align="stretch" justify="center">
          <v-col
            v-for="item in props.benefits"
            :key="item.label"
            cols="12"
            sm="6"
            md="3"
            class="home-solution__item"
            tag="li"
          >
            <v-card class="home-solution__card" variant="outlined">
              <span class="home-solution__emoji" aria-hidden="true">{{ item.emoji }}</span>
              <p class="home-solution__label">{{ item.label }}</p>
            </v-card>
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

.home-section__header
  max-width: 760px
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-solution__list
  --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
  --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

.home-solution__item
  display: flex
  list-style: none

.home-solution__card
  position: relative
  z-index: 1
  height: 100%
  width: 100%
  display: flex
  flex-direction: column
  gap: 1rem
  padding: clamp(1.75rem, 4vw, 2.5rem)
  border-radius: clamp(1.5rem, 4vw, 2.25rem)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.32)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.92), rgba(var(--v-theme-surface-default), 0.96))
  box-shadow: 0 18px 30px rgba(var(--v-theme-shadow-primary-600), 0.14)

.home-solution__emoji
  font-size: clamp(2.5rem, 7vw, 3.6rem)
  line-height: 1

.home-solution__label
  margin: 0
  font-size: clamp(1.1rem, 2.6vw, 1.45rem)
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

@media (max-width: 599px)
  .home-solution__card
    align-items: center
    text-align: center
</style>

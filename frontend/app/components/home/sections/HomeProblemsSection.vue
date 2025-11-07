<script setup lang="ts">
type ProblemItem = {
  icon: string
  text: string
}

const props = defineProps<{
  items: ProblemItem[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="home-section home-problems" aria-labelledby="home-problems-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <header class="home-section__header">
          <h2 id="home-problems-title">{{ t('home.problems.title') }}</h2>
        </header>
        <v-row class="home-problems__grid" align="stretch">
          <v-col
            v-for="item in props.items"
            :key="item.text"
            cols="12"
            md="6"
            lg="4"
            class="home-problems__col"
          >
            <article class="home-problems__card">
              <div class="home-problems__card-inner">
                <div class="home-problems__icon-wrapper" aria-hidden="true">
                  <v-icon class="home-problems__icon" :icon="item.icon" size="56" />
                </div>
                <p class="home-problems__text">{{ item.text }}</p>
              </div>
            </article>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(3rem, 6vw, 5rem)
  background: rgba(var(--v-theme-surface-default), 0.98)

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3rem)

.home-section__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-problems__grid
  --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
  --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

.home-problems__col
  display: flex

.home-problems__card
  position: relative
  width: 100%
  border-radius: clamp(1.5rem, 4vw, 2rem)
  background: linear-gradient(135deg, rgba(var(--v-theme-surface-primary-080), 0.8), rgba(var(--v-theme-surface-default), 0.95))
  box-shadow: 0 18px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)
  overflow: hidden

.home-problems__card-inner
  position: relative
  z-index: 1
  display: flex
  gap: 1.5rem
  align-items: center
  padding: clamp(1.75rem, 4vw, 2.5rem)

.home-problems__card::before
  content: ''
  position: absolute
  inset: 0
  background: rgba(var(--v-theme-surface-primary-050), 0.55)
  opacity: 0
  transition: opacity 0.3s ease

.home-problems__card:hover::before
  opacity: 1

.home-problems__icon-wrapper
  width: clamp(4rem, 8vw, 4.5rem)
  height: clamp(4rem, 8vw, 4.5rem)
  display: flex
  align-items: center
  justify-content: center
  border-radius: 1.5rem
  background: rgba(var(--v-theme-hero-gradient-start), 0.12)
  box-shadow: inset 0 0 0 1px rgba(var(--v-theme-hero-gradient-start), 0.18)

.home-problems__icon
  color: rgba(var(--v-theme-hero-gradient-start), 0.95)

.home-problems__text
  margin: 0
  font-size: 1.05rem
  line-height: 1.45
  color: rgb(var(--v-theme-text-neutral-strong))

@media (max-width: 599px)
  .home-problems__card-inner
    flex-direction: column
    text-align: center
</style>

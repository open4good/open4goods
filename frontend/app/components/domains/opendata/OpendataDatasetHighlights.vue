<script setup lang="ts">
interface DatasetCardFeature {
  icon?: string
  text: string
}

interface DatasetCard {
  id: string
  icon: string
  title: string
  description: string
  features: DatasetCardFeature[]
  ctaLabel: string
  ctaAriaLabel: string
  href: string
}

const props = defineProps<{
  title: string
  subtitle?: string
  cards: DatasetCard[]
}>()

const handleSubtitleClick = (event: MouseEvent) => {
  if (!import.meta.client || !props.subtitle) {
    return
  }

  const anchor = (event.target as HTMLElement | null)?.closest('[data-scroll-target]')
  const targetSelector = anchor?.getAttribute('data-scroll-target')

  if (anchor && targetSelector) {
    const target = document.querySelector(targetSelector)
    if (target instanceof HTMLElement) {
      event.preventDefault()
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }
}
</script>

<template>
  <section class="opendata-datasets" aria-labelledby="opendata-datasets-heading">
    <v-container max-width="lg">
      <div class="opendata-datasets__header">
        <h2 id="opendata-datasets-heading" class="opendata-datasets__title">{{ title }}</h2>
        <!-- eslint-disable vue/no-v-html -->
        <p
          v-if="subtitle"
          class="opendata-datasets__subtitle subtitle-text"
          @click="handleSubtitleClick"
          v-html="subtitle"
        ></p>
        <!-- eslint-enable vue/no-v-html -->
      </div>

      <v-row class="g-6" align="stretch" justify="center">
        <v-col
          v-for="card in cards"
          :key="card.id"
          cols="12"
          md="6"
          class="d-flex justify-center"
        >
          <v-card class="opendata-dataset-card" rounded="xl" elevation="6">
            <div class="opendata-dataset-card__icon" aria-hidden="true">
              <v-icon :icon="card.icon" size="40" />
            </div>
            <div class="opendata-dataset-card__content">
              <h3 class="opendata-dataset-card__title">{{ card.title }}</h3>
              <p class="opendata-dataset-card__description">{{ card.description }}</p>
              <ul class="opendata-dataset-card__features">
                <li v-for="feature in card.features" :key="feature.text">
                  <v-icon v-if="feature.icon" :icon="feature.icon" size="small" />
                  <span>{{ feature.text }}</span>
                </li>
              </ul>
            </div>
            <div class="opendata-dataset-card__cta">
              <v-btn
                :href="card.href"
                :aria-label="card.ctaAriaLabel"
                color="primary"
                variant="flat"
                size="large"
                append-icon="mdi-arrow-right"
              >
                {{ card.ctaLabel }}
              </v-btn>
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.opendata-datasets
  padding: clamp(3rem, 6vw, 5rem) 0
  background: rgba(var(--v-theme-surface-default), 1)

.opendata-datasets__header
  text-align: center
  max-width: 720px
  margin: 0 auto clamp(2rem, 5vw, 3.5rem)

.opendata-datasets__title
  margin: 0
  font-size: clamp(2rem, 4vw, 2.75rem)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-datasets__subtitle
  margin-top: 1rem
  margin-bottom: 0
  --subtitle-size: 1.05rem

.opendata-dataset-card
  display: flex
  flex-direction: column
  gap: 1.25rem
  padding: clamp(1.75rem, 4vw, 2.5rem)
  background: linear-gradient(160deg, rgba(var(--v-theme-surface-primary-080), 0.8), rgba(var(--v-theme-surface-default), 1))
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)
  width: 100%

.opendata-dataset-card__icon
  display: inline-flex
  width: 64px
  height: 64px
  border-radius: 20px
  align-items: center
  justify-content: center
  background: rgba(var(--v-theme-surface-primary-120), 0.9)
  color: rgba(var(--v-theme-primary), 1)

.opendata-dataset-card__content
  display: flex
  flex-direction: column
  gap: 0.75rem

.opendata-dataset-card__title
  margin: 0
  font-size: 1.5rem
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.opendata-dataset-card__description
  margin: 0
  font-size: 1rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.opendata-dataset-card__features
  list-style: none
  margin: 0
  padding: 0
  display: flex
  flex-direction: column
  gap: 0.5rem

.opendata-dataset-card__features li
  display: flex
  align-items: center
  gap: 0.5rem
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.opendata-dataset-card__cta
  margin-top: auto

@media (max-width: 959px)
  .opendata-dataset-card
    height: 100%

  .opendata-dataset-card__cta
    display: flex
    justify-content: flex-start
</style>


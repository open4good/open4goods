<script setup lang="ts">
interface Feature {
  title: string
  description: string
}

interface Props {
  mainTitle?: string
  features?: Feature[]
  showImage?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  mainTitle: '"Features ..."',
  features: () => [
    {
      title: 'Economies',
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor'
    },
    {
      title: 'Gain de temps',
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor'
    },
    {
      title: 'Réconcilier vos valeurs et vos achats',
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor'
    },
    {
      title: 'Assistance',
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor'
    }
  ],
  showImage: true
})
</script>

<template>
  <v-container class="bloc-features">
    <div class="bloc-features__content">
      <div class="bloc-features__text-section">
        <!-- Main Title -->
        <div class="bloc-features__title">
          {{ mainTitle }}
        </div>

        <!-- Features Grid -->
        <div class="bloc-features__features-grid">
          <!-- First row: 2 features -->
          <div class="bloc-features__features-row">
            <div class="bloc-features__feature-item">
              <h3 class="bloc-features__feature-title">{{ features[0].title }}</h3>
              <p class="bloc-features__feature-description">{{ features[0].description }}</p>
            </div>
            <div class="bloc-features__feature-item">
              <h3 class="bloc-features__feature-title">{{ features[1].title }}</h3>
              <p class="bloc-features__feature-description">{{ features[1].description }}</p>
            </div>
          </div>

          <!-- Second row: 2 features -->
          <div class="bloc-features__features-row">
            <div class="bloc-features__feature-item">
              <h3 class="bloc-features__feature-title">{{ features[2].title }}</h3>
              <p class="bloc-features__feature-description">{{ features[2].description }}</p>
            </div>
            <div class="bloc-features__feature-item">
              <h3 class="bloc-features__feature-title">{{ features[3].title }}</h3>
              <p class="bloc-features__feature-description">{{ features[3].description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Image placeholder -->
      <div v-if="showImage" class="bloc-features__image-section">
        <div class="bloc-features__image-placeholder" />
      </div>
    </div>
  </v-container>
</template>

<style scoped lang="sass">
.bloc-features
  background-color: white
  padding: 64px 19px 69px
  width: 100%
  display: flex
  justify-content: center
  align-items: center

  &__content
    display: flex
    gap: 42px
    align-items: flex-start
    width: 100%
    max-width: 1600px

  &__text-section
    display: flex
    flex-direction: column
    gap: 42px
    flex: 1

  &__title
    font-family: 'Roboto', sans-serif
    font-weight: 700
    font-size: 50px
    line-height: 1.348
    color: #000
    word-wrap: break-word

  &__features-grid
    display: flex
    flex-direction: column
    gap: 42px

  &__features-row
    display: flex
    gap: 42px
    align-items: flex-start

  &__feature-item
    flex: 1
    display: flex
    flex-direction: column
    gap: 17px
    min-width: 0

  &__feature-title
    font-family: 'Roboto', sans-serif
    font-weight: 700
    font-size: 43px
    line-height: 1.348
    color: #000
    margin: 0
    word-wrap: break-word

  &__feature-description
    font-family: 'Roboto', sans-serif
    font-weight: 300
    font-size: 29px
    line-height: 1.348
    color: #000
    margin: 0
    word-wrap: break-word
    max-width: 349px

  &__image-section
    flex-shrink: 0
    width: 593px

  &__image-placeholder
    width: 100%
    height: 365px
    background-color: #d9d9d9
    border-radius: 22px

@media (max-width: 1279px)
  .bloc-features
    &__content
      flex-direction: column

    &__image-section
      width: 100%
      max-width: 593px

    &__title
      font-size: clamp(2.5rem, 8vw, 80px)

    &__feature-title
      font-size: clamp(1.5rem, 4vw, 43px)

    &__feature-description
      font-size: clamp(1rem, 2vw, 29px)

@media (max-width: 768px)
  .bloc-features
    &__features-row
      flex-direction: column
</style>

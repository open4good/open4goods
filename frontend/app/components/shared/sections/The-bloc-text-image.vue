<template>
  <v-container class="the-bloc-text-image">
    <v-row align="center" class="the-bloc-text-image__row">
      <v-col
        cols="12"
        md="4"
        :order="imageFirst ? 2 : 1"
        class="the-bloc-text-image__image-col"
      >
        <div class="the-bloc-text-image__image-container">
          <img
            v-if="imageSrc"
            :src="imageSrc"
            :alt="imageAlt"
            class="the-bloc-text-image__image"
          />
          <v-skeleton-loader
            v-else
            type="image"
            class="the-bloc-text-image__skeleton"
          />
        </div>
      </v-col>

      <v-col
        cols="12"
        md="8"
        :order="imageFirst ? 1 : 2"
        class="the-bloc-text-image__content-col"
      >
        <div class="the-bloc-text-image__content">
          <!-- Title -->
          <h2 v-if="title" class="the-bloc-text-image__title">
            {{ title }}
          </h2>

          <!-- Description -->
          <p v-if="description" class="the-bloc-text-image__description">
            {{ description }}
          </p>

          <!-- CTA Button -->
          <div v-if="ctaLabel" class="the-bloc-text-image__cta-wrapper">
            <v-btn
              :href="ctaHref"
              rounded="lg"
              class="the-bloc-text-image__cta"
              @click="handleCtaClick"
            >
              {{ ctaLabel }}
            </v-btn>
          </div>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
interface Props {
  /** Image source URL */
  imageSrc?: string
  /** Image alt text */
  imageAlt?: string
  /** Title of the content block */
  title?: string
  /** Description text */
  description?: string
  /** Call-to-action button label */
  ctaLabel?: string
  /** CTA button href (for navigation) */
  ctaHref?: string
  /** Whether image appears first (right) instead of last (left) */
  imageFirst?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  imageSrc: undefined,
  imageAlt: 'Image illustration',
  title: undefined,
  description: undefined,
  ctaLabel: undefined,
  ctaHref: '#',
  imageFirst: false
})

const emit = defineEmits<{
  /** Emitted when CTA button is clicked */
  ctaClicked: []
}>()

const handleCtaClick = (event: Event) => {
  emit('ctaClicked')
}
</script>

<style lang="sass" scoped>
.the-bloc-text-image
  background: white
  width: 100%
  min-height: 675px
  padding: 64px 0 69px 0

  &__row
    height: 100%

  &__image-col
    display: flex
    justify-content: center
    align-items: center

  &__image-container
    width: 100%
    max-width: 462px
    height: 549px
    background: #d9d9d9
    border-radius: 34px
    overflow: hidden
    display: flex
    align-items: center
    justify-content: center

  &__image
    width: 100%
    height: 100%
    object-fit: contain
    display: block

  &__skeleton
    width: 100%
    height: 100%

  &__content-col
    display: flex
    justify-content: center
    align-items: center

  &__content
    width: 100%
    max-width: 1065px
    display: flex
    flex-direction: column
    align-items: center
    gap: 42px
    text-align: center

  &__title
    font-family: 'Roboto', sans-serif
    font-weight: 700
    font-size: 49px
    line-height: 1.348
    color: black
    margin: 0
    width: 100%

  &__description
    font-family: 'Roboto', sans-serif
    font-weight: 300
    font-size: 29px
    line-height: 1.348
    color: black
    margin: 0
    width: 531px

  &__cta-wrapper
    display: flex
    justify-content: center

  &__cta
    background: #d9d9d9
    color: black
    font-family: 'Roboto', sans-serif
    font-weight: 300
    font-size: 29px
    line-height: 1.348
    text-transform: none
    padding: 20px 15px
    min-width: 305px
    height: 79px
    border-radius: 17px

    &:hover
      background: #c9c9c9

  // Mobile responsive adjustments
  @media (max-width: 960px)
    min-height: auto
    padding: 40px 0
    display: flex
    align-items: center
    justify-content: center

    &__row
      align-items: center

    &__image-col, &__content-col
      min-height: 300px
      display: flex
      align-items: center
      justify-content: center

    &__image-container
      height: 400px

    &__title
      font-size: 36px

    &__description
      font-size: 20px
      width: 100%
      max-width: 531px

    &__cta
      font-size: 20px
      min-width: 250px
      height: 60px
</style>

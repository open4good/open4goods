<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    id: string
    title: string
    description?: string
    visualPosition?: 'left' | 'right'
    image?: {
      src: string
      alt: string
      sizes?: string
      loading?: 'eager' | 'lazy'
      width?: number
      height?: number
    }
  }>(),
  {
    description: undefined,
    image: undefined,
    visualPosition: 'right',
  },
)

const titleId = computed(() => `${props.id}-title`)
const sectionClasses = computed(() => [
  'home-section',
  'home-split',
  props.visualPosition === 'left' ? 'home-split--visual-left' : 'home-split--visual-right',
])
</script>

<template>
  <section :id="props.id" :class="sectionClasses" :aria-labelledby="titleId" v-bind="$attrs">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <v-row class="home-split__content" align="center" justify="space-between">
          <v-col cols="12" md="6" class="home-split__col home-split__col--copy">
              <slot name="eyebrow" />
              <p class="home-hero__subtitle" :id="titleId">{{ props.title }}</p>
              <p v-if="props.description" class="home-section__subtitle">{{ props.description }}</p>
              <div class="home-section__body">
                <slot />
              </div>
          </v-col>
          <v-col cols="12" md="6" class="home-split__col home-split__col--visual card__nudger">
            <div class="home-split__visual" role="presentation">
              <slot name="visual">
                <img
                  v-if="props.image?.src"
                  :src="props.image.src"
                  :alt="props.image.alt"
                  class="home-split__image"
                  :loading="props.image.loading ?? 'lazy'"
                  :width="props.image.width"
                  :height="props.image.height"
                  decoding="async"
                />
              </slot>
            </div>
          </v-col>
        </v-row>
      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
.home-section
  padding-block: clamp(1.5rem, 3vw, 2.75rem)
  background: rgb(var(--v-theme-surface-default))

.home-section__container
  padding-inline: clamp(1.5rem, 5vw, 4rem)

.home-hero__subtitle
  text-align: left

.home-section__inner
  max-width: 1180px
  margin: 0 auto
  display: flex
  flex-direction: column
  gap: clamp(2rem, 5vw, 3.25rem)

.home-section__header
  display: flex
  flex-direction: column
  gap: 0.75rem

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.home-section__body
  display: flex
  flex-direction: column
  gap: clamp(1.5rem, 4vw, 2.5rem)

.home-split__content
  row-gap: clamp(2rem, 5vw, 3rem)
  //background: rgb(var(--v-theme-hero-overlay-strong))

.home-split__col--copy
  display: flex
  flex-direction: column
  gap: clamp(0.875rem, 2vw, 1.25rem)

.home-split__col--visual
  display: flex
  justify-content: center
  position: sticky;
  top: 64px // hauteur header menu

.home-split__visual
  position: relative
  width: min(100%, 460px)
  display: flex
  justify-content: center
  align-items: center

.home-split__image
  position: relative
  z-index: 1
  width: min(66%, 320px)
  height: auto
  display: block
  margin-inline: auto

.home-split--visual-left .home-split__col--visual
  order: -1

@media (max-width: 959px)
  .home-split__col--visual
    order: -1

</style>

<script setup lang="ts">
import { computed } from 'vue'
import gainImageSrc from '~/assets/homepage/gain/nudger-screaming.webp'
import painImageSrc from '~/assets/homepage/pain/nudger-problem.webp'

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
  }
)

const { t } = useI18n()
const titleId = computed(() => `${props.id}-title`)
const fallbackImage = computed(() => {
  const isPainVisual = props.visualPosition === 'left'

  return {
    src: isPainVisual ? painImageSrc : gainImageSrc,
    alt: isPainVisual ? t('home.split.painAlt') : t('home.split.gainAlt'),
    sizes: '(min-width: 960px) 320px, 70vw',
    loading: 'lazy' as const,
  }
})
const resolvedImage = computed(() => props.image ?? fallbackImage.value)
const resolvedImageClass = computed(() =>
  props.image ? null : props.visualPosition === 'right'
    ? 'home-split__image--gain'
    : 'home-split__image--pain'
)
const isLocalAsset = computed(() =>
  Boolean(resolvedImage.value?.src?.startsWith('/'))
)
const sectionClasses = computed(() => [
  'home-section',
  'home-split',
  props.visualPosition === 'left'
    ? 'home-split--visual-left'
    : 'home-split--visual-right',
])
</script>

<template>
  <section
    :id="props.id"
    :class="sectionClasses"
    :aria-labelledby="titleId"
    v-bind="$attrs"
  >
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
        <v-row
          class="home-split__content"
          align="center"
          justify="space-between"
        >
          <v-col cols="12" md="6" class="home-split__col home-split__col--copy">
            <header class="home-section__header">
              <slot name="eyebrow" />
              <h2 :id="titleId">{{ props.title }}</h2>
              <!-- eslint-disable vue/no-v-html -->
              <p
                v-if="props.description"
                class="home-section__subtitle subtitle-text"
                v-html="props.description"
              ></p>
              <!-- eslint-enable vue/no-v-html -->
            </header>
            <div class="home-section__body">
              <slot />
            </div>
          </v-col>
          <v-col
            cols="12"
            md="6"
            class="home-split__col home-split__col--visual"
          >
            <div class="home-split__visual" role="presentation">
              <slot name="visual">
                <img
                  v-if="resolvedImage?.src && isLocalAsset"
                  :src="resolvedImage.src"
                  :alt="resolvedImage.alt"
                  :class="['home-split__image', resolvedImageClass]"
                  :loading="resolvedImage.loading ?? 'lazy'"
                  :width="resolvedImage.width"
                  :height="resolvedImage.height"
                  decoding="async"
                />
                <NuxtImg
                  v-else-if="resolvedImage?.src"
                  :src="resolvedImage.src"
                  :alt="resolvedImage.alt"
                  :class="['home-split__image', resolvedImageClass]"
                  :sizes="resolvedImage.sizes ?? '(min-width: 960px) 320px, 70vw'"
                  :loading="resolvedImage.loading ?? 'lazy'"
                  :width="resolvedImage.width"
                  :height="resolvedImage.height"
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
  position: relative

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

.home-split__image--gain
  transform: rotate(8deg)

.home-split--visual-left .home-split__col--visual
  order: -1

@media (max-width: 959px)
  .home-split__col--visual
    order: -1
</style>

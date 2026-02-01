<script setup lang="ts">
import { computed } from 'vue'
import painImageSrc from '~/assets/homepage/pain/nudger-problem.webp'
import NudgerCard from '~/components/shared/cards/NudgerCard.vue'

const gainImageSrc = '/homepage/gain/nudger-screaming.webp'

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
  props.image
    ? null
    : props.visualPosition === 'right'
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
          <v-col cols="12" md="6" class="d-flex flex-column ga-3">
            <header class="d-flex flex-column ga-2">
              <slot name="eyebrow" />
              <h2 :id="titleId">{{ props.title }}</h2>
              <!-- eslint-disable vue/no-v-html -->
              <p
                v-if="props.description"
                class="home-section__subtitle subtitle-text ma-0"
                v-html="props.description"
              ></p>
              <!-- eslint-enable vue/no-v-html -->
            </header>
            <div class="d-flex flex-column ga-6">
              <slot />
            </div>
          </v-col>
          <v-col
            cols="12"
            md="6"
            class="home-split__col--visual d-flex justify-center"
          >
            <div
              class="home-split__visual d-flex justify-center align-center"
              style="position: relative; width: min(100%, 460px)"
              role="presentation"
            >
              <NudgerCard
                :flat-corners="[]"
                :accent-corners="[]"
                base-radius="24px"
                padding="0"
                :border="false"
                :shadow="false"
                :hoverable="false"
                background="transparent"
                class="home-split__card"
              >
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
                    :sizes="
                      resolvedImage.sizes ?? '(min-width: 960px) 320px, 70vw'
                    "
                    :loading="resolvedImage.loading ?? 'lazy'"
                    :width="resolvedImage.width"
                    :height="resolvedImage.height"
                    decoding="async"
                  />
                </slot>
              </NudgerCard>
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

// .home-section__header styles now handled by utility classes: d-flex flex-column ga-2

.home-section__subtitle
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

// .home-section__body styles now handled by utility classes: d-flex flex-column ga-6

.home-split__content
  row-gap: clamp(2rem, 5vw, 3rem)
  //background: rgb(var(--v-theme-hero-overlay-strong))

// .home-split__col--copy styles now handled by utility classes: d-flex flex-column ga-3

.home-split__col--visual
  // d-flex justify-center now handled by utility classes
  position: sticky
  top: 64px // hauteur header menu

// .home-split__visual styles now handled inline and by utility classes: d-flex justify-center align-center

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

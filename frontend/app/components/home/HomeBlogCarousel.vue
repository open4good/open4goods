<script setup lang="ts">
import { computed } from 'vue'
import { useDisplay } from 'vuetify'
import type { BlogPostDto } from '~~/shared/api-client'

type EnrichedBlogSlide = BlogPostDto & {
  formattedDate?: string
  link?: string
  isExternal?: boolean
}

const props = defineProps<{
  items: EnrichedBlogSlide[]
  loading?: boolean
}>()

const { t } = useI18n()
const localePath = useLocalePath()
const display = useDisplay()

const slidesPerView = computed(() => {
  if (display.lgAndUp.value) {
    return 3
  }

  if (display.mdAndUp.value) {
    return 2
  }

  return 1
})

const chunkItems = <T,>(input: readonly T[], size: number) => {
  if (size <= 0) {
    return [input.slice()]
  }

  const chunks: T[][] = []
  for (let index = 0; index < input.length; index += size) {
    chunks.push(input.slice(index, index + size))
  }

  return chunks
}

const resolveArticleLink = (article: BlogPostDto) => {
  const rawUrl = article.url?.trim()

  if (!rawUrl) {
    return localePath('blog')
  }

  if (/^https?:\/\//i.test(rawUrl)) {
    try {
      const parsedUrl = new URL(rawUrl)
      const slug = parsedUrl.pathname.split('/').filter(Boolean).pop()
      if (slug) {
        return localePath({ name: 'blog-slug', params: { slug } })
      }
    } catch (error) {
      console.warn('Failed to parse blog article URL', rawUrl, error)
    }

    return rawUrl
  }

  if (rawUrl.startsWith('/')) {
    return rawUrl
  }

  return localePath({ name: 'blog-slug', params: { slug: rawUrl } })
}

const enrichedItems = computed(() =>
  props.items.map(item => {
    const link = resolveArticleLink(item)
    const isExternal = /^https?:\/\//i.test(link)
    return {
      ...item,
      link,
      isExternal,
    }
  })
)

const chunkedItems = computed(() =>
  chunkItems(enrichedItems.value, slidesPerView.value)
)
const shouldShowArrows = computed(() => chunkedItems.value.length > 1)
const shouldCycle = computed(
  () => enrichedItems.value.length > slidesPerView.value
)
</script>

<template>
  <div class="home-blog-carousel">
    <v-skeleton-loader
      v-if="loading"
      type="heading, image, paragraph"
      class="home-blog-carousel__skeleton"
    />
    <v-alert
      v-else-if="!items.length"
      type="info"
      variant="tonal"
      class="home-blog-carousel__empty"
      border="start"
    >
      {{ t('home.blog.emptyState') }}
    </v-alert>
    <v-carousel
      v-else
      class="home-blog-carousel__carousel"
      :show-arrows="shouldShowArrows"
      :cycle="shouldCycle"
      :interval="7500"
      hide-delimiter-background
      height="auto"
      :aria-label="t('home.blog.carouselAriaLabel')"
    >
      <v-carousel-item
        v-for="(slide, index) in chunkedItems"
        :key="`blog-slide-${index}`"
      >
        <v-row class="home-blog-carousel__row" no-gutters>
          <v-col
            v-for="article in slide"
            :key="article.url ?? article.title ?? index"
            cols="12"
            md="6"
            lg="4"
          >
            <component
              :is="article.isExternal ? 'a' : NuxtLink"
              :to="!article.isExternal ? article.link : undefined"
              :href="article.isExternal ? article.link : undefined"
              class="home-blog-carousel__link"
              :target="article.isExternal ? '_blank' : undefined"
              :rel="article.isExternal ? 'noopener' : undefined"
            >
              <v-card
                class="home-blog-carousel__card"
                variant="elevated"
                elevation="6"
              >
                <div class="home-blog-carousel__media">
                  <v-img
                    v-if="article.image"
                    :src="article.image"
                    :alt="article.title ?? ''"
                    cover
                    height="160"
                    class="home-blog-carousel__image"
                  />
                  <div
                    v-else
                    class="home-blog-carousel__icon"
                    aria-hidden="true"
                  >
                    <v-icon icon="mdi-post-outline" size="52" />
                  </div>
                </div>
                <div class="home-blog-carousel__content">
                  <p class="home-blog-carousel__date">
                    {{ article.formattedDate ?? '' }}
                  </p>
                  <h3 class="home-blog-carousel__title">{{ article.title }}</h3>
                  <p class="home-blog-carousel__summary">
                    {{ article.summary }}
                  </p>
                  <span class="home-blog-carousel__cta">{{
                    t('home.blog.readMore')
                  }}</span>
                </div>
              </v-card>
            </component>
          </v-col>
        </v-row>
      </v-carousel-item>
    </v-carousel>
  </div>
</template>

<style scoped lang="sass">
.home-blog-carousel
  position: relative

  &__skeleton
    border-radius: 1.5rem
    overflow: hidden

  &__empty
    border-radius: 1.5rem

  &__carousel
    border-radius: clamp(1.5rem, 4vw, 2rem)
    overflow: hidden
    background: transparent

  &__row
    gap: 1.5rem
    padding: clamp(1.5rem, 4vw, 2rem)

  &__link
    text-decoration: none
    display: block
    height: 100%
    color: inherit

  &__card
    height: 100%
    display: flex
    flex-direction: column
    border-radius: clamp(1.25rem, 3vw, 1.75rem)
    background: rgba(var(--v-theme-surface-default), 0.92)
    transition: transform 0.25s ease, box-shadow 0.25s ease

    &:hover
      transform: translateY(-6px)
      box-shadow: 0 24px 36px rgba(var(--v-theme-shadow-primary-600), 0.18)

  &__media
    position: relative
    overflow: hidden
    border-radius: clamp(1.25rem, 3vw, 1.75rem) clamp(1.25rem, 3vw, 1.75rem) 0 0

  &__image
    object-fit: cover

  &__icon
    display: flex
    align-items: center
    justify-content: center
    height: 160px
    background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.18), rgba(var(--v-theme-hero-gradient-end), 0.18))
    color: rgba(var(--v-theme-hero-gradient-start), 0.95)

  &__content
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: clamp(1.25rem, 3vw, 1.75rem)

  &__date
    margin: 0
    color: rgb(var(--v-theme-text-neutral-soft))
    font-size: 0.95rem

  &__title
    margin: 0
    font-size: clamp(1.15rem, 2vw, 1.35rem)
    color: rgb(var(--v-theme-text-neutral-strong))

  &__summary
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))
    flex-grow: 1

  &__cta
    font-weight: 600
    color: rgba(var(--v-theme-hero-gradient-end), 0.95)
</style>

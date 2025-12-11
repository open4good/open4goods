<script setup lang="ts">
  import { toRefs } from 'vue'

  interface BlogItem {
    title?: string | null
    summary?: string | null
    formattedDate?: string
    image?: string | null
    link: string
    hasImage: boolean
  }

  const props = defineProps<{
    loading: boolean
    featuredItem: BlogItem | null
    secondaryItems: BlogItem[]
  }>()

  const { loading, featuredItem, secondaryItems } = toRefs(props)

  const { t } = useI18n()
  const localePath = useLocalePath()

  const featuredFallbackIconSize = 68
  const secondaryFallbackIconSize = 48
</script>

<template>
  <section class="home-section home-blog" aria-labelledby="home-blog-title">
    <v-container fluid class="home-section__container">
      <div class="home-section__inner">
          <p id="home-blog-title" class="home-hero__subtitle">{{ t('home.blog.title') }}</p>
          <p class="home-section__subtitle text-center">{{ t('home.blog.subtitle') }}</p>
          <v-btn
            class="home-section__cta  nudger_degrade-defaut mx-auto"
            :to="localePath({ name: 'blog' })"
            color="primary"
            variant="tonal"
            size="large"
          >
            {{ t('home.blog.cta') }}
          </v-btn>

        <div v-if="loading" class="home-blog__skeletons">
          <v-skeleton-loader
            v-for="index in 3"
            :key="`blog-skeleton-${index}`"
            type="image, article"
            class="home-blog__skeleton"
          />
        </div>
        <template v-else>
          <div v-if="featuredItem" class="home-blog__featured">
            <NuxtLink :to="featuredItem.link" class="home-blog__featured-link">
              <article class="home-blog__featured-card">
                <div class="home-blog__media" aria-hidden="true">
                  <v-img
                    v-if="featuredItem.hasImage"
                    :src="featuredItem.image"
                    :alt="featuredItem.title ?? ''"
                    cover
                  />
                  <div v-else class="home-blog__placeholder">
                    <v-icon icon="mdi-post-outline" :size="featuredFallbackIconSize" />
                  </div>
                </div>
                <div class="home-blog__content">
                  <p class="home-blog__date">{{ featuredItem.formattedDate }}</p>
                  <h3 class="home-blog__title home-hero__subtitle text-left">{{ featuredItem.title }}</h3>
                  <p class="home-blog__summary">{{ featuredItem.summary }}</p>
                  <span class="home-blog__link-label">{{ t('home.blog.readMore') }}</span>
                </div>
              </article>
            </NuxtLink>
          </div>

          <v-row
            v-if="secondaryItems.length"
            class="home-blog__grid"
            align="stretch"
          >
            <v-col
              v-for="article in secondaryItems"
              :key="article.link"
              cols="12"
              sm="6"
              md="5"
              lg="4"
              class="home-blog__col"
            >
              <NuxtLink :to="article.link" class="home-blog__item">
                <article class="home-blog__card">
                  <div class="home-blog__media" aria-hidden="true">
                    <v-img
                      v-if="article.hasImage"
                      :src="article.image"
                      :alt="article.title ?? ''"
                      cover
                    />
                    <div v-else class="home-blog__placeholder">
                      <v-icon icon="mdi-post-outline" :size="secondaryFallbackIconSize" />
                    </div>
                  </div>
                  <div class="home-blog__content">
                    <p class="home-blog__date">{{ article.formattedDate }}</p>
                    <h3 class="home-blog__title">{{ article.title }}</h3>
                    <p class="home-blog__summary">{{ article.summary }}</p>
                    <span class="home-blog__link-label">{{ t('home.blog.readMore') }}</span>
                  </div>
                </article>
              </NuxtLink>
            </v-col>
          </v-row>

          <v-alert
            v-else-if="!featuredItem"
            type="info"
            variant="tonal"
            border="start"
            class="home-blog__empty"
          >
            {{ t('home.blog.emptyState') }}
          </v-alert>
        </template>

      </div>
    </v-container>
  </section>
</template>

<style scoped lang="sass">
  .home-section
    padding-block: clamp(1.5rem, 3vw, 2.75rem)
    background: rgb(var(--v-theme-surface-default))

  .home-section__container
    padding-inline: 0

  .home-section__inner
    max-width: 1180px
    margin: 0 auto
    display: flex
    flex-direction: column
    gap: clamp(0.875rem, 2vw, 1.25rem);

  .home-section__header
    max-width: 760px
    display: flex
    flex-direction: column
    gap: 0.75rem

  .home-section__cta
    align-self: flex-start
    margin-top: 0.35rem
    border-radius: 999px
    font-weight: 600

  .home-section__subtitle
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))

  .home-blog__skeletons
    display: grid
    gap: 1.5rem
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr))

  .home-blog__skeleton
    border-radius: clamp(1.25rem, 3vw, 1.75rem)

  .home-blog__featured
    display: flex

  .home-blog__featured-link
    flex: 1
    text-decoration: none
    color: inherit

  .home-blog__featured-card
    display: grid
    gap: clamp(1.5rem, 4vw, 2rem)
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr))
    border-radius: clamp(1.5rem, 4vw, 2.2rem)
    overflow: hidden
    background: rgba(var(--v-theme-surface-default), 0.98)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.28)
    box-shadow: 0 22px 36px rgba(var(--v-theme-shadow-primary-600), 0.14)
    transition: transform 0.25s ease, box-shadow 0.25s ease

  .home-blog__featured-link:hover .home-blog__featured-card
    transform: translateY(-6px)
    box-shadow: 0 28px 44px rgba(var(--v-theme-shadow-primary-600), 0.18)

  .home-blog__grid
    --v-gutter-x: clamp(1.5rem, 4vw, 2.5rem)
    --v-gutter-y: clamp(1.5rem, 4vw, 2.5rem)

  .home-blog__col
    display: flex

  .home-blog__item
    text-decoration: none
    color: inherit
    display: flex
    flex: 1

  .home-blog__card
    height: 100%
    flex: 1
    display: flex
    flex-direction: column
    border-radius: clamp(1.25rem, 3vw, 1.85rem)
    overflow: hidden
    background: rgba(var(--v-theme-surface-default), 0.96)
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.28)
    box-shadow: 0 18px 28px rgba(var(--v-theme-shadow-primary-600), 0.12)
    transition: transform 0.25s ease, box-shadow 0.25s ease

  .home-blog__item:hover .home-blog__card
    transform: translateY(-6px)
    box-shadow: 0 26px 40px rgba(var(--v-theme-shadow-primary-600), 0.16)

  .home-blog__media
    position: relative
    aspect-ratio: 16 / 10
    background: rgba(var(--v-theme-surface-primary-050), 0.8)

  .home-blog__media :deep(.v-img)
    width: 100%
    height: 100%
    object-fit: cover

  .home-blog__placeholder
    width: 100%
    height: 100%
    display: flex
    align-items: center
    justify-content: center
    color: rgba(var(--v-theme-hero-gradient-start), 0.7)

  .home-blog__content
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: clamp(1.5rem, 4vw, 2rem)

  .home-blog__date
    margin: 0
    font-size: 0.95rem
    color: rgb(var(--v-theme-text-neutral-secondary))

  .home-blog__title
    margin: 0
    font-size: 1.25rem
    font-weight: 600
    color: rgb(var(--v-theme-text-neutral-strong))

  .home-blog__summary
    margin: 0
    color: rgb(var(--v-theme-text-neutral-secondary))

  .home-blog__link-label
    margin-top: auto
    font-weight: 600
    color: rgba(var(--v-theme-hero-gradient-end), 0.95)

  .home-blog__empty
    border-radius: clamp(1.25rem, 3vw, 1.75rem)


  @media (max-width: 959px)
    .home-blog__featured-card
      grid-template-columns: 1fr
</style>
<template>
  <HeroSurface
    tag="section"
    class="category-hero"
    :aria-labelledby="headingId"
    data-testid="category-hero"
    variant="halo"
  >
    <v-sheet
      class="category-hero__wrapper"
      :class="{
        'category-hero__wrapper--solo': !hasInfoCard && !shouldShowImage,
      }"
      elevation="0"
    >
      <div class="category-hero__content">
        <div class="category-hero__copy">
          <h1
            :id="headingId"
            class="category-hero__title"
          >
            {{ title }}
          </h1>
          <CategoryNavigationBreadcrumbs
            v-if="heroBreadcrumbs.length"
            v-bind="heroBreadcrumbProps"
            class="category-hero__breadcrumbs animate-in-up"
            style="--delay: 250ms"
          />
          <div
            v-if="description"
            class="category-hero__description animate-in-up"
            style="--delay: 300ms"
          >
            <MDC :value="description" />
          </div>

          <v-row
            density="comfortable"
            class="category-hero__actions-row animate-in-up"
            style="--delay: 400ms"
          >
            <v-col cols="12" class="flex-grow-1">
              <slot name="actions" />
            </v-col>
          </v-row>
        </div>
      </div>

      <aside
        v-if="hasInfoCard"
        class="category-hero__info-card animate-in-up"
        style="--delay: 350ms"
      >
        <h2 v-if="rightInfoCard?.title" class="category-hero__info-title">
          {{ rightInfoCard.title }}
        </h2>
        <div
          v-if="rightInfoCard?.body"
          class="category-hero__info-body"
        >
          <MDC :value="rightInfoCard.body" />
        </div>
      </aside>

      <div
        v-else-if="shouldShowImage"
        class="category-hero__media"
        aria-hidden="true"
      >
        <v-img :src="image" alt="" class="category-hero__image" cover>
          <template #placeholder>
            <v-skeleton-loader type="image" class="h-100" />
          </template>
        </v-img>
      </div>
    </v-sheet>
  </HeroSurface>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import type {
  CategoryBreadcrumbItemDto,
  VerticalSubCategoryHeroBlockDto,
} from '~~/shared/api-client'

import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'

type HeroBreadcrumbItem = {
  title: string
  link?: string
}

const props = withDefaults(
  defineProps<{
    title: string
    description?: string | null
    image?: string | null
    breadcrumbs?: CategoryBreadcrumbItemDto[]
    eyebrow?: string | null
    showImage?: boolean
    rightInfoCard?: VerticalSubCategoryHeroBlockDto | null
  }>(),
  {
    description: null,
    image: null,
    breadcrumbs: () => [],
    eyebrow: null,
    showImage: true,
    rightInfoCard: null,
  }
)

const headingId = useId()
const { t } = useI18n()

const breadcrumbs = computed(() => props.breadcrumbs ?? [])
const hasInfoCard = computed(
  () =>
    Boolean(props.rightInfoCard?.title?.trim()) ||
    Boolean(props.rightInfoCard?.body?.trim())
)
const shouldShowImage = computed(() => Boolean(props.image && props.showImage))

const heroBreadcrumbs = computed<HeroBreadcrumbItem[]>(() => {
  const items = breadcrumbs.value
    .map(item => ({
      title: item.title?.trim().length
        ? item.title
        : item.link?.trim().length
          ? item.link
          : t('category.hero.missingBreadcrumbTitle'),
      link: item.link?.trim().length ? item.link : undefined,
    }))
    .filter(item => item.title.trim().length)

  if (!items.length) {
    return []
  }

  return items.map((item, index) => ({
    ...item,
    link: index === items.length - 1 ? undefined : item.link,
  }))
})

const heroBreadcrumbProps = computed(() => ({
  items: heroBreadcrumbs.value,
  ariaLabel: t('category.hero.breadcrumbAriaLabel'),
}))

defineExpose({ headingId, t })
</script>

<style scoped lang="sass">
.category-hero
  position: relative
  display: block
  margin-bottom: 2rem

  &__wrapper
    position: relative
    display: grid
    grid-template-columns: minmax(0, 1fr)
    gap: 1.5rem
    overflow: hidden
    min-height: clamp(160px, 26vw, 240px)
    background: rgba(var(--v-theme-surface-glass), 0.92)
    color: rgb(var(--v-theme-text-neutral-strong))
    padding: clamp(1.5rem, 4vw + 1rem, 3rem)
    border-radius: 0
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
    backdrop-filter: blur(18px)

  &__wrapper--solo
    justify-items: center
    text-align: center

  &__content
    display: flex
    flex-direction: column
    gap: 1rem
    max-width: none
    width: 100%

  &__wrapper--solo &__content
    max-width: 840px
    align-items: center

  &__breadcrumbs
    display: inline-flex
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

    :deep(.category-navigation-breadcrumbs)
      color: inherit

    :deep(.category-navigation-breadcrumbs__separator)
      color: rgba(var(--v-theme-text-neutral-secondary), 0.85)

    :deep(.category-navigation-breadcrumbs__current)
      color: rgba(var(--v-theme-text-neutral-strong), 1)

    :deep(.category-navigation-breadcrumbs__link)
      color: inherit

    :deep(.category-navigation-breadcrumbs__link--interactive:hover),
    :deep(.category-navigation-breadcrumbs__link--interactive:focus-visible)
      color: rgba(var(--v-theme-text-neutral-strong), 0.95)
      text-decoration: underline

  &__eyebrow
    display: inline-flex
    align-items: center
    gap: 0.5rem
    padding: 0.25rem 0.75rem
    border-radius: 999px
    background: rgba(var(--v-theme-accent-supporting), 0.15)
    color: rgb(var(--v-theme-text-neutral-strong))
    font-size: 0.75rem
    letter-spacing: 0.08em
    text-transform: uppercase

  &__title
    margin: 0
    font-weight: 700
    font-size: clamp(2rem, 2vw + 1.25rem, 2.75rem)
    line-height: 1.1

  &__copy
    display: flex
    flex-direction: column
    gap: 0.75rem
    align-items: flex-start

  &__wrapper--solo &__copy
    align-items: center
    text-align: center

  &__wrapper--solo &__description
    text-align: center

  &__description
    margin: 0
    font-size: 1.05rem
    line-height: 1.6
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

    :deep(p)
      margin: 0

    :deep(strong)
      color: rgb(var(--v-theme-text-neutral-strong))
      font-weight: 700

  &__info-card
    align-self: center
    display: flex
    flex-direction: column
    gap: 0.75rem
    padding: 1.25rem
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.42)
    border-radius: 8px
    background: rgba(var(--v-theme-surface-default), 0.78)
    box-shadow: 0 18px 42px -32px rgba(var(--v-theme-shadow-primary-600), 0.5)

  &__info-title
    margin: 0
    font-size: 1rem
    line-height: 1.35
    font-weight: 700
    color: rgb(var(--v-theme-text-neutral-strong))

  &__info-body
    font-size: 0.95rem
    line-height: 1.55
    color: rgba(var(--v-theme-text-neutral-secondary), 0.96)

    :deep(p)
      margin: 0

    :deep(p + p)
      margin-top: 0.75rem

    :deep(strong)
      color: rgb(var(--v-theme-text-neutral-strong))
      font-weight: 700

  &__media
    position: relative
    flex: 0 0 auto
    display: flex
    align-items: center
    justify-content: center
    overflow: hidden
    background: rgba(var(--v-theme-hero-overlay-soft), 0.12)
    transform: scale(0.6)
    transform-origin: center

  &__image
    height: 100%
    min-height: 180px
    width: 100%

@media (min-width: 960px)
  .category-hero__wrapper
    grid-template-columns: minmax(0, 1fr) clamp(200px, 22vw, 320px)
    align-items: stretch
    gap: clamp(1.25rem, 3vw, 3rem)

  .category-hero__media
    display: flex
    grid-column: 2

  .category-hero__content
    grid-column: 1
    align-self: center

  .category-hero__info-card
    grid-column: 2

  .category-hero__copy
    align-items: flex-start
    text-align: left

  .category-hero__description
    text-align: left

  .category-hero__wrapper--solo .category-hero__copy
    align-items: center
    text-align: center

  .category-hero__wrapper--solo .category-hero__description
    text-align: center

@media (max-width: 959px)
  .category-hero__media
    display: none

  .category-hero__info-card
    width: 100%

.animate-in-up
  animation: animate-in-up 0.8s cubic-bezier(0.22, 1, 0.36, 1) both
  animation-delay: var(--delay, 0ms)

@keyframes animate-in-up
  from
    opacity: 0
    transform: translateY(20px)
  to
    opacity: 1
    transform: translateY(0)

@media (prefers-reduced-motion: reduce)
  .animate-in-up
    animation: none
    opacity: 1
    transform: none
</style>

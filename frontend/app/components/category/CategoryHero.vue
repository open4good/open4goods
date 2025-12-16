<template>
  <HeroSurface
    tag="section"
    class="category-hero"
    :aria-labelledby="headingId"
    data-testid="category-hero"
    variant="halo"
  >
    <v-sheet class="category-hero__wrapper" elevation="0">
      <div v-if="image" class="category-hero__media" aria-hidden="true">
        <v-img :src="image" alt="" class="category-hero__image" cover>
          <template #placeholder>
            <v-skeleton-loader type="image" class="h-100" />
          </template>
        </v-img>
      </div>

      <div class="category-hero__content">
        <CategoryNavigationBreadcrumbs
          v-if="heroBreadcrumbs.length"
          v-bind="heroBreadcrumbProps"
          class="category-hero__breadcrumbs"
        />

        <div class="category-hero__copy">
          <p v-if="eyebrow" class="category-hero__eyebrow">
            {{ eyebrow }}
          </p>
          <h1 :id="headingId" class="category-hero__title">
            {{ title }}
          </h1>
          <p v-if="description" class="category-hero__description">
            {{ description }}
          </p>

          <div v-if="$slots.actions" class="category-hero__actions">
            <slot name="actions" />
          </div>
        </div>
      </div>
    </v-sheet>
  </HeroSurface>
</template>

<script setup lang="ts">
import { useId } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CategoryBreadcrumbItemDto } from '~~/shared/api-client'

import CategoryNavigationBreadcrumbs from '~/components/category/navigation/CategoryNavigationBreadcrumbs.vue'

type HeroBreadcrumbItem = {
  title: string
  link?: string
}

const props = defineProps<{
  title: string
  description?: string | null
  image?: string | null
  breadcrumbs?: CategoryBreadcrumbItemDto[]
  eyebrow?: string | null
}>()

const headingId = useId()
const { t } = useI18n()

const breadcrumbs = computed(() => props.breadcrumbs ?? [])
const eyebrow = computed(() => props.eyebrow ?? null)

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

  &__content
    display: flex
    flex-direction: column
    gap: 1rem
    max-width: none
    width: 100%

  &__breadcrumbs
    display: inline-flex
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

    :deep(.category-navigation-breadcrumbs)
      color: inherit

    :deep(.category-navigation-breadcrumbs__separator)
      color: rgba(var(--v-theme-text-neutral-secondary), 0.6)

    :deep(.category-navigation-breadcrumbs__current)
      color: rgba(var(--v-theme-text-neutral-strong), 0.95)

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

  &__description
    margin: 0
    font-size: 1.05rem
    line-height: 1.6
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

  &__actions
    display: flex
    flex-wrap: wrap
    gap: 0.75rem
    align-items: center

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
    grid-template-columns: clamp(200px, 22vw, 320px) minmax(0, 1fr)
    align-items: stretch
    gap: clamp(1.25rem, 3vw, 3rem)

  .category-hero__media
    display: flex
    grid-column: 1
    align-self: center

  .category-hero__content
    grid-column: 2
    align-self: center

  .category-hero__copy
    align-items: flex-start
    text-align: left

  .category-hero__description
    text-align: left

@media (max-width: 959px)
  .category-hero__media
    display: none
</style>

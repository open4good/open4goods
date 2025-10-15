<template>
  <section
    class="category-hero"
    :aria-labelledby="headingId"
    data-testid="category-hero"
  >
    <v-sheet class="category-hero__wrapper" elevation="0" rounded="xl">
      <v-img
        v-if="image"
        :src="image"
        :alt="title"
        class="category-hero__image"
        cover
      >
        <template #placeholder>
          <v-skeleton-loader type="image" class="h-100" />
        </template>
        <template #default>
          <v-sheet class="category-hero__overlay" color="transparent" />
        </template>
      </v-img>

      <div class="category-hero__content">
        <v-breadcrumbs
          v-if="breadcrumbs.length"
          :aria-label="t('category.hero.breadcrumbAriaLabel')"
          class="category-hero__breadcrumbs"
        >
          <v-breadcrumbs-item
            v-for="(item, index) in breadcrumbs"
            :key="`${index}-${item.title ?? item.link ?? index}`"
            :href="item.link || undefined"
          >
            {{ item.title ?? item.link ?? t('category.hero.missingBreadcrumbTitle') }}
          </v-breadcrumbs-item>
        </v-breadcrumbs>

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
        </div>
      </div>
    </v-sheet>
  </section>
</template>

<script setup lang="ts">
import { useId } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CategoryBreadcrumbItemDto } from '~~/shared/api-client'

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

defineExpose({ headingId, t })
</script>

<style scoped lang="sass">
.category-hero
  position: relative
  display: block
  margin-bottom: 2.5rem

  &__wrapper
    position: relative
    overflow: hidden
    min-height: 280px
    background: linear-gradient(135deg, rgba(var(--v-theme-hero-gradient-start), 0.9), rgba(var(--v-theme-hero-gradient-end), 0.85))
    color: rgb(var(--v-theme-hero-overlay-strong))

  &__image
    position: absolute
    inset: 0
    z-index: 1
    opacity: 0.5

  &__overlay
    position: absolute
    inset: 0
    background: radial-gradient(circle at top right, rgba(var(--v-theme-hero-overlay-soft), 0.25), transparent)

  &__content
    position: relative
    z-index: 2
    display: flex
    flex-direction: column
    gap: 1rem
    padding: clamp(1.75rem, 3vw + 1rem, 3.5rem)

  &__breadcrumbs
    --v-breadcrumbs-divider-color: rgba(var(--v-theme-hero-overlay-strong), 0.6)
    color: rgba(var(--v-theme-hero-overlay-strong), 0.9)
    font-size: 0.875rem

    :deep(.v-breadcrumbs-item)
      color: inherit

    :deep(.v-breadcrumbs-divider)
      color: inherit

  &__copy
    max-width: 60ch

  &__eyebrow
    display: inline-flex
    align-items: center
    gap: 0.5rem
    padding: 0.25rem 0.75rem
    border-radius: 999px
    background: rgba(var(--v-theme-hero-overlay-soft), 0.25)
    color: rgb(var(--v-theme-hero-pill-on-dark))
    font-size: 0.75rem
    letter-spacing: 0.08em
    text-transform: uppercase

  &__title
    margin: 0
    font-weight: 700
    font-size: clamp(2rem, 2vw + 1.5rem, 3rem)
    line-height: 1.1

  &__description
    margin: 0
    font-size: 1.125rem
    line-height: 1.6
    color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

@media (min-width: 1280px)
  .category-hero__wrapper
    min-height: 360px

  .category-hero__copy
    max-width: 48ch
</style>

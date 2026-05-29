<template>
  <v-card
    v-if="items.length"
    class="category-related-subcategories"
    variant="flat"
    rounded="lg"
  >
    <v-card-item class="category-related-subcategories__header">
      <template #prepend>
        <v-avatar
          class="category-related-subcategories__avatar"
          color="surface-primary-120"
          size="36"
        >
          <v-icon icon="mdi-shape-outline" size="20" />
        </v-avatar>
      </template>
      <v-card-title class="category-related-subcategories__title">
        {{ t('category.relatedSubcategories.title') }}
      </v-card-title>
      <v-card-subtitle class="category-related-subcategories__subtitle">
        {{ t('category.relatedSubcategories.subtitle') }}
      </v-card-subtitle>
    </v-card-item>

    <v-divider />

    <v-list
      class="category-related-subcategories__list"
      density="compact"
      nav
      lines="two"
    >
      <v-list-item
        v-for="item in items"
        :key="item.id"
        :to="item.to"
        :title="item.title"
        :subtitle="item.subtitle"
        append-icon="mdi-arrow-right"
        rounded="lg"
      />
    </v-list>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { VerticalSubCategoryDto } from '~~/shared/api-client'

const props = withDefaults(
  defineProps<{
    subcategories?: VerticalSubCategoryDto[] | null
    activeSubcategoryId?: string | null
    activeSubcategorySlug?: string | null
    parentUrl?: string | null
  }>(),
  {
    subcategories: () => [],
    activeSubcategoryId: null,
    activeSubcategorySlug: null,
    parentUrl: null,
  }
)

const { t } = useI18n()

const joinUrl = (baseUrl: string, slug: string) => {
  const normalizedBase = baseUrl.replace(/\/+$/, '')
  const normalizedSlug = slug.replace(/^\/+/, '')

  return `${normalizedBase}/${normalizedSlug}`
}

const items = computed(() => {
  const parentUrl = props.parentUrl?.trim()

  if (!parentUrl) {
    return []
  }

  const activeId = props.activeSubcategoryId?.trim()
  const activeSlug = props.activeSubcategorySlug?.trim()

  return (props.subcategories ?? [])
    .filter(subcategory => {
      const slug = subcategory.slug?.trim()
      const hasSlug = Boolean(slug)
      const isActive = Boolean(
        (activeId && subcategory.id === activeId) ||
          (activeSlug && slug === activeSlug)
      )

      return hasSlug && !isActive
    })
    .map(subcategory => {
      const title =
        subcategory.h1Title?.trim() ||
        subcategory.slug?.trim() ||
        t('category.relatedSubcategories.fallbackTitle')

      return {
        id: subcategory.id ?? subcategory.slug ?? title,
        title,
        subtitle: t('category.relatedSubcategories.itemSubtitle'),
        to: joinUrl(parentUrl, subcategory.slug ?? ''),
      }
    })
})
</script>

<style scoped lang="sass">
.category-related-subcategories
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.38)
  background: rgba(var(--v-theme-surface-default), 0.82)
  box-shadow: 0 14px 30px -26px rgba(var(--v-theme-shadow-primary-600), 0.42)
  overflow: hidden

  &__header
    padding: 1rem 1rem 0.875rem

  &__avatar
    color: rgb(var(--v-theme-primary))

  &__title
    font-size: 0.95rem
    font-weight: 700
    line-height: 1.3
    color: rgb(var(--v-theme-text-neutral-strong))

  &__subtitle
    opacity: 1
    color: rgba(var(--v-theme-text-neutral-secondary), 0.88)
    font-size: 0.8rem
    line-height: 1.35

  &__list
    padding: 0.5rem
    background: transparent

    :deep(.v-list-item)
      min-height: 58px
      transition: background-color 0.2s ease, transform 0.2s ease

    :deep(.v-list-item:hover),
    :deep(.v-list-item:focus-visible)
      transform: translateX(2px)

    :deep(.v-list-item-title)
      font-size: 0.9rem
      font-weight: 650

    :deep(.v-list-item-subtitle)
      font-size: 0.76rem
      color: rgba(var(--v-theme-text-neutral-secondary), 0.82)

@media (prefers-reduced-motion: reduce)
  .category-related-subcategories__list
    :deep(.v-list-item)
      transition: none

    :deep(.v-list-item:hover),
    :deep(.v-list-item:focus-visible)
      transform: none
</style>

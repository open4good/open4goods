<template>
  <section class="category-navigation-grid">
    <v-container class="py-12 px-4" max-width="xl">
      <div class="category-navigation-grid__header">
        <div>
          <h2 class="text-h4 text-md-h3 font-weight-bold mb-2">
            {{ t('categories.navigation.grid.title') }}
          </h2>
          <p class="text-body-2 text-neutral-secondary mb-0">
            {{ t('categories.navigation.grid.subtitle') }}
          </p>
        </div>
      </div>

      <div v-if="!categories.length" class="category-navigation-grid__empty">
        <v-icon icon="mdi-folder-search-outline" size="48" class="mb-4" />
        <p class="text-h6 text-center mb-2">
          {{ t('categories.navigation.grid.emptyTitle') }}
        </p>
        <p class="text-body-2 text-neutral-secondary text-center mb-0">
          {{ t('categories.navigation.grid.emptySubtitle') }}
        </p>
      </div>

      <v-row v-else :gap="24" align="stretch">
        <v-col
          v-for="category in categories"
          :key="category.path ?? category.slug ?? category.googleCategoryId"
          cols="12"
          md="6"
          lg="4"
        >
          <article class="category-navigation-grid__card" :aria-label="ariaLabel(category.title)">
            <v-card
              class="h-100 d-flex flex-column"
              elevation="0"
              variant="tonal"
            >
              <v-img
                v-if="category.vertical?.imageMedium"
                :src="category.vertical.imageMedium"
                :alt="category.vertical?.verticalHomeTitle ?? category.title ?? ''"
                class="category-navigation-grid__image"
                cover
              />

              <div class="category-navigation-grid__card-body">
                <div class="d-flex align-center justify-space-between mb-2">
                  <v-chip
                    v-if="category.hasVertical"
                    color="accent-supporting"
                    size="small"
                    variant="flat"
                    prepend-icon="mdi-star-outline"
                  >
                    {{ t('categories.navigation.grid.verticalLabel') }}
                  </v-chip>
                  <v-chip
                    v-if="category.leaf"
                    color="surface-primary-120"
                    size="small"
                    variant="text"
                  >
                    {{ t('categories.navigation.grid.leafLabel') }}
                  </v-chip>
                </div>

                <h3 class="text-h5 font-weight-semibold mb-2">
                  {{ category.title }}
                </h3>

                <ul
                  v-if="childrenPreview(category).length"
                  class="category-navigation-grid__children"
                >
                  <li
                    v-for="child in childrenPreview(category)"
                    :key="child.slug ?? child.googleCategoryId"
                  >
                    {{ child.title }}
                  </li>
                </ul>
              </div>

              <div class="category-navigation-grid__actions">
                <NuxtLink
                  v-if="category.path"
                  :to="`/categories/${category.path}`"
                  class="category-navigation-grid__link"
                  :aria-label="t('categories.navigation.grid.openSubcategory', { category: category.title })"
                >
                  <span>{{ t('categories.navigation.grid.openSubcategoryCta') }}</span>
                  <v-icon icon="mdi-arrow-right" size="small" />
                </NuxtLink>

                <NuxtLink
                  v-if="category.vertical?.verticalHomeUrl"
                  :to="`/${category.vertical.verticalHomeUrl}`"
                  class="category-navigation-grid__link"
                  :aria-label="t('categories.navigation.grid.viewVertical', { category: category.title })"
                >
                  <span>{{ t('categories.navigation.grid.viewVerticalCta') }}</span>
                  <v-icon icon="mdi-open-in-new" size="small" />
                </NuxtLink>
              </div>
            </v-card>
          </article>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import type { CategoryNavigationDtoChildCategoriesInner } from '~~/shared/api-client'
import { useI18n } from 'vue-i18n'

const { categories } = defineProps<{
  categories: CategoryNavigationDtoChildCategoriesInner[]
}>()

const { t } = useI18n()

const ariaLabel = (title?: string) =>
  t('categories.navigation.grid.cardAriaLabel', {
    category: title ?? '',
  })

const childrenPreview = (category: CategoryNavigationDtoChildCategoriesInner) =>
  (category.children as CategoryNavigationDtoChildCategoriesInner[] | undefined)?.slice(0, 4) ?? []

defineExpose({
  ariaLabel,
  childrenPreview,
})
</script>

<style scoped>
.category-navigation-grid {
  background: rgb(var(--v-theme-surface-default));
}

.category-navigation-grid__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2rem;
  gap: 1.5rem;
}

.category-navigation-grid__empty {
  text-align: center;
  padding: 4rem 1rem;
  border-radius: 1.5rem;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
}

.category-navigation-grid__card-body {
  padding: 1.5rem;
  flex: 1;
}

.category-navigation-grid__image {
  height: 180px;
}

.category-navigation-grid__children {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.35rem;
  color: rgb(var(--v-theme-text-neutral-secondary));
  font-size: 0.9rem;
}

.category-navigation-grid__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0 1.5rem 1.5rem;
}

.category-navigation-grid__link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  transition: color 0.2s ease;
}

.category-navigation-grid__link:hover,
.category-navigation-grid__link:focus-visible {
  color: rgb(var(--v-theme-accent-supporting));
}

@media (min-width: 1280px) {
  .category-navigation-grid__actions {
    justify-content: flex-start;
  }
}

article.category-navigation-grid__card {
  height: 100%;
}
</style>

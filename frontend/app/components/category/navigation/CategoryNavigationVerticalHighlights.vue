<template>
  <section v-if="linkedVerticals.length" class="category-navigation-verticals">
    <v-container class="py-12 px-4" max-width="xl">
      <header class="category-navigation-verticals__header">
        <p class="text-overline text-primary mb-2">
          {{ t('categories.navigation.verticals.eyebrow') }}
        </p>
        <h2 class="text-h5 text-md-h4 font-weight-bold mb-2">
          {{ t('categories.navigation.verticals.title') }}
        </h2>
        <p class="text-body-2 text-neutral-secondary mb-0">
          {{ t('categories.navigation.verticals.subtitle') }}
        </p>
      </header>

      <v-row :gap="24" align="stretch">
        <v-col
          v-for="verticalCategory in linkedVerticals"
          :key="
            verticalCategory.path ??
            verticalCategory.slug ??
            verticalCategory.googleCategoryId
          "
          cols="12"
          md="6"
          lg="4"
        >
          <article class="category-navigation-verticals__card">
            <v-hover v-slot="{ isHovering, props: hoverProps }">
              <v-card
                v-bind="hoverProps"
                :to="verticalRoute(verticalCategory)"
                class="category-navigation-verticals__sheet"
                :class="{
                  'category-navigation-verticals__sheet--hover': isHovering,
                }"
                rounded="lg"
                :elevation="isHovering ? 4 : 0"
              >
                <div class="category-navigation-verticals__media">
                  <v-img
                    v-if="verticalCategory.vertical?.imageLarge"
                    :src="verticalCategory.vertical.imageLarge"
                    :alt="
                      verticalCategory.vertical?.verticalHomeTitle ??
                      verticalCategory.title ??
                      ''
                    "
                    cover
                  />
                  <div
                    v-else
                    class="category-navigation-verticals__media-placeholder"
                  >
                    <v-icon icon="mdi-image-outline" size="36" />
                  </div>
                </div>

                <div class="category-navigation-verticals__content">
                  <div class="category-navigation-verticals__text">
                    <h3 class="text-h6 font-weight-semibold mb-0">
                      {{
                        verticalCategory.vertical?.verticalHomeTitle ??
                        verticalCategory.title
                      }}
                    </h3>
                    <CategoryNavigationSubcategoryChips
                      :subcategories="
                        verticalCategory.vertical?.subCategories ?? []
                      "
                      :parent-url="
                        verticalCategory.vertical?.verticalHomeUrl
                          ? `/${verticalCategory.vertical.verticalHomeUrl}`
                          : null
                      "
                      :max="3"
                    />
                    <div
                      v-if="guidesForVertical(verticalCategory).length"
                      class="category-navigation-verticals__guides"
                    >
                      <v-chip
                        v-for="guide in guidesForVertical(verticalCategory)"
                        :key="guide.path"
                        :to="guidePublicUrl(verticalCategory, guide)"
                        size="x-small"
                        variant="tonal"
                        color="secondary"
                        prepend-icon="mdi-book-open-outline"
                        class="category-navigation-verticals__guide-chip"
                        @click.stop
                      >
                        {{ guide.title }}
                      </v-chip>
                    </div>
                  </div>
                  <v-icon
                    icon="mdi-arrow-right"
                    size="20"
                    class="category-navigation-verticals__icon"
                    aria-hidden="true"
                  />
                </div>
              </v-card>
            </v-hover>
          </article>
        </v-col>
      </v-row>
    </v-container>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CategoryNavigationDtoChildCategoriesInner } from '~~/shared/api-client'
import { useI18n } from 'vue-i18n'
import CategoryNavigationSubcategoryChips from '~/components/category/navigation/CategoryNavigationSubcategoryChips.vue'
import type { DocsDoc } from '~/composables/useDocsContent'

const { verticals, guidesMap } = withDefaults(
  defineProps<{
    verticals: CategoryNavigationDtoChildCategoriesInner[]
    guidesMap?: Map<string, DocsDoc[]>
  }>(),
  {
    guidesMap: () => new Map(),
  }
)

const { t } = useI18n()

const linkedVerticals = computed(() =>
  verticals
    .filter(verticalCategory =>
      Boolean(verticalCategory.vertical?.verticalHomeUrl?.trim())
    )
    .slice(0, 6)
)

const verticalRoute = (
  verticalCategory: CategoryNavigationDtoChildCategoriesInner
) => {
  const url = verticalCategory.vertical?.verticalHomeUrl?.trim()
  if (!url) {
    return '/'
  }

  return url.startsWith('/') ? url : `/${url}`
}

const guidesForVertical = (
  verticalCategory: CategoryNavigationDtoChildCategoriesInner
): DocsDoc[] => {
  const verticalId = verticalCategory.vertical?.id?.trim()
  if (!verticalId) return []
  return guidesMap.get(verticalId) ?? []
}

const guidePublicUrl = (
  verticalCategory: CategoryNavigationDtoChildCategoriesInner,
  guide: DocsDoc
): string => {
  const homeUrl = verticalCategory.vertical?.verticalHomeUrl?.trim()
  const segments = guide.path.split('/').filter(Boolean)
  const guideSlug = segments[segments.length - 1] ?? ''
  if (!homeUrl || !guideSlug) return '/'
  const base = homeUrl.startsWith('/') ? homeUrl : `/${homeUrl}`
  return `${base}/${guideSlug}`
}
</script>

<style scoped>
.category-navigation-verticals {
  background: rgba(var(--v-theme-surface-muted), 0.55);
}

.category-navigation-verticals__header {
  max-width: 760px;
  margin-bottom: 2rem;
}

.category-navigation-verticals__sheet {
  display: flex;
  height: 100%;
  min-height: 132px;
  background: rgb(var(--v-theme-surface-default));
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.18);
  overflow: hidden;
  text-decoration: none;
  color: rgb(var(--v-theme-text-neutral-strong));
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    box-shadow 0.22s ease;
}

.category-navigation-verticals__sheet--hover {
  transform: translateY(-3px);
  border-color: rgba(var(--v-theme-primary), 0.35);
}

.category-navigation-verticals__media {
  position: relative;
  width: 132px;
  flex: 0 0 132px;
  overflow: hidden;
}

.category-navigation-verticals__media-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--v-theme-surface-primary-080), 0.6);
}

.category-navigation-verticals__content {
  padding: 1.1rem;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex: 1;
}

.category-navigation-verticals__text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.category-navigation-verticals__icon {
  color: rgb(var(--v-theme-primary));
  flex: 0 0 auto;
}

.category-navigation-verticals__guides {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-top: 0.4rem;
}

.category-navigation-verticals__guide-chip {
  text-decoration: none;
}

@media (max-width: 599px) {
  .category-navigation-verticals__sheet {
    min-height: 112px;
  }

  .category-navigation-verticals__media {
    width: 108px;
    flex-basis: 108px;
  }
}
</style>

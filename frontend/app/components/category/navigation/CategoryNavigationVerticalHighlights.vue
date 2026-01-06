<template>
  <section v-if="verticals.length" class="category-navigation-verticals">
    <v-container class="py-12 px-4" max-width="xl">
      <header class="category-navigation-verticals__header">
        <div>
          <h2 class="text-h4 text-md-h3 font-weight-bold mb-2 text-center">
            {{ t('categories.navigation.verticals.title') }}
          </h2>
          <p class="text-body-2 text-neutral-secondary mb-0 text-center">
            {{ t('categories.navigation.verticals.subtitle') }}
          </p>
        </div>
      </header>

      <v-row :gap="24" align="stretch">
        <v-col
          v-for="verticalCategory in limitedVerticals"
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
            <v-sheet
              v-if="verticalCategory.vertical?.verticalHomeUrl"
              class="category-navigation-verticals__sheet cursor-pointer"
              rounded="xl"
              elevation="8"
              role="link"
              @click="
                navigateToCategory(verticalCategory.vertical.verticalHomeUrl)
              "
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
                <h3 class="text-h5 font-weight-semibold mb-2">
                  {{
                    verticalCategory.vertical?.verticalHomeTitle ??
                    verticalCategory.title
                  }}
                </h3>
              </div>
            </v-sheet>
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

const { verticals } = defineProps<{
  verticals: CategoryNavigationDtoChildCategoriesInner[]
}>()

const { t } = useI18n()

const limitedVerticals = computed(() => verticals.slice(0, 6))

const router = useRouter()
const navigateToCategory = (url: string) => {
  router.push(`/${url}`)
}
</script>

<style scoped>
.category-navigation-verticals {
  background: linear-gradient(
    180deg,
    rgba(var(--v-theme-surface-ice-050), 0.9),
    rgba(var(--v-theme-surface-default), 1)
  );
}

.category-navigation-verticals__header {
  max-width: 720px;
  margin-bottom: 3rem;
}

.category-navigation-verticals__sheet {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: rgba(var(--v-theme-surface-glass), 0.9);
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2);
  overflow: hidden;
}

.category-navigation-verticals__media {
  position: relative;
  aspect-ratio: 16 / 9;
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
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.category-navigation-verticals__actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.category-navigation-verticals__cta,
.category-navigation-verticals__secondary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  text-decoration: none;
  font-weight: 600;
}

.category-navigation-verticals__cta {
  color: rgb(var(--v-theme-primary));
}

.category-navigation-verticals__secondary {
  color: rgb(var(--v-theme-text-neutral-secondary));
}

.category-navigation-verticals__cta:hover,
.category-navigation-verticals__cta:focus-visible,
.category-navigation-verticals__secondary:hover,
.category-navigation-verticals__secondary:focus-visible {
  color: rgb(var(--v-theme-accent-supporting));
}
</style>

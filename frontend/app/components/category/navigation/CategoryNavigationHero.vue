<template>
  <HeroSurface
    class="category-navigation-hero"
    :aria-labelledby="titleId"
    variant="orbit"
  >
    <v-container class="py-16 px-4" max-width="lg">
      <v-row align="center" class="g-8">
        <v-col cols="12" md="7" class="d-flex flex-column gap-6">
          <div class="category-navigation-hero__header">
            <CategoryNavigationBreadcrumbs
              v-if="breadcrumbs?.length"
              class="category-navigation-hero__breadcrumbs"
              v-bind="{ items: breadcrumbs, ariaLabel: breadcrumbAriaLabel }"
              variant="pills"
            />

            <h1 :id="titleId" class="category-navigation-hero__title">
              {{ title }}
            </h1>

            <p v-if="description" class="category-navigation-hero__subtitle">
              {{ description }}
            </p>

            <p v-if="resultSummary" class="category-navigation-hero__summary" aria-live="polite">
              {{ resultSummary }}
            </p>
          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-card class="category-navigation-hero__search-card" rounded="xl" elevation="6">
            <div class="category-navigation-hero__search-card-inner" role="search">
              <p class="category-navigation-hero__search-label">
                {{ searchLabel }}
              </p>

              <v-text-field
                :model-value="modelValue"
                :placeholder="searchPlaceholder"
                prepend-inner-icon="mdi-magnify"
                variant="solo"
                density="comfortable"
                color="primary"
                hide-details
                class="category-navigation-hero__search-field"
                :aria-label="searchLabel"
                @update:model-value="onUpdateModelValue"
              />
            </div>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </HeroSurface>
</template>

<script setup lang="ts">
import { useId } from 'vue'
import CategoryNavigationBreadcrumbs from './CategoryNavigationBreadcrumbs.vue'

interface BreadcrumbItem {
  title: string
  link?: string
}

const titleId = useId()

defineProps<{
  title: string
  description?: string
  breadcrumbs?: BreadcrumbItem[]
  modelValue: string
  searchLabel: string
  searchPlaceholder: string
  resultSummary?: string
  breadcrumbAriaLabel: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const onUpdateModelValue = (value: string) => emit('update:modelValue', value)
</script>

<style scoped lang="sass">
.category-navigation-hero
  position: relative
  overflow: hidden
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

.category-navigation-hero__header
  display: flex
  flex-direction: column
  gap: 0.9rem

.category-navigation-hero__title
  margin: 0
  font-size: clamp(2.4rem, 5vw, 3.5rem)
  line-height: 1.1

.category-navigation-hero__subtitle
  margin: 0
  font-size: 1.15rem
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

.category-navigation-hero__summary
  margin: 0
  font-size: 0.95rem
  color: rgba(var(--v-theme-hero-overlay-soft), 0.9)

.category-navigation-hero__search-card
  background: rgba(var(--v-theme-surface-glass), 0.18)
  border: 1px solid rgba(var(--v-theme-hero-overlay-strong), 0.28)
  backdrop-filter: blur(14px)
  box-shadow: 0 20px 60px rgba(var(--v-theme-shadow-primary-600), 0.14)

.category-navigation-hero__search-card-inner
  padding: clamp(1.25rem, 3vw, 1.75rem)
  display: flex
  flex-direction: column
  gap: 0.75rem

.category-navigation-hero__search-label
  margin: 0
  font-size: 0.85rem
  letter-spacing: 0.08em
  text-transform: uppercase
  color: rgba(var(--v-theme-hero-overlay-strong), 0.9)

/* --- Correctif contraste (light & dark) --- */
/* On force le champ à utiliser des tokens "surface/text" neutres,
   plutôt que des tokens "hero overlay" (souvent blancs). */

.category-navigation-hero__search-field :deep(.v-field)
  border-radius: 16px
  background: rgba(var(--v-theme-surface-default), 0.92)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.category-navigation-hero__search-field :deep(.v-field__overlay)
  opacity: 0

/* Texte saisi */
.category-navigation-hero__search-field :deep(.v-field__input input)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

/* Placeholder */
.category-navigation-hero__search-field :deep(.v-field__input input::placeholder)
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  opacity: 1

/* Icône */
.category-navigation-hero__search-field :deep(.v-field__prepend-inner)
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

/* Focus */
.category-navigation-hero__search-field :deep(.v-field--focused)
  border-color: rgba(var(--v-theme-primary), 0.55)
</style>

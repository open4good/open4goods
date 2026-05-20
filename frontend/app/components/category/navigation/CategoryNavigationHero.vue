<template>
  <HeroSurface
    :aria-labelledby="titleId"
    variant="orbit"
    class="category-navigation-hero"
  >
    <div
      v-if="backgroundAsset"
      class="category-navigation-hero__background"
      aria-hidden="true"
    >
      <img
        :src="backgroundAsset"
        alt=""
        class="category-navigation-hero__background-image"
      />
      <div class="category-navigation-hero__background-overlay" />
    </div>

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

            <p
              v-if="resultSummary"
              class="category-navigation-hero__summary"
              aria-live="polite"
            >
              {{ resultSummary }}
            </p>
          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-card
            class="category-navigation-hero__search-card"
            rounded="xl"
            elevation="6"
          >
            <div
              class="category-navigation-hero__search-card-inner"
              role="search"
            >
              <p class="category-navigation-hero__search-label">
                {{ searchLabel }}
              </p>

              <v-text-field
                :model-value="modelValue"
                :placeholder="searchPlaceholder"
                prepend-inner-icon="mdi-magnify"
                clearable
                clear-icon="mdi-close-circle-outline"
                variant="solo"
                density="comfortable"
                color="primary"
                hide-details
                class="category-navigation-hero__search-field"
                :aria-label="searchLabel"
                :clear-label="clearLabel"
                @update:model-value="onUpdateModelValue"
                @click:clear="emit('clear')"
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
import { useThemeAsset } from '~/composables/useThemedAsset'

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
  clearLabel: string
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
  (event: 'clear'): void
}>()

const onUpdateModelValue = (value: string | null) =>
  emit('update:modelValue', value ?? '')

const backgroundAsset = useThemeAsset('categoriesBackground')
</script>

<style scoped lang="sass">
.category-navigation-hero
  position: relative
  overflow: hidden
  color: rgb(var(--v-theme-text-neutral-strong))

.category-navigation-hero__background
  position: absolute
  inset: 0
  z-index: 0
  pointer-events: none

.category-navigation-hero__background-image
  position: absolute
  inset: 0
  width: 100%
  height: 100%
  object-fit: cover
  opacity: 0.98

.category-navigation-hero__background-overlay
  position: absolute
  inset: 0
  background: radial-gradient(circle at 16% 24%, rgba(var(--v-theme-hero-gradient-start), 0.22), transparent 32%), linear-gradient(180deg, rgba(var(--v-theme-surface-default), 0.1) 0%, rgba(var(--v-theme-surface-default), 0.45) 100%)

.category-navigation-hero__header
  display: flex
  flex-direction: column
  gap: 0.9rem

.category-navigation-hero__title
  margin: 0
  font-size: clamp(2.35rem, 3.05rem, 3.5rem)
  line-height: 1.1
  color: rgba(var(--v-theme-text-neutral-strong), 1)


.category-navigation-hero__subtitle
  margin: 0
  font-size: 1.15rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

.category-navigation-hero__summary
  margin: 0
  font-size: 0.95rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

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
  letter-spacing: 0
  text-transform: uppercase
  color: rgba(var(--v-theme-text-neutral-strong), 0.9)

.category-navigation-hero__search-field :deep(.v-field)
  border-radius: 16px
  background: rgba(var(--v-theme-surface-default), 0.92)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35)

.category-navigation-hero__search-field :deep(.v-field__overlay)
  opacity: 0

.category-navigation-hero__search-field :deep(.v-field__input input)
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.category-navigation-hero__search-field :deep(.v-field__input input::placeholder)
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)
  opacity: 1

.category-navigation-hero__search-field :deep(.v-field__prepend-inner)
  color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

.category-navigation-hero__search-field :deep(.v-field--focused)
  border-color: rgba(var(--v-theme-primary), 0.55)
</style>

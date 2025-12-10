<template>
  <div class="nudge-step-category">
    <h2 class="nudge-step-category__title">{{ $t('nudge-tool.steps.category.title') }}</h2>
    <p class="nudge-step-category__subtitle">
      {{ $t('nudge-tool.steps.category.subtitle') }}
    </p>

    <v-row dense>
      <v-col
        v-for="category in categories"
        :key="category.id"
        cols="6"
        sm="4"
        md="3"
      >
        <v-card
          class="nudge-step-category__card"
          :elevation="selectedCategoryId === category.id ? 6 : 2"
          :color="selectedCategoryId === category.id ? 'primary' : undefined"
          :variant="selectedCategoryId === category.id ? 'elevated' : 'tonal'"
          rounded="xl"
        role="button"
        :aria-pressed="(selectedCategoryId === category.id).toString()"
        @click="() => emit('select', category.id ?? '')"
      >
          <div class="nudge-step-category__image">
            <v-img
              :src="category.imageSmall"
              :alt="category.verticalHomeTitle ?? category.id ?? ''"
              aspect-ratio="1"
              class="nudge-step-category__img"
              cover
            >
              <template #placeholder>
                <div class="nudge-step-category__fallback">
                  <v-icon icon="mdi-tag" size="28" />
                </div>
              </template>

              <template #error>
                <div class="nudge-step-category__fallback">
                  <v-icon icon="mdi-tag" size="28" />
                </div>
              </template>
            </v-img>
          </div>
          <p class="nudge-step-category__name">{{ category.verticalHomeTitle ?? category.id }}</p>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'

defineProps<{
  categories: VerticalConfigDto[]
  selectedCategoryId?: string | null
}>()

const emit = defineEmits<{ (event: 'select', categoryId: string): void }>()
</script>

<style scoped lang="scss">
.nudge-step-category {
  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin-bottom: 4px;
  }

  &__subtitle {
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin-bottom: 16px;
  }

  &__card {
    text-align: center;
    padding: 16px;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }

  &__image {
    width: 72px;
    aspect-ratio: 1 / 1;
    border-radius: 12px;
    overflow: hidden;
    background: rgba(var(--v-theme-primary), 0.08);
  }

  &__img {
    height: 100%;
  }

  &__fallback {
    display: grid;
    place-items: center;
    height: 100%;
    background: rgba(var(--v-theme-primary), 0.06);
    color: rgb(var(--v-theme-primary));
  }

  &__name {
    margin: 0;
    font-weight: 600;
  }
}
</style>

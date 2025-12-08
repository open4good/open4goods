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
          <div class="nudge-step-category__icon">
            <v-icon :icon="category.mdiIcon ?? 'mdi-tag'" size="28" />
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

  &__icon {
    display: inline-flex;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    align-items: center;
    justify-content: center;
    background: rgba(var(--v-theme-primary), 0.1);
  }

  &__name {
    margin: 0;
    font-weight: 600;
  }
}
</style>

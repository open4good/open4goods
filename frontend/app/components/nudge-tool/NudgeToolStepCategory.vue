<template>
  <div class="nudge-step-category">
    <div class="nudge-step-category__header">
      <div class="nudge-step-category__step" :aria-label="$t('nudge-tool.steps.category.step', { step: 1 })">
        <v-avatar color="surface-primary-080" size="40" rounded="lg">
          <v-icon icon="mdi-numeric-1-circle" color="accent-primary-highlight" size="26" />
        </v-avatar>
      </div>

      <div class="nudge-step-category__titles">
        <h2 class="nudge-step-category__title">{{ $t('nudge-tool.steps.category.title') }}</h2>
        <p class="nudge-step-category__subtitle">
          {{ $t('nudge-tool.steps.category.subtitle') }}
        </p>
      </div>
    </div>

    <v-slide-group
      v-model="selected"
      class="nudge-step-category__slider"
      show-arrows
      center-active
      mandatory
    >
      <template #prev="{ props: prevArrowProps }">
        <v-btn
          v-bind="prevArrowProps"
          class="nudge-step-category__arrow"
          icon="mdi-chevron-left"
          variant="flat"
          :aria-label="$t('nudge-tool.actions.previous')"
        />
      </template>

      <template #next="{ props: nextArrowProps }">
        <v-btn
          v-bind="nextArrowProps"
          class="nudge-step-category__arrow"
          icon="mdi-chevron-right"
          variant="flat"
          :aria-label="$t('nudge-tool.actions.next')"
        />
      </template>

      <v-slide-group-item
        v-for="category in categories"
        :key="category.id"
        :value="category.id ?? ''"
      >
        <v-card
          class="nudge-step-category__card"
          :class="{ 'nudge-step-category__card--selected': selected === category.id }"
          :elevation="selected === category.id ? 8 : 2"
          rounded="xl"
          role="button"
          :aria-pressed="(selected === category.id).toString()"
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
      </v-slide-group-item>
    </v-slide-group>
  </div>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'

const props = defineProps<{
  categories: VerticalConfigDto[]
  selectedCategoryId?: string | null
}>()

const emit = defineEmits<{ (event: 'select', categoryId: string): void }>()

const selected = ref<string | null>(props.selectedCategoryId ?? null)

watch(
  () => props.selectedCategoryId,
  (value) => {
    selected.value = value ?? null
  },
)

watch(
  selected,
  (value) => {
    if (value) {
      emit('select', value)
    }
  },
  { immediate: true },
)
</script>

<style scoped lang="scss">
.nudge-step-category {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    text-align: center;
  }

  &__step {
    display: flex;
    align-items: center;
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__subtitle {
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin: 0;
  }

  &__slider {
    display: flex;
    align-items: center;
    padding-inline: 8px;
    gap: 8px;
  }

  &__arrow {
    background: rgb(var(--v-theme-surface-primary-080));
    color: rgb(var(--v-theme-accent-primary-highlight));
    box-shadow: none;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
  }

  &__card {
    text-align: center;
    padding: 16px 14px;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-width: 160px;
    background: rgb(var(--v-theme-surface-default));
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);

    &--selected {
      border-color: rgb(var(--v-theme-accent-primary-highlight));
      box-shadow: 0 10px 25px -12px rgba(var(--v-theme-shadow-primary-600), 0.5);
    }
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

<template>
  <div class="nudge-step-category">
	<v-row justify="center" class="mt-2" >
    <h2 class="align-center nudge-step-category__title">{{ $t('nudge-tool.steps.category.title') }}</h2>
    </v-row>
    <div class="nudge-step-category__header">

      <div class="nudge-step-category__subtitle-row">
        <div class="nudge-step-category__step" :aria-label="$t('nudge-tool.steps.category.step', { step: 1 })">
            <v-icon icon="mdi-numeric-1-circle" color="accent-primary-highlight" size="22" />
        </div>

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
  gap: 12px;

  &__header {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    text-align: left;
  }

  &__step {
    display: flex;
    align-items: center;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__subtitle-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__subtitle {
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin: 0;
  }

  &__slider {
    display: flex;
    align-items: stretch;
    padding-inline: 0;
    gap: 12px;

    :deep(.v-slide-group__container) {
      align-items: stretch;
    }

    :deep(.v-slide-group__content) {
      justify-content: center;
      gap: 12px;
      padding-block: 4px;
    }
  }

  &__arrow {
    background: rgba(var(--v-theme-surface-primary-080), 0.9);
    color: rgb(var(--v-theme-accent-primary-highlight));
    box-shadow: 0 6px 16px rgba(var(--v-theme-shadow-primary-600), 0.08);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  }

  &__card {
    text-align: center;
    padding: 16px 14px;
    height: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-width: 160px;
    background: rgba(var(--v-theme-surface-glass), 0.96);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.6);
    box-shadow: 0 14px 30px rgba(var(--v-theme-shadow-primary-600), 0.08);
    transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;

    &--selected {
      border-color: rgb(var(--v-theme-accent-primary-highlight));
      box-shadow: 0 16px 32px -10px rgba(var(--v-theme-shadow-primary-600), 0.35);
      transform: translateY(-2px);
    }
  }

  &__image {
    width: 72px;
    aspect-ratio: 1 / 1;
    border-radius: 14px;
    overflow: hidden;
    background: rgba(var(--v-theme-surface-muted), 0.96);
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
  }

  &__img {
    height: 100%;
  }

  &__fallback {
    display: grid;
    place-items: center;
    height: 100%;
    background: rgba(var(--v-theme-surface-primary-080), 0.9);
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__name {
    margin: 0;
    font-weight: 700;
    color: rgb(var(--v-theme-text-neutral-strong));
  }
}
</style>

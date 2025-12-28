<template>
  <div class="nudge-step-category">
    <!-- Header removed, moved to Wizard -->

    <v-slide-group
      v-model="selected"
      class="nudge-step-category__slider"
      :class="{ 'nudge-step-category__slider--stacked': isMobile }"
      :show-arrows="!isMobile"
      center-active
      mandatory
    >
      <template v-if="!isMobile" #prev="{ props: prevArrowProps }">
        <v-btn
          v-bind="prevArrowProps"
          class="nudge-step-category__arrow"
          icon="mdi-chevron-left"
          variant="flat"
          :aria-label="$t('nudge-tool.actions.previous')"
        />
      </template>

      <template v-if="!isMobile" #next="{ props: nextArrowProps }">
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
        :disabled="isCategoryDisabled(category)"
        :aria-disabled="isCategoryDisabled(category).toString()"
      >
        <v-card
          class="nudge-step-category__card nudge-option-card"
          :class="{
            'nudge-option-card--selected': selected === category.id,
            'nudge-option-card--disabled': isCategoryDisabled(category),
          }"
          variant="flat"
          rounded="xl"
          role="button"
          :aria-pressed="(selected === category.id).toString()"
          :ripple="!isCategoryDisabled(category)"
          :tabindex="isCategoryDisabled(category) ? -1 : 0"
          @click="() => handleSelect(category)"
        >
          <div class="nudge-step-category__image">
            <v-img
              :src="category.imageMedium || category.imageSmall"
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
          <p class="nudge-step-category__name">
            {{ category.verticalHomeTitle ?? category.id }}
          </p>
        </v-card>
      </v-slide-group-item>
    </v-slide-group>
  </div>
</template>

<script setup lang="ts">
import type { VerticalConfigDto } from '~~/shared/api-client'
import { useDisplay } from 'vuetify'

const props = defineProps<{
  categories: VerticalConfigDto[]
  selectedCategoryId?: string | null
  isAuthenticated?: boolean
}>()

const emit = defineEmits<{ (event: 'select', categoryId: string): void }>()

const selected = ref<string | null>(props.selectedCategoryId ?? null)
const display = useDisplay()
const isMobile = computed(() => display.smAndDown.value)
const allowDisabledSelection = computed(() => props.isAuthenticated ?? false)

const isCategoryDisabled = (category: VerticalConfigDto) =>
  !allowDisabledSelection.value && category.enabled === false

const handleSelect = (category: VerticalConfigDto) => {
  if (isCategoryDisabled(category)) {
    return
  }

  emit('select', category.id ?? '')
}

watch(
  () => props.selectedCategoryId,
  value => {
    selected.value = value ?? null
  }
)

watch(
  selected,
  value => {
    if (value) {
      emit('select', value)
    }
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.nudge-step-category {
  display: flex;
  flex-direction: column;
  gap: clamp(1.5rem, 4vw, 2.5rem);
  padding-bottom: 1.5rem;

  &__header {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    text-align: center;
  }

  &__title-row {
    display: flex;
    align-items: center;
    gap: 12px;
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

  &__subtitle {
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin: 0;
  }

  &__slider {
    display: flex;
    align-items: stretch;
    padding-inline: 4px;
    gap: 10px;

    :deep(.v-slide-group__container) {
      align-items: stretch;
    }

    :deep(.v-slide-group__content) {
      justify-content: center;
      gap: 12px;
      padding-block: 4px;
    }

    &--stacked {
      padding-inline: 0;

      :deep(.v-slide-group__container) {
        overflow: visible;
      }

      :deep(.v-slide-group__content) {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 12px;
        width: 100%;
      }
    }
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
    height: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-width: 160px;
    box-shadow: none;
    background: transparent !important;
    border: none !important;
  }

  &__image {
    width: 96px; /* Increased for medium image */
    aspect-ratio: 1 / 1;
    border-radius: 12px;
    overflow: hidden;
    background: transparent;
  }

  &__img {
    height: 100%;
  }

  &__fallback {
    display: grid;
    place-items: center;
    height: 100%;
    background: transparent;
    color: rgb(var(--v-theme-primary));
  }

  &__name {
    margin: 0;
    font-weight: 600;
  }

  &__card.nudge-option-card--disabled {
    cursor: not-allowed;

    .nudge-step-category__img {
      filter: grayscale(1);
      opacity: 0.55;
    }

    .nudge-step-category__name {
      color: rgba(var(--v-theme-text-neutral-secondary), 0.7);
    }
  }

  @media (max-width: 600px) {
    &__card {
      min-width: 140px;
      padding: 12px 10px;
      gap: 6px;
    }

    &__image {
      width: 80px;
      border-radius: 10px;
    }

    &__name {
      font-size: 0.95rem;
    }
  }
}
</style>

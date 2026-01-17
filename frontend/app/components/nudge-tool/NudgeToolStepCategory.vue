<template>
  <div
    class="nudge-step-category"
    :class="{ 'nudge-step-category--compact': compact }"
  >
    <!-- Header removed, moved to Wizard -->

    <v-slide-group
      v-model="selected"
      class="nudge-step-category__slider"
      show-arrows
      center-active
      mandatory
    >
      <template #prev="prevArrowProps">
        <v-btn
          v-if="!prevArrowProps.disabled"
          v-bind="prevArrowProps"
          class="nudge-step-category__arrow nudge-step-category__arrow--prev"
          icon="mdi-chevron-left"
          variant="flat"
          size="small"
          :aria-label="$t('nudge-tool.actions.previous')"
        />
      </template>

      <template #next="nextArrowProps">
        <v-btn
          v-if="!nextArrowProps.disabled"
          v-bind="nextArrowProps"
          class="nudge-step-category__arrow nudge-step-category__arrow--next"
          icon="mdi-chevron-right"
          variant="flat"
          size="small"
          :aria-label="$t('nudge-tool.actions.next')"
        />
      </template>

      <v-slide-group-item
        v-for="category in categories"
        :key="category.id"
        :value="category.id ?? ''"
        :disabled="isCategoryDisabled(category)"
      >
        <template #default="{ isSelected, toggle }">
          <v-tooltip
            v-if="category.tooltip"
            location="top"
            :text="category.tooltip"
          >
            <template #activator="{ props: tooltipProps }">
              <ClientOnly v-if="isStaticCategory(category)">
                <component
                  :is="category.externalLink ? 'a' : 'div'"
                  class="nudge-step-category__card-link"
                  :href="category.externalLink"
                  target="_blank"
                  rel="noopener noreferrer nofollow"
                  v-bind="tooltipProps"
                >
                  <v-card
                    class="nudge-step-category__card nudge-option-card"
                    :class="{
                      'nudge-option-card--selected':
                        !isExternalCategory(category) && isSelected,
                      'nudge-option-card--disabled':
                        isCategoryDisabled(category),
                      'nudge-step-category__card--static':
                        isStaticCategory(category),
                    }"
                    variant="flat"
                    rounded="xl"
                    :role="category.externalLink ? 'link' : 'button'"
                    :aria-pressed="
                      (!isExternalCategory(category) && isSelected).toString()
                    "
                    :ripple="!isCategoryDisabled(category)"
                    :tabindex="isCategoryDisabled(category) ? -1 : 0"
                    @click="() => handleSelect(category, toggle)"
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
                            <v-icon
                              :icon="category.mdiIcon ?? 'mdi-tag'"
                              size="28"
                            />
                          </div>
                        </template>

                        <template #error>
                          <div class="nudge-step-category__fallback">
                            <v-icon
                              :icon="category.mdiIcon ?? 'mdi-tag'"
                              size="28"
                            />
                          </div>
                        </template>
                      </v-img>
                    </div>
                    <p class="nudge-step-category__name">
                      {{ category.verticalHomeTitle ?? category.id }}
                    </p>
                  </v-card>
                </component>
              </ClientOnly>
              <component
                :is="category.externalLink ? 'a' : 'div'"
                v-else
                class="nudge-step-category__card-link"
                :href="category.externalLink"
                target="_blank"
                rel="noopener noreferrer nofollow"
                v-bind="tooltipProps"
              >
                <v-card
                  class="nudge-step-category__card nudge-option-card"
                  :class="{
                    'nudge-option-card--selected':
                      !isExternalCategory(category) && isSelected,
                    'nudge-option-card--disabled': isCategoryDisabled(category),
                    'nudge-step-category__card--static':
                      isStaticCategory(category),
                  }"
                  variant="flat"
                  rounded="xl"
                  :role="category.externalLink ? 'link' : 'button'"
                  :aria-pressed="
                    (!isExternalCategory(category) && isSelected).toString()
                  "
                  :ripple="!isCategoryDisabled(category)"
                  :tabindex="isCategoryDisabled(category) ? -1 : 0"
                  @click="() => handleSelect(category, toggle)"
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
                          <v-icon
                            :icon="category.mdiIcon ?? 'mdi-tag'"
                            size="28"
                          />
                        </div>
                      </template>

                      <template #error>
                        <div class="nudge-step-category__fallback">
                          <v-icon
                            :icon="category.mdiIcon ?? 'mdi-tag'"
                            size="28"
                          />
                        </div>
                      </template>
                    </v-img>
                  </div>
                  <p class="nudge-step-category__name">
                    {{ category.verticalHomeTitle ?? category.id }}
                  </p>
                </v-card>
              </component>
            </template>
          </v-tooltip>
          <ClientOnly v-else-if="isStaticCategory(category)">
            <component
              :is="category.externalLink ? 'a' : 'div'"
              class="nudge-step-category__card-link"
              :href="category.externalLink"
              target="_blank"
              rel="noopener noreferrer nofollow"
            >
              <v-card
                class="nudge-step-category__card nudge-option-card"
                :class="{
                  'nudge-option-card--selected':
                    !isExternalCategory(category) && isSelected,
                  'nudge-option-card--disabled': isCategoryDisabled(category),
                  'nudge-step-category__card--static':
                    isStaticCategory(category),
                }"
                variant="flat"
                rounded="xl"
                :role="category.externalLink ? 'link' : 'button'"
                :aria-pressed="
                  (!isExternalCategory(category) && isSelected).toString()
                "
                :ripple="!isCategoryDisabled(category)"
                :tabindex="isCategoryDisabled(category) ? -1 : 0"
                @click="() => handleSelect(category, toggle)"
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
                        <v-icon
                          :icon="category.mdiIcon ?? 'mdi-tag'"
                          size="28"
                        />
                      </div>
                    </template>

                    <template #error>
                      <div class="nudge-step-category__fallback">
                        <v-icon
                          :icon="category.mdiIcon ?? 'mdi-tag'"
                          size="28"
                        />
                      </div>
                    </template>
                  </v-img>
                </div>
                <p class="nudge-step-category__name">
                  {{ category.verticalHomeTitle ?? category.id }}
                </p>
              </v-card>
            </component>
          </ClientOnly>
          <component
            :is="category.externalLink ? 'a' : 'div'"
            v-else
            class="nudge-step-category__card-link"
            :href="category.externalLink"
            target="_blank"
            rel="noopener noreferrer nofollow"
          >
            <v-card
              class="nudge-step-category__card nudge-option-card"
              :class="{
                'nudge-option-card--selected':
                  !isExternalCategory(category) && isSelected,
                'nudge-option-card--disabled': isCategoryDisabled(category),
                'nudge-step-category__card--static': isStaticCategory(category),
              }"
              variant="flat"
              rounded="xl"
              :role="category.externalLink ? 'link' : 'button'"
              :aria-pressed="
                (!isExternalCategory(category) && isSelected).toString()
              "
              :ripple="!isCategoryDisabled(category)"
              :tabindex="isCategoryDisabled(category) ? -1 : 0"
              @click="() => handleSelect(category, toggle)"
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
                      <v-icon :icon="category.mdiIcon ?? 'mdi-tag'" size="28" />
                    </div>
                  </template>

                  <template #error>
                    <div class="nudge-step-category__fallback">
                      <v-icon :icon="category.mdiIcon ?? 'mdi-tag'" size="28" />
                    </div>
                  </template>
                </v-img>
              </div>
              <p class="nudge-step-category__name">
                {{ category.verticalHomeTitle ?? category.id }}
              </p>
            </v-card>
          </component>
        </template>
      </v-slide-group-item>
    </v-slide-group>
  </div>
</template>

<script setup lang="ts">
import type { NudgeToolCategory } from '~/types/nudge-tool'

const props = defineProps<{
  categories: NudgeToolCategory[]
  selectedCategoryId?: string | null
  isAuthenticated?: boolean
  compact?: boolean
}>()

const emit = defineEmits<{ (event: 'select', categoryId: string): void }>()

const selected = ref<string | null>(props.selectedCategoryId ?? null)
const allowDisabledSelection = computed(() => props.isAuthenticated ?? false)

const isCategoryDisabled = (category: NudgeToolCategory) =>
  !allowDisabledSelection.value && category.enabled === false

const isExternalCategory = (category: NudgeToolCategory) =>
  Boolean(category.externalLink)

const isStaticCategory = (category: NudgeToolCategory) =>
  category.id === 'static'

const handleSelect = (category: NudgeToolCategory, toggle: () => void) => {
  if (isCategoryDisabled(category)) {
    return
  }

  if (isExternalCategory(category)) {
    return
  }

  toggle()
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
  gap: clamp(1rem, 2.5vw, 1.75rem);
  padding-bottom: 0.75rem;

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

    /* Center the slider and limit width to show max 4 items (4 * 240px + gaps) */
    margin-inline: auto;
    max-width: 1050px;
    width: 100%;

    :deep(.v-slide-group__container) {
      align-items: stretch;
      /* Ensure the container doesn't overflow its parent in a way that hides arrows */
      contain: content;
    }

    :deep(.v-slide-group__content) {
      gap: 12px;
      padding-block: 4px;
    }

    &--stacked {
      display: none; // No longer used, keeping for reference or removal
    }
  }

  &__arrow {
    background: rgba(var(--v-theme-surface-glass-strong), 0.9) !important;
    backdrop-filter: blur(8px);
    color: rgb(var(--v-theme-accent-primary-highlight));
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3) !important;
    transition: all 0.3s ease;
    z-index: 10;
    width: 40px !important;
    height: 40px !important;

    &:hover {
      background: rgb(var(--v-theme-surface-primary-100)) !important;
      transform: scale(1.1);
    }

    &--prev {
      margin-right: -8px;
    }

    &--next {
      margin-left: -8px;
    }
  }

  &__card {
    text-align: center;
    padding: 12px 10px;
    height: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 8px;
    width: 220px;
    box-shadow: none;
    background: transparent !important;
    border: none !important;

    @media (max-width: 960px) {
      width: 160px;
    }
  }

  &__card-link {
    text-decoration: none;
    color: inherit;
    display: inline-flex;
  }

  &__card--static {
    opacity: 0.8;
    transform: scale(0.75);
    transform-origin: center;
  }

  &__image {
    width: 64px; /* Reduced from 96px for compact height */
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
    white-space: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
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
      width: 140px;
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

.nudge-step-category--compact {
  gap: clamp(0.75rem, 2.5vw, 1.25rem);
  padding-bottom: 0.5rem;

  .nudge-step-category__slider {
    gap: 6px;

    :deep(.v-slide-group__content) {
      gap: 8px;
    }
  }

  .nudge-step-category__card {
    padding: 10px 12px;
    min-width: 180px;
    max-width: 220px;
    flex-direction: row;
    text-align: left;
    gap: 10px;
  }

  .nudge-step-category__image {
    width: clamp(56px, 8vw, 72px);
    border-radius: 10px;
  }

  .nudge-step-category__name {
    font-size: 0.95rem;
    -webkit-line-clamp: 1;
  }
}
</style>

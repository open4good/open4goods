<template>
  <div
    class="nudge-step-category"
    :class="{ 'nudge-step-category--compact': compact }"
  >
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
import { useDisplay } from 'vuetify'
import type { NudgeToolCategory } from '~/types/nudge-tool'

const props = defineProps<{
  categories: NudgeToolCategory[]
  selectedCategoryId?: string | null
  isAuthenticated?: boolean
  compact?: boolean
}>()

const emit = defineEmits<{ (event: 'select', categoryId: string): void }>()

const selected = ref<string | null>(props.selectedCategoryId ?? null)
const display = useDisplay()
const isMobile = computed(() => display.smAndDown.value)
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
    min-width: 22%; /* Adjusted to ensure ~4 items fit on screen */
    max-width: 250px;
    box-shadow: none;
    background: transparent !important;
    border: none !important;

    @media (max-width: 960px) {
      min-width: 160px; /* Fallback for smaller screens */
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

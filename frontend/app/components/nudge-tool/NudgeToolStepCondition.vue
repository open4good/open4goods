<template>
  <div
    class="nudge-step-condition"
    :class="{ 'nudge-step-condition--compact': compact }"
  >
    <v-row dense class="nudge-step-condition__row" justify="center">
      <v-col
        v-for="option in options"
        :key="option.value"
        cols="12"
        sm="6"
        md="5"
        class="d-flex"
      >
        <v-card
          class="nudge-step-condition__card nudge-toggle-card"
          elevation="2"
          :class="{
            'nudge-toggle-card--selected': isSelected(option.value),
            'nudge-toggle-card--disabled': isDisabled(option.value),
          }"
          rounded="xl"
          role="button"
          :aria-pressed="isSelected(option.value).toString()"
          :aria-disabled="isDisabled(option.value).toString()"
          :aria-label="option.label"
          :tabindex="isDisabled(option.value) ? -1 : 0"
          @click="() => toggleOption(option.value)"
          @keydown.enter.prevent="toggleOption(option.value)"
          @keydown.space.prevent="toggleOption(option.value)"
        >
          <div class="nudge-toggle-card__header">
            <div class="nudge-toggle-card__selection-indicator">
              <v-icon
                :icon="
                  isSelected(option.value)
                    ? 'mdi-checkbox-marked-circle'
                    : 'mdi-checkbox-blank-circle-outline'
                "
                :color="
                  isSelected(option.value) ? 'primary' : 'medium-emphasis'
                "
                size="24"
              />
            </div>
            <p class="nudge-toggle-card__title">{{ option.label }}</p>
          </div>

          <div class="nudge-toggle-card__body">
            <component
              :is="option.iconComponent"
              class="nudge-toggle-card__illustration"
            />
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import type { ProductConditionChoice } from '~/utils/_nudge-tool-filters'

const NudgeConditionNewIcon = defineAsyncComponent(
  () => import('./NudgeConditionNewIcon.vue')
)
const NudgeConditionUsedIcon = defineAsyncComponent(
  () => import('./NudgeConditionUsedIcon.vue')
)

const props = defineProps<{
  modelValue: ProductConditionChoice[]
  compact?: boolean
  isZeroResults?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: ProductConditionChoice[]): void
}>()

const { t } = useI18n()

const options: Array<{
  value: ProductConditionChoice
  label: string
  iconComponent: Component
}> = [
  {
    value: 'new',
    label: t('nudge-tool.steps.condition.options.new'),
    iconComponent: NudgeConditionNewIcon,
  },
  {
    value: 'occasion',
    label: t('nudge-tool.steps.condition.options.occasion'),
    iconComponent: NudgeConditionUsedIcon,
  },
]

const isSelected = (choice: ProductConditionChoice) => {
  return props.modelValue.includes(choice)
}

const isDisabled = (choice: ProductConditionChoice) =>
  Boolean(props.isZeroResults) && !isSelected(choice)

const toggleOption = (choice: ProductConditionChoice) => {
  if (isDisabled(choice)) {
    return
  }

  const nextSelection = isSelected(choice)
    ? props.modelValue.filter(entry => entry !== choice)
    : [...props.modelValue, choice]

  emit('update:modelValue', nextSelection)
}
</script>

<style scoped lang="scss">
.nudge-step-condition {
  &__row {
    row-gap: 16px;
    column-gap: 16px;
  }

  .nudge-toggle-card {
    width: 100%;
    display: flex;
    flex-direction: column;
    padding: 24px;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-primary-050)) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    /* box-shadow: none; Removed to allow elevation */
    transition:
      transform 140ms ease,
      border-color 160ms ease,
      box-shadow 160ms ease,
      background-color 160ms ease;
    cursor: pointer;
    min-height: 200px;

    &__header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 24px;
    }

    &__title {
      margin: 0;
      font-weight: 700;
      font-size: 1.15rem;
      line-height: 1.35;
      color: rgb(var(--v-theme-text-primary));
    }

    &__body {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 12px;
    }

    &__illustration {
      max-width: 160px;
      max-height: 120px;
      font-size: 72px;
      line-height: 1;
      transition: transform 200ms ease;
    }

    &:hover,
    &:focus-visible {
      transform: translateY(-2px);
      border-color: rgba(var(--v-theme-accent-supporting), 0.6);
      /* Custom hover shadow */
      box-shadow: 0 12px 24px rgba(var(--v-theme-shadow-primary-600), 0.15) !important;
      outline: none;

      .nudge-toggle-card__illustration {
        transform: scale(1.05);
      }
    }

    &:focus-visible {
      box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-supporting), 0.25);
    }
  }

  .nudge-toggle-card--selected {
    background: rgba(var(--v-theme-accent-supporting), 0.08) !important;
    border-color: rgb(var(--v-theme-accent-supporting));
    border-width: 2px; // Make border slightly more visible
    padding: 23px; // Adjust padding to compensate for border width change

    .nudge-toggle-card__title {
      color: rgb(var(--v-theme-accent-supporting));
    }
  }

  .nudge-toggle-card--disabled {
    opacity: 0.5;
    cursor: not-allowed;
    box-shadow: none !important;
    pointer-events: none;

    &:hover,
    &:focus-visible {
      transform: none;
      border-color: rgba(var(--v-theme-border-primary-strong), 0.4);
      box-shadow: none !important;
    }
  }
}

.nudge-step-condition--compact {
  .nudge-step-condition__row {
    row-gap: 12px;
    column-gap: 12px;
  }

  .nudge-toggle-card {
    padding: 16px;
    min-height: 150px;

    &__header {
      margin-bottom: 12px;
    }

    &__title {
      font-size: 1rem;
      line-height: 1.2;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &__illustration {
      max-width: 140px;
      max-height: 96px;
    }
  }

  .nudge-toggle-card--selected {
    padding: 15px;
  }
}
</style>

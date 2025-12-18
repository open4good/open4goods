<template>
  <div class="nudge-step-condition">
    <h2 class="nudge-step-condition__title">
      <v-icon
        icon="mdi-numeric-3-circle"
        color="accent-primary-highlight"
        size="30"
      />
      {{ $t('nudge-tool.steps.condition.title') }}
    </h2>
    <p class="nudge-step-condition__subtitle">
      {{ $t('nudge-tool.steps.condition.subtitle') }}
    </p>

    <v-row dense class="nudge-step-condition__row" justify="center">
      <v-col
        v-for="option in options"
        :key="option.value"
        cols="12"
        sm="4"
        class="d-flex"
      >
        <v-card
          class="nudge-step-condition__card nudge-toggle-card"
          :class="{
            'nudge-toggle-card--selected': isSelected(option.value),
          }"
          rounded="lg"
          role="button"
          :aria-pressed="isSelected(option.value).toString()"
          :aria-label="option.label"
          tabindex="0"
          @click="() => toggleOption(option.value)"
          @keydown.enter.prevent="toggleOption(option.value)"
          @keydown.space.prevent="toggleOption(option.value)"
        >
          <div class="nudge-toggle-card__body">
            <div class="nudge-toggle-card__icon-rail">
              <div class="nudge-toggle-card__icon-shell">
                <v-icon :icon="option.icon" size="26" />
              </div>
            </div>

            <div class="nudge-toggle-card__content">
              <p class="nudge-toggle-card__title">{{ option.label }}</p>
            </div>

            <div class="nudge-toggle-card__indicator" aria-hidden="true">
              <v-icon v-if="isSelected(option.value)" icon="mdi-check" size="20" color="white" />
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import type { ProductConditionChoice } from '~/utils/_nudge-tool-filters'

const props = defineProps<{
  modelValue: ProductConditionChoice[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: ProductConditionChoice[]): void
}>()

const { t } = useI18n()

const options: Array<{
  value: ProductConditionChoice
  label: string
  icon: string
}> = [
  {
    value: 'new',
    label: t('nudge-tool.steps.condition.options.new'),
    icon: 'mdi-star-outline',
  },
  {
    value: 'occasion',
    label: t('nudge-tool.steps.condition.options.occasion'),
    icon: 'mdi-recycle-variant',
  },
]

const isSelected = (choice: ProductConditionChoice) => {
  return props.modelValue.includes(choice)
}

const toggleOption = (choice: ProductConditionChoice) => {
  const nextSelection = isSelected(choice)
    ? props.modelValue.filter(entry => entry !== choice)
    : [...props.modelValue, choice]

  emit('update:modelValue', nextSelection)
}
</script>

<style scoped lang="scss">
.nudge-step-condition {
  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin-bottom: 4px;
  }

  &__subtitle {
    color: rgb(var(--v-theme-text-neutral-secondary));
    margin-bottom: 16px;
  }

  &__row {
    row-gap: 12px;
  }

  .nudge-toggle-card {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    justify-content: space-between;
    padding: 0;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-primary-050)) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    box-shadow: none;
    transition: transform 140ms ease, border-color 160ms ease,
      box-shadow 160ms ease, background-color 160ms ease;
    cursor: pointer;

    &__body {
      display: grid;
      grid-template-columns: 0.32fr 1fr auto;
      align-items: stretch;
      height: 100%;
    }

    &__icon-rail {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(var(--v-theme-accent-supporting), 0.08);
      padding: 14px;
      transition: background 160ms ease;
    }

    &__icon-shell {
      width: 44px;
      height: 44px;
      border-radius: 14px;
      display: grid;
      place-items: center;
      background: rgba(var(--v-theme-accent-supporting), 0.12);
      color: rgb(var(--v-theme-accent-supporting));
      box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.2);
      transition: background 160ms ease, color 160ms ease, box-shadow 160ms ease;
    }

    &__content {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 14px 18px;
      min-height: 100%;
    }

    &__title {
      margin: 0;
      font-weight: 700;
      flex: 1;
      line-height: 1.35;
    }

    &__indicator {
      width: 52px;
      display: grid;
      place-items: center;
      background: rgba(var(--v-theme-accent-supporting), 0.12);
      border-left: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
      transition: background 160ms ease, border-color 160ms ease;
    }

    &:hover,
    &:focus-visible {
      transform: translateY(-2px);
      border-color: rgba(var(--v-theme-accent-supporting), 0.6);
      box-shadow: 0 10px 20px rgba(var(--v-theme-shadow-primary-600), 0.08);
      outline: none;
    }

    &:focus-visible {
      box-shadow: 0 0 0 3px rgba(var(--v-theme-accent-supporting), 0.25);
    }
  }

  .nudge-toggle-card--selected {
    background: rgba(var(--v-theme-accent-supporting), 0.12) !important;
    border-color: rgb(var(--v-theme-accent-supporting));

    .nudge-toggle-card__icon-rail {
      background: rgba(var(--v-theme-accent-supporting), 0.18);
    }

    .nudge-toggle-card__icon-shell {
      background: rgba(var(--v-theme-accent-supporting), 0.2);
      color: rgb(var(--v-theme-surface-default));
      box-shadow: inset 0 0 0 1px rgba(var(--v-theme-accent-supporting), 0.5);
    }

    .nudge-toggle-card__indicator {
      background: rgb(var(--v-theme-accent-supporting));
      border-color: rgb(var(--v-theme-accent-supporting));
      color: rgb(var(--v-theme-surface-default));
    }
  }
}
</style>

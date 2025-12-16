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
          class="nudge-step-condition__card nudge-option-card"
          :class="{
            'nudge-option-card--selected': isSelected(option.value),
          }"
          rounded="lg"
          role="button"
          :aria-pressed="isSelected(option.value).toString()"
          @click="() => toggleOption(option.value)"
        >
          <div class="nudge-step-condition__icon-wrapper">
            <v-icon :icon="option.icon" size="26" />
            <v-icon
              v-if="isSelected(option.value)"
              icon="mdi-check-circle"
              size="18"
              class="nudge-step-condition__check"
            />
          </div>
          <p class="nudge-step-condition__label">{{ option.label }}</p>
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

  &__card {
    width: 100%;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 12px;
    justify-content: space-between;
    padding: 14px 16px;
  }

  &__label {
    margin: 0;
    font-weight: 600;
    flex: 1;
  }

  &__icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 14px;
    background: rgba(var(--v-theme-primary), 0.08);
    color: rgb(var(--v-theme-primary));
    position: relative;
  }

  &__check {
    position: absolute;
    right: -6px;
    top: -6px;
    color: rgb(var(--v-theme-primary));
  }
}
</style>

<template>
  <div class="nudge-step-condition">
    <h2 class="nudge-step-condition__title">{{ $t('nudge-tool.steps.condition.title') }}</h2>
    <p class="nudge-step-condition__subtitle">{{ $t('nudge-tool.steps.condition.subtitle') }}</p>

    <v-row dense>
      <v-col
        v-for="option in options"
        :key="option.value"
        cols="12"
        sm="4"
      >
        <v-card
          class="nudge-step-condition__card"
          :elevation="isSelected(option.value) ? 8 : 2"
          :color="isSelected(option.value) ? 'primary' : undefined"
          :variant="isSelected(option.value) ? 'elevated' : 'tonal'"
          rounded="xl"
          role="button"
          :aria-pressed="isSelected(option.value).toString()"
          @click="() => toggleOption(option.value)"
        >
          <v-icon :icon="option.icon" size="32" class="mb-2" />
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

const emit = defineEmits<{ (event: 'update:modelValue', value: ProductConditionChoice[]): void }>()

const { t } = useI18n()

const options: Array<{ value: ProductConditionChoice; label: string; icon: string }> = [
  { value: 'new', label: t('nudge-tool.steps.condition.options.new'), icon: 'mdi-star-outline' },
  { value: 'occasion', label: t('nudge-tool.steps.condition.options.occasion'), icon: 'mdi-recycle-variant' },
]

const isSelected = (choice: ProductConditionChoice) => {
  return props.modelValue.includes(choice)
}

const toggleOption = (choice: ProductConditionChoice) => {
  const nextSelection = isSelected(choice)
    ? props.modelValue.filter((entry) => entry !== choice)
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

  &__card {
    padding: 16px;
    text-align: center;
  }

  &__label {
    margin: 0;
    font-weight: 600;
  }
}
</style>

<template>
  <div class="nudge-step-subset">
    <div class="nudge-step-subset__header">
      <h2 class="nudge-step-subset__title">
        <v-icon
          :icon="`mdi-numeric-${stepNumber}-circle`"
          color="accent-primary-highlight"
          size="30"
        />
        {{ group.title }}
      </h2>
      <p v-if="group.description" class="nudge-step-subset__description">
        {{ group.description }}
      </p>
    </div>

    <v-row dense>
      <v-col v-for="subset in subsets" :key="subset.id" cols="12" sm="6">
        <v-tooltip
          location="top"
          :text="subset.description"
          :disabled="!subset.description"
        >
          <template #activator="{ props: activatorProps }">
            <v-card
              v-bind="activatorProps"
              class="nudge-step-subset__card nudge-option-card"
              :class="{
                'nudge-option-card--selected': modelValue.includes(
                  subset.id ?? ''
                ),
              }"
              rounded="lg"
              role="button"
              :aria-pressed="modelValue.includes(subset.id ?? '')"
              @click="toggle(subset.id ?? '')"
            >
              <div class="nudge-step-subset__body">
                <p class="nudge-step-subset__name">{{ subset.title }}</p>
                <p v-if="subset.caption" class="nudge-step-subset__caption">
                  {{ subset.caption }}
                </p>
              </div>
            </v-card>
          </template>
        </v-tooltip>
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import type {
  NudgeToolSubsetGroupDto,
  VerticalSubsetDto,
} from '~~/shared/api-client'

const props = defineProps<{
  group: NudgeToolSubsetGroupDto
  subsets: VerticalSubsetDto[]
  modelValue: string[]
  stepNumber: number
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string[]): void
  (event: 'continue'): void
}>()

const toggle = (subsetId: string) => {
  const next = new Set(props.modelValue)

  if (next.has(subsetId)) {
    next.delete(subsetId)
  } else {
    next.add(subsetId)
  }

  emit('update:modelValue', Array.from(next))
}
</script>

<style scoped lang="scss">
.nudge-step-subset {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__description {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__card {
    display: flex;
    flex-direction: column;
    align-items: center;
    height: 100%;
  }

  &__body {
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__name {
    margin: 0;
    font-weight: 700;
  }

  &__caption {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>

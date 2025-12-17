<template>
  <div class="nudge-step-subset">
    <!-- Header removed, moved to Wizard -->

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
                'nudge-option-card--selected': isSelected(subset.id),
              }"
              rounded="lg"
              role="button"
              :aria-pressed="isSelected(subset.id).toString()"
              @click="toggle(subset.id ?? '')"
            >
              <div class="d-flex align-stretch flex-grow-1">
                <div class="nudge-step-subset__content">
                  <div class="nudge-step-subset__text">
                    <p class="nudge-step-subset__name">{{ subset.title }}</p>
                    <p v-if="subset.caption" class="nudge-step-subset__caption">
                      {{ subset.caption }}
                    </p>
                  </div>
                  <div class="nudge-step-subset__icon-wrapper">
                    <v-icon :icon="getSubsetIcon(subset)" size="24" />
                  </div>
                </div>

                <div class="nudge-step-subset__indicator">
                  <v-icon v-if="isSelected(subset.id)" icon="mdi-check" size="20" color="white" />
                </div>
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

const isSelected = (subsetId?: string | null) =>
  subsetId ? props.modelValue.includes(subsetId) : false

const getSubsetIcon = (subset: VerticalSubsetDto) =>
  subset.mdiIcon ?? 'mdi-sprout'

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
    height: 100%;
    border-radius: 16px;
    padding: 0;
    overflow: hidden;
    background: rgb(var(--v-theme-surface-primary-050)) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
    transition: all 0.2s ease;
  }
  
  .nudge-option-card--selected {
    background: rgb(var(--v-theme-surface-primary-100)) !important;
    border-color: rgb(var(--v-theme-primary));
    
    .nudge-step-subset__indicator {
        background: rgb(var(--v-theme-primary));
        border-left: 1px solid rgb(var(--v-theme-primary));
    }
  }

  &__content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 16px;
  }

  &__indicator {
      width: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(var(--v-theme-primary), 0.1);
      border-left: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
      transition: background 0.2s ease;
  }

  &__text {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
  }

  &__name {
    margin: 0;
    font-weight: 700;
  }

  &__caption {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 14px;
    background: rgba(var(--v-theme-surface-default), 0.6);
    color: rgb(var(--v-theme-primary));
  }
}
</style>

<template>
  <div class="nudge-step-subset">
    <div class="nudge-step-subset__header">
      <div class="nudge-step-subset__title-block">
        <v-avatar
          class="nudge-step-subset__group-avatar"
          size="44"
          color="rgba(var(--v-theme-surface-primary-080), 0.9)"
        >
          <v-icon :icon="groupIcon" size="24" />
        </v-avatar>
        <div class="nudge-step-subset__titles">
          <p class="nudge-step-subset__title">{{ group.title }}</p>
          <p v-if="group.description" class="nudge-step-subset__description">
            {{ group.description }}
          </p>
        </div>
      </div>

      <v-chip
        v-if="categoryLabel"
        class="nudge-step-subset__category-chip"
        color="primary"
        variant="tonal"
        size="small"
        @click="emit('return-to-category')"
      >
        <v-icon start :icon="categoryIcon || 'mdi-tag-outline'" />
        {{ categoryLabel }}
      </v-chip>
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
import { computed } from 'vue'
import type {
  NudgeToolSubsetGroupDto,
  VerticalSubsetDto,
} from '~~/shared/api-client'

const props = defineProps<{
  group: NudgeToolSubsetGroupDto
  subsets: VerticalSubsetDto[]
  modelValue: string[]
  stepNumber: number
  categoryIcon?: string | null
  categoryLabel?: string | null
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string[]): void
  (event: 'continue'): void
  (event: 'return-to-category'): void
}>()

const isSelected = (subsetId?: string | null) =>
  subsetId ? props.modelValue.includes(subsetId) : false

const getSubsetIcon = (subset: VerticalSubsetDto) =>
  subset.mdiIcon ?? 'mdi-sprout'

const groupIcon = computed(() => props.group.mdiIcon ?? 'mdi-format-list-bulleted')

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

  &__title-block {
    display: inline-flex;
    align-items: center;
    gap: 10px;
  }

  &__group-avatar {
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__title {
    font-size: 1.1rem;
    font-weight: 700;
    margin: 0;
  }

  &__description {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__category-chip {
    font-weight: 700;
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

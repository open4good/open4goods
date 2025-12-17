<template>
  <div class="nudge-step-scores">
    <!-- Header removed, moved to Wizard -->

    <v-row class="nudge-step-scores__grid">
      <v-col v-for="score in scores" :key="score.scoreName" cols="12" sm="6">
        <v-tooltip
          location="top"
          :text="score.description"
          :disabled="!score.description"
        >
          <template #activator="{ props: activatorProps }">
            <v-card
              v-bind="activatorProps"
              class="nudge-step-scores__card nudge-option-card" variant="flat" border="thin"
              :class="{
                'nudge-option-card--selected': isSelected(score.scoreName),
                'nudge-step-scores__card--selected': isSelected(
                  score.scoreName ?? ''
                ),
              }"
              rounded="lg"
              role="button"
              :aria-pressed="isSelected(score.scoreName).toString()"
              @click="toggle(score.scoreName ?? '')"
            >
              <div class="d-flex align-stretch flex-grow-1">
                <div class="nudge-step-scores__content">
                  <div class="nudge-step-scores__titles">
                    <p class="nudge-step-scores__name">{{ score.title }}</p>
                    <p v-if="score.caption" class="nudge-step-scores__caption">
                      {{ score.caption }}
                    </p>
                  </div>
                  <div class="nudge-step-scores__icon-wrapper">
                    <v-icon :icon="getScoreIcon(score)" size="28" />
                  </div>
                </div>
                
                <div class="nudge-step-scores__indicator">
                  <v-icon v-if="isSelected(score.scoreName)" icon="mdi-check" size="20" color="white" />
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
import type { NudgeToolScoreDto } from '~~/shared/api-client'

const props = defineProps<{
  scores: NudgeToolScoreDto[]
  modelValue: string[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: string[]): void
  (event: 'continue'): void
}>()

const selectedNames = computed(() => props.modelValue)

const getScoreIcon = (score: NudgeToolScoreDto) => score.mdiIcon ?? 'mdi-leaf'

const isSelected = (scoreName?: string | null) =>
  scoreName ? selectedNames.value.includes(scoreName) : false

const toggle = (scoreName: string) => {
  const next = new Set(selectedNames.value)

  if (next.has(scoreName)) {
    next.delete(scoreName)
  } else {
    next.add(scoreName)
  }

  emit('update:modelValue', Array.from(next))
}
</script>

<style scoped lang="scss">
.nudge-step-scores {
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    flex-wrap: wrap;
  }

  &__title {
    font-size: 1.5rem;
    font-weight: 700;
    margin: 0;
  }

  &__grid {
    row-gap: 12px;
  }

  &__card {
    display: flex;
    flex-direction: column;
    height: 100%;
    border-radius: 16px;
    padding: 0;
    overflow: hidden;
    position: relative;
    background: rgb(var(--v-theme-surface-primary-050)) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.3);
    transition: all 0.2s ease;
  }

  &__card--selected {
    background: rgb(var(--v-theme-surface-primary-100)) !important;
    border-color: rgb(var(--v-theme-primary));
    
    .nudge-step-scores__indicator {
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

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
  }

  &__name {
    margin: 0;
    font-weight: 700;
    line-height: 1.4;
  }

  &__caption {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
    font-size: 0.95rem;
  }

  &__icon-wrapper {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 14px;
    background: rgba(var(--v-theme-surface-default), 0.6);
    position: relative;
    color: rgb(var(--v-theme-primary));
  }
}
</style>

<template>
  <div class="nudge-step-scores">
    <div class="nudge-step-scores__header">
      <div>
        <h2 class="nudge-step-scores__title">
          <v-icon
            icon="mdi-numeric-2-circle"
            color="accent-primary-highlight"
            size="30"
          />
          {{ $t('nudge-tool.steps.scores.title') }}
        </h2>
      </div>
    </div>

    <v-row dense class="nudge-step-scores__grid">
      <v-col v-for="score in scores" :key="score.scoreName" cols="12" sm="6">
        <v-tooltip
          location="top"
          :text="score.description"
          :disabled="!score.description"
        >
          <template #activator="{ props: activatorProps }">
            <v-card
              v-bind="activatorProps"
              class="nudge-step-scores__card nudge-option-card"
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
              <div class="nudge-step-scores__top-row">
                <div class="nudge-step-scores__titles">
                  <p class="nudge-step-scores__name">{{ score.title }}</p>
                  <p v-if="score.caption" class="nudge-step-scores__caption">
                    {{ score.caption }}
                  </p>
                </div>
                <div class="nudge-step-scores__icon-wrapper">
                  <v-icon :icon="getScoreIcon(score)" size="28" />
                  <v-icon
                    v-if="isSelected(score.scoreName)"
                    icon="mdi-check-circle"
                    size="18"
                    class="nudge-step-scores__check"
                  />
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
    gap: 8px;
    height: 100%;
    border-radius: 22px;
    padding: 14px 16px;
    position: relative;
  }

  &__card--selected {
    background: rgba(var(--v-theme-primary), 0.08) !important;
  }

  &__top-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  &__titles {
    display: flex;
    flex-direction: column;
    gap: 4px;
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
    background: rgba(var(--v-theme-primary), 0.08);
    position: relative;
    color: rgb(var(--v-theme-primary));
  }

  &__check {
    position: absolute;
    right: -6px;
    top: -6px;
    color: rgb(var(--v-theme-primary));
  }
}
</style>

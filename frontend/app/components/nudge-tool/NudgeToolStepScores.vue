<template>
  <div class="nudge-step-scores">
    <div class="nudge-step-scores__header">
      <div>
        <h2 class="nudge-step-scores__title">{{ $t('nudge-tool.steps.scores.title') }}</h2>
        <p class="nudge-step-scores__subtitle">{{ $t('nudge-tool.steps.scores.subtitle') }}</p>
      </div>

    </div>

    <v-row dense>
      <v-col
        v-for="score in scores"
        :key="score.scoreName"
        cols="12"
        sm="6"
      >
        <v-card
          class="nudge-step-scores__card"
          :elevation="selectedNames.includes(score.scoreName ?? '') ? 6 : 2"
          :variant="selectedNames.includes(score.scoreName ?? '') ? 'elevated' : 'tonal'"
          rounded="xl"
          role="button"
          :aria-pressed="selectedNames.includes(score.scoreName ?? '')"
          @click="toggle(score.scoreName ?? '')"
        >
          <div class="nudge-step-scores__icon">
            <v-icon :icon="getScoreIcon(score)" size="28" />
          </div>
          <div class="nudge-step-scores__content">
            <p class="nudge-step-scores__name">{{ score.title }}</p>
            <p class="nudge-step-scores__description">{{ score.description }}</p>
          </div>
        </v-card>
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

const emit = defineEmits<{ (event: 'update:modelValue', value: string[]): void; (event: 'continue'): void }>()

const selectedNames = computed(() => props.modelValue)
const hasSelection = computed(() => selectedNames.value.length > 0)

const getScoreIcon = (score: NudgeToolScoreDto) => score.mdiIcon ?? 'mdi-leaf'

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

  &__subtitle {
    margin: 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }

  &__card {
    display: flex;
    gap: 12px;
    padding: 16px;
    height: 100%;
    align-items: center;
  }

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    background: rgba(var(--v-theme-primary), 0.08);
  }

  &__name {
    margin: 0;
    font-weight: 700;
  }

  &__description {
    margin: 2px 0 0;
    color: rgb(var(--v-theme-text-neutral-secondary));
  }
}
</style>

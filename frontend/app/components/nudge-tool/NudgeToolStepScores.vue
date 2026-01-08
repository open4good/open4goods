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
              class="nudge-step-scores__card nudge-toggle-card"
              elevation="2"
              border="thin"
              :class="{
                'nudge-toggle-card--selected': isSelected(score.scoreName),
              }"
              rounded="lg"
              role="button"
              :aria-pressed="isSelected(score.scoreName).toString()"
              :aria-label="score.title"
              tabindex="0"
              @click="toggle(score.scoreName ?? '')"
              @keydown.enter.prevent="toggle(score.scoreName ?? '')"
              @keydown.space.prevent="toggle(score.scoreName ?? '')"
            >
              <div class="nudge-toggle-card__body">
                <div class="nudge-toggle-card__icon-rail">
                  <div class="nudge-toggle-card__icon-shell">
                    <v-icon :icon="getScoreIcon(score)" size="28" />
                  </div>
                </div>

                <div class="nudge-toggle-card__content">
                  <p class="nudge-toggle-card__title">{{ score.title }}</p>
                  <p v-if="score.caption" class="nudge-toggle-card__caption">
                    {{ score.caption }}
                  </p>
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

  .nudge-toggle-card {
    display: flex;
    flex-direction: column;
    height: 100%;
    border-radius: 16px;
    padding: 0;
    overflow: hidden;
    position: relative;
    background: rgb(var(--v-theme-surface-primary-050)) !important;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    /* box-shadow: none; Removed for elevation */
    transition:
      transform 140ms ease,
      border-color 160ms ease,
      box-shadow 160ms ease,
      background-color 160ms ease;
    cursor: pointer;

    &__body {
      display: grid;
      grid-template-columns: auto 1fr;
      align-items: center;
      height: 100%;
    }

    &__icon-rail {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(var(--v-theme-accent-supporting), 0.08);
      padding: 14px 16px;
      min-height: 100%;
      border-right: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
      border-radius: 14px 10px 10px 14px;
      transition:
        background 160ms ease,
        border-color 160ms ease;
    }

    &__icon-shell {
      width: 48px;
      height: 48px;
      border-radius: 16px;
      display: grid;
      place-items: center;
      background: rgba(var(--v-theme-accent-supporting), 0.12);
      color: rgb(var(--v-theme-accent-supporting));
      box-shadow: inset 0 0 0 1px
        rgba(var(--v-theme-border-primary-strong), 0.2);
      transition:
        background 160ms ease,
        color 160ms ease,
        box-shadow 160ms ease,
        transform 160ms ease;
    }

    &__content {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 14px 18px;
      justify-content: center;
      min-height: 100%;
    }

    &__title {
      margin: 0;
      font-weight: 700;
      line-height: 1.4;
    }

    &__caption {
      margin: 0;
      color: rgb(var(--v-theme-text-neutral-secondary));
      font-size: 0.95rem;
      line-height: 1.4;
    }

    &:hover,
    &:focus-visible {
      transform: translateY(-2px);
      border-color: rgba(var(--v-theme-accent-supporting), 0.6);
      /* Custom hover shadow */
      box-shadow: 0 10px 20px rgba(var(--v-theme-shadow-primary-600), 0.15) !important;
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
      background: rgba(var(--v-theme-accent-supporting), 0.2);
      border-color: rgba(var(--v-theme-accent-supporting), 0.55);
    }

    .nudge-toggle-card__icon-shell {
      background: rgba(var(--v-theme-accent-supporting), 0.22);
      color: rgb(var(--v-theme-surface-default));
      box-shadow: inset 0 0 0 1px rgba(var(--v-theme-accent-supporting), 0.55);
      transform: translateY(-1px);
    }

    .nudge-toggle-card__title {
      color: rgb(var(--v-theme-text-neutral-strong));
    }
  }
}
</style>

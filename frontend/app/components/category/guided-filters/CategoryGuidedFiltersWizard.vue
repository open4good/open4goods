<template>
  <v-dialog v-model="localModel" :scrim="true" transition="dialog-bottom-transition">
    <v-card
      v-if="wizard && wizard.questions && wizard.questions.length"
      class="category-guided-wizard"
      elevation="12"
      max-width="760"
    >
      <div class="category-guided-wizard__header">
        <div class="category-guided-wizard__progress">
          <v-progress-linear
            :model-value="progressValue"
            color="primary"
            height="6"
            rounded
            class="category-guided-wizard__progress-bar"
          />
          <span class="category-guided-wizard__progress-label">
            {{
              t('category.guidedFilters.progressLabel', {
                current: currentStepDisplay,
                total: totalSteps,
              })
            }}
          </span>
        </div>
        <v-btn
          icon
          variant="text"
          density="comfortable"
          :aria-label="t('category.guidedFilters.close')"
          @click="closeWizard"
        >
          <v-icon icon="mdi-close" />
        </v-btn>
      </div>

      <v-card-text>
        <div v-if="currentQuestion" class="category-guided-wizard__question">
          <div class="category-guided-wizard__question-copy">
            <p class="category-guided-wizard__eyebrow">
              {{ t('category.guidedFilters.stepTitle', { current: currentStepDisplay, total: totalSteps }) }}
            </p>
            <h3 class="category-guided-wizard__title">
              {{ currentQuestion.title }}
            </h3>
            <p v-if="currentQuestion.subtitle" class="category-guided-wizard__subtitle">
              {{ currentQuestion.subtitle }}
            </p>
            <p v-if="currentQuestion.description" class="category-guided-wizard__description">
              {{ currentQuestion.description }}
            </p>
            <v-alert
              v-if="currentQuestion.moreInformations"
              type="info"
              variant="tonal"
              border="start"
              class="mt-3"
            >
              {{ currentQuestion.moreInformations }}
            </v-alert>
            <v-btn
              v-if="currentQuestion.linkUrl && currentQuestion.linkTitle"
              class="mt-4"
              :href="currentQuestion.linkUrl"
              target="_blank"
              variant="text"
              color="primary"
              append-icon="mdi-open-in-new"
            >
              {{ currentQuestion.linkTitle }}
            </v-btn>
          </div>

          <div v-if="currentQuestion.image" class="category-guided-wizard__question-media">
            <v-img :src="currentQuestion.image" alt="" cover />
          </div>
        </div>

        <div v-if="currentQuestion" class="category-guided-wizard__choices">
          <v-sheet
            v-for="choice in currentQuestion.choices ?? []"
            :key="choice.id"
            v-ripple
            :tag="'button'"
            class="category-guided-wizard__choice"
            :class="{
              'category-guided-wizard__choice--selected': isChoiceSelected(choice.id ?? ''),
              'category-guided-wizard__choice--list': choice.display === 'BULLET_LIST',
            }"
            :disabled="isChoiceDisabled(choice.id ?? '')"
            role="button"
            :aria-pressed="isChoiceSelected(choice.id ?? '').toString()"
            :aria-disabled="isChoiceDisabled(choice.id ?? '') ? 'true' : 'false'"
            type="button"
            @click="onToggleChoice(choice.id ?? '')"
            @keydown.enter.prevent="onToggleChoice(choice.id ?? '')"
            @keydown.space.prevent="onToggleChoice(choice.id ?? '')"
          >
            <div class="category-guided-wizard__choice-content">
              <div>
                <p class="category-guided-wizard__choice-title">
                  {{ choice.title }}
                </p>
                <p v-if="choice.description" class="category-guided-wizard__choice-description">
                  {{ choice.description }}
                </p>
                <ul v-if="choice.display === 'BULLET_LIST' && choice.bulletPoints?.length" class="category-guided-wizard__choice-bullets">
                  <li v-for="point in choice.bulletPoints" :key="point">
                    <v-icon icon="mdi-check-circle" size="18" color="primary" />
                    <span>{{ point }}</span>
                  </li>
                </ul>
              </div>
              <v-icon
                v-if="isChoiceSelected(choice.id ?? '')"
                icon="mdi-check-circle"
                color="primary"
                size="28"
              />
            </div>
          </v-sheet>
        </div>

        <div v-if="showResultsSection" class="category-guided-wizard__results" aria-live="polite">
          <v-progress-circular
            v-if="loadingCount"
            indeterminate
            color="primary"
            size="20"
            class="mr-2"
          />
          <p>
            {{
              loadingCount
                ? t('category.guidedFilters.resultsLoading')
                : t('category.guidedFilters.resultsLabel', {
                    count: formattedResultsCount,
                    category: resolvedWizardCategory,
                  })
            }}
          </p>
        </div>
      </v-card-text>

      <v-card-actions class="category-guided-wizard__actions">
        <v-btn variant="text" color="secondary" @click="skipCurrentQuestion">
          {{ t('category.guidedFilters.skip') }}
        </v-btn>
        <v-spacer />
        <v-btn
          variant="text"
          color="secondary"
          :disabled="currentStepDisplay === 1"
          @click="goToPrevious"
        >
          {{ t('category.guidedFilters.previous') }}
        </v-btn>
        <v-btn
          color="primary"
          :loading="false"
          :disabled="!hasSelectionForCurrent && !isLastStep"
          @click="onPrimaryAction"
        >
          {{ isLastStep ? t('category.guidedFilters.apply') : t('category.guidedFilters.next') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { FilterRequestDto, ModalFilterWizardDto } from '~~/shared/api-client'

import {
  buildWizardFilterRequest,
  type ModalWizardSelections,
  isSelectionLimitReached,
} from '~/utils/guided-modal-filters'

const props = defineProps<{
  modelValue: boolean
  wizard: ModalFilterWizardDto | null
  productCount: number | null
  loadingCount: boolean
  categoryName?: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  'filters-change': [FilterRequestDto | null]
  apply: [FilterRequestDto | null]
}>()

const { t, n } = useI18n()

const resolvedWizardCategory = computed(() => {
  const name = props.categoryName?.trim()
  if (name) {
    return name
  }

  return t('category.guidedFilters.defaultCategory')
})

const localModel = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const currentStep = ref(0)
const selections = ref<ModalWizardSelections>({})
const questions = computed(() => props.wizard?.questions ?? [])
const totalSteps = computed(() => questions.value.length || 1)
const currentQuestion = computed(() => questions.value[currentStep.value] ?? null)
const currentStepDisplay = computed(() => Math.min(currentStep.value + 1, totalSteps.value))
const progressValue = computed(() => (totalSteps.value ? (currentStepDisplay.value / totalSteps.value) * 100 : 0))
const selectionForCurrent = computed(() => {
  if (!currentQuestion.value?.id) {
    return []
  }
  return selections.value[currentQuestion.value.id] ?? []
})
const hasSelectionForCurrent = computed(() => selectionForCurrent.value.length > 0)
const isLastStep = computed(() => currentStepDisplay.value === totalSteps.value)
const formattedResultsCount = computed(() =>
  props.productCount == null ? t('category.guidedFilters.resultsFallback') : n(props.productCount, 'decimal'),
)
const showResultsSection = computed(() => props.loadingCount || props.productCount != null)

const resetWizard = () => {
  selections.value = {}
  currentStep.value = 0
}

const emitFiltersChange = () => {
  if (!props.modelValue) {
    return
  }

  const filters = buildWizardFilterRequest(props.wizard, selections.value, currentStep.value)
  emit('filters-change', filters)
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      currentStep.value = 0
      emitFiltersChange()
      return
    }

    emit('filters-change', null)
    resetWizard()
  },
)

watch(
  () => props.wizard,
  () => {
    resetWizard()
  },
)

watch(
  selections,
  () => {
    emitFiltersChange()
  },
  { deep: true },
)

const isChoiceSelected = (choiceId: string): boolean => {
  if (!choiceId || !currentQuestion.value?.id) {
    return false
  }
  const selected = selections.value[currentQuestion.value.id] ?? []
  return selected.includes(choiceId)
}

const isChoiceDisabled = (choiceId: string): boolean => {
  if (!currentQuestion.value || !choiceId) {
    return false
  }
  const selected = selections.value[currentQuestion.value.id] ?? []
  return !selected.includes(choiceId) && isSelectionLimitReached(currentQuestion.value, selected, choiceId)
}

const onToggleChoice = (choiceId: string) => {
  if (!choiceId || !currentQuestion.value?.id) {
    return
  }

  const questionId = currentQuestion.value.id
  const currentSelection = selections.value[questionId] ?? []

  if ((currentQuestion.value.selectionType ?? 'SINGLE') === 'SINGLE') {
    selections.value = { ...selections.value, [questionId]: [choiceId] }
    return
  }

  const exists = currentSelection.includes(choiceId)
  const next = exists
    ? currentSelection.filter((value) => value !== choiceId)
    : isChoiceDisabled(choiceId)
      ? currentSelection
      : [...currentSelection, choiceId]

  selections.value = { ...selections.value, [questionId]: next }
}

const skipCurrentQuestion = () => {
  if (currentQuestion.value?.id) {
    const questionId = currentQuestion.value.id
    const { [questionId]: _removed, ...remaining } = selections.value
    selections.value = remaining
  }

  if (!isLastStep.value) {
    currentStep.value = Math.min(currentStep.value + 1, totalSteps.value - 1)
    emitFiltersChange()
  }
}

const goToPrevious = () => {
  if (currentStepDisplay.value === 1) {
    return
  }

  currentStep.value = Math.max(0, currentStep.value - 1)
  emitFiltersChange()
}

const closeWizard = () => {
  emit('update:modelValue', false)
}

const onPrimaryAction = () => {
  if (isLastStep.value) {
    const filters = buildWizardFilterRequest(props.wizard, selections.value)
    emit('apply', filters)
    emit('update:modelValue', false)
    return
  }

  if (!hasSelectionForCurrent.value) {
    return
  }

  currentStep.value = Math.min(currentStep.value + 1, totalSteps.value - 1)
  emitFiltersChange()
}
</script>

<style scoped lang="sass">
.category-guided-wizard
  display: flex
  flex-direction: column
  width: 100%
  max-width: 760px
  border-radius: 20px
  background: rgb(var(--v-theme-surface-default))

  &__header
    display: flex
    align-items: center
    justify-content: space-between
    padding: 1.5rem 1.5rem 0

  &__progress
    flex: 1
    margin-right: 1rem

  &__progress-label
    display: inline-flex
    margin-top: 0.35rem
    font-size: 0.85rem
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

  &__question
    display: grid
    gap: 1.5rem
    align-items: flex-start

  &__question-copy
    display: flex
    flex-direction: column
    gap: 0.5rem

  &__eyebrow
    text-transform: uppercase
    letter-spacing: 0.08em
    font-size: 0.75rem
    color: rgba(var(--v-theme-text-neutral-secondary), 0.9)

  &__title
    margin: 0
    font-weight: 700
    font-size: 1.5rem

  &__subtitle
    margin: 0
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

  &__description
    margin: 0
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

  &__question-media
    display: none

  &__choices
    display: grid
    gap: 0.75rem
    margin-top: 1.5rem

  &__choice
    border-radius: 16px
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)
    padding: 1rem 1.25rem
    cursor: pointer
    text-align: left
    transition: border-color 0.2s ease, box-shadow 0.2s ease

    &:disabled
      opacity: 0.5
      cursor: not-allowed

    &--selected
      border-color: rgba(var(--v-theme-primary), 0.6)
      box-shadow: 0 8px 20px rgba(var(--v-theme-shadow-primary-600), 0.25)

  &__choice-content
    display: flex
    align-items: flex-start
    justify-content: space-between
    gap: 0.75rem

  &__choice-title
    margin: 0
    font-weight: 600
    font-size: 1rem

  &__choice-description
    margin: 0.25rem 0 0
    color: rgba(var(--v-theme-text-neutral-secondary), 0.95)

  &__choice-bullets
    list-style: none
    padding: 0
    margin: 0.5rem 0 0
    display: flex
    flex-direction: column
    gap: 0.5rem

    li
      display: flex
      align-items: center
      gap: 0.5rem

  &__results
    display: flex
    align-items: center
    gap: 0.5rem
    margin-top: 1rem
    font-weight: 600

  &__actions
    padding: 1.5rem

@media (min-width: 720px)
  .category-guided-wizard__question
    grid-template-columns: minmax(0, 1fr) 200px

  .category-guided-wizard__question-media
    display: block

  .category-guided-wizard__choices
    grid-template-columns: repeat(2, minmax(0, 1fr))
</style>

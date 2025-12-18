<template>
  <RoundedCornerCard
    class="nudge-wizard"
    rounded="xl"
    :elevation="3"
    accent-corner="top-left"
    corner-variant="custom"
    :corner-size="resolvedCornerSize"
    :style="wizardStyle"
    :class="{ 'nudge-wizard--content-mode': isContentMode }"
  >
    <template #corner>
      <div
        class="nudge-wizard__corner-content"
        :class="{
          'nudge-wizard__corner-content--clickable': shouldShowMatches,
          'nudge-wizard__corner-content--expanded': isContentMode,
        }"
        @click="handleCornerClick"
      >
        <div v-if="activeStepKey === 'category'" class="text-h5 font-weight-bold text-white">
          {{ $t('nudge-tool.wizard.welcome') }}
        </div>
        <div v-else-if="categorySummary" class="d-flex flex-column align-center justify-center fill-height pt-4 pb-2">
             <div class="nudge-wizard__corner-icon mb-1">
                <v-icon :icon="categorySummary.icon" :size="cornerIconSize" color="white" />
             </div>
             <div class="text-caption font-weight-bold lh-1 text-white mb-1">{{ categorySummary.label }}</div>
             <div class="text-caption text-white font-weight-black" style="font-size: 1.1em;">{{ animatedMatches }}</div>
        </div>
      </div>
    </template>
    <div ref="headerRef">
      <NudgeWizardHeader
        :title="currentStepTitle"
        :subtitle="currentStepSubtitle"
        :accent-corner="'top-left'"
        :corner-size="resolvedCornerSize"
        :category-summary="categorySummary"
      />
    </div>

    <div v-if="loading" class="nudge-wizard__progress">
      <v-progress-linear indeterminate color="primary" rounded bar-height="4" />
    </div>

    <div ref="windowWrapperRef" class="nudge-wizard__window-wrapper" :style="windowWrapperStyle">
      <v-window
        v-model="activeStepKey"
        class="nudge-wizard__window"
        :touch="false"
        :transition="windowTransition"
        :reverse-transition="windowReverseTransition"
        :style="windowContentStyle"
      >
        <v-window-item v-for="step in steps" :key="step.key" :value="step.key">
          <component
            :is="step.component"
            v-bind="step.props"
            @select="onCategorySelect"
            @update:model-value="(value: unknown) => step.onUpdate?.(value)"
            @continue="goToNext"
            @return-to-category="resetForCategorySelection"
          />
        </v-window-item>
      </v-window>
    </div>
    <div ref="footerRef" class="nudge-wizard__footer">
      <div
        v-if="progressSteps.length"
        class="nudge-wizard__progress-bubbles"
        :style="footerOffsetStyle"
      >
        <v-btn
          v-for="step in progressSteps"
          :key="step.key"
          class="nudge-wizard__progress-bubble"
          :variant="step.key === activeStepKey ? 'flat' : 'text'"
          :color="step.key === activeStepKey ? 'primary' : undefined"
          :aria-label="step.title"
          :disabled="!canAccessStep(step.key)"
          icon
          size="44"
          @click="() => handleProgressClick(step.key)"
        >
          <span class="nudge-wizard__progress-index">{{ step.index }}</span>
        </v-btn>
      </div>
      <div class="nudge-wizard__footer-actions">
        <v-btn
          v-if="hasPreviousStep"
          variant="text"
          prepend-icon="mdi-chevron-left"
          class="nudge-wizard__footer-btn"
          @click="goToPrevious"
        >
          {{ $t('nudge-tool.actions.previous') }}
        </v-btn>
        <div v-else></div>

        <v-btn
          v-if="hasNextStep"
          color="primary"
          variant="flat"
          :disabled="isNextDisabled"
          append-icon="mdi-chevron-right"
          class="nudge-wizard__footer-btn"
          @click="goToNext"
        >
          {{ $t('nudge-tool.actions.next') }}
        </v-btn>
      </div>
    </div>
  </RoundedCornerCard>
</template>

<script setup lang="ts">
import { useDebounceFn, useElementSize } from '@vueuse/core'
import { useCategories } from '~/composables/categories/useCategories'
import NudgeWizardHeader from '~/components/nudge-tool/NudgeWizardHeader.vue'
import type { CornerSize } from '~/components/shared/cards/RoundedCornerCard.vue'
import {
  NudgeToolStepCategory,
  NudgeToolStepCondition,
  NudgeToolStepRecommendations,
  NudgeToolStepScores,
  NudgeToolStepSubsetGroup,
  RoundedCornerCard,
} from '#components'
import type {
  CategoryHashState,
  Filter,
  FilterRequestDto,
  NudgeToolSubsetGroupDto,
  ProductConditionSelection,
  ProductDto,
  ProductSearchResponseDto,
  VerticalCategoryDto,
  VerticalConfigDto,
  VerticalSubsetDto,
} from '~/shared/api-client'
import { buildCategoryHash } from '~/utils/_category-filter-state'
import {
  buildConditionFilter,
  buildNudgeFilterRequest,
  buildScoreFilters,
} from '~/utils/_nudge-tool-filters'
import { buildFilterRequestFromSubsets } from '~/utils/_subset-to-filters'

const props = defineProps<{
  verticals?: VerticalConfigDto[]
  initialFilters?: FilterRequestDto
  initialCategoryId?: string | null
  initialSubsets?: string[]
}>()

const accentCornerOffsets: Record<CornerSize, string> = {
  sm: '46px',
  md: '58px',
  lg: '72px',
  xl: '120px',
}

const emit = defineEmits<{
  (e: 'navigate', payload: { hash: string; categorySlug: string }): void
}>()

const { t } = useI18n()
const router = useRouter()
const { fetchCategories } = useCategories()

const categories = useState<VerticalCategoryDto[]>('nudge-categories', () => [])
const selectedCategoryId = ref<string | null>(props.initialCategoryId ?? null)
const condition = ref<ProductConditionSelection>([])
const selectedScores = ref<string[]>([])
const activeSubsetIds = ref<string[]>(props.initialSubsets ?? [])
const baseFilters = ref<Filter[]>(props.initialFilters?.filters ?? [])
const recommendations = ref<ProductDto[]>([])
const totalMatches = ref(0)
const loading = ref(false)
const animatedMatches = ref(0)

const activeStepKey = ref('category')
const previousStepKey = ref<string | null>(null)
const visitedStepKeys = ref<string[]>(['category'])

const selectedCategory = computed(
  () =>
    categories.value.find(entry => entry.id === selectedCategoryId.value) ??
    null
)

const nudgeConfig = computed(() => selectedCategory.value?.nudgeToolConfig)

const subsetGroups = computed<NudgeToolSubsetGroupDto[]>(() => {
  const explicit = nudgeConfig.value?.subsetGroups ?? []
  if (explicit.length) {
    return explicit
  }

  const seen = new Set<string>()
  const subsets = nudgeConfig.value?.subsets ?? []

  return subsets
    .map(subset => subset.group)
    .filter((groupId): groupId is string => Boolean(groupId))
    .filter(groupId => {
      if (seen.has(groupId)) {
        return false
      }
      seen.add(groupId)
      return true
    })
    .map(groupId => ({ id: groupId, title: groupId }))
})

const groupedSubsets = computed<Record<string, VerticalSubsetDto[]>>(() => {
  const groups: Record<string, VerticalSubsetDto[]> = {}

  ;(nudgeConfig.value?.subsets ?? []).forEach(subset => {
    const key = subset.group ?? 'default'
    if (!groups[key]) {
      groups[key] = []
    }
    groups[key].push(subset)
  })

  return groups
})

const subsetFilterRequest = computed(() =>
  buildFilterRequestFromSubsets(
    nudgeConfig.value?.subsets ?? [],
    activeSubsetIds.value
  )
)

const scoreFilters = computed(() =>
  buildScoreFilters(nudgeConfig.value?.scores ?? [], selectedScores.value)
)

const conditionFilter = computed(() => buildConditionFilter(condition.value))

const filterRequest = computed<FilterRequestDto>(() => {
  const subsetGroups = subsetFilterRequest.value.filterGroups ?? []
  return buildNudgeFilterRequest(
    baseFilters.value,
    conditionFilter.value,
    scoreFilters.value,
    subsetGroups
  )
})

const hashState = computed<CategoryHashState>(() => ({
  filters:
    filterRequest.value.filters?.length ||
    filterRequest.value.filterGroups?.length
      ? filterRequest.value
      : undefined,
  activeSubsets: activeSubsetIds.value,
}))

type WizardStep = {
  key: string
  component: Component
  props: Record<string, unknown>
  title: string
  subtitle?: string
  onUpdate?: (value: unknown) => void
}

const steps = computed<WizardStep[]>(() => {
  const sequence: WizardStep[] = []

  sequence.push({
    key: 'category',
    component: NudgeToolStepCategory,
    title: t('nudge-tool.steps.category.title'),
    subtitle: t('nudge-tool.steps.category.subtitle'),
    props: {
      categories: categories.value,
      selectedCategoryId: selectedCategoryId.value,
    },
  })

  if ((nudgeConfig.value?.scores?.length ?? 0) > 0) {
    sequence.push({
      key: 'scores',
      component: NudgeToolStepScores,
      title: t('nudge-tool.steps.scores.title'),
      subtitle: t('nudge-tool.steps.scores.subtitle'),
      props: {
        modelValue: selectedScores.value,
        scores: nudgeConfig.value?.scores ?? [],
      },
      onUpdate: (value: string[]) => (selectedScores.value = value),
    })
  }

  sequence.push({
    key: 'condition',
    component: NudgeToolStepCondition,
    title: t('nudge-tool.steps.condition.title'),
    subtitle: t('nudge-tool.steps.condition.subtitle'),
    props: { modelValue: condition.value },
    onUpdate: (value: ProductConditionSelection) => {
      condition.value = value
    },
  })

  let subsetStepNumber = sequence.length + 1

  subsetGroups.value.forEach(group => {
    const subsets = groupedSubsets.value[group.id ?? ''] ?? []
    if (!subsets.length) {
      return
    }

    sequence.push({
      key: `group-${group.id}`,
      component: NudgeToolStepSubsetGroup,
      title: group.title ?? '',
      subtitle: t('nudge-tool.steps.subset.subtitle'),
      props: {
        group,
        subsets,
        modelValue: activeSubsetIds.value,
        stepNumber: subsetStepNumber,
        categoryIcon: selectedCategory.value?.mdiIcon,
        categoryLabel:
          selectedCategory.value?.verticalHomeTitle ??
          selectedCategory.value?.id ??
          '',
      },
      onUpdate: (value: string[]) => (activeSubsetIds.value = value),
    })

    subsetStepNumber += 1
  })

  sequence.push({
    key: 'recommendations',
    component: NudgeToolStepRecommendations,
    title: t('nudge-tool.steps.recommendations.title'),
    subtitle: t('nudge-tool.steps.recommendations.subtitle'),
    props: {
      products: recommendations.value,
      popularAttributes: selectedCategory.value?.attributesConfig?.configs,
      totalCount: totalMatches.value,
      loading: loading.value,
    },
  })

  return sequence
})

const currentStep = computed(() =>
  steps.value.find(step => step.key === activeStepKey.value)
)

const currentStepTitle = computed(() => currentStep.value?.title ?? '')
const currentStepSubtitle = computed(() => currentStep.value?.subtitle ?? '')

watch(
  activeStepKey,
  next => {
    if (!visitedStepKeys.value.includes(next)) {
      visitedStepKeys.value = [...visitedStepKeys.value, next]
    }
  },
  { immediate: true }
)

watch(
  steps,
  allSteps => {
    if (!allSteps.find(step => step.key === activeStepKey.value)) {
      activeStepKey.value = allSteps[0]?.key ?? 'recommendations'
    }

    const validKeys = allSteps.map(step => step.key)
    visitedStepKeys.value = visitedStepKeys.value.filter(key =>
      validKeys.includes(key)
    )
  },
  { immediate: true, deep: true }
)

const goToNext = () => {
  if (isNextDisabled.value) {
    return
  }

  const index = steps.value.findIndex(step => step.key === activeStepKey.value)
  const nextStep = steps.value[index + 1]
  if (nextStep) {
    activeStepKey.value = nextStep.key
  }
}

const goToPrevious = () => {
  const index = steps.value.findIndex(step => step.key === activeStepKey.value)
  const previousStep = steps.value[index - 1]
  if (previousStep) {
    if (previousStep.key === 'category') {
      resetForCategorySelection()
    } else {
      activeStepKey.value = previousStep.key
    }
  }
}

const getFirstContentStepKey = () =>
  steps.value.find(step => step.key !== 'category')?.key

const onCategorySelect = async (categoryId: string) => {
  selectedCategoryId.value = categoryId
  await nextTick()
  const nextStepKey = getFirstContentStepKey()
  if (nextStepKey) {
    activeStepKey.value = nextStepKey
  }
}

const fetchRecommendations = async () => {
  if (!selectedCategoryId.value) {
    return
  }

  loading.value = true

  try {
    const hasFilters =
      (filterRequest.value.filters?.length ?? 0) > 0 ||
      (filterRequest.value.filterGroups?.length ?? 0) > 0

    const response = await $fetch<ProductSearchResponseDto>(
      '/api/products/search',
      {
        method: 'POST',
        body: {
          verticalId: selectedCategoryId.value,
          pageNumber: 0,
          pageSize: 3,
          sort: { sorts: [{ field: 'scores.ECOSCORE.value', order: 'desc' }] },
          filters: hasFilters ? filterRequest.value : undefined,
        },
      }
    )

    recommendations.value = response.products?.data ?? []
    totalMatches.value = response.products?.page?.totalElements ?? 0
  } finally {
    loading.value = false
  }
}

const debouncedFetch = useDebounceFn(fetchRecommendations, 250)

watch(
  () => [filterRequest.value, selectedCategoryId.value],
  () => {
    debouncedFetch()
  },
  { deep: true }
)

let animationFrame: number | null = null

watch(
  totalMatches,
  next => {
    if (!import.meta.client) {
      animatedMatches.value = next
      return
    }

    if (animationFrame != null) {
      cancelAnimationFrame(animationFrame)
    }

    const startValue = animatedMatches.value
    const delta = next - startValue
    const duration = 450
    const startTime = performance.now()

    const step = (timestamp: number) => {
      const progress = Math.min((timestamp - startTime) / duration, 1)
      animatedMatches.value = Math.round(startValue + delta * progress)

      if (progress < 1) {
        animationFrame = requestAnimationFrame(step)
      } else {
        animationFrame = null
      }
    }

    animationFrame = requestAnimationFrame(step)
  },
  { immediate: true }
)

const hydrateCategories = async () => {
  if (categories.value.length) {
    return
  }

  const result = await fetchCategories(true)
  categories.value = result
}

const navigateToCategoryPage = () => {
  if (!selectedCategory.value) {
    return
  }

  const slug =
    selectedCategory.value.verticalHomeUrl?.replace(/^\//u, '') ??
    selectedCategory.value.id ??
    ''
  const hash = buildCategoryHash(hashState.value)

  emit('navigate', { hash, categorySlug: slug })

  void router.push({ path: `/${slug}`, hash })
}

const handleCornerClick = () => {
    if (shouldShowMatches.value) {
        navigateToCategoryPage()
    }
}

const hasPreviousStep = computed(() => {
  const index = steps.value.findIndex(step => step.key === activeStepKey.value)
  return index > 0
})

const hasNextStep = computed(() => {
  if (activeStepKey.value === 'category') {
    return false
  }

  const index = steps.value.findIndex(step => step.key === activeStepKey.value)
  return index >= 0 && index < steps.value.length - 1
})

const isNextDisabled = computed(() => {
  if (activeStepKey.value === 'category') {
    return !selectedCategoryId.value
  }

  return false
})

const canAccessStep = (stepKey: string) =>
  visitedStepKeys.value.includes(stepKey)

const progressSteps = computed(() => {
  if (activeStepKey.value === 'category') {
    return []
  }

  return steps.value
    .filter(step => step.key !== 'category')
    .filter(step => visitedStepKeys.value.includes(step.key))
    .map((step, index) => ({
      key: step.key,
      title: step.title,
      index: index + 1,
    }))
})

const handleProgressClick = (stepKey: string) => {
  if (!canAccessStep(stepKey)) {
    return
  }

  activeStepKey.value = stepKey
}

const shouldShowMatches = computed(
  () => Boolean(selectedCategory.value) && activeStepKey.value !== 'category'
)

const isCategoryToCondition = computed(
  () =>
    previousStepKey.value === 'category' && activeStepKey.value === 'condition'
)

const isConditionToCategory = computed(
  () =>
    previousStepKey.value === 'condition' && activeStepKey.value === 'category'
)

const windowTransition = computed(() =>
  isCategoryToCondition.value
    ? 'nudge-wizard-lift-fade'
    : 'nudge-wizard-slide-fade'
)

const windowReverseTransition = computed(() =>
  isConditionToCategory.value
    ? 'nudge-wizard-lift-fade-reverse'
    : 'nudge-wizard-slide-fade-reverse'
)

const categorySummary = computed(() => {
  if (!selectedCategory.value || activeStepKey.value === 'category') {
    return null
  }

  return {
    label:
      selectedCategory.value.verticalHomeTitle ??
      selectedCategory.value.id ??
      '',
    image: selectedCategory.value.imageSmall,
    icon: selectedCategory.value.mdiIcon ?? 'mdi-tag',
    alt:
      selectedCategory.value.verticalHomeTitle ??
      selectedCategory.value.id ??
      '',
  }
})

const windowTransitionDurationMs = 500

const resetCategorySelectionState = () => {
  selectedCategoryId.value = null
  selectedScores.value = []
  activeSubsetIds.value = []
  condition.value = []
  recommendations.value = []
  totalMatches.value = 0
  visitedStepKeys.value = ['category']
  // Keep locked heights intact so returning to the wizard keeps the layout stable
}

let resetTimeout: ReturnType<typeof setTimeout> | null = null

const clearResetTimeout = () => {
  if (resetTimeout) {
    clearTimeout(resetTimeout)
    resetTimeout = null
  }
}

const scheduleCategoryReset = () => {
  clearResetTimeout()
  resetTimeout = setTimeout(() => {
    resetCategorySelectionState()
    resetTimeout = null
  }, windowTransitionDurationMs)
}

const resetForCategorySelection = () => {
  activeStepKey.value = 'category'
  scheduleCategoryReset()
}

watch(
  activeStepKey,
  (next, previous) => {
    previousStepKey.value = previous ?? null

    if (next === 'category' && previous !== 'category') {
      scheduleCategoryReset()
      return
    }

    if (next !== 'category') {
      clearResetTimeout()
      // Lock current height as base if moving from category to content
      if (previous === 'category') {
         // Logic handled in height watcher
      }
    }
  },
  { flush: 'pre' }
)

onMounted(async () => {
  await hydrateCategories()

  if (selectedCategoryId.value && !selectedCategory.value) {
    selectedCategoryId.value =
      categories.value.find(cat => cat.id === selectedCategoryId.value)?.id ??
      null
  }

  if (props.initialCategoryId) {
    const nextStepKey = getFirstContentStepKey()
    if (nextStepKey) {
      activeStepKey.value = nextStepKey
    }
  }

  debouncedFetch()
})

const headerRef = ref<HTMLElement>()
const windowWrapperRef = ref<HTMLElement>()
const footerRef = ref<HTMLElement>()

const { height: headerHeight } = useElementSize(headerRef)
const { height: windowHeight } = useElementSize(windowWrapperRef)
const { height: footerHeight } = useElementSize(footerRef)

const isContentMode = computed(() => activeStepKey.value !== 'category')

const WIZARD_MIN_HEIGHT = 300

const lockedLayoutHeight = ref<number | null>(null)
const lockedWindowHeight = ref<number | null>(null)

const attemptLockHeights = () => {
  if (!isContentMode.value) {
    return
  }

  if (
    lockedLayoutHeight.value !== null ||
    lockedWindowHeight.value !== null ||
    headerHeight.value <= 0 ||
    windowHeight.value <= 0 ||
    footerHeight.value <= 0
  ) {
    return
  }

  const lockedWindow = Math.max(windowHeight.value, WIZARD_MIN_HEIGHT)
  const layoutHeight = headerHeight.value + lockedWindow + footerHeight.value
  lockedLayoutHeight.value = Math.max(layoutHeight, WIZARD_MIN_HEIGHT)
  lockedWindowHeight.value = lockedWindow
}

watch([isContentMode, headerHeight, windowHeight, footerHeight], attemptLockHeights, {
  immediate: true,
})

const wizardStyle = computed(() => {
  if (!lockedLayoutHeight.value) {
    return undefined
  }

  return { minHeight: `${lockedLayoutHeight.value}px` }
})

const windowWrapperStyle = computed(() => {
  if (!lockedWindowHeight.value) {
    return undefined
  }

  return {
    minHeight: `${lockedWindowHeight.value}px`,
    maxHeight: `${lockedWindowHeight.value}px`,
  }
})

const windowContentStyle = computed(() => {
  if (!lockedWindowHeight.value) {
    return undefined
  }

  return { minHeight: `${lockedWindowHeight.value}px` }
})

const resolvedCornerSize = computed(() =>
  isContentMode.value ? 'xl' : 'lg'
)

const footerOffsetStyle = computed(() => ({
  paddingLeft: accentCornerOffsets[resolvedCornerSize.value],
}))

const cornerIconSize = computed(() => (isContentMode.value ? 38 : 32))

</script>

<style scoped lang="scss">
.nudge-wizard__stepper :deep(.v-stepper-window) {
  display: none !important;
}

.nudge-wizard {
  position: relative;
  padding: clamp(1.5rem, 3vw, 2rem);
  width: 100%;
  max-width: none;
  display: flex;
  flex-direction: column;

  &__corner-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    text-align: center;
    line-height: 1.1;
    transition: transform 300ms ease, opacity 260ms ease;

    &--clickable {
      cursor: pointer;

      &:hover {
        opacity: 0.8;
      }
    }

    &--expanded {
      transform: translateY(2px) scale(1.04);
    }
  }

  &__stepper {
    width: 100%;
    min-width: 0;
  }



  &__progress {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }

  &__window {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    height: 100%;
  }

  &__window-wrapper {
    width: 100%;
    overflow: hidden;
  }

  &__footer {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 12px;
    margin-top: 16px;
    align-items: center;

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
      justify-items: stretch;
    }
  }

  &__progress-bubbles {
    display: flex;
    gap: 10px;
    align-items: center;
    flex-wrap: wrap;
  }

  &__progress-bubble {
    min-width: 44px;
    min-height: 44px;
    border-radius: 50%;
    box-shadow: none;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    transition: transform 140ms ease, border-color 140ms ease;

    &:hover:not(.v-btn--disabled) {
      transform: translateY(-2px);
      border-color: rgba(var(--v-theme-border-primary-strong), 0.7);
    }
  }

  &__progress-index {
    font-weight: 700;
    font-size: 0.95rem;
    color: currentColor;
  }

  &__footer-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    flex-wrap: wrap;

    @media (max-width: 960px) {
      justify-content: space-between;
    }
  }

  &__footer-btn {
    font-weight: 700;
  }
}

.nudge-wizard--content-mode .nudge-wizard__corner-content {
  transform: translateY(2px) scale(1.04);
}

:deep(.nudge-wizard-slide-fade-enter-active),
:deep(.nudge-wizard-slide-fade-leave-active),
:deep(.nudge-wizard-slide-fade-reverse-enter-active),
:deep(.nudge-wizard-slide-fade-reverse-leave-active) {
  transition:
    transform 500ms ease,
    opacity 500ms ease;
  will-change: transform, opacity;
}

:deep(.nudge-wizard-slide-fade-enter-from),
:deep(.nudge-wizard-slide-fade-reverse-leave-to) {
  transform: translateX(16px);
  opacity: 0;
}

:deep(.nudge-wizard-slide-fade-leave-to),
:deep(.nudge-wizard-slide-fade-reverse-enter-from) {
  transform: translateX(-16px);
  opacity: 0;
}

:deep(.nudge-wizard-slide-fade-enter-to),
:deep(.nudge-wizard-slide-fade-leave-from),
:deep(.nudge-wizard-slide-fade-reverse-enter-to),
:deep(.nudge-wizard-slide-fade-reverse-leave-from) {
  transform: translateX(0);
  opacity: 1;
}

:deep(.nudge-wizard-expand-fade-enter-active),
:deep(.nudge-wizard-expand-fade-leave-active),
:deep(.nudge-wizard-expand-fade-reverse-enter-active),
:deep(.nudge-wizard-expand-fade-reverse-leave-active) {
  transition:
    transform 500ms ease,
    opacity 400ms ease;
  transform-origin: top center;
  will-change: transform, opacity;
}

:deep(.nudge-wizard-expand-fade-enter-from),
:deep(.nudge-wizard-expand-fade-reverse-leave-to) {
  transform: scaleY(0.9);
  opacity: 0;
}

:deep(.nudge-wizard-expand-fade-leave-to),
:deep(.nudge-wizard-expand-fade-reverse-enter-from) {
  transform: scaleY(0.94);
  opacity: 0;
}

:deep(.nudge-wizard-expand-fade-enter-to),
:deep(.nudge-wizard-expand-fade-leave-from),
:deep(.nudge-wizard-expand-fade-reverse-enter-to),
:deep(.nudge-wizard-expand-fade-reverse-leave-from) {
  transform: scaleY(1);
  opacity: 1;
}

:deep(.nudge-wizard-lift-fade-enter-active),
:deep(.nudge-wizard-lift-fade-leave-active),
:deep(.nudge-wizard-lift-fade-reverse-enter-active),
:deep(.nudge-wizard-lift-fade-reverse-leave-active) {
  transition:
    transform 1000ms cubic-bezier(0.22, 1, 0.36, 1),
    opacity 1000ms cubic-bezier(0.22, 1, 0.36, 1);
  transform-origin: top center;
  will-change: transform, opacity;
}

:deep(.nudge-wizard-lift-fade-enter-from),
:deep(.nudge-wizard-lift-fade-reverse-leave-to) {
  transform: translateY(18px) scale(0.96);
  opacity: 0;
}

:deep(.nudge-wizard-lift-fade-leave-to),
:deep(.nudge-wizard-lift-fade-reverse-enter-from) {
  transform: translateY(12px) scale(0.98);
  opacity: 0;
}

:deep(.nudge-wizard-lift-fade-enter-to),
:deep(.nudge-wizard-lift-fade-leave-from),
:deep(.nudge-wizard-lift-fade-reverse-enter-to),
:deep(.nudge-wizard-lift-fade-reverse-leave-from) {
  transform: translateY(0) scale(1);
  opacity: 1;
}
</style>

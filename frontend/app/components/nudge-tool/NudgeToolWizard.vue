<template>
  <v-card
    class="nudge-wizard"
    rounded="xl"
    elevation="3"
    :style="{ height: formattedHeight }"
  >
    <div ref="headerRef" class="nudge-wizard__header">
      <button
        v-if="showCategoryBadge"
        type="button"
        class="nudge-wizard__category-badge"
        :aria-label="$t('nudge-tool.meta.matches', { count: animatedMatches })"
        @click="navigateToCategoryPage"
      >
        <span class="nudge-wizard__badge-backdrop" aria-hidden="true" />
        <span class="nudge-wizard__badge-content">
          <v-avatar
            v-if="categorySummary?.image"
            size="56"
            rounded="lg"
            class="nudge-wizard__badge-avatar"
          >
            <v-img
              :src="categorySummary.image"
              :alt="categorySummary.alt"
              cover
            />
          </v-avatar>
          <div class="nudge-wizard__badge-text">
            <span class="nudge-wizard__badge-label">{{
              categorySummary?.label
            }}</span>
            <span class="nudge-wizard__badge-count">{{
              $t('nudge-tool.meta.matches', { count: animatedMatches })
            }}</span>
          </div>
        </span>
      </button>

      <div class="nudge-wizard__header-main">
        <div class="nudge-wizard__title-row">
          <p class="nudge-wizard__headline">{{ activeStepTitle }}</p>
          <v-btn
            v-if="activeStepKey !== 'category'"
            class="nudge-wizard__headline-action"
            variant="text"
            size="small"
            prepend-icon="mdi-gesture-tap-button"
            @click="resetForCategorySelection"
          >
            {{ $t('nudge-tool.steps.category.title') }}
          </v-btn>
        </div>

        <v-stepper
          v-if="showStepper"
          v-model="stepperActiveKey"
          density="compact"
          :alt-labels="!display.smAndDown.value"
          :items="stepperItems"
          :item-props="true"
          editable
          flat
          hide-actions
          class="nudge-wizard__stepper elevation-0 border-0"
        />
      </div>
    </div>

    <div v-if="loading" class="nudge-wizard__progress">
      <v-progress-linear indeterminate color="primary" rounded bar-height="4" />
    </div>

    <div ref="windowWrapperRef">
      <v-window
        v-model="activeStepKey"
        class="nudge-wizard__window"
        :touch="false"
        :transition="windowTransition"
        :reverse-transition="windowReverseTransition"
        :style="{ minHeight: contentMinHeight }"
      >
        <v-window-item v-for="step in steps" :key="step.key" :value="step.key">
          <component
            :is="step.component"
            v-bind="step.props"
            @select="onCategorySelect"
            @update:model-value="(value: unknown) => step.onUpdate?.(value)"
            @continue="goToNext"
          />
        </v-window-item>
      </v-window>
    </div>

    <div ref="footerRef" class="nudge-wizard__footer">
      <v-btn
        v-if="hasPreviousStep"
        variant="text"
        prepend-icon="mdi-chevron-left"
        @click="goToPrevious"
      >
        {{ $t('nudge-tool.actions.previous') }}
      </v-btn>

      <v-spacer />

      <v-btn
        v-if="hasNextStep"
        color="primary"
        variant="flat"
        :disabled="isNextDisabled"
        append-icon="mdi-chevron-right"
        @click="goToNext"
      >
        {{ $t('nudge-tool.actions.next') }}
      </v-btn>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { useDebounceFn, useElementSize, useTransition } from '@vueuse/core'
import { useDisplay } from 'vuetify'
import { useCategories } from '~/composables/categories/useCategories'
import {
  NudgeToolStepCategory,
  NudgeToolStepCondition,
  NudgeToolStepRecommendations,
  NudgeToolStepScores,
  NudgeToolStepSubsetGroup,
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

const props = defineProps<{
  verticals?: VerticalConfigDto[]
  initialFilters?: FilterRequestDto
  initialCategoryId?: string | null
  initialSubsets?: string[]
}>()

const emit = defineEmits<{
  (e: 'navigate', payload: { hash: string; categorySlug: string }): void
}>()

const { t } = useI18n()
const display = useDisplay()
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

const defaultStepIcons = {
  category: 'mdi-shape-outline',
  scores: 'mdi-star-check-outline',
  condition: 'mdi-recycle-variant',
  subset: 'mdi-tune-variant',
  recommendations: 'mdi-lightbulb-on-outline',
}

type WizardStep = {
  key: string
  component: Component
  props: Record<string, unknown>
  title: string
  onUpdate?: (value: unknown) => void
}

const steps = computed<WizardStep[]>(() => {
  const sequence: WizardStep[] = []

  sequence.push({
    key: 'category',
    component: NudgeToolStepCategory,
    title: t('nudge-tool.steps.category.title'),
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
      props: {
        group,
        subsets,
        modelValue: activeSubsetIds.value,
        stepNumber: subsetStepNumber,
      },
      onUpdate: (value: string[]) => (activeSubsetIds.value = value),
    })

    subsetStepNumber += 1
  })

  sequence.push({
    key: 'recommendations',
    component: NudgeToolStepRecommendations,
    title: t('nudge-tool.steps.recommendations.title'),
    props: {
      products: recommendations.value,
      popularAttributes: selectedCategory.value?.attributesConfig?.configs,
      totalCount: totalMatches.value,
      loading: loading.value,
    },
  })

  return sequence
})

const getSubsetGroupIcon = (key: string) => {
  const groupId = key.replace('group-', '')
  const group = subsetGroups.value.find(entry => entry.id === groupId)

  return group?.mdiIcon || defaultStepIcons.subset
}

const resolveStepIcon = (stepKey: string) => {
  if (stepKey === 'category') {
    return defaultStepIcons.category
  }

  if (stepKey === 'scores') {
    return defaultStepIcons.scores
  }

  if (stepKey === 'condition') {
    return defaultStepIcons.condition
  }

  if (stepKey.startsWith('group-')) {
    return getSubsetGroupIcon(stepKey)
  }

  return defaultStepIcons.recommendations
}

watch(
  steps,
  allSteps => {
    if (!allSteps.find(step => step.key === activeStepKey.value)) {
      activeStepKey.value = allSteps[0]?.key ?? 'recommendations'
    }
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

const stepperItems = computed(() => {
  const allSteps = steps.value
    .filter(step => step.key !== 'category' && step.key !== 'recommendations')
    .map(step => ({
      title: display.smAndDown.value ? undefined : step.title,
      value: step.key,
      icon: resolveStepIcon(step.key),
      disabled: false,
    }))

  const currentIndex = allSteps.findIndex(item => item.value === activeStepKey.value)
  if (currentIndex === -1) return allSteps

  // Show only current and previous steps
  return allSteps.slice(0, currentIndex + 1)
})

const stepperActiveKey = computed({
  get: () =>
    stepperItems.value.find(item => item.value === activeStepKey.value)
      ?.value ??
    stepperItems.value.at(-1)?.value ??
    activeStepKey.value,
  set: (value: string) => {
    activeStepKey.value = value
  },
})

const showStepper = computed(
  () => activeStepKey.value !== 'category' && Boolean(selectedCategoryId.value)
)

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

const activeStepTitle = computed(
  () => steps.value.find(step => step.key === activeStepKey.value)?.title ?? ''
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
    alt:
      selectedCategory.value.verticalHomeTitle ??
      selectedCategory.value.id ??
      '',
  }
})

const showCategoryBadge = computed(
  () => Boolean(categorySummary.value) && shouldShowMatches.value
)

const windowTransitionDurationMs = 500

const resetCategorySelectionState = () => {
  selectedCategoryId.value = null
  selectedScores.value = []
  activeSubsetIds.value = []
  condition.value = []
  recommendations.value = []
  totalMatches.value = 0
  baseWindowHeight.value = 0
  maxContentHeight.value = 0
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

const baseWindowHeight = ref(0)
const maxContentHeight = ref(0)

watch(windowHeight, val => {
  if (val <= 0) {
    return
  }

  if (activeStepKey.value === 'category' || baseWindowHeight.value === 0) {
    baseWindowHeight.value = Math.max(baseWindowHeight.value, val)
  }

  if (activeStepKey.value !== 'category') {
    maxContentHeight.value = Math.max(maxContentHeight.value, val, baseWindowHeight.value)
  }
})

const stableWindowHeight = computed(() => {
  if (activeStepKey.value === 'category') {
    return Math.max(windowHeight.value, baseWindowHeight.value)
  }

  const floorHeight = Math.max(baseWindowHeight.value, maxContentHeight.value)
  return Math.max(windowHeight.value, floorHeight)
})

const totalHeight = computed(
  () => headerHeight.value + stableWindowHeight.value + footerHeight.value + 32
) // 32 = padding

const animatedHeight = useTransition(totalHeight, {
  duration: 500,
  transition: [0.4, 0, 0.2, 1], // Ease-out-like curve
})

// Only apply fixed height when content is ready/stable to avoid jumps during hydration
const isReady = ref(false)
onMounted(() => {
  setTimeout(() => {
    isReady.value = true
  }, 100)
})

const formattedHeight = computed(() => {
  if (!isReady.value) return 'auto'
  return `${animatedHeight.value}px`
})

const contentMinHeight = computed(() => {
  if (!isReady.value) {
    return undefined
  }

  if (activeStepKey.value === 'category') {
    const minHeight = Math.max(windowHeight.value, baseWindowHeight.value)
    return minHeight ? `${minHeight}px` : undefined
  }

  const minHeight = Math.max(maxContentHeight.value, baseWindowHeight.value)
  return minHeight ? `${minHeight}px` : undefined
})
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
  transition: height 500ms ease;
  overflow: visible;
  background: linear-gradient(
      135deg,
      rgba(var(--v-theme-surface-glass), 0.96),
      rgba(var(--v-theme-surface-primary-080), 0.95)
    ),
    radial-gradient(
      circle at 12% 18%,
      rgba(var(--v-theme-hero-gradient-start), 0.12),
      transparent 38%
    );
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.45);
  box-shadow: 0 18px 32px rgba(var(--v-theme-shadow-primary-600), 0.12);

  &__header {
    display: grid;
    grid-template-columns: auto 1fr;
    align-items: center;
    gap: clamp(0.75rem, 2vw, 1.35rem);
    margin-bottom: 12px;
  }

  &__header-main {
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: stretch;
  }

  &__title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  &__headline {
    margin: 0;
    font-weight: 700;
    font-size: clamp(1.05rem, 2vw, 1.25rem);
    color: rgb(var(--v-theme-text-neutral-strong));
  }

  &__headline-action {
    align-self: center;
    text-transform: none;
    font-weight: 600;
  }

  &__stepper {
    width: 100%;
    min-width: 0;
  }

  &__category-badge {
    position: relative;
    isolation: isolate;
    display: inline-flex;
    align-items: center;
    gap: 0.8rem;
    padding: 0.85rem 1.1rem;
    border-radius: 999px;
    background: linear-gradient(
      135deg,
      rgba(var(--v-theme-hero-gradient-start), 0.22),
      rgba(var(--v-theme-hero-gradient-end), 0.36)
    );
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.55);
    box-shadow: 0 16px 30px rgba(var(--v-theme-shadow-primary-600), 0.16);
    color: rgb(var(--v-theme-text-on-accent));
    text-align: left;
    cursor: pointer;
    overflow: hidden;
    transition:
      transform 220ms ease,
      box-shadow 220ms ease,
      border-color 220ms ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 22px 38px rgba(var(--v-theme-shadow-primary-600), 0.2);
      border-color: rgba(var(--v-theme-border-primary-strong), 0.7);
    }
  }

  &__badge-backdrop {
    position: absolute;
    inset: auto auto auto 0;
    width: 180px;
    height: 180px;
    transform: translate(-42%, -48%);
    background: radial-gradient(
      circle,
      rgba(var(--v-theme-hero-gradient-mid), 0.22),
      rgba(var(--v-theme-hero-gradient-start), 0.05) 60%,
      transparent 70%
    );
    border-radius: 50%;
    z-index: 0;
  }

  &__badge-content {
    position: relative;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    gap: 0.8rem;
  }

  &__badge-avatar {
    box-shadow: 0 12px 22px rgba(var(--v-theme-shadow-primary-600), 0.18);
  }

  &__badge-text {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
  }

  &__badge-label {
    font-weight: 800;
    letter-spacing: 0.01em;
    color: rgb(var(--v-theme-text-on-accent));
  }

  &__badge-count {
    font-size: 0.95rem;
    color: rgba(var(--v-theme-text-on-accent), 0.9);
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
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: 12px;
    flex-wrap: wrap;
  }
}

@media (max-width: 600px) {
  .nudge-wizard {
    &__header {
      grid-template-columns: 1fr;
    }

    &__category-badge {
      width: 100%;
      justify-content: center;
    }

    &__title-row {
      justify-content: flex-start;
      gap: 0.65rem;
    }
  }
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

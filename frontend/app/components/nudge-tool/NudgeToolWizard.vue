<template>
  <RoundedCornerCard
    class="nudge-wizard"
    rounded="xl"
    :elevation="3"
    :hover-elevation="3"
    accent-corner="top-left"
    corner-variant="custom"
    :corner-size="resolvedCornerSize"
    :style="wizardStyle"
    :class="{
      'nudge-wizard--content-mode': isContentMode,
      'nudge-wizard--compact': compact,
    }"
    :selectable="false"
  >
    <template #corner>
      <div
        class="nudge-wizard__corner-content"
        :class="{
          'nudge-wizard__corner-content--expanded': isContentMode,
        }"
      >
        <NudgeToolAnimatedIcon v-if="activeStepKey === 'category'" />
        <v-btn
          v-else-if="categorySummary"
          class="nudge-wizard__corner-summary d-flex flex-column align-center justify-center fill-height pt-1 pb-1"
          variant="text"
          color="white"
          :aria-label="cornerSummaryLabel"
          :ripple="false"
          @click="navigateToCategoryResults"
        >
          <div
            class="nudge-wizard__corner-visual d-flex align-center justify-center"
          >
            <div
              class="nudge-wizard__corner-icon"
              :class="{
                'nudge-wizard__corner-icon--enlarged': shouldEnlargeCornerIcon,
              }"
              :style="cornerIconDimensions"
            >
              <v-icon
                :icon="categorySummary.icon"
                :size="cornerIconSize"
                color="white"
              />
            </div>
          </div>
          <div
            class="nudge-wizard__corner-text text-white text-center d-flex flex-column align-center"
          >
            <div class="nudge-wizard__corner-count font-weight-black lh-1">
              {{ animatedMatches }}
            </div>
          </div>
        </v-btn>
      </div>
    </template>
    <div ref="headerRef">
      <NudgeWizardHeader
        :title="currentStepTitle"
        :subtitle="currentStepSubtitle"
        :title-icon="currentStepIcon"
        :accent-corner="'top-left'"
        :corner-size="resolvedCornerSize"
      />
    </div>

    <div class="nudge-wizard__progress mb-4" aria-hidden="true">
      <v-progress-linear
        :indeterminate="loading"
        :model-value="loading ? undefined : 0"
        color="primary"
        rounded
        bar-height="4"
      />
    </div>

    <div
      ref="windowWrapperRef"
      class="nudge-wizard__window-wrapper"
      :style="windowWrapperStyle"
    >
      <v-window
        v-model="activeStepKey"
        class="nudge-wizard__window"
        :touch="false"
        :transition="windowTransition"
        :reverse-transition="windowReverseTransition"
        :style="windowContentStyle"
      >
        <v-window-item
          v-for="step in steps"
          :key="step.key"
          :value="step.key"
          class="fill-height"
        >
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
        v-if="progressSteps.length > 1"
        class="nudge-wizard__progress-bubbles"
        :style="footerOffsetStyle"
      >
        <v-btn
          v-if="activeStepKey !== 'category'"
          variant="text"
          class="px-2"
          color="primary"
          @click="navigateToCategoryResults"
        >
          {{ $t('nudge-tool.actions.advancedSearch') }}
        </v-btn>
        <v-tooltip
          v-for="step in progressSteps"
          :key="step.key"
          location="top"
          :text="step.key === 'category' ? step.subtitle : step.title"
        >
          <template #activator="{ props: tooltipProps }">
            <span
              class="nudge-wizard__progress-bubble-wrapper"
              v-bind="tooltipProps"
            >
              <v-btn
                class="nudge-wizard__progress-bubble"
                :variant="step.key === activeStepKey ? 'flat' : 'text'"
                :color="step.key === activeStepKey ? 'primary' : undefined"
                :aria-label="step.title"
                :disabled="!canAccessStep(step.key)"
                icon
                size="44"
                @click="() => handleProgressClick(step.key)"
              >
                <span class="nudge-wizard__progress-index">{{
                  step.index
                }}</span>
              </v-btn>
            </span>
          </template>
        </v-tooltip>
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
import { useDebounceFn, useElementSize, useWindowSize } from '@vueuse/core'
import { useCategories } from '~/composables/categories/useCategories'
import { useAuth } from '~/composables/useAuth'
import NudgeWizardHeader from '~/components/nudge-tool/NudgeWizardHeader.vue'
import NudgeToolAnimatedIcon from '~/components/nudge-tool/NudgeToolAnimatedIcon.vue'
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
import type { NudgeToolCategory } from '~/types/nudge-tool'
import unknownCategoryIcon from '~/assets/nudge-tool/unknown-category.svg?url'

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
  compact?: boolean
}>()

const emit = defineEmits<{ (event: 'navigate', target: string): void }>()

const accentCornerOffsets: Record<CornerSize, string> = {
  sm: '46px',
  md: '58px',
  lg: '72px',
  xl: '120px',
}

const { t } = useI18n()
const { isLoggedIn } = useAuth()
const router = useRouter()

const { fetchCategories, selectCategoryBySlug, currentCategory } =
  useCategories()

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
const hasFetchedResults = ref(false)

const activeStepKey = ref('category')
const previousStepKey = ref<string | null>(null)
const visitedStepKeys = ref<string[]>(['category'])

const selectedCategory = computed(() => {
  // Prefer detailed category if loaded and matching
  if (currentCategory.value?.id === selectedCategoryId.value) {
    return currentCategory.value
  }
  return (
    categories.value.find(entry => entry.id === selectedCategoryId.value) ??
    null
  )
})

const categoryIcon = computed(
  () =>
    selectedCategory.value?.mdiIcon ??
    selectedCategory.value?.nudgeToolConfig?.mdiIcon ??
    selectedCategory.value?.icon ??
    'mdi-tag'
)

const nudgeConfig = computed(() => selectedCategory.value?.nudgeToolConfig)

const unknownCategory = computed<NudgeToolCategory>(() => ({
  id: 'unknown-category',
  verticalHomeTitle: t('nudge-tool.categories.unknown.title'),
  imageSmall: unknownCategoryIcon,
  imageMedium: unknownCategoryIcon,
  tooltip: t('nudge-tool.categories.unknown.tooltip'),
  externalLink: 'https://www.linkedin.com/company/105517334/',
}))

const displayCategories = computed<NudgeToolCategory[]>(() => [
  ...categories.value,
  unknownCategory.value,
])

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

const isCategoryStep = computed(() => activeStepKey.value === 'category')

const windowTransition = computed(() => 'slide-x-transition')

const windowReverseTransition = computed(() => 'slide-x-reverse-transition')

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
    icon: categoryIcon.value,
    alt:
      selectedCategory.value.verticalHomeTitle ??
      selectedCategory.value.id ??
      '',
  }
})

const shouldEnlargeCornerIcon = computed(
  () => isCategoryStep.value || Boolean(categorySummary.value)
)

const categorySlug = computed(() => {
  if (!selectedCategory.value) {
    return null
  }

  const slug =
    selectedCategory.value.verticalHomeUrl?.replace(/^\/+/, '') ??
    selectedCategory.value.id ??
    null

  return slug && slug.trim().length > 0 ? slug : null
})

const categoryHash = computed(() => {
  if (!selectedCategoryId.value) {
    return ''
  }

  const hash = buildCategoryHash({
    filters: filterRequest.value,
    activeSubsets: activeSubsetIds.value.length
      ? activeSubsetIds.value
      : undefined,
  })

  return hash ?? ''
})

const categoryNavigationTarget = computed(() => {
  if (!categorySlug.value) {
    return null
  }

  return categoryHash.value
    ? `/${categorySlug.value}${categoryHash.value}`
    : `/${categorySlug.value}`
})

const cornerSummaryLabel = computed(() => {
  if (!categorySummary.value) {
    return ''
  }

  return t('nudge-tool.actions.viewCategoryResults', {
    category: categorySummary.value.label,
  })
})

type WizardStep = {
  key: string
  component: Component
  props: Record<string, unknown>
  title: string
  subtitle?: string
  icon?: string | null
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
      categories: displayCategories.value,
      selectedCategoryId: selectedCategoryId.value,
      isAuthenticated: isLoggedIn.value,
      compact: props.compact ?? false,
    },
  })

  if ((nudgeConfig.value?.scores?.length ?? 0) > 0) {
    sequence.push({
      key: 'scores',
      component: NudgeToolStepScores,
      title: t('nudge-tool.steps.scores.title'),
      subtitle: t('nudge-tool.steps.scores.subtitle'),
      icon: 'mdi-earth',
      props: {
        modelValue: selectedScores.value,
        scores: nudgeConfig.value?.scores ?? [],
        isZeroResults: hasZeroMatches.value,
      },
      onUpdate: (value: string[]) => (selectedScores.value = value),
    })
  }

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
      subtitle: group.description ?? t('nudge-tool.steps.subset.subtitle'),
      icon: group.mdiIcon ?? 'mdi-format-list-bulleted',
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
        isZeroResults: hasZeroMatches.value,
      },
      onUpdate: (value: string[]) => (activeSubsetIds.value = value),
    })

    subsetStepNumber += 1
  })

  sequence.push({
    key: 'condition',
    component: NudgeToolStepCondition,
    title: t('nudge-tool.steps.condition.title'),
    subtitle: t('nudge-tool.steps.condition.subtitle'),
    icon: 'mdi-compare-horizontal',
    props: {
      modelValue: condition.value,
      compact: props.compact ?? false,
      isZeroResults: hasZeroMatches.value,
    },
    onUpdate: (value: ProductConditionSelection) => {
      condition.value = value
    },
  })

  sequence.push({
    key: 'recommendations',
    component: NudgeToolStepRecommendations,
    title: t('nudge-tool.steps.recommendations.title'),
    subtitle: t('nudge-tool.steps.recommendations.subtitle'),
    icon: 'mdi-lightbulb-on',
    props: {
      products: recommendations.value,
      popularAttributes: selectedCategory.value?.attributesConfig?.configs,
      totalCount: totalMatches.value,
      loading: loading.value,
      categoryLink: categoryNavigationTarget.value,
      categoryLinkLabel: cornerSummaryLabel.value,
    },
  })

  return sequence
})

const currentStep = computed(() =>
  steps.value.find(step => step.key === activeStepKey.value)
)

const currentStepTitle = computed(() => currentStep.value?.title ?? '')
const currentStepSubtitle = computed(() => currentStep.value?.subtitle ?? '')
const currentStepIcon = computed(() => currentStep.value?.icon ?? null)

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

const navigateToCategoryResults = () => {
  if (!categoryNavigationTarget.value) {
    return
  }

  emit('navigate', categoryNavigationTarget.value)
  void router.push(categoryNavigationTarget.value)
}

const getFirstContentStepKey = () =>
  steps.value.find(step => step.key !== 'category')?.key

const onCategorySelect = async (categoryId: string) => {
  selectedCategoryId.value = categoryId

  // Optimistically fetch details to ensure attributes are available
  const simpleCategory = categories.value.find(c => c.id === categoryId)
  if (simpleCategory?.verticalHomeUrl) {
    const slug = simpleCategory.verticalHomeUrl.replace(/^\/+/, '')
    // Don't await strictly to allow transition to start, but we need it for the end step
    selectCategoryBySlug(slug).catch(() => {
      /* ignore */
    })
  }

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
          sort: {
            sorts: [{ field: 'scores.ECOSCORE.value', order: 'desc' }],
          },
          filters: hasFilters ? filterRequest.value : undefined,
        },
      }
    )

    recommendations.value = response.products?.data ?? []
    totalMatches.value = response.products?.page?.totalElements ?? 0
    hasFetchedResults.value = true
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

  const result = await fetchCategories()
  categories.value = result
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

const hasZeroMatches = computed(
  () => hasFetchedResults.value && !loading.value && totalMatches.value === 0
)

const isNextDisabled = computed(() => {
  if (activeStepKey.value === 'category') {
    return !selectedCategoryId.value
  }

  return hasZeroMatches.value
})

const canAccessStep = (stepKey: string) =>
  visitedStepKeys.value.includes(stepKey)

const progressSteps = computed(() =>
  steps.value
    .map((step, index) => ({
      key: step.key,
      title: step.title,
      subtitle: step.subtitle ?? '',
      index: index + 1,
    }))
    .filter(step => visitedStepKeys.value.includes(step.key))
)

const handleProgressClick = (stepKey: string) => {
  if (!canAccessStep(stepKey)) {
    return
  }

  activeStepKey.value = stepKey
}

const windowTransitionDurationMs = 0

const resetCategorySelectionState = () => {
  selectedCategoryId.value = null
  selectedScores.value = []
  activeSubsetIds.value = []
  condition.value = []
  recommendations.value = []
  totalMatches.value = 0
  hasFetchedResults.value = false
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
const { height: viewportHeight } = useWindowSize()

const isContentMode = computed(() => activeStepKey.value !== 'category')

const WIZARD_MIN_HEIGHT = 300

const lockedLayoutHeight = ref<number | null>(null)
const lockedWindowHeight = ref<number | null>(null)

const attemptLockHeights = () => {
  if (props.compact) {
    return
  }

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

watch(
  [isContentMode, headerHeight, windowHeight, footerHeight],
  attemptLockHeights,
  {
    immediate: true,
  }
)

const wizardStyle = computed(() => {
  if (!lockedLayoutHeight.value) {
    return undefined
  }

  return { minHeight: `${lockedLayoutHeight.value}px` }
})

const windowWrapperStyle = computed(() => {
  if (props.compact) {
    const reservedSpace = headerHeight.value + footerHeight.value + 64
    const availableHeight = Math.max(
      viewportHeight.value - reservedSpace,
      WIZARD_MIN_HEIGHT
    )
    const maxHeight = Math.min(availableHeight, 420)
    return {
      maxHeight: `${maxHeight}px`,
    }
  }

  if (!lockedWindowHeight.value) {
    return undefined
  }

  return {
    minHeight: `${lockedWindowHeight.value}px`,
    maxHeight: `${lockedWindowHeight.value}px`,
  }
})

const windowContentStyle = computed(() => {
  if (props.compact || !lockedWindowHeight.value) {
    return undefined
  }

  return { minHeight: `${lockedWindowHeight.value}px` }
})

const resolvedCornerSize = computed(() => (isContentMode.value ? 'xl' : 'lg'))

const footerOffsetStyle = computed(() => ({
  paddingLeft: accentCornerOffsets[resolvedCornerSize.value],
}))

const cornerIconScaleFactor = 1.3
const baseCornerIconSize = 26
const contentCornerIconSize = 30
const cornerIconWrapperSize = 40

const cornerIconSize = computed(() => {
  if (shouldEnlargeCornerIcon.value) {
    return Math.round(baseCornerIconSize * cornerIconScaleFactor)
  }

  return isContentMode.value ? contentCornerIconSize : baseCornerIconSize
})

const cornerIconDimensions = computed(() => {
  const wrapperSize = shouldEnlargeCornerIcon.value
    ? Math.round(cornerIconWrapperSize * cornerIconScaleFactor)
    : cornerIconWrapperSize

  return {
    width: `${wrapperSize}px`,
    height: `${wrapperSize}px`,
  }
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

  &__corner-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    text-align: center;
    line-height: 1.1;
    transition:
      transform 300ms ease,
      opacity 260ms ease;

    &--expanded {
      transform: translateY(2px) scale(1.04);
    }
  }

  &__corner-summary {
    gap: 2px;
    width: 100%;
    height: 100%;
    min-width: 0;
    padding-inline: 4px;
    text-transform: none;

    :deep(.v-btn__content) {
      width: 100%;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    :deep(.v-btn__overlay) {
      display: none;
    }
  }

  &__corner-visual {
    gap: 8px;
  }

  &__corner-avatar {
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.35);
    box-shadow: 0 6px 20px rgba(var(--v-theme-shadow-primary-600), 0.12);
  }

  &__corner-icon {
    width: 40px;
    height: 40px;
    display: grid;
    place-items: center;
    /* Removed background and border as per new design */
    /* background: rgba(var(--v-theme-hero-gradient-mid), 0.22); */
    /* box-shadow: inset 0 0 0 1px rgba(var(--v-theme-border-primary-strong), 0.4); */

    &--enlarged {
      /* border-radius: 20px; */
    }
  }

  &__corner-text {
    line-height: 1.2;
  }

  &__corner-count {
    font-size: clamp(1.2rem, 3vw, 1.5rem);
    line-height: 1;
    text-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  }

  &__stepper {
    width: 100%;
    min-width: 0;
  }

  &__progress {
    width: 100%;
    margin-top: 8px;
    margin-bottom: 8px;
    height: 4px; /* fixed height layout space */
  }

  &__window {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    overflow-y: auto;
    height: 100%;

    :deep(.v-window__container) {
      align-items: center;
    }

    :deep(.v-window-item) {
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__window-wrapper {
    width: 100%;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: clamp(0.5rem, 1vw, 1rem) 0;
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

  &__progress-bubble-wrapper {
    display: inline-flex;
  }

  &__progress-bubble {
    min-width: 44px;
    min-height: 44px;
    border-radius: 50%;
    box-shadow: none;
    border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4);
    transition:
      transform 140ms ease,
      border-color 140ms ease;

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

.nudge-wizard--compact {
  padding: clamp(1rem, 2.5vw, 1.5rem);

  .nudge-wizard__progress {
    margin-top: 4px;
    margin-bottom: 4px;
  }

  .nudge-wizard__window-wrapper {
    padding: clamp(0.25rem, 0.8vw, 0.6rem) 0;
  }

  .nudge-wizard__window :deep(.v-window__container) {
    align-items: stretch;
  }
}
</style>

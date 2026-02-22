<template>
  <RoundedCornerCard
    ref="wizardRef"
    surface="strong"
    rounded="xl"
    :elevation="3"
    :hover-elevation="3"
    :accent-corner="'top-left'"
    :corner-variant="isCategoryStep ? 'none' : 'custom'"
    :corner-size="resolvedCornerSize"
    :class="[
      'nudge-wizard',
      {
        'nudge-wizard--content-mode': isContentMode,
        'nudge-wizard--category': isCategoryStep,
        'nudge-wizard--compact': compact,
        'nudge-wizard--reduced-motion': shouldReduceMotion,
      },
    ]"
    :style="{
      ...wizardStyle,
      transition: 'height 0.3s ease-in-out, min-height 0.3s ease-in-out',
    }"
    :selectable="false"
  >
    <template #corner>
      <div
        :class="[
          'nudge-wizard__corner-content',
          {
            'nudge-wizard__corner-content--expanded': isContentMode,
          },
        ]"
      >
        <v-btn
          v-if="!isCategoryStep && categorySummary"
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
              :class="[
                'nudge-wizard__corner-icon',
                {
                  'nudge-wizard__corner-icon--enlarged':
                    shouldEnlargeCornerIcon,
                },
              ]"
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
    <template #header>
      <div ref="headerRef">
        <NudgeWizardHeader
          :title="currentStepTitle"
          :subtitle="currentStepSubtitle"
          :title-icon="currentStepIcon"
          :accent-corner="isCategoryStep ? undefined : 'top-left'"
          :corner-size="resolvedCornerSize"
        >
          <template #prepend-title>
            <NudgeToolAnimatedIcon
              v-if="isCategoryStep"
              class="me-2"
              variant="bounce"
              :frequency-range="[1000, 2500]"
              :max-scale="1.4"
            />
          </template>
        </NudgeWizardHeader>
      </div>
    </template>

    <div
      v-if="!isCategoryStep"
      ref="progressRef"
      class="nudge-wizard__progress mb-4"
      aria-hidden="true"
    >
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
        :class="{ 'nudge-wizard__window--transitioning': isTransitioning }"
        :touch="false"
        :transition="windowTransition"
        :reverse-transition="windowReverseTransition"
      >
        <v-window-item
          v-for="step in steps"
          :key="step.key"
          :value="step.key"
          class="fill-height"
        >
          <div
            :ref="element => setStepRef(element, step.key)"
            class="nudge-wizard__step-content"
          >
            <component
              :is="step.component"
              v-bind="step.props"
              @select="onCategorySelect"
              @update:model-value="(value: unknown) => step.onUpdate?.(value)"
              @continue="goToNext"
              @return-to-category="resetForCategorySelection"
            />
          </div>
        </v-window-item>
      </v-window>
    </div>

    <template #actions>
      <div ref="footerRef" class="nudge-wizard__footer w-100">
        <!-- Left: Previous -->
        <div class="nudge-wizard__footer-left d-flex align-center">
          <transition name="rapid-fade">
            <v-btn
              v-if="hasPreviousStep"
              key="wizard-prev-btn"
              variant="text"
              prepend-icon="mdi-chevron-left"
              class="nudge-wizard__footer-btn"
              :disabled="!hasPreviousStep"
              @click="goToPrevious"
            >
              {{ $t('nudge-tool.actions.previous') }}
            </v-btn>
          </transition>
        </div>

        <!-- Center: Steppers -->
        <div
          class="nudge-wizard__footer-center d-flex justify-start flex-grow-1 ps-4"
        >
          <div
            v-if="progressSteps.length > 1"
            class="nudge-wizard__progress-bubbles"
          >
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
        </div>

        <!-- Right: Result Count + Next -->
        <div
          class="nudge-wizard__footer-right d-flex align-center justify-end gap-2"
        >
          <!-- Result Count -->
          <div
            v-if="!isCategoryStep && categoryNavigationTarget"
            class="nudge-wizard__reco-count-wrapper px-2"
          >
            <NuxtLink
              :to="categoryNavigationTarget"
              :aria-label="cornerSummaryLabel"
              class="nudge-wizard__reco-count"
            >
              {{
                $t('nudge-tool.steps.recommendations.total', {
                  count: animatedMatches,
                })
              }}
              <v-icon icon="mdi-arrow-right" size="small" class="ms-1" />
            </NuxtLink>
          </div>
          <div
            v-else-if="!isCategoryStep"
            class="nudge-wizard__reco-count-wrapper px-2"
          >
            <p class="nudge-wizard__reco-count">
              {{
                $t('nudge-tool.steps.recommendations.total', {
                  count: animatedMatches,
                })
              }}
            </p>
          </div>

          <!-- Next Button -->
          <v-btn
            v-if="hasNextStep"
            color="primary"
            variant="flat"
            :disabled="isNextDisabled"
            append-icon="mdi-chevron-right"
            class="nudge-wizard__footer-btn ms-2"
            @click="goToNext"
          >
            {{ $t('nudge-tool.actions.next') }}
          </v-btn>
        </div>
      </div>
    </template>
  </RoundedCornerCard>
</template>

<script setup lang="ts">
import type { ComponentPublicInstance } from 'vue'
import {
  useDebounceFn,
  useElementSize,
  usePreferredReducedMotion,
  useWindowSize,
} from '@vueuse/core'
import { useCategories } from '~/composables/categories/useCategories'
import { useAuth } from '~/composables/useAuth'
import NudgeWizardHeader from '~/components/nudge-tool/NudgeWizardHeader.vue'
import NudgeToolAnimatedIcon from '~/components/nudge-tool/NudgeToolAnimatedIcon.vue'

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
  NudgeToolConfigDto,
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
  assistantConfig?: NudgeToolConfigDto
  assistantCategoryId?: string | null
  compact?: boolean
}>()

const emit = defineEmits<{ (event: 'navigate', target: string): void }>()

const { t } = useI18n()
const { isLoggedIn } = useAuth()
const router = useRouter()
const prefersReducedMotion = usePreferredReducedMotion()
const shouldReduceMotion = computed(
  () => prefersReducedMotion.value === 'reduce'
)

const { fetchCategories, selectCategoryBySlug, currentCategory } =
  useCategories()

const categories = useState<VerticalCategoryDto[]>('nudge-categories', () => [])
const selectedCategoryId = ref<string | null>(
  props.assistantCategoryId ?? props.initialCategoryId ?? null
)
const condition = ref<ProductConditionSelection>([])
const selectedScores = ref<string[]>([])
const activeSubsetIds = ref<string[]>(props.initialSubsets ?? [])
const baseFilters = ref<Filter[]>(props.initialFilters?.filters ?? [])
const recommendations = ref<ProductDto[]>([])
const totalMatches = ref(0)
const loading = ref(false)
const animatedMatches = ref(0)
const hasFetchedResults = ref(false)

const hasZeroMatches = computed(
  () => hasFetchedResults.value && !loading.value && totalMatches.value === 0
)

const activeStepKey = ref('category')
const previousStepKey = ref<string | null>(null)
const visitedStepKeys = ref<string[]>([])

const isAssistantMode = computed(() => Boolean(props.assistantConfig))

watch(
  () => props.assistantCategoryId,
  nextValue => {
    if (nextValue !== undefined) {
      selectedCategoryId.value = nextValue ?? null
    }
  }
)

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

const nudgeConfig = computed(
  () => props.assistantConfig ?? selectedCategory.value?.nudgeToolConfig
)

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

const windowTransition = computed(() => {
  if (
    activeStepKey.value === 'category' ||
    previousStepKey.value === 'category'
  ) {
    return 'fade-transition'
  }
  return 'slide-x-transition'
})

const windowReverseTransition = computed(() => {
  if (
    activeStepKey.value === 'category' ||
    previousStepKey.value === 'category'
  ) {
    return 'fade-transition'
  }
  return 'slide-x-reverse-transition'
})

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

  if (!isAssistantMode.value) {
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

  // Scores step moved after subset groups (Distance, Budget, etc.)
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
      popularAttributes: selectedCategory.value?.popularAttributes,
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
    const firstStepKey = allSteps[0]?.key
    if (!allSteps.find(step => step.key === activeStepKey.value)) {
      activeStepKey.value = firstStepKey ?? 'recommendations'
    }

    const validKeys = allSteps.map(step => step.key)
    visitedStepKeys.value = visitedStepKeys.value.filter(key =>
      validKeys.includes(key)
    )
    if (!visitedStepKeys.value.length && firstStepKey) {
      visitedStepKeys.value = [firstStepKey]
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
  if (isAssistantMode.value) {
    return
  }

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
        query: {
          include:
            'base,identity,names,attributes,resources,scores,offers,vertical',
        },
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
  if (isAssistantMode.value) {
    return
  }

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
  if (isAssistantMode.value) {
    return
  }

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
        lockedContentHeight.value = categoryHeight.value
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

  if (props.initialCategoryId && !isAssistantMode.value) {
    const nextStepKey = getFirstContentStepKey()
    if (nextStepKey) {
      activeStepKey.value = nextStepKey
    }
  }

  debouncedFetch()
})

const headerRef = ref<HTMLElement>()
const progressRef = ref<HTMLElement>()
const windowWrapperRef = ref<HTMLElement>()
const footerRef = ref<HTMLElement>()
const activeStepRef = ref<HTMLElement | null>(null)
const stepRefs = ref<Record<string, HTMLElement | null>>({})

const { height: headerHeight } = useElementSize(headerRef)
const { height: progressHeight } = useElementSize(progressRef)
const { height: footerHeight } = useElementSize(footerRef)
const { height: viewportHeight } = useWindowSize()
const { height: activeStepHeight } = useElementSize(activeStepRef)

const isContentMode = computed(() => activeStepKey.value !== 'category')

const WIZARD_MIN_HEIGHT = 300
const WINDOW_MIN_HEIGHT = 370
const VIEWPORT_PADDING = 32

const wizardRef = ref<HTMLElement>()
const wizardPadding = ref(0)
const windowWrapperPadding = ref(0)
const lockedContentHeight = ref<number | null>(null)
const lastViewportHeight = ref<number | null>(null)

const setStepRef = (
  element: Element | ComponentPublicInstance | null,
  key: string
) => {
  const resolved =
    element && '$el' in element
      ? (element.$el as HTMLElement | null)
      : (element as HTMLElement | null)

  if (!resolved) {
    stepRefs.value[key] = null
    if (activeStepKey.value === key) {
      activeStepRef.value = null
    }
    return
  }

  stepRefs.value[key] = resolved
  if (activeStepKey.value === key) {
    activeStepRef.value = resolved
  }
}

const isTransitioning = ref(false)

watch(
  activeStepKey,
  key => {
    activeStepRef.value = stepRefs.value[key] ?? null
    isTransitioning.value = true
    setTimeout(() => {
      isTransitioning.value = false
    }, 550)
  },
  { flush: 'post' }
)

const updateLayoutPadding = () => {
  if (!import.meta.client) {
    return
  }

  const wizardEl =
    wizardRef.value && '$el' in wizardRef.value
      ? (wizardRef.value.$el as HTMLElement)
      : (wizardRef.value as HTMLElement | null)

  const wrapperEl = windowWrapperRef.value

  if (
    !wizardEl ||
    !wrapperEl ||
    !(wizardEl instanceof Element) ||
    !(wrapperEl instanceof Element)
  ) {
    return
  }

  const wizardStyles = getComputedStyle(wizardEl)
  const wrapperStyles = getComputedStyle(wrapperEl)

  wizardPadding.value =
    Number.parseFloat(wizardStyles.paddingTop) +
    Number.parseFloat(wizardStyles.paddingBottom)
  windowWrapperPadding.value =
    Number.parseFloat(wrapperStyles.paddingTop) +
    Number.parseFloat(wrapperStyles.paddingBottom)
}

onMounted(() => {
  updateLayoutPadding()
  lastViewportHeight.value = viewportHeight.value
})

watch(
  [viewportHeight, isCategoryStep],
  async () => {
    await nextTick()
    updateLayoutPadding()
  },
  { flush: 'post' }
)

const baseSpacing = computed(
  () =>
    headerHeight.value +
    progressHeight.value +
    footerHeight.value +
    wizardPadding.value +
    windowWrapperPadding.value
)

const clampHeight = (height: number) => {
  const maxHeight = Math.max(
    viewportHeight.value - VIEWPORT_PADDING,
    WIZARD_MIN_HEIGHT
  )
  return Math.min(Math.max(height, WIZARD_MIN_HEIGHT), maxHeight)
}

const baseTotalHeight = computed(() => {
  const windowHeight = Math.max(activeStepHeight.value, WINDOW_MIN_HEIGHT)
  return baseSpacing.value + windowHeight
})

const categoryHeight = computed(() => {
  // Add 48px to pad the height more generously to prevent any scrollbars
  return clampHeight(baseSpacing.value + activeStepHeight.value + 48)
})

watch(
  [isContentMode, viewportHeight],
  async () => {
    if (props.compact || !isContentMode.value) {
      lockedContentHeight.value = null
      lastViewportHeight.value = viewportHeight.value
      return
    }

    const viewportChanged = lastViewportHeight.value !== viewportHeight.value

    // Always update if we're in content mode and height is unset or viewport changed
    // OR if we seeded it (non-null) but need to transition to the actual content height
    if (
      lockedContentHeight.value === null ||
      viewportChanged ||
      (isContentMode.value &&
        lockedContentHeight.value === categoryHeight.value)
    ) {
      await nextTick()
      lockedContentHeight.value = clampHeight(baseTotalHeight.value)
      lastViewportHeight.value = viewportHeight.value
    }
  },
  { flush: 'post', immediate: true }
)

const wizardStyle = computed(() => {
  if (props.compact) {
    return undefined
  }

  if (isCategoryStep.value) {
    return {
      height: `${categoryHeight.value}px`,
      minHeight: `${categoryHeight.value}px`,
    }
  }

  if (!lockedContentHeight.value) {
    return undefined
  }

  return {
    height: `${lockedContentHeight.value}px`,
    minHeight: `${lockedContentHeight.value}px`,
  }
})

const windowWrapperStyle = computed(() => {
  if (props.compact) {
    const reservedSpace =
      headerHeight.value + progressHeight.value + footerHeight.value + 64
    const availableHeight = Math.max(
      viewportHeight.value - reservedSpace,
      WIZARD_MIN_HEIGHT
    )
    const maxHeight = Math.min(availableHeight, 420)
    return {
      maxHeight: `${maxHeight}px`,
    }
  }

  return undefined
})

const resolvedCornerSize = computed(() => (isContentMode.value ? 'xl' : 'lg'))

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
  transition:
    height 280ms cubic-bezier(0.4, 0, 0.2, 1),
    min-height 280ms cubic-bezier(0.4, 0, 0.2, 1);

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
      align-items: stretch;
    }

    :deep(.v-window-item) {
      display: flex;
      align-items: flex-start;
      justify-content: center;
    }
  }

  &__step-content {
    width: 100%;
    margin: auto;
  }

  &__window-wrapper {
    width: 100%;
    overflow-x: hidden;
    overflow-y: auto;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: clamp(0.5rem, 1vw, 1rem) 0;
    flex-grow: 1; /* Allow it to fill the fixed height */
    min-height: 0; /* Enable shrinking below content size */
  }

  &__footer {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 12px;
    margin-top: 4px;
    align-items: center;

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
      justify-items: center;
      gap: 16px;
      text-align: center;
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

  &__reco-count-wrapper {
    display: flex;
    align-items: center;
  }

  &__reco-count {
    color: rgb(var(--v-theme-text-neutral-secondary));
    text-decoration: none;
    font-size: 0.9rem;
    display: inline-flex;
    align-items: center;
    transition: color 0.2s;
    margin: 0;

    &:hover {
      color: rgb(var(--v-theme-primary));
      text-decoration: underline;
    }
  }
}

.nudge-wizard--content-mode .nudge-wizard__corner-content {
  transform: translateY(2px) scale(1.04);
}

.nudge-wizard--category {
  .nudge-wizard__window {
    justify-content: flex-start;
    overflow-y: auto;

    :deep(.v-window__container) {
      align-items: flex-start;
    }

    :deep(.v-window-item) {
      align-items: flex-start;
      justify-content: flex-start;
    }
  }

  .nudge-wizard__window-wrapper {
    padding-block: clamp(0.25rem, 0.6vw, 0.5rem);
  }
}

.nudge-wizard--reduced-motion {
  transition: none;
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

.nudge-wizard__window--transitioning {
  overflow-y: hidden !important;
}

.rapid-fade-enter-active,
.rapid-fade-leave-active {
  transition: opacity 0.15s ease-out;
}

.rapid-fade-enter-from,
.rapid-fade-leave-to {
  opacity: 0;
}
</style>

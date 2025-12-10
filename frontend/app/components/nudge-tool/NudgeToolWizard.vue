<template>
  <v-card class="nudge-wizard" rounded="xl" elevation="3">
    <div class="nudge-wizard__header">
      <v-stepper
        v-if="showStepper"
        v-model="activeStepKey"
        density="compact"
        alt-labels
        :items="stepperItems"
        class="nudge-wizard__stepper"
        @click:step="onStepClick"
      />

      <div v-if="totalMatches >= 0 && selectedCategory" class="nudge-wizard__matches">
        <v-btn
          variant="text"
          color="primary"
          :disabled="!selectedCategory"
          @click="navigateToCategoryPage"
        >
          {{ $t('nudge-tool.meta.matches', { count: totalMatches }) }}
        </v-btn>
      </div>
    </div>

    <div v-if="loading" class="nudge-wizard__progress">
      <v-progress-linear indeterminate color="primary" rounded bar-height="4" />
    </div>

    <v-window v-model="activeStepKey" class="nudge-wizard__window" :touch="false">
      <v-window-item v-for="step in steps" :key="step.key" :value="step.key">
        <component
          :is="step.component"
          v-bind="step.props"
          @select="onCategorySelect"
          @update:model-value="(value: unknown) => step.onUpdate?.(value)"
          @continue="goToNext"
          @see-all="navigateToCategoryPage"
        />
      </v-window-item>
    </v-window>

    <div class="nudge-wizard__footer">
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
import { computed, onMounted, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import {
  buildCategoryHash,
  type CategoryHashState,
} from '~/utils/_category-filter-state'
import { buildFilterRequestFromSubsets } from '~/utils/_subset-to-filters'
import {
  buildConditionFilter,
  buildNudgeFilterRequest,
  buildScoreFilters,
  type ProductConditionChoice,
} from '~/utils/_nudge-tool-filters'
import NudgeToolStepCategory from './NudgeToolStepCategory.vue'
import NudgeToolStepCondition from './NudgeToolStepCondition.vue'
import NudgeToolStepScores from './NudgeToolStepScores.vue'
import NudgeToolStepSubsetGroup from './NudgeToolStepSubsetGroup.vue'
import NudgeToolStepRecommendations from './NudgeToolStepRecommendations.vue'
import type {
  Filter,
  FilterRequestDto,
  NudgeToolSubsetGroupDto,
  ProductDto,
  ProductSearchResponseDto,
  VerticalConfigDto,
  VerticalSubsetDto,
} from '~~/shared/api-client'
import { useCategories } from '~/composables/categories/useCategories'

const props = defineProps<{
  initialCategoryId?: string | null
  initialFilters?: FilterRequestDto
  initialSubsets?: string[]
  verticals?: VerticalConfigDto[]
}>()

const emit = defineEmits<{ (event: 'navigate', payload: { hash: string; categorySlug: string }): void }>()

const router = useRouter()
const { t } = useI18n()
const { fetchCategories } = useCategories()

const categories = ref<VerticalConfigDto[]>(props.verticals ?? [])
const selectedCategoryId = ref<string | null>(props.initialCategoryId ?? null)
const condition = ref<ProductConditionChoice>('any')
const selectedScores = ref<string[]>([])
const activeSubsetIds = ref<string[]>(props.initialSubsets ?? [])
const baseFilters = ref<Filter[]>(props.initialFilters?.filters ?? [])

const activeStepKey = ref<string>('category')
const loading = ref(false)
const totalMatches = ref(0)
const recommendations = ref<ProductDto[]>([])

const selectedCategory = computed(() =>
  categories.value.find((entry) => entry.id === selectedCategoryId.value) ?? null,
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
    .map((subset) => subset.group)
    .filter((groupId): groupId is string => Boolean(groupId))
    .filter((groupId) => {
      if (seen.has(groupId)) {
        return false
      }
      seen.add(groupId)
      return true
    })
    .map((groupId) => ({ id: groupId, title: groupId }))
})

const groupedSubsets = computed<Record<string, VerticalSubsetDto[]>>(() => {
  const groups: Record<string, VerticalSubsetDto[]> = {}

  ;(nudgeConfig.value?.subsets ?? []).forEach((subset) => {
    const key = subset.group ?? 'default'
    if (!groups[key]) {
      groups[key] = []
    }
    groups[key].push(subset)
  })

  return groups
})

const subsetFilterRequest = computed(() =>
  buildFilterRequestFromSubsets(nudgeConfig.value?.subsets ?? [], activeSubsetIds.value),
)

const scoreFilters = computed(() =>
  buildScoreFilters(nudgeConfig.value?.scores ?? [], selectedScores.value),
)

const conditionFilter = computed(() => buildConditionFilter(condition.value))

const filterRequest = computed<FilterRequestDto>(() => {
  const subsetFilters = subsetFilterRequest.value.filters ?? []
  return buildNudgeFilterRequest(baseFilters.value, conditionFilter.value, scoreFilters.value, subsetFilters)
})

const hashState = computed<CategoryHashState>(() => ({
  filters: filterRequest.value.filters?.length ? filterRequest.value : undefined,
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

  if (!props.initialCategoryId) {
    sequence.push({
      key: 'category',
      component: NudgeToolStepCategory,
      title: t('nudge-tool.steps.category.title'),
      props: { categories: categories.value, selectedCategoryId: selectedCategoryId.value },
    })
  }

  if ((nudgeConfig.value?.scores?.length ?? 0) > 0) {
    sequence.push({
      key: 'scores',
      component: NudgeToolStepScores,
      title: t('nudge-tool.steps.scores.title'),
      props: { modelValue: selectedScores.value, scores: nudgeConfig.value?.scores ?? [] },
      onUpdate: (value: string[]) => (selectedScores.value = value),
    })
  }

  sequence.push({
    key: 'condition',
    component: NudgeToolStepCondition,
    title: t('nudge-tool.steps.condition.title'),
    props: { modelValue: condition.value },
    onUpdate: (value: ProductConditionChoice) => {
      condition.value = value
      goToNext()
    },
  })

  subsetGroups.value.forEach((group) => {
    const subsets = groupedSubsets.value[group.id ?? ''] ?? []
    if (!subsets.length) {
      return
    }

    sequence.push({
      key: `group-${group.id}`,
      component: NudgeToolStepSubsetGroup,
      title: group.title ?? '',
      props: { group, subsets, modelValue: activeSubsetIds.value },
      onUpdate: (value: string[]) => (activeSubsetIds.value = value),
    })
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
  const group = subsetGroups.value.find((entry) => entry.id === groupId)

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
  (allSteps) => {
    if (!allSteps.find((step) => step.key === activeStepKey.value)) {
      activeStepKey.value = allSteps[0]?.key ?? 'recommendations'
    }
  },
  { immediate: true, deep: true },
)

const goToNext = () => {
  const index = steps.value.findIndex((step) => step.key === activeStepKey.value)
  const nextStep = steps.value[index + 1]
  if (nextStep) {
    activeStepKey.value = nextStep.key
  }
}

const goToPrevious = () => {
  const index = steps.value.findIndex((step) => step.key === activeStepKey.value)
  const previousStep = steps.value[index - 1]
  if (previousStep) {
    if (previousStep.key === 'category') {
      resetForCategorySelection()
    } else {
      activeStepKey.value = previousStep.key
    }
  }
}

const onCategorySelect = (categoryId: string) => {
  selectedCategoryId.value = categoryId
  goToNext()
}

const fetchRecommendations = async () => {
  if (!selectedCategoryId.value) {
    return
  }

  loading.value = true

  try {
    const response = await $fetch<ProductSearchResponseDto>('/api/products/search', {
      method: 'POST',
      body: {
        verticalId: selectedCategoryId.value,
        pageNumber: 0,
        pageSize: 3,
        sort: { sorts: [{ field: 'scores.ECOSCORE.value', order: 'desc' }] },
        filters: filterRequest.value.filters?.length ? filterRequest.value : undefined,
      },
    })

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
  { deep: true },
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

  const slug = selectedCategory.value.verticalHomeUrl?.replace(/^\//u, '') ?? selectedCategory.value.id ?? ''
  const hash = buildCategoryHash(hashState.value)

  emit('navigate', { hash, categorySlug: slug })

  void router.push({ path: `/${slug}`, hash })
}

const getStepGroupSelection = (key: string) => {
  const groupId = key.replace('group-', '')
  const subsets = groupedSubsets.value[groupId] ?? []
  const subsetIds = subsets.map((subset) => subset.id)

  return activeSubsetIds.value.filter((subsetId) => subsetIds.includes(subsetId))
}

const hasPreviousStep = computed(() => {
  const index = steps.value.findIndex((step) => step.key === activeStepKey.value)
  return index > 0
})

const hasNextStep = computed(() => {
  const index = steps.value.findIndex((step) => step.key === activeStepKey.value)
  return index >= 0 && index < steps.value.length - 1
})

const isMultiSelectStep = computed(() =>
  activeStepKey.value === 'scores' || activeStepKey.value.startsWith('group-'),
)

const hasSelectionForStep = computed(() => {
  if (activeStepKey.value === 'scores') {
    return selectedScores.value.length > 0
  }

  if (activeStepKey.value.startsWith('group-')) {
    return getStepGroupSelection(activeStepKey.value).length > 0
  }

  if (activeStepKey.value === 'category') {
    return Boolean(selectedCategoryId.value)
  }

  return true
})

const isNextDisabled = computed(() => {
  if (activeStepKey.value === 'category') {
    return !selectedCategoryId.value
  }

  if (isMultiSelectStep.value) {
    return !hasSelectionForStep.value
  }

  return false
})

const stepperItems = computed(() =>
  steps.value.map((step) => ({
    title: step.title,
    value: step.key,
    icon: resolveStepIcon(step.key),
    disabled: false,
  })),
)

const showStepper = computed(
  () => activeStepKey.value !== 'category' && Boolean(selectedCategoryId.value),
)

const onStepClick = (value: string | number) => {
  const targetKey = String(value)
  if (targetKey === 'category') {
    resetForCategorySelection()
    return
  }

  if (steps.value.some((step) => step.key === targetKey)) {
    activeStepKey.value = targetKey
  }
}

const resetForCategorySelection = () => {
  selectedCategoryId.value = null
  selectedScores.value = []
  activeSubsetIds.value = []
  condition.value = 'any'
  recommendations.value = []
  totalMatches.value = 0
  activeStepKey.value = 'category'
}

onMounted(async () => {
  await hydrateCategories()

  if (selectedCategoryId.value && !selectedCategory.value) {
    selectedCategoryId.value = categories.value.find((cat) => cat.id === selectedCategoryId.value)?.id ?? null
  }

  debouncedFetch()
})
</script>

<style scoped lang="scss">
.nudge-wizard {
  position: relative;
  padding: 16px;
  width: 100%;
  max-width: none;
  display: flex;
  flex-direction: column;
  height: 100%;

  &__header {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    margin-bottom: 12px;
  }

  &__stepper {
    width: 100%;
  }

  &__matches {
    display: flex;
    justify-content: center;
  }

  &__progress {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }

  &__window {
    min-height: 420px;
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
</style>

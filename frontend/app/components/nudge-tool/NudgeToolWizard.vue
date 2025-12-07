<template>
  <v-card class="nudge-wizard" rounded="xl" elevation="3">
    <div class="nudge-wizard__progress" v-if="loading">
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
      <div class="nudge-wizard__meta">
        <v-chip v-if="totalMatches > 0" size="small" color="surface-primary-080">
          {{ $t('nudge-tool.meta.matches', { count: totalMatches }) }}
        </v-chip>
      </div>
      <v-btn
        color="primary"
        variant="flat"
        :disabled="!canNavigate"
        @click="navigateToCategoryPage"
      >
        {{ $t('nudge-tool.actions.seeAll') }}
      </v-btn>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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

const canNavigate = computed(() => Boolean(selectedCategory?.value))

const steps = computed(() => {
  const sequence: Array<{
    key: string
    component: any
    props: Record<string, unknown>
    onUpdate?: (...args: any[]) => void
  }> = []

  if (!props.initialCategoryId) {
    sequence.push({
      key: 'category',
      component: NudgeToolStepCategory,
      props: { categories: categories.value, selectedCategoryId: selectedCategoryId.value },
    })
  }

  sequence.push({
    key: 'condition',
    component: NudgeToolStepCondition,
    props: { modelValue: condition.value },
    onUpdate: (value: ProductConditionChoice) => {
      condition.value = value
      goToNext()
    },
  })

  if ((nudgeConfig.value?.scores?.length ?? 0) > 0) {
    sequence.push({
      key: 'scores',
      component: NudgeToolStepScores,
      props: { modelValue: selectedScores.value, scores: nudgeConfig.value?.scores ?? [] },
      onUpdate: (value: string[]) => (selectedScores.value = value),
    })
  }

  subsetGroups.value.forEach((group) => {
    const subsets = groupedSubsets.value[group.id ?? ''] ?? []
    if (!subsets.length) {
      return
    }

    sequence.push({
      key: `group-${group.id}`,
      component: NudgeToolStepSubsetGroup,
      props: { group, subsets, modelValue: activeSubsetIds.value },
      onUpdate: (value: string[]) => (activeSubsetIds.value = value),
    })
  })

  sequence.push({
    key: 'recommendations',
    component: NudgeToolStepRecommendations,
    props: {
      products: recommendations.value,
      popularAttributes: selectedCategory.value?.attributesConfig?.configs,
      totalCount: totalMatches.value,
      loading: loading.value,
    },
  })

  return sequence
})

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
        sort: { sorts: [{ field: 'impactScore.global', order: 'desc' }] },
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

  &__progress {
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }

  &__window {
    min-height: 420px;
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

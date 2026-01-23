import type {
  AttributeConfigDto,
  VerticalConfigDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'
import { useCategories } from '~/composables/categories/useCategories'

export type ImpactScoreCriterion = {
  key: string
  name: string
  utility: string
  icon: string | null
}

type CriteriaByVerticalId = Record<string, ImpactScoreCriterion[]>

const buildAttributeMap = (attributes: AttributeConfigDto[]) => {
  return attributes.reduce((map, attribute) => {
    if (attribute.key) {
      map.set(attribute.key, attribute)
    }
    return map
  }, new Map<string, AttributeConfigDto>())
}

export const buildCriteriaFromCategory = (
  category: VerticalConfigFullDto
): ImpactScoreCriterion[] => {
  const available = category.availableImpactScoreCriterias ?? []
  const attributeConfigs = category.attributesConfig?.configs ?? []
  const attributeMap = buildAttributeMap(attributeConfigs)

  return available.map(key => {
    const attribute = attributeMap.get(key)
    const fallbackName = attribute?.scoreTitle ?? attribute?.name ?? key

    return {
      key,
      name: fallbackName,
      utility: attribute?.scoreUtility ?? '',
      icon: attribute?.icon ?? null,
    }
  })
}

const mergeCriteria = (
  target: Map<string, ImpactScoreCriterion>,
  criteria: ImpactScoreCriterion[]
) => {
  criteria.forEach(criterion => {
    const existing = target.get(criterion.key)
    if (!existing) {
      target.set(criterion.key, { ...criterion })
      return
    }

    target.set(criterion.key, {
      ...existing,
      name: existing.name || criterion.name,
      utility: existing.utility || criterion.utility,
      icon: existing.icon || criterion.icon,
    })
  })
}

const normalizeVerticalLabel = (vertical: VerticalConfigDto) =>
  vertical.verticalHomeTitle?.trim() ||
  vertical.verticalMetaTitle?.trim() ||
  vertical.id ||
  ''

export const useImpactScoreCriteria = () => {
  const { categories, fetchCategories, fetchCategoryById } = useCategories()
  const criteriaByVerticalId = useState<CriteriaByVerticalId>(
    'impact-score-criteria-by-vertical',
    () => ({})
  )
  const loading = useState('impact-score-criteria-loading', () => false)
  const error = useState<string | null>('impact-score-criteria-error', () => null)

  const verticalOptions = computed(() =>
    categories.value
      .filter(vertical => Boolean(vertical.id))
      .map(vertical => ({
        id: vertical.id as string,
        label: normalizeVerticalLabel(vertical),
        order: vertical.order ?? Number.MAX_SAFE_INTEGER,
      }))
      .filter(option => Boolean(option.label))
      .sort((a, b) => a.order - b.order)
  )

  const allCriteria = computed(() => {
    const merged = new Map<string, ImpactScoreCriterion>()
    Object.values(criteriaByVerticalId.value).forEach(criteria => {
      mergeCriteria(merged, criteria)
    })
    return Array.from(merged.values()).sort((a, b) =>
      a.name.localeCompare(b.name, 'fr', { sensitivity: 'base' })
    )
  })

  const ensureCategories = async () => {
    if (!categories.value.length) {
      await fetchCategories()
    }
  }

  const loadCriteriaForVertical = async (
    verticalId: string,
    options?: { silent?: boolean }
  ) => {
    if (criteriaByVerticalId.value[verticalId]) {
      return
    }

    if (!options?.silent) {
      loading.value = true
      error.value = null
    }

    try {
      const category = await fetchCategoryById(verticalId)
      if (category) {
        criteriaByVerticalId.value[verticalId] =
          buildCriteriaFromCategory(category)
      }
    } catch (err) {
      error.value =
        err instanceof Error ? err.message : 'Failed to load criteria'
      console.error('Unable to load impact score criteria', err)
    } finally {
      if (!options?.silent) {
        loading.value = false
      }
    }
  }

  const loadAllCriteria = async () => {
    await ensureCategories()

    const verticalIds = categories.value
      .map(vertical => vertical.id)
      .filter((id): id is string => Boolean(id))
      .filter(id => !criteriaByVerticalId.value[id])

    if (!verticalIds.length) {
      return
    }

    loading.value = true
    error.value = null

    try {
      await Promise.all(
        verticalIds.map(id => loadCriteriaForVertical(id, { silent: true }))
      )
    } finally {
      loading.value = false
    }
  }

  return {
    criteriaByVerticalId,
    allCriteria,
    verticalOptions,
    loading: readonly(loading),
    error: readonly(error),
    loadCriteriaForVertical,
    loadAllCriteria,
  }
}

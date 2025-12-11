import type { Filter, FilterGroup, FilterRequestDto, SubsetCriteria, VerticalSubsetDto } from '~~/shared/api-client'

const parseNumericValue = (value: string | undefined): number | undefined => {
  if (value == null) {
    return undefined
  }

  const parsed = Number.parseFloat(value)

  return Number.isFinite(parsed) ? parsed : undefined
}

const convertCriteria = (criteria: SubsetCriteria | undefined): Filter | null => {
  if (!criteria?.field || !criteria.operator) {
    return null
  }

  if (criteria.operator === 'EQUALS') {
    if (!criteria.value) {
      return null
    }

    return {
      field: criteria.field,
      operator: 'term',
      terms: [criteria.value],
    }
  }

  const numericValue = parseNumericValue(criteria.value)

  if (numericValue == null) {
    return null
  }

  if (criteria.operator === 'LOWER_THAN') {
    return {
      field: criteria.field,
      operator: 'range',
      max: numericValue,
    }
  }

  if (criteria.operator === 'GREATER_THAN') {
    return {
      field: criteria.field,
      operator: 'range',
      min: numericValue,
    }
  }

  return null
}

export const convertSubsetCriteriaToFilters = (subset: VerticalSubsetDto): Filter[] => {
  const clauses = subset.criterias ?? []

  return clauses
    .map((criteria) => convertCriteria(criteria))
    .filter((filter): filter is Filter => Boolean(filter))
}

const normalizeTerms = (terms: string[] | undefined): string[] => {
  return (terms ?? []).map((term) => `${term}`)
}

const areTermFiltersEqual = (left: Filter, right: Filter): boolean => {
  const leftTerms = normalizeTerms(left.terms)
  const rightTerms = normalizeTerms(right.terms)

  if (leftTerms.length !== rightTerms.length) {
    return false
  }

  const sortedLeft = [...leftTerms].sort()
  const sortedRight = [...rightTerms].sort()

  return sortedLeft.every((value, index) => value === sortedRight[index])
}

const areRangeFiltersEqual = (left: Filter, right: Filter): boolean => {
  return (left.min ?? null) === (right.min ?? null) && (left.max ?? null) === (right.max ?? null)
}

export const areFiltersEquivalent = (left: Filter, right: Filter): boolean => {
  if (left.field !== right.field || left.operator !== right.operator) {
    return false
  }

  if (left.operator === 'term' && right.operator === 'term') {
    return areTermFiltersEqual(left, right)
  }

  if (left.operator === 'range' && right.operator === 'range') {
    return areRangeFiltersEqual(left, right)
  }

  return false
}

export const mergeFiltersWithoutDuplicates = (existing: Filter[], additions: Filter[]): Filter[] => {
  if (!additions.length) {
    return [...existing]
  }

  const merged = [...existing]

  additions.forEach((candidate) => {
    if (!merged.some((entry) => areFiltersEquivalent(entry, candidate))) {
      merged.push(candidate)
    }
  })

  return merged
}

export const getRemainingSubsetFilters = (
  subset: VerticalSubsetDto | undefined,
  removedIndex: number,
): Filter[] => {
  if (!subset) {
    return []
  }

  return convertSubsetCriteriaToFilters(subset).filter((_, index) => index !== removedIndex)
}

const buildGroupKey = (subset: VerticalSubsetDto): string => {
  return subset.group ?? subset.id ?? 'ungrouped'
}

export const buildFilterRequestFromSubsets = (
  subsets: VerticalSubsetDto[],
  activeSubsetIds: string[],
): FilterRequestDto => {
  const groups = new Map<string, Filter[]>()

  activeSubsetIds.forEach((subsetId) => {
    const subset = subsets.find((candidate) => candidate.id === subsetId)
    if (!subset) {
      return
    }

    const groupKey = buildGroupKey(subset)
    const current = groups.get(groupKey) ?? []
    const filters = convertSubsetCriteriaToFilters(subset)
    groups.set(groupKey, [...current, ...filters])
  })

  const filterGroups: FilterGroup[] = Array.from(groups.values())
    .map((filters) => mergeFiltersWithoutDuplicates([], filters))
    .map((filters) => ({ filters }))
    .filter((group): group is FilterGroup => Boolean(group.filters?.length))

  return filterGroups.length ? { filterGroups } : {}
}

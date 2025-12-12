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

const mergeRangeClauses = (filters: Filter[]): Filter[] => {
  if (!filters.length) {
    return []
  }

  const mergedRanges = new Map<string, { min?: number, max?: number }>()
  const mergedFilters: Filter[] = []

  for (const filter of filters) {
    if (filter.operator === 'range' && filter.field) {
      const existing = mergedRanges.get(filter.field) ?? {}
      const min = filter.min != null ? filter.min : existing.min
      const max = filter.max != null ? filter.max : existing.max
      mergedRanges.set(filter.field, { min, max })
    }
    else {
      mergedFilters.push(filter)
    }
  }

  for (const [field, bounds] of mergedRanges.entries()) {
    mergedFilters.push({ field, operator: 'range', min: bounds.min, max: bounds.max })
  }

  return mergedFilters
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



export const buildFilterRequestFromSubsets = (
  subsets: VerticalSubsetDto[],
  activeSubsetIds: string[],
): FilterRequestDto => {
  const seen = new Set<string>()
  const groupedFilters = new Map<string, Filter[]>()

  activeSubsetIds
    .map((subsetId) => subsets.find((candidate) => candidate.id === subsetId))
    .filter((subset): subset is VerticalSubsetDto => Boolean(subset) && !seen.has(subset.id) && seen.add(subset.id))
    .forEach((subset) => {
      const mergedClauses = mergeRangeClauses(convertSubsetCriteriaToFilters(subset))
      if (!mergedClauses.length) {
        return
      }

      const groupKey = subset.group ?? subset.id
      const existingClauses = groupedFilters.get(groupKey) ?? []
      groupedFilters.set(groupKey, mergeFiltersWithoutDuplicates(existingClauses, mergedClauses))
    })

  const filterGroups: FilterGroup[] = Array.from(groupedFilters.values())
    .map((clauses) => ({ should: clauses }))
    .filter((group) => Boolean(group.should?.length))

  if (!filterGroups.length) {
    return {}
  }

  return { filterGroups }
}

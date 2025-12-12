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

const mergeRangeFilters = (filters: Filter[]): Filter[] => {
  const rangeFilters = filters.filter((filter) => filter.operator === 'range' && filter.field)
  const nonRangeFilters = filters.filter((filter) => filter.operator !== 'range')

  const mergedByField = Array.from(
    rangeFilters.reduce((acc, filter) => {
      const existing = acc.get(filter.field)
      const candidateMin = filter.min ?? null
      const candidateMax = filter.max ?? null

      if (!existing) {
        acc.set(filter.field, { min: candidateMin, max: candidateMax })
        return acc
      }

      const mergedMin = existing.min == null ? candidateMin : Math.max(existing.min, candidateMin ?? existing.min)
      const mergedMax = existing.max == null ? candidateMax : Math.min(existing.max, candidateMax ?? existing.max)
      acc.set(filter.field, { min: mergedMin ?? undefined, max: mergedMax ?? undefined })
      return acc
    }, new Map<string, { min?: number | null; max?: number | null }>()),
  ).map(([field, bounds]) => ({
    field,
    operator: 'range',
    min: bounds.min ?? undefined,
    max: bounds.max ?? undefined,
  })) as Filter[]

  return [...nonRangeFilters, ...mergedByField]
}

const mergeTermFilters = (filters: Filter[]): Filter[] => {
  const termFilters = filters.filter((filter) => filter.operator === 'term' && filter.field)
  const nonTermFilters = filters.filter((filter) => filter.operator !== 'term')

  const mergedByField = Array.from(
    termFilters.reduce((acc, filter) => {
      const existingTerms = acc.get(filter.field) ?? []
      const normalizedTerms = normalizeTerms(filter.terms)
      acc.set(filter.field, Array.from(new Set([...existingTerms, ...normalizedTerms])))
      return acc
    }, new Map<string, string[]>()),
  ).map(([field, terms]) => ({
    field,
    operator: 'term',
    terms,
  })) as Filter[]

  return [...nonTermFilters, ...mergedByField]
}

export const convertSubsetCriteriaToFilters = (subset: VerticalSubsetDto): Filter[] => {
  const clauses = subset.criterias ?? []

  const converted = clauses
    .map((criteria) => convertCriteria(criteria))
    .filter((filter): filter is Filter => Boolean(filter))

  const mergedRanges = mergeRangeFilters(converted)
  return mergeTermFilters(mergedRanges)
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

  const remainingCriterias = (subset.criterias ?? []).filter((_, index) => index !== removedIndex)
  const filteredSubset: VerticalSubsetDto = { ...subset, criterias: remainingCriterias }

  return convertSubsetCriteriaToFilters(filteredSubset)
}



export const buildFilterRequestFromSubsets = (
  subsets: VerticalSubsetDto[],
  activeSubsetIds: string[],
): FilterRequestDto => {
  const seen = new Set<string>()
  const groupedFilters = activeSubsetIds
    .map((subsetId) => subsets.find((candidate) => candidate.id === subsetId))
    .filter((subset): subset is VerticalSubsetDto => Boolean(subset) && !seen.has(subset.id) && seen.add(subset.id))
    .reduce<Map<string, Filter[]>>((acc, subset) => {
      const filters = convertSubsetCriteriaToFilters(subset)
      if (!filters.length) {
        return acc
      }

      const groupKey = subset.group ?? subset.id
      const currentFilters = acc.get(groupKey) ?? []
      acc.set(groupKey, mergeFiltersWithoutDuplicates(currentFilters, filters))
      return acc
    }, new Map())

  const filterGroups: FilterGroup[] = Array.from(groupedFilters.values())
    .map((shouldClauses) => (shouldClauses.length ? { should: shouldClauses } : null))
    .filter((group): group is FilterGroup => Boolean(group))

  if (!filterGroups.length) {
    return {}
  }

  return { filterGroups }
}

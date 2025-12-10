import type { Filter, FilterRequestDto, SubsetCriteria, VerticalSubsetDto } from '~~/shared/api-client'

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

const groupFiltersByField = (filters: Filter[]): Map<string, Filter[]> => {
  const buckets = new Map<string, Filter[]>()

  filters.forEach((filter) => {
    if (!filter.field || !filter.operator) {
      return
    }

    const key = `${filter.field ?? ''}|${filter.operator}`
    const bucket = buckets.get(key) ?? []
    bucket.push(filter)
    buckets.set(key, bucket)
  })

  return buckets
}

const mergeTermFilters = (field: string, filters: Filter[]): Filter | null => {
  const terms = filters.flatMap((filter) => normalizeTerms(filter.terms))
  if (!terms.length) {
    return null
  }

  return { field, operator: 'term', terms: Array.from(new Set(terms)) }
}

const mergeRangeFilters = (field: string, filters: Filter[]): Filter | null => {
  const minValues = filters
    .map((filter) => filter.min)
    .filter((value): value is number => typeof value === 'number')
  const maxValues = filters
    .map((filter) => filter.max)
    .filter((value): value is number => typeof value === 'number')

  const merged: Filter = { field, operator: 'range' }

  if (minValues.length) {
    merged.min = Math.min(...minValues)
  }

  if (maxValues.length) {
    merged.max = Math.max(...maxValues)
  }

  if (merged.min != null && merged.max != null && merged.min > merged.max) {
    return null
  }

  if (merged.min == null && merged.max == null) {
    return null
  }

  return merged
}

const mergeGroupFilters = (filters: Filter[]): Filter[] => {
  const groupedByField = groupFiltersByField(filters)

  return Array.from(groupedByField.entries())
    .map(([, entries]) => {
      const operator = entries[0]?.operator
      const field = entries[0]?.field ?? ''

      if (operator === 'term') {
        return mergeTermFilters(field, entries)
      }

      if (operator === 'range') {
        return mergeRangeFilters(field, entries)
      }

      return null
    })
    .filter((filter): filter is Filter => Boolean(filter))
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

  const mergedFilters = Array.from(groups.values()).flatMap((filters) => mergeGroupFilters(filters))

  return mergedFilters.length ? { filters: mergedFilters } : {}
}

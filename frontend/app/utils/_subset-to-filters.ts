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

export const buildFilterRequestFromSubsets = (
  subsets: VerticalSubsetDto[],
  activeSubsetIds: string[],
): FilterRequestDto => {
  const filters = activeSubsetIds.flatMap((subsetId) => {
    const subset = subsets.find((candidate) => candidate.id === subsetId)
    return subset ? convertSubsetCriteriaToFilters(subset) : []
  })

  return filters.length ? { filters } : {}
}

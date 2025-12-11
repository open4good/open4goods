import type { Filter, FilterGroup, FilterRequestDto, NudgeToolScoreDto } from '~~/shared/api-client'

import { mergeFiltersWithoutDuplicates } from './_subset-to-filters'

export type ProductConditionChoice = 'new' | 'occasion'
export type ProductConditionSelection = ProductConditionChoice[]

const PRODUCT_STATE_FIELD = 'price.conditions'

export const buildConditionFilter = (choices: ProductConditionSelection): Filter | null => {
  if (!choices.length) {
    return null
  }

  const terms = Array.from(new Set(choices.map((choice) => (choice === 'new' ? 'NEW' : 'OCCASION'))))

  return {
    field: PRODUCT_STATE_FIELD,
    operator: 'term',
    terms,
  }
}

export const buildScoreFilters = (
  scores: NudgeToolScoreDto[],
  selectedNames: string[],
): Filter[] => {
  if (!selectedNames.length) {
    return []
  }

  return selectedNames
    .map((name) => {
      const matchedScore = scores.find((candidate) => candidate.scoreName === name)
      if (!matchedScore?.scoreMinValue) {
        return null
      }

      return {
        field: `scores.${name}.value`,
        operator: 'range',
        min: matchedScore.scoreMinValue,
      } satisfies Filter
    })
    .filter((filter): filter is Filter => Boolean(filter))
}

export const buildNudgeFilterRequest = (
  baseFilters: Filter[],
  conditionFilter: Filter | null,
  scoreFilters: Filter[],
  subsetFilterGroups: FilterGroup[],
): FilterRequestDto => {
  const withCondition = conditionFilter
    ? mergeFiltersWithoutDuplicates(baseFilters, [conditionFilter])
    : [...baseFilters]

  const enriched = mergeFiltersWithoutDuplicates(withCondition, [...scoreFilters])
  const filterGroups = subsetFilterGroups.filter(
    (group) =>
      (group.filters?.length ?? 0) > 0 ||
      (group.filterSets?.some((set) => (set.filters?.length ?? 0) > 0) ?? false),
  )

  return {
    ...(enriched.length ? { filters: enriched } : {}),
    ...(filterGroups.length ? { filterGroups } : {}),
  }
}

import { describe, expect, it } from 'vitest'

import type { Filter } from '~~/shared/api-client'

import {
  buildConditionFilter,
  buildNudgeFilterRequest,
  buildScoreFilters,
  type ProductConditionChoice,
} from './_nudge-tool-filters'

describe('nudge tool filters', () => {
  it('builds condition filters according to the choice', () => {
    const cases: Record<ProductConditionChoice, Filter | null> = {
      any: null,
      new: { field: 'price.conditions', operator: 'term', terms: ['NEW'] },
      occasion: { field: 'price.conditions', operator: 'term', terms: ['OCCASION'] },
    }

    Object.entries(cases).forEach(([choice, expected]) => {
      expect(buildConditionFilter(choice as ProductConditionChoice)).toEqual(expected)
    })
  })

  it('creates score filters using score min values', () => {
    const filters = buildScoreFilters(
      [
        { scoreName: 'IMPACT_SCORE', scoreMinValue: 70 },
        { scoreName: 'REPAIRABILITY', scoreMinValue: 6 },
      ],
      ['REPAIRABILITY'],
    )

    expect(filters).toEqual([
      { field: 'scores.REPAIRABILITY', operator: 'range', min: 6 },
    ])
  })

  it('merges base, condition, score and subset filters without duplicates', () => {
    const baseFilters: Filter[] = [{ field: 'price.minPrice.productState', operator: 'term', terms: ['NEW'] }]
    const conditionFilter: Filter = { field: 'price.minPrice.productState', operator: 'term', terms: ['NEW'] }
    const scoreFilters: Filter[] = [{ field: 'scores.IMPACT', operator: 'range', min: 50 }]
    const subsetFilters: Filter[] = [{ field: 'attributes.indexed.SIZE', operator: 'range', max: 40 }]

    const request = buildNudgeFilterRequest(baseFilters, conditionFilter, scoreFilters, subsetFilters)

    expect(request).toEqual({
      filters: [
        { field: 'price.minPrice.productState', operator: 'term', terms: ['NEW'] },
        { field: 'scores.IMPACT', operator: 'range', min: 50 },
        { field: 'attributes.indexed.SIZE', operator: 'range', max: 40 },
      ],
    })
  })
})

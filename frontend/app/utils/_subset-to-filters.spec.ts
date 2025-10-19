import { describe, expect, it } from 'vitest'
import type { Filter, VerticalSubsetDto } from '~~/shared/api-client'

import {
  convertSubsetCriteriaToFilters,
  getRemainingSubsetFilters,
  mergeFiltersWithoutDuplicates,
} from './_subset-to-filters'

describe('_subset-to-filters helpers', () => {
  const sampleSubset: VerticalSubsetDto = {
    id: 'sample',
    title: 'Sample subset',
    criterias: [
      { field: 'price.min', operator: 'LOWER_THAN', value: '500' },
      { field: 'price.min', operator: 'GREATER_THAN', value: '0' },
    ],
  }

  it('returns remaining subset filters when removing a specific clause', () => {
    const remaining = getRemainingSubsetFilters(sampleSubset, 0)
    expect(remaining).toHaveLength(1)

    const remainingFilter = remaining[0]
    expect(remainingFilter).toMatchObject({
      field: 'price.min',
      operator: 'range',
      min: 0,
    })
  })

  it('deduplicates filters when merging manual and subset clauses', () => {
    const existing: Filter[] = [
      { field: 'price.min', operator: 'range', max: 500 },
    ]
    const additions = convertSubsetCriteriaToFilters(sampleSubset)

    const merged = mergeFiltersWithoutDuplicates(existing, additions)
    expect(merged).toHaveLength(2)
    expect(merged).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ field: 'price.min', operator: 'range', max: 500 }),
        expect.objectContaining({ field: 'price.min', operator: 'range', min: 0 }),
      ]),
    )
  })
})

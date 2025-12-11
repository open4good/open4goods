import { describe, expect, it } from 'vitest'
import type { Filter, VerticalSubsetDto } from '~~/shared/api-client'

import {
  convertSubsetCriteriaToFilters,
  buildFilterRequestFromSubsets,
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

  it('merges subset selections within the same group using OR semantics for term filters', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'new-items',
        group: 'condition',
        criterias: [{ field: 'price.conditions', operator: 'EQUALS', value: 'NEW' }],
      },
      {
        id: 'used-items',
        group: 'condition',
        criterias: [{ field: 'price.conditions', operator: 'EQUALS', value: 'OCCASION' }],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, ['new-items', 'used-items'])

    expect(request).toEqual({
      filterGroups: [
        {
          must: [
            { field: 'price.conditions', operator: 'term', terms: ['NEW'] },
            { field: 'price.conditions', operator: 'term', terms: ['OCCASION'] },
          ],
        },
      ],
    })
  })

  it('combines compatible range filters by widening the bounds', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'mid-range',
        group: 'price',
        criterias: [{ field: 'price.min', operator: 'LOWER_THAN', value: '800' }],
      },
      {
        id: 'entry',
        group: 'price',
        criterias: [{ field: 'price.min', operator: 'LOWER_THAN', value: '500' }],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, ['mid-range', 'entry'])

    expect(request.filterGroups?.[0]?.must).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ field: 'price.min', operator: 'range', max: 800 }),
        expect.objectContaining({ field: 'price.min', operator: 'range', max: 500 }),
      ]),
    )
  })

  it('drops conflicting range filters when they cannot be merged safely', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'budget',
        group: 'price',
        criterias: [{ field: 'price.min', operator: 'LOWER_THAN', value: '500' }],
      },
      {
        id: 'premium',
        group: 'price',
        criterias: [{ field: 'price.min', operator: 'GREATER_THAN', value: '1000' }],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, ['budget', 'premium'])

    expect(request.filterGroups?.[0]?.must).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ field: 'price.min', operator: 'range', max: 500 }),
        expect.objectContaining({ field: 'price.min', operator: 'range', min: 1000 }),
      ]),
    )
  })
})

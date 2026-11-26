import { describe, expect, it } from 'vitest'
import type { Filter, VerticalSubsetDto } from '~~/shared/api-client'

import {
  areFiltersEquivalent,
  convertCriteriaListToFilters,
  convertSubsetCriteriaToFilters,
  buildFilterRequestFromCriteria,
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
      { field: 'price.min', operator: 'range', max: 500, maxExclusive: true },
    ]
    const additions = convertSubsetCriteriaToFilters(sampleSubset)

    const merged = mergeFiltersWithoutDuplicates(existing, additions)
    expect(merged).toHaveLength(2)
    expect(merged).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          field: 'price.min',
          operator: 'range',
          max: 500,
        }),
        expect.objectContaining({
          field: 'price.min',
          operator: 'range',
          min: 0,
        }),
      ])
    )
  })

  it('merges subset selections within the same group using OR semantics for term filters', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'new-items',
        group: 'condition',
        criterias: [
          { field: 'price.conditions', operator: 'EQUALS', value: 'NEW' },
        ],
      },
      {
        id: 'used-items',
        group: 'condition',
        criterias: [
          { field: 'price.conditions', operator: 'EQUALS', value: 'OCCASION' },
        ],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, [
      'new-items',
      'used-items',
    ])

    expect(request).toEqual({
      filterGroups: [
        {
          should: [
            { field: 'price.conditions', operator: 'term', terms: ['NEW'] },
            {
              field: 'price.conditions',
              operator: 'term',
              terms: ['OCCASION'],
            },
          ],
        },
      ],
    })
  })

  it('unions range subsets from the same group into should clauses', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'mid-range',
        group: 'price',
        criterias: [
          { field: 'price.min', operator: 'LOWER_THAN', value: '800' },
        ],
      },
      {
        id: 'entry',
        group: 'price',
        criterias: [
          { field: 'price.min', operator: 'LOWER_THAN', value: '500' },
        ],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, [
      'mid-range',
      'entry',
    ])

    expect(request).toEqual({
      filterGroups: [
        {
          should: [
            {
              field: 'price.min',
              operator: 'range',
              max: 800,
              maxExclusive: true,
            },
            {
              field: 'price.min',
              operator: 'range',
              max: 500,
              maxExclusive: true,
            },
          ],
        },
      ],
    })
  })

  it('converts CONTAINS subsets into contains filters within should groups', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'cleaning_pyrolysis',
        group: 'cleaning',
        criterias: [
          {
            field: 'attributes.indexed.OVEN_CLEANING_MODE',
            operator: 'CONTAINS',
            value: 'PYROLYSE',
          },
        ],
      },
      {
        id: 'cleaning_catalysis',
        group: 'cleaning',
        criterias: [
          {
            field: 'attributes.indexed.OVEN_CLEANING_MODE',
            operator: 'CONTAINS',
            value: 'CATALYSE',
          },
        ],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, [
      'cleaning_pyrolysis',
      'cleaning_catalysis',
    ])

    expect(request).toEqual({
      filterGroups: [
        {
          should: [
            {
              field: 'attributes.indexed.OVEN_CLEANING_MODE',
              operator: 'contains',
              terms: ['PYROLYSE'],
            },
            {
              field: 'attributes.indexed.OVEN_CLEANING_MODE',
              operator: 'contains',
              terms: ['CATALYSE'],
            },
          ],
        },
      ],
    })
  })

  it('keeps distinct range buckets when they cannot be merged', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'budget',
        group: 'price',
        criterias: [
          { field: 'price.min', operator: 'LOWER_THAN', value: '500' },
        ],
      },
      {
        id: 'premium',
        group: 'price',
        criterias: [
          { field: 'price.min', operator: 'GREATER_THAN', value: '1000' },
        ],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, [
      'budget',
      'premium',
    ])

    expect(request).toEqual({
      filterGroups: [
        {
          should: [
            {
              field: 'price.min',
              operator: 'range',
              max: 500,
              maxExclusive: true,
            },
            {
              field: 'price.min',
              operator: 'range',
              min: 1000,
              minExclusive: true,
            },
          ],
        },
      ],
    })
  })

  it('supports GREATER_THAN_OR_EQUAL and LOWER_THAN_OR_EQUAL operators', () => {
    const subsets: VerticalSubsetDto[] = [
      {
        id: 'distance-comfort',
        group: 'distance',
        criterias: [
          {
            field: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
            operator: 'GREATER_THAN_OR_EQUAL',
            value: '24',
          },
          {
            field: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
            operator: 'LOWER_THAN_OR_EQUAL',
            value: '47',
          },
        ],
      },
    ]

    const request = buildFilterRequestFromSubsets(subsets, ['distance-comfort'])

    expect(request).toEqual({
      filterGroups: [
        {
          should: [
            {
              field: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
              operator: 'range',
              min: 24,
              max: 47,
            },
          ],
        },
      ],
    })
  })

  it('builds immutable sub-category filters with AND semantics', () => {
    const request = buildFilterRequestFromCriteria([
      { field: 'price.min', operator: 'LOWER_THAN', value: '800' },
      { field: 'price.min', operator: 'GREATER_THAN', value: '300' },
      {
        field: 'attributes.indexed.INSTALLATION_TYPE.value',
        operator: 'EQUALS',
        value: 'ENCASTRABLE',
      },
    ])

    expect(request).toEqual({
      filters: [
        {
          field: 'attributes.indexed.INSTALLATION_TYPE.value',
          operator: 'term',
          terms: ['ENCASTRABLE'],
        },
        {
          field: 'price.min',
          operator: 'range',
          min: 300,
          max: 800,
          minExclusive: true,
          maxExclusive: true,
        },
      ],
    })
  })

  describe('bound exclusivity (GOU-2586)', () => {
    const field = 'attributes.indexed.WASHING_CAPACITY.numericValue'

    it.each([
      ['LOWER_THAN', { max: 8, maxExclusive: true }],
      ['LOWER_THAN_OR_EQUAL', { max: 8 }],
      ['GREATER_THAN', { min: 8, minExclusive: true }],
      ['GREATER_THAN_OR_EQUAL', { min: 8 }],
    ] as const)(
      'converts %s into the matching range bound',
      (operator, bound) => {
        const [filter] = convertCriteriaListToFilters([
          { field, operator, value: '8' },
        ])

        expect(filter).toStrictEqual({ field, operator: 'range', ...bound })
      }
    )

    it('keeps the exclusive upper bound of the "8-9 kg" sub-category', () => {
      const request = buildFilterRequestFromCriteria([
        { field, operator: 'GREATER_THAN_OR_EQUAL', value: '8' },
        { field, operator: 'LOWER_THAN', value: '10' },
      ])

      expect(request).toStrictEqual({
        filters: [
          { field, operator: 'range', min: 8, max: 10, maxExclusive: true },
        ],
      })
    })

    it('takes the exclusivity of the clause that sets each merged bound', () => {
      const request = buildFilterRequestFromCriteria([
        { field, operator: 'GREATER_THAN', value: '6' },
        { field, operator: 'LOWER_THAN', value: '12' },
        { field, operator: 'GREATER_THAN_OR_EQUAL', value: '8' },
      ])

      expect(request).toStrictEqual({
        filters: [
          { field, operator: 'range', min: 8, max: 12, maxExclusive: true },
        ],
      })
    })

    it('does not treat exclusive and inclusive bounds as duplicates', () => {
      const inclusive: Filter = { field, operator: 'range', max: 8 }
      const exclusive: Filter = {
        field,
        operator: 'range',
        max: 8,
        maxExclusive: true,
      }

      expect(areFiltersEquivalent(inclusive, exclusive)).toBe(false)
      expect(
        areFiltersEquivalent(exclusive, { ...exclusive, minExclusive: false })
      ).toBe(true)
      expect(mergeFiltersWithoutDuplicates([inclusive], [exclusive])).toEqual([
        inclusive,
        exclusive,
      ])
    })
  })
})

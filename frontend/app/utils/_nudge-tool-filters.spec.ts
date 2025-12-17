import { describe, expect, it } from 'vitest'

import type { Filter } from '~~/shared/api-client'

import {
  buildConditionFilter,
  buildNudgeFilterRequest,
  buildScoreFilters,
  type ProductConditionSelection,
} from './_nudge-tool-filters'

describe('nudge tool filters', () => {
  it('builds condition filters according to the choice', () => {
    const cases: Record<string, Filter | null> = {
      none: null,
      new: { field: 'price.conditions', operator: 'term', terms: ['NEW'] },
      occasion: {
        field: 'price.conditions',
        operator: 'term',
        terms: ['OCCASION'],
      },
      multi: {
        field: 'price.conditions',
        operator: 'term',
        terms: ['NEW', 'OCCASION'],
      },
    }

    Object.entries(cases).forEach(([choice, expected]) => {
      const selection: ProductConditionSelection =
        choice === 'none'
          ? []
          : choice === 'multi'
            ? ['new', 'occasion']
            : [choice as ProductConditionSelection[number]]

      expect(buildConditionFilter(selection)).toEqual(expected)
    })
  })

  it('creates score filters using score min values', () => {
    const filters = buildScoreFilters(
      [
        { scoreName: 'IMPACT_SCORE', scoreMinValue: 70 },
        { scoreName: 'REPAIRABILITY', scoreMinValue: 6 },
      ],
      ['REPAIRABILITY']
    )

    expect(filters).toEqual([
      { field: 'scores.REPAIRABILITY.value', operator: 'range', min: 6 },
    ])
  })

  it('merges base, condition, score and subset filters without duplicates', () => {
    const baseFilters: Filter[] = [
      {
        field: 'price.minPrice.productState',
        operator: 'term',
        terms: ['NEW'],
      },
    ]
    const conditionFilter: Filter = {
      field: 'price.minPrice.productState',
      operator: 'term',
      terms: ['NEW'],
    }
    const scoreFilters: Filter[] = [
      { field: 'scores.IMPACT.value', operator: 'range', min: 50 },
    ]
    const subsetFilters = [
      {
        must: [
          { field: 'attributes.indexed.SIZE', operator: 'range', max: 40 },
        ],
      },
    ]

    const request = buildNudgeFilterRequest(
      baseFilters,
      conditionFilter,
      scoreFilters,
      subsetFilters
    )

    expect(request).toEqual({
      filters: [
        {
          field: 'price.minPrice.productState',
          operator: 'term',
          terms: ['NEW'],
        },
        { field: 'scores.IMPACT.value', operator: 'range', min: 50 },
      ],
      filterGroups: [
        {
          must: [
            { field: 'attributes.indexed.SIZE', operator: 'range', max: 40 },
          ],
          should: [],
        },
      ],
    })
  })

  it('preserves multiple subset filter groups', () => {
    const request = buildNudgeFilterRequest(
      [],
      null,
      [],
      [
        {
          must: [{ field: 'price.min', operator: 'range', max: 500 }],
          should: [],
        },
        {
          should: [
            { field: 'scores.ECOSCORE.value', operator: 'range', min: 2 },
          ],
          must: [],
        },
      ]
    )

    expect(request.filterGroups).toEqual([
      {
        must: [{ field: 'price.min', operator: 'range', max: 500 }],
        should: [],
      },
      {
        should: [{ field: 'scores.ECOSCORE.value', operator: 'range', min: 2 }],
        must: [],
      },
    ])
  })
})

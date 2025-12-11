import { describe, expect, it } from 'vitest'

import type { FilterRequestDto } from '~~/shared/api-client'

import { mergeFilterRequests } from './_merge-filter-requests'

describe('mergeFilterRequests', () => {
  it('returns undefined when both payloads are empty', () => {
    expect(mergeFilterRequests({}, {})).toBeUndefined()
  })

  it('merges filters and filter groups from both payloads', () => {
    const primary: FilterRequestDto = {
      filters: [{ field: 'fieldA', operator: 'term', terms: ['a'] }],
      filterGroups: [{ must: [{ field: 'groupA', operator: 'range', min: 1 }] }],
    }
    const secondary: FilterRequestDto = {
      filters: [{ field: 'fieldB', operator: 'term', terms: ['b'] }],
      filterGroups: [{ must: [{ field: 'groupB', operator: 'range', max: 10 }] }],
    }

    expect(mergeFilterRequests(primary, secondary)).toEqual({
      filters: [
        { field: 'fieldA', operator: 'term', terms: ['a'] },
        { field: 'fieldB', operator: 'term', terms: ['b'] },
      ],
      filterGroups: [
        { must: [{ field: 'groupA', operator: 'range', min: 1 }] },
        { must: [{ field: 'groupB', operator: 'range', max: 10 }] },
      ],
    })
  })

  it('keeps filter groups when no plain filters exist', () => {
    const primary: FilterRequestDto = {
      filterGroups: [{ must: [{ field: 'only-group', operator: 'term', terms: ['x'] }] }],
    }

    expect(mergeFilterRequests(primary, undefined)).toEqual({
      filterGroups: [{ must: [{ field: 'only-group', operator: 'term', terms: ['x'] }] }],
    })
  })
})

import { describe, expect, it } from 'vitest'

import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

import {
  CATEGORY_INITIAL_SORT,
  matchesCategoryInitialSort,
} from './_category-initial-sort'

describe('category initial sort (GOU-1940)', () => {
  it('sorts the server-rendered listing by ImpactScore, best first', () => {
    expect(CATEGORY_INITIAL_SORT).toEqual({
      sorts: [{ field: ECOSCORE_RELATIVE_FIELD, order: 'desc' }],
    })
  })

  it('reuses the server-rendered listing only for the same sort', () => {
    expect(matchesCategoryInitialSort(ECOSCORE_RELATIVE_FIELD, 'desc')).toBe(
      true
    )
    expect(matchesCategoryInitialSort(ECOSCORE_RELATIVE_FIELD, 'asc')).toBe(
      false
    )
    expect(matchesCategoryInitialSort('price.minPrice.price', 'desc')).toBe(
      false
    )
    expect(matchesCategoryInitialSort(null, 'desc')).toBe(false)
  })
})

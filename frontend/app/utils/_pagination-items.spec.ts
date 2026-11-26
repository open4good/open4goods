import { describe, expect, it } from 'vitest'
import { buildPaginationItems } from './_pagination-items'

describe('buildPaginationItems', () => {
  it('returns nothing for a single page', () => {
    expect(buildPaginationItems(1, 1)).toEqual([])
  })

  it('lists every page when there are few', () => {
    expect(buildPaginationItems(5, 3)).toEqual([1, 2, 3, 4, 5])
  })

  it('collapses distant pages into ellipses', () => {
    expect(buildPaginationItems(20, 10)).toEqual([
      1,
      null,
      8,
      9,
      10,
      11,
      12,
      null,
      20,
    ])
  })

  it('fills a single-page gap instead of an ellipsis', () => {
    expect(buildPaginationItems(10, 5)).toEqual([1, 2, 3, 4, 5, 6, 7, null, 10])
  })
})

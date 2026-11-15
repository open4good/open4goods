import { describe, expect, it } from 'vitest'
import {
  haveDifferentCriteriaSets,
  shouldShowNotRatedAfterRatedNotice,
  resolveCriteriaCount,
  resolveImpactScoreV2,
} from './_impact-score-v2'

const status = (real?: number, total?: number, version = 2) => ({
  status: 'RATED' as const,
  currentMethodologyVersion: version,
  realCriteriaCount: real,
  totalCriteriaCount: total,
})

describe('resolveImpactScoreV2', () => {
  it('returns null on a V1 vertical', () => {
    expect(resolveImpactScoreV2({ impactScore: status(3, 5, 1) })).toBeNull()
  })
})

describe('resolveCriteriaCount', () => {
  it('returns the counter in V2 and null when a count is missing', () => {
    expect(resolveCriteriaCount(status(3, 5))).toEqual({ count: 3, total: 5 })
    expect(resolveCriteriaCount(status(undefined, 5))).toBeNull()
    expect(resolveCriteriaCount(null)).toBeNull()
  })
})

describe('haveDifferentCriteriaSets', () => {
  it('is false for equal sets, V1 products and a single product', () => {
    expect(haveDifferentCriteriaSets([status(3, 5), status(3, 5)])).toBe(false)
    expect(haveDifferentCriteriaSets([status(3, 5), null])).toBe(false)
    expect(haveDifferentCriteriaSets([status(3, 5)])).toBe(false)
  })
  it('is true when counts differ', () => {
    expect(haveDifferentCriteriaSets([status(3, 5), status(4, 5)])).toBe(true)
  })
})

describe('shouldShowNotRatedAfterRatedNotice', () => {
  const v2 = { scores: { impactScore: status(3, 5) } } as never
  const v1 = { scores: { impactScore: status(3, 5, 1) } } as never

  it('shows only on the Impact Score sort of a V2 vertical', () => {
    expect(
      shouldShowNotRatedAfterRatedNotice('scores.ECOSCORE.value', [v2])
    ).toBe(true)
    expect(shouldShowNotRatedAfterRatedNotice('price', [v2])).toBe(false)
    expect(shouldShowNotRatedAfterRatedNotice(null, [v2])).toBe(false)
    expect(
      shouldShowNotRatedAfterRatedNotice('scores.ECOSCORE.value', [v1])
    ).toBe(false)
  })
})

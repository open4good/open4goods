import { describe, expect, it } from 'vitest'
import {
  clampDistance,
  computeTvSize,
  TV_SIZE_SUBCATEGORY,
  TV_STANDARD_SIZES,
} from './tv-size'

describe('computeTvSize', () => {
  it.each([
    // distance, resolution, minCm, maxCm, minIn, maxIn, recommended
    [1, '4k', 62.5, 83.3, 25, 33, 32],
    [2, '4k', 125, 166.7, 49, 66, 55],
    [3, '4k', 187.5, 250, 74, 98, 85],
    [4, '4k', 250, 333.3, 98, 131, 85],
    [6, '4k', 375, 500, 148, 197, 85],
    [1, 'full-hd', 50, 62.5, 20, 25, 32],
    [2, 'full-hd', 100, 125, 39, 49, 43],
    [3, 'full-hd', 150, 187.5, 59, 74, 65],
    [4, 'full-hd', 200, 250, 79, 98, 85],
    [6, 'full-hd', 300, 375, 118, 148, 85],
  ] as const)(
    '%s m %s → %s–%s cm, %s–%s", recommended %s"',
    (d, res, minCm, maxCm, minIn, maxIn, rec) => {
      const r = computeTvSize(d, res)
      expect(r.minCm).toBe(minCm)
      expect(r.maxCm).toBe(maxCm)
      expect(r.minInches).toBe(minIn)
      expect(r.maxInches).toBe(maxIn)
      expect(r.recommendedInches).toBe(rec)
    }
  )

  it('always recommends a standard size', () => {
    for (let d = 1; d <= 6; d += 0.1) {
      expect(TV_STANDARD_SIZES).toContain(
        computeTvSize(d, '4k').recommendedInches
      )
    }
  })

  it('computes the 16:9 width and height of the recommended size', () => {
    const r = computeTvSize(2, '4k') // 55"
    expect(r.widthCm).toBe(121.8)
    expect(r.heightCm).toBe(68.5)
  })

  it('flags a range outside the standard sizes', () => {
    expect(computeTvSize(4, '4k').beyond).toBe('above')
    expect(computeTvSize(6, 'full-hd').beyond).toBe('above')
    expect(computeTvSize(3, '4k').beyond).toBeNull()
    expect(computeTvSize(2, '4k').beyond).toBeNull()
    expect(computeTvSize(1, 'full-hd').beyond).toBe('below')
    expect(computeTvSize(1, '4k').beyond).toBeNull()
  })

  it('clamps the distance to 1–6 m and falls back on NaN', () => {
    expect(clampDistance(0.2)).toBe(1)
    expect(clampDistance(12)).toBe(6)
    expect(clampDistance(Number.NaN)).toBe(3)
    expect(computeTvSize(12, '4k').distanceM).toBe(6)
  })

  it('maps every standard size to a sub-category', () => {
    for (const size of TV_STANDARD_SIZES) {
      expect(TV_SIZE_SUBCATEGORY[size]).toBeTruthy()
    }
  })
})

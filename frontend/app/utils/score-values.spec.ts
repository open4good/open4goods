import { describe, expect, it } from 'vitest'
import type { ProductScoreDto } from 'shared/api-client'
import { extractScoreValue, resolveScoreNumericValue } from './score-values'

describe('score value helpers', () => {
  const baseScore: ProductScoreDto = {
    id: 'TEST',
    name: 'Test score',
  }

  it('prioritises the canonical value field', () => {
    const resolved = resolveScoreNumericValue({
      ...baseScore,
      value: 4.2,
      relativ: { value: 1.1 },
    })

    expect(resolved).toEqual({ value: 4.2, source: 'value' })
  })

  it('falls back to relativ and legacy relative values', () => {
    const relativScore = resolveScoreNumericValue({
      ...baseScore,
      relativ: { value: 3.3 },
    })
    const legacyScore = resolveScoreNumericValue({
      ...baseScore,
      relative: { value: 2.5 },
    })

    expect(relativScore).toEqual({ value: 3.3, source: 'relative' })
    expect(legacyScore).toEqual({ value: 2.5, source: 'legacyRelative' })
  })

  it('uses percent or on20 when no relative values exist', () => {
    const percentScore = resolveScoreNumericValue({ ...baseScore, percent: 75 })
    const on20Score = resolveScoreNumericValue({ ...baseScore, on20: 12 })

    expect(percentScore).toEqual({ value: 75, source: 'percent' })
    expect(on20Score).toEqual({ value: 12, source: 'on20' })
  })

  it('returns null when no usable value is present', () => {
    expect(resolveScoreNumericValue(baseScore)).toBeNull()
  })

  it('extracts only the numeric value', () => {
    expect(extractScoreValue({ ...baseScore, value: 4.6 })).toBe(4.6)
    expect(extractScoreValue({ ...baseScore, relativ: { value: 2.4 } })).toBe(
      2.4
    )
    expect(extractScoreValue(baseScore)).toBeNull()
  })
})

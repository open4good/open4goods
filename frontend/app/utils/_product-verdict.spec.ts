import { describe, expect, it } from 'vitest'
import type { ProductDto } from '~~/shared/api-client'
import {
  getDataQualityOn20,
  getReliabilityVerdict,
  isDataQualityBelowCategoryAverage,
} from './_product-verdict'

const withDataQuality = (
  value: number,
  relativ: { value?: number; avg?: number } | undefined
): ProductDto =>
  ({
    scores: { scores: { DATA_QUALITY: { value, relativ } } },
  }) as ProductDto

describe('data quality verdict', () => {
  it('rates the position in the category, not the raw criteria count', () => {
    // 19 filled criteria but the lowest of its category
    const product = withDataQuality(19, { value: 0, avg: 2.5 })

    expect(getDataQualityOn20(product)).toBe(0)
    expect(getReliabilityVerdict(product).level).toBe('insufficient')
  })

  it('rates a well documented product of its category as reliable', () => {
    // 7 filled criteria, the best of a category with fewer criteria
    const product = withDataQuality(7, { value: 4.5, avg: 2.5 })

    expect(getDataQualityOn20(product)).toBe(18)
    expect(getReliabilityVerdict(product).level).toBe('excellent')
  })

  it('has no verdict without a relative data quality score', () => {
    const product = withDataQuality(12, undefined)

    expect(getDataQualityOn20(product)).toBeNull()
    expect(getReliabilityVerdict(product).score).toBeNull()
    expect(isDataQualityBelowCategoryAverage(product)).toBe(false)
  })
})

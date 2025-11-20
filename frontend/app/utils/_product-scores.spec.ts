import { describe, expect, it } from 'vitest'
import type { ProductDto, ProductScoreDto } from '~~/shared/api-client'
import { resolvePrimaryImpactScore } from './_product-scores'

const buildProduct = (score: Partial<ProductScoreDto>): ProductDto => ({
  scores: {
    scores: {
      ECOSCORE: {
        id: 'ECOSCORE',
        ...score,
      } as ProductScoreDto,
    },
  },
}) as ProductDto

describe('resolvePrimaryImpactScore', () => {
  it('prioritises canonical conversions before fallbacks', () => {
    const product = buildProduct({
      on20: 10,
      percent: 80,
      value: 4.8,
      relativeValue: '3.2',
      relativ: { value: 2 },
    })

    expect(resolvePrimaryImpactScore(product)).toBe(2.5)
  })

  it('handles relativeValue strings as five point scores', () => {
    const product = buildProduct({ relativeValue: '4.4' })

    expect(resolvePrimaryImpactScore(product)).toBe(4.4)
  })

  it('clamps relative scores to the allowed range', () => {
    const product = buildProduct({ relativ: { value: 7.2 } })

    expect(resolvePrimaryImpactScore(product)).toBe(5)
  })
})

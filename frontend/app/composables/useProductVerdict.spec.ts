import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import { useProductVerdict } from './useProductVerdict'
import type {
  ProductDto,
  ProductImpactScoreStatusDto,
} from '~~/shared/api-client'

const build = (
  value: number | null,
  impactScore?: ProductImpactScoreStatusDto
): ProductDto =>
  ({
    scores: {
      scores: value === null ? {} : { ECOSCORE: { value } },
      impactScore,
    },
  }) as unknown as ProductDto

const impact = (product: ProductDto) =>
  useProductVerdict(ref(product)).impactVerdict.value

describe('useProductVerdict impact dimension', () => {
  it('keeps the V1 thresholds on the score', () => {
    expect(
      impact(
        build(15, {
          status: 'RATED',
          currentMethodologyVersion: 1,
          level: null,
        })
      )?.labelKey
    ).toBe('product.verdict.levels.good')
    expect(impact(build(null))?.labelKey).toBe('product.verdict.levels.unrated')
  })

  it('labels a V2 product from its position, not from the score', () => {
    const verdict = impact(
      build(5, {
        status: 'RATED',
        currentMethodologyVersion: 2,
        level: 'excellent',
      })
    )
    expect(verdict?.labelKey).toBe(
      'components.impactScore.v2.positionLevels.excellent'
    )
    expect(verdict?.color).toBe('success')
  })

  it('shows a neutral "not rated" in V2, never "insufficient"', () => {
    const verdict = impact(
      build(null, {
        status: 'NOT_RATED',
        notRatedReason: 'LOW_COVERAGE',
        currentMethodologyVersion: 2,
      })
    )
    expect(verdict?.labelKey).toBe('components.impactScore.v2.notRated')
    expect(verdict?.color).toBe('grey')
    expect(verdict?.score).toBeNull()
  })
})

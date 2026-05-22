import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import type { ProductDto } from '~~/shared/api-client'
import { useProductVigilance } from './useProductVigilance'

describe('useProductVigilance', () => {
  it('identifies end of life warning', () => {
    const product = ref<ProductDto>({
      eprel: {
        onMarketEndDate: '2020-01-01T00:00:00Z',
      },
    } as ProductDto)

    const { isEndOfLife, alerts } = useProductVigilance(product)
    expect(isEndOfLife.value).toBe(true)
    expect(alerts.value.some(a => a.key === 'eol')).toBe(true)
  })

  it('identifies conflicting attributes warning', () => {
    const product = ref<ProductDto>({
      attributes: {
        allAttributes: {
          attr1: {
            sourcing: {
              conflicts: true,
            },
          },
        },
      },
    } as ProductDto)

    const { hasConflictingAttributes, alerts } = useProductVigilance(product)
    expect(hasConflictingAttributes.value).toBe(true)
    expect(alerts.value.some(a => a.key === 'conflicts')).toBe(true)
  })

  it('identifies low data quality warning', () => {
    const product = ref<ProductDto>({
      scores: {
        scores: {
          DATA_QUALITY: {
            value: 2.0,
            relativ: {
              avg: 3.0,
            },
          },
        },
      },
    } as ProductDto)

    const { isLowDataQuality, alerts } = useProductVigilance(product)
    expect(isLowDataQuality.value).toBe(true)
    expect(alerts.value.some(a => a.key === 'quality')).toBe(true)
  })

  it('identifies obsolescence warning', () => {
    const product = ref<ProductDto>({
      aiReview: {
        review: {
          obsolescenceWarning: 'Warning detail',
        },
      },
    } as ProductDto)

    const { hasObsolescenceWarning, alerts } = useProductVigilance(product)
    expect(hasObsolescenceWarning.value).toBe(true)
    expect(alerts.value.some(a => a.key === 'obsolescence')).toBe(true)
  })

  it('identifies low competition warning', () => {
    const product = ref<ProductDto>({
      offers: {
        offersByCondition: {
          NEW: [{ price: 10 }],
        },
      },
    } as ProductDto)

    const { isLowCompetition, alerts } = useProductVigilance(product)
    expect(isLowCompetition.value).toBe(true)
    expect(alerts.value.some(a => a.key === 'competition')).toBe(true)
  })
})

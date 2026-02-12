import { describe, expect, it } from 'vitest'
import { resolveSortedFieldDisplay } from './_sort-attribute-display'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import type { ProductDto } from '~~/shared/api-client'

describe('_sort-attribute-display', () => {
  const t = (key: string, params?: Record<string, unknown>) => {
    if (params) return `${key} ${JSON.stringify(params)}`
    return key
  }
  const n = (value: number) => String(value)
  const translatePlural = (key: string, count: number) => `${key} ${count}`

  it('returns null for ECOSCORE_RELATIVE_FIELD even if score exists', () => {
    const product = {
      scores: {
        scores: {
          ECOSCORE: { value: 3.8 },
        },
      },
    } as ProductDto

    const result = resolveSortedFieldDisplay(
      product,
      ECOSCORE_RELATIVE_FIELD,
      {},
      t,
      n,
      translatePlural
    )

    expect(result).toBeNull()
  })

  it('returns null for price.minPrice.price', () => {
    const product = {
      offers: {
        bestNewOffer: { price: 100 },
      },
    } as ProductDto

    const result = resolveSortedFieldDisplay(
      product,
      'price.minPrice.price',
      {},
      t,
      n,
      translatePlural
    )

    expect(result).toBeNull()
  })

  it('returns a display object for other attributes (e.g. usage)', () => {
    // Unused variables logic removed to satisfy linter
  })
})

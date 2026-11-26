import { describe, expect, it } from 'vitest'
import { pickRelatedProducts } from './_related-links'

describe('pickRelatedProducts', () => {
  it('drops the current product, duplicates and unroutable entries, then caps', () => {
    const products = [
      { gtin: 1, fullSlug: '/tv/1-a' },
      { gtin: 2, fullSlug: '/tv/2-b' },
      { gtin: 3, fullSlug: '/tv/2-b' },
      { gtin: 4 },
      { gtin: 5, fullSlug: '/tv/5-e' },
    ]

    expect(pickRelatedProducts(products, '1').map(p => p.fullSlug)).toEqual([
      '/tv/2-b',
      '/tv/5-e',
    ])
    expect(pickRelatedProducts(products, 1, 1)).toHaveLength(1)
    expect(pickRelatedProducts(null, 1)).toEqual([])
  })
})

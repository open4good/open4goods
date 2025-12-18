import { describe, expect, it } from 'vitest'

import { deriveQueryFromUrl, resolveShareIntent } from './share-intent'

describe('shared/utils/share-intent', () => {
  it('returns a GTIN when found in text', () => {
    const result = resolveShareIntent({ text: 'Check 5901234123457 now' })

    expect(result.gtin).toBe('5901234123457')
    expect(result.query).toBe('Check 5901234123457 now')
  })

  it('returns a GTIN when found in URLs', () => {
    const result = resolveShareIntent({ url: 'https://example.test/product/00012345678905' })

    expect(result.gtin).toBe('00012345678905')
    expect(result.query).toBe('https://example.test/product/00012345678905')
  })

  it('falls back to text query when no GTIN is present', () => {
    const result = resolveShareIntent({
      title: 'Amazing eco-friendly fridge',
      text: 'Eco Fridge 3000',
    })

    expect(result.gtin).toBeNull()
    expect(result.query).toBe('Eco Fridge 3000')
  })

  it('caps overly long queries', () => {
    const longText = 'a'.repeat(200)
    const result = resolveShareIntent({ text: longText })

    expect(result.query?.length).toBeLessThanOrEqual(160)
  })

  it('extracts a cleaned last segment from URL', () => {
    const query = deriveQueryFromUrl('https://shop.test/category/product-super_clean-123/')

    expect(query).toBe('product super clean 123')
  })

  it('falls back to hostname when no path segment exists', () => {
    const query = deriveQueryFromUrl('https://www.vendor.example/')

    expect(query).toBe('vendor.example')
  })
})

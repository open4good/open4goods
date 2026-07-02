import { describe, expect, it } from 'vitest'
import {
  buildAffiliateClickProps,
  normalizeAnalyticsSlug,
  resolveAnalyticsHost,
  resolvePriceBucket,
} from './useAnalytics'

describe('useAnalytics affiliate properties', () => {
  it('normalizes merchant labels into stable slugs', () => {
    expect(normalizeAnalyticsSlug('Fnac / Darty Marketplace')).toBe(
      'fnac-darty-marketplace'
    )
    expect(normalizeAnalyticsSlug('Électro Dépôt')).toBe('electro-depot')
    expect(normalizeAnalyticsSlug('   ')).toBeNull()
  })

  it('extracts destination hosts without keeping full urls', () => {
    expect(
      resolveAnalyticsHost('https://www.example.com/path?token=secret')
    ).toBe('example.com')
    expect(resolveAnalyticsHost('/contrib/abc123')).toBeNull()
    expect(resolveAnalyticsHost('not a url')).toBeNull()
  })

  it('maps offer prices to low-cardinality buckets', () => {
    expect(resolvePriceBucket(12)).toBe('0-25')
    expect(resolvePriceBucket(99.99)).toBe('50-100')
    expect(resolvePriceBucket(1000)).toBe('1000-plus')
    expect(resolvePriceBucket(null)).toBeNull()
  })

  it('builds compact affiliate click props for Plausible custom properties', () => {
    const props = buildAffiliateClickProps({
      token: 'abc123',
      url: 'https://www.merchant.example/products/1?session=private',
      merchantName: 'Merchant Example',
      placement: 'offers-table',
      productId: 1234567890123,
      gtin: 1234567890123,
      vertical: 'televisions',
      categorySlug: 'tv',
      offerRank: 2,
      price: 349.9,
      currency: 'EUR',
      condition: 'NEW',
    })

    expect(props).toEqual({
      token: 'abc123',
      affiliatePlatform: 'unknown',
      merchantName: 'Merchant Example',
      merchantSlug: 'merchant-example',
      placement: 'offers-table',
      productId: 1234567890123,
      gtin: 1234567890123,
      vertical: 'televisions',
      categorySlug: 'tv',
      offerRank: 2,
      priceBucket: '250-500',
      currency: 'EUR',
      condition: 'NEW',
      destinationHost: 'merchant.example',
    })
    expect(props).not.toHaveProperty('url')
  })
})

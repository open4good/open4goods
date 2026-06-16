import { describe, it, expect } from 'vitest'
import { FACET_PRICE, FACETS, buildSampleResponse } from './facets'

describe('FACET_PRICE descriptor', () => {
  it('has the expected id and endpoint', () => {
    expect(FACET_PRICE.id).toBe('product.price')
    expect(FACET_PRICE.endpointPath).toContain('{gtin}')
    expect(FACET_PRICE.credits).toBe(5)
  })

  it('is registered in FACETS', () => {
    expect(FACETS['product.price']).toBe(FACET_PRICE)
  })
})

describe('buildSampleResponse', () => {
  it('returns billable response for fresh-offer GTIN', () => {
    const r = buildSampleResponse(FACET_PRICE, '0885909950805', 2500)
    expect(r.meta.billable).toBe(true)
    expect(r.meta.creditsConsumed).toBe(5)
    expect(r.meta.creditsRemaining).toBe(2495)
    expect(r.meta.facet).toBe('product.price')
    expect(r.meta.reason).toBe('fresh-offer')
    expect(r.data).not.toBeNull()
  })

  it('returns non-billable for stale GTIN', () => {
    const r = buildSampleResponse(FACET_PRICE, '0194253408994', 2500)
    expect(r.meta.billable).toBe(false)
    expect(r.meta.creditsConsumed).toBe(0)
    expect(r.meta.creditsRemaining).toBe(2500)
    expect(r.meta.reason).toBe('stale-data')
    expect(r.data).toBeNull()
  })

  it('returns not-found for unknown GTIN', () => {
    const r = buildSampleResponse(FACET_PRICE, '9999999999999', 100)
    expect(r.meta.billable).toBe(false)
    expect(r.meta.reason).toBe('product-not-found')
  })
})

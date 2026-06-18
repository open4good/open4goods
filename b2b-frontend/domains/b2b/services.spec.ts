import { describe, it, expect } from 'vitest'
import { SERVICES, allServices, featuredServices, getServiceBySlug } from './services'

describe('SERVICES catalog', () => {
  it('contains 13 entries', () => {
    expect(SERVICES).toHaveLength(13)
  })

  it('has product.price as live', () => {
    const price = SERVICES.find(s => s.id === 'product.price')
    expect(price).toBeDefined()
    expect(price?.status).toBe('live')
    expect(price?.credits).toBe(5)
    expect(price?.slug).toBe('price')
  })

  it('has barcode.check as free and live', () => {
    const check = SERVICES.find(s => s.id === 'barcode.check')
    expect(check).toBeDefined()
    expect(check?.status).toBe('live')
    expect(check?.credits).toBe(0)
  })
})

describe('allServices()', () => {
  it('returns all services sorted by order', () => {
    const services = allServices()
    expect(services).toHaveLength(13)
    for (let i = 1; i < services.length; i++) {
      expect(services[i]!.order).toBeGreaterThanOrEqual(services[i - 1]!.order)
    }
  })
})

describe('featuredServices()', () => {
  it('returns only featured services', () => {
    const featured = featuredServices()
    expect(featured.length).toBeGreaterThan(0)
    expect(featured.every(s => s.featured)).toBe(true)
  })

  it('includes product.price', () => {
    expect(featuredServices().some(s => s.id === 'product.price')).toBe(true)
  })
})

describe('getServiceBySlug()', () => {
  it('returns the matching service', () => {
    const s = getServiceBySlug('price')
    expect(s?.id).toBe('product.price')
  })

  it('returns undefined for unknown slug', () => {
    expect(getServiceBySlug('nonexistent')).toBeUndefined()
  })
})

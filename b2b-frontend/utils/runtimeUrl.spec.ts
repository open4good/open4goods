import { describe, it, expect } from 'vitest'
import { resolveRuntimeUrl, resolveServerRuntimeBaseUrl } from './runtimeUrl'

describe('resolveRuntimeUrl', () => {
  it('returns absolute URL with path for absolute base', () => {
    expect(resolveRuntimeUrl('https://api.product-data-api.com', '/api/v1')).toBe(
      'https://api.product-data-api.com/api/v1',
    )
  })

  it('returns relative URL for relative base with slash', () => {
    expect(resolveRuntimeUrl('/', '/api/v1')).toBe('/api/v1')
  })

  it('strips trailing slash from result', () => {
    expect(resolveRuntimeUrl('https://api.product-data-api.com', '')).toBe(
      'https://api.product-data-api.com',
    )
  })

  it('handles empty base as relative root', () => {
    const result = resolveRuntimeUrl('', '')
    expect(result).toBe('/')
  })

  it('returns root for relative base with no path', () => {
    expect(resolveRuntimeUrl('/')).toBe('/')
  })

  it('strips trailing slashes from absolute base', () => {
    expect(resolveRuntimeUrl('https://api.product-data-api.com/', '/price')).toBe(
      'https://api.product-data-api.com/price',
    )
  })
})

describe('resolveServerRuntimeBaseUrl', () => {
  it('returns absolute base unchanged', () => {
    expect(resolveServerRuntimeBaseUrl('https://localhost:8087', 'http://ignored')).toBe(
      'https://localhost:8087',
    )
  })

  it('resolves relative base against origin', () => {
    expect(resolveServerRuntimeBaseUrl('/', 'http://localhost:3000')).toBe('http://localhost:3000')
  })

  it('strips trailing slash from result', () => {
    expect(resolveServerRuntimeBaseUrl('https://api.product-data-api.com/', 'http://ignored')).toBe(
      'https://api.product-data-api.com',
    )
  })
})

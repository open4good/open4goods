import { describe, expect, it, vi } from 'vitest'

import {
  extractGtinFromProductSlug,
  normalizeFullSlug,
  validateCategorySlug,
} from './product-helpers'

const createErrorMock = vi.fn((input: { statusCode: number; statusMessage: string }) =>
  Object.assign(new Error(input.statusMessage), input)
)

vi.mock('nuxt/app', () => ({
  createError: (...args: Parameters<typeof createErrorMock>) => createErrorMock(...args),
}))

describe('product page helpers', () => {
  it('validates category slugs', () => {
    expect(validateCategorySlug('small-appliances')).toBe('small-appliances')
    expect(createErrorMock).not.toHaveBeenCalled()
  })

  it('throws a 404 error for invalid category slugs', () => {
    expect(() => validateCategorySlug('invalid_slug')).toThrow('Page not found')
    expect(createErrorMock).toHaveBeenCalledWith({ statusCode: 404, statusMessage: 'Page not found' })
  })

  it('extracts GTIN from product slugs', () => {
    expect(extractGtinFromProductSlug('123456-toaster')).toBe('123456')
    expect(extractGtinFromProductSlug('789012')).toBe('789012')
  })

  it('throws a 404 error for invalid product slugs', () => {
    expect(() => extractGtinFromProductSlug('abc123')).toThrow('Page not found')
    expect(createErrorMock).toHaveBeenLastCalledWith({ statusCode: 404, statusMessage: 'Page not found' })
  })

  it('normalizes full slugs and preserves nullish values', () => {
    expect(normalizeFullSlug('/kitchen/654321-toaster')).toBe('kitchen/654321-toaster')
    expect(normalizeFullSlug('appliances/123456-toaster')).toBe('appliances/123456-toaster')
    expect(normalizeFullSlug(null)).toBeNull()
    expect(normalizeFullSlug(undefined)).toBeNull()
  })
})

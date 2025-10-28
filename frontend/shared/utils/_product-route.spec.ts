import { describe, expect, it } from 'vitest'

import {
  isBackendNotFoundError,
  matchProductRouteFromSegments,
} from './_product-route'

describe('_product-route', () => {
  describe('matchProductRouteFromSegments', () => {
    it('returns route details when both segments are valid', () => {
      const match = matchProductRouteFromSegments([
        'electronique',
        '1234567890123-super-produit',
      ])

      expect(match).toEqual({
        categorySlug: 'electronique',
        gtin: '1234567890123',
        slug: 'super-produit',
      })
    })

    it('normalises category slug casing', () => {
      const match = matchProductRouteFromSegments([
        'Beaute-Soins',
        '123456-soin-visage',
      ])

      expect(match).toEqual({
        categorySlug: 'beaute-soins',
        gtin: '123456',
        slug: 'soin-visage',
      })
    })

    it('accepts product segments starting with at least five digits', () => {
      const match = matchProductRouteFromSegments([
        'audio',
        '12345-haut-parleur',
      ])

      expect(match).toEqual({
        categorySlug: 'audio',
        gtin: '12345',
        slug: 'haut-parleur',
      })
    })

    it('returns null when the route does not contain exactly two segments', () => {
      expect(matchProductRouteFromSegments([])).toBeNull()
      expect(matchProductRouteFromSegments(['only-one'])).toBeNull()
      expect(
        matchProductRouteFromSegments([
          'too',
          'many',
          'segments',
        ])
      ).toBeNull()
    })

    it('rejects category slugs with invalid characters', () => {
      expect(
        matchProductRouteFromSegments(['electronics123', '123456-valid-slug'])
      ).toBeNull()
      expect(
        matchProductRouteFromSegments(['invalid_slug', '123456-valid-slug'])
      ).toBeNull()
    })

    it('requires the product segment to expose a GTIN and slug separated by a hyphen', () => {
      expect(matchProductRouteFromSegments(['tech', '1234-produit'])).toBeNull()
      expect(matchProductRouteFromSegments(['tech', '12345'])).toBeNull()
      expect(matchProductRouteFromSegments(['tech', '123456'])).toBeNull()
      expect(
        matchProductRouteFromSegments(['tech', '1234567890'])
      ).toBeNull()
    })
  })

  describe('isBackendNotFoundError', () => {
    it('detects a Nitro fetch error with statusCode 404', () => {
      expect(
        isBackendNotFoundError({ statusCode: 404, statusMessage: 'Not Found' })
      ).toBe(true)
    })

    it('detects a Nitro fetch error exposing the status on the response object', () => {
      expect(
        isBackendNotFoundError({ response: { status: 404 } })
      ).toBe(true)
    })

    it('returns false for non-object inputs', () => {
      expect(isBackendNotFoundError(null)).toBe(false)
      expect(isBackendNotFoundError(undefined)).toBe(false)
      expect(isBackendNotFoundError('error')).toBe(false)
    })

    it('returns false for non-404 errors', () => {
      expect(isBackendNotFoundError({ statusCode: 500 })).toBe(false)
      expect(isBackendNotFoundError({ response: { status: 200 } })).toBe(false)
    })
  })
})

import { describe, expect, it } from 'vitest'

import {
  extractGtinParam,
  extractRawGtinParam,
  isValidGtinParam,
  isValidRawGtin,
} from './_gtin'

describe('shared/utils/_gtin', () => {
  describe('isValidGtinParam', () => {
    it('accepts strings with at least six digits', () => {
      expect(isValidGtinParam('123456')).toBe(true)
      expect(isValidGtinParam('9876543210')).toBe(true)
    })

    it('rejects non-strings and short values', () => {
      expect(isValidGtinParam(123456 as unknown as string)).toBe(false)
      expect(isValidGtinParam('12345')).toBe(false)
      expect(isValidGtinParam('12345a')).toBe(false)
      expect(isValidGtinParam('123456a')).toBe(false)
      expect(isValidGtinParam('123456-some-product')).toBe(false)
    })
  })

  describe('isValidRawGtin', () => {
    it('accepts only raw numeric GTINs', () => {
      expect(isValidRawGtin('123456')).toBe(true)
      expect(isValidRawGtin('9876543210')).toBe(true)
    })

    it('rejects non-string or slugged values', () => {
      expect(isValidRawGtin(null)).toBe(false)
      expect(isValidRawGtin('123')).toBe(false)
      expect(isValidRawGtin('123456-with-slug')).toBe(false)
    })
  })

  describe('extractGtinParam', () => {
    it('returns the GTIN from valid strings', () => {
      expect(extractGtinParam('123456')).toBe('123456')
      expect(extractGtinParam('123456-some-product')).toBe('123456')
    })

    it('returns the first GTIN from arrays', () => {
      expect(extractGtinParam(['foo', '654321', '987654'])).toBe('654321')
      expect(extractGtinParam(['foo', '654321-product'])).toBe('654321')
    })

    it('returns null for invalid inputs', () => {
      expect(extractGtinParam(undefined)).toBeNull()
      expect(extractGtinParam('123')).toBeNull()
      expect(extractGtinParam(['abc', '12345'])).toBeNull()
      expect(extractGtinParam('123456a')).toBeNull()
    })
  })

  describe('extractRawGtinParam', () => {
    it('returns the GTIN when it is a raw numeric value', () => {
      expect(extractRawGtinParam('123456')).toBe('123456')
      expect(extractRawGtinParam(['foo', '654321', '987654'])).toBe('654321')
    })

    it('rejects slugged or malformed values', () => {
      expect(extractRawGtinParam('123456-product')).toBeNull()
      expect(extractRawGtinParam(['abc', '12345'])).toBeNull()
      expect(extractRawGtinParam(undefined)).toBeNull()
    })
  })
})

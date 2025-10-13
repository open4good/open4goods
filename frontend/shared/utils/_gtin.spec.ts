import { describe, expect, it } from 'vitest'

import { extractGtinParam, isValidGtinParam } from './_gtin'

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
    })
  })

  describe('extractGtinParam', () => {
    it('returns the GTIN from valid strings', () => {
      expect(extractGtinParam('123456')).toBe('123456')
    })

    it('returns the first GTIN from arrays', () => {
      expect(extractGtinParam(['foo', '654321', '987654'])).toBe('654321')
    })

    it('returns null for invalid inputs', () => {
      expect(extractGtinParam(undefined)).toBeNull()
      expect(extractGtinParam('123')).toBeNull()
      expect(extractGtinParam(['abc', '12345'])).toBeNull()
    })
  })
})

import { describe, expect, it } from 'vitest'

import { extractGtinParam, findGtinInText, isValidGtinParam } from './_gtin'

describe('shared/utils/_gtin', () => {
  describe('isValidGtinParam', () => {
    it('accepts strings with at least six digits', () => {
      expect(isValidGtinParam('123456')).toBe(true)
      expect(isValidGtinParam('9876543210')).toBe(true)
      expect(isValidGtinParam('123456-some-product')).toBe(true)
    })

    it('rejects non-strings and short values', () => {
      expect(isValidGtinParam(123456 as unknown as string)).toBe(false)
      expect(isValidGtinParam('12345')).toBe(false)
      expect(isValidGtinParam('12345a')).toBe(false)
      expect(isValidGtinParam('123456a')).toBe(false)
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

  describe('findGtinInText', () => {
    it('finds GTINs embedded within text', () => {
      expect(findGtinInText('Produit 5901234123457 disponible')).toBe('5901234123457')
      expect(findGtinInText('Code: 01234567. Merci')).toBe('01234567')
    })

    it('ignores numbers shorter than eight digits', () => {
      expect(findGtinInText('id=123456')).toBeNull()
      expect(findGtinInText('test 1234 567')).toBeNull()
    })

    it('returns null for non-string inputs', () => {
      expect(findGtinInText(undefined)).toBeNull()
      expect(findGtinInText(12345678 as unknown as string)).toBeNull()
    })
  })
})

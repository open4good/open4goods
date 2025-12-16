import { describe, expect, it } from 'vitest'
import {
  buildCompareHash,
  buildCompareHashFragment,
  parseCompareHash,
} from './_compare-url'

describe('compare url helpers', () => {
  it('parses hash fragments ignoring case and duplicates', () => {
    expect(parseCompareHash('#123Vs456vs123')).toEqual(['123', '456'])
    expect(parseCompareHash('')).toEqual([])
    expect(parseCompareHash('#not-a-number')).toEqual([])
  })

  it('limits parsed gtins to the compare cap', () => {
    expect(parseCompareHash('#1Vs2Vs3Vs4Vs5')).toEqual(['1', '2', '3', '4'])
  })

  it('builds hash fragments from gtins', () => {
    expect(buildCompareHashFragment(['00123', '456'])).toBe('00123Vs456')
    expect(buildCompareHashFragment(['foo', 'bar'])).toBe('')
  })

  it('wraps hash fragments with # when building hash', () => {
    expect(buildCompareHash(['1', '2'])).toBe('#1Vs2')
    expect(buildCompareHash([])).toBe('')
  })
})

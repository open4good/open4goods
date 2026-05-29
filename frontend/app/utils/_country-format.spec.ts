import { describe, expect, it } from 'vitest'
import { countryFlag, countryName, formatCountryLabel } from './_country-format'

describe('_country-format', () => {
  it('builds a regional-indicator flag from an alpha-2 code', () => {
    expect(countryFlag('FR')).toBe('🇫🇷')
    expect(countryFlag('de')).toBe('🇩🇪')
  })

  it('returns an empty flag for invalid codes', () => {
    expect(countryFlag('')).toBe('')
    expect(countryFlag('FRA')).toBe('')
    expect(countryFlag(null)).toBe('')
  })

  it('localises country names with Intl.DisplayNames', () => {
    // Resolves to a human-readable name, not the raw code.
    expect(countryName('FR', 'en')).toBe('France')
    const germanyFr = countryName('DE', 'fr')
    expect(germanyFr).not.toBe('DE')
    expect(germanyFr.length).toBeGreaterThan(2)
  })

  it('falls back to the upper-cased code for invalid input', () => {
    expect(countryName('ZZZ', 'fr')).toBe('ZZZ')
    expect(countryName('', 'fr')).toBe('')
  })

  it('combines flag and name', () => {
    expect(formatCountryLabel('FR', 'fr')).toBe('🇫🇷 France')
    expect(formatCountryLabel('ZZZ', 'fr')).toBe('ZZZ')
  })
})

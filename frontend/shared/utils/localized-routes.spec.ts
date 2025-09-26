import { describe, expect, it } from 'vitest'

import {
  LOCALIZED_ROUTE_PATHS,
  buildI18nPagesConfig,
  normalizeLocale,
  resolveLocalizedRoutePath,
} from './localized-routes'

describe('localized-routes utilities', () => {
  it('normalizes supported locales', () => {
    expect(normalizeLocale('fr-FR')).toBe('fr-FR')
    expect(normalizeLocale('en-US')).toBe('en-US')
    expect(normalizeLocale('es-ES')).toBe('en-US')
    expect(normalizeLocale(undefined)).toBe('en-US')
  })

  it('resolves localized static paths', () => {
    expect(resolveLocalizedRoutePath('team', 'fr-FR')).toBe('/equipe')
    expect(resolveLocalizedRoutePath('team', 'en-US')).toBe('/team')
    expect(resolveLocalizedRoutePath('impact-score', 'fr-FR')).toBe('/ecoscore')
    expect(resolveLocalizedRoutePath('impact-score', 'en-US')).toBe('/impact-score')
  })


  it('falls back to default paths when no mapping exists', () => {
    expect(resolveLocalizedRoutePath('privacy', 'fr-FR')).toBe('/privacy')
    expect(resolveLocalizedRoutePath('account', 'en-US')).toBe('/account')
  })



  it('builds a compatible i18n pages configuration', () => {
    const config = buildI18nPagesConfig()

    Object.entries(LOCALIZED_ROUTE_PATHS).forEach(([routeName, locales]) => {
      expect(config[routeName]).toEqual(locales)
    })
  })
})

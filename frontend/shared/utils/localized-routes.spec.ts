import { describe, expect, it } from 'vitest'

import {
  LOCALIZED_ROUTE_PATHS,
  LOCALIZED_WIKI_PATHS,
  buildI18nPagesConfig,
  matchLocalizedRouteByPath,
  matchLocalizedWikiRouteByPath,
  normalizeLocale,
  resolveLocalizedRoutePath,
} from './localized-routes'

describe('localized-routes utilities', () => {
  it('normalizes supported locales', () => {
    expect(normalizeLocale('fr-FR')).toBe('fr-FR')
    expect(normalizeLocale('en-US')).toBe('en-US')
    expect(normalizeLocale('es-ES')).toBe('fr-FR')
    expect(normalizeLocale(undefined)).toBe('fr-FR')
  })

  it('resolves localized static paths', () => {
    expect(resolveLocalizedRoutePath('team', 'fr-FR')).toBe('/equipe')
    expect(resolveLocalizedRoutePath('team', 'en-US')).toBe('/team')
    expect(resolveLocalizedRoutePath('legal-notice', 'fr-FR')).toBe(
      '/mentions-legales'
    )
    expect(resolveLocalizedRoutePath('legal-notice', 'en-US')).toBe(
      '/legal-notice'
    )
    expect(resolveLocalizedRoutePath('data-privacy', 'fr-FR')).toBe(
      '/politique-confidentialite'
    )
    expect(resolveLocalizedRoutePath('data-privacy', 'en-US')).toBe(
      '/data-privacy'
    )
  })

  it('matches paths back to their localized routes', () => {
    expect(matchLocalizedRouteByPath('/equipe')).toEqual({
      routeName: 'team',
      locale: 'fr-FR',
    })
    expect(matchLocalizedRouteByPath('/team')).toEqual({
      routeName: 'team',
      locale: 'en-US',
    })
    expect(matchLocalizedRouteByPath('/mentions-legales')).toEqual({
      routeName: 'legal-notice',
      locale: 'fr-FR',
    })
    expect(matchLocalizedRouteByPath('/legal-notice')).toEqual({
      routeName: 'legal-notice',
      locale: 'en-US',
    })
    expect(matchLocalizedRouteByPath('/politique-confidentialite')).toEqual({
      routeName: 'data-privacy',
      locale: 'fr-FR',
    })
    expect(matchLocalizedRouteByPath('/data-privacy')).toEqual({
      routeName: 'data-privacy',
      locale: 'en-US',
    })
    expect(matchLocalizedRouteByPath('/unknown')).toBeNull()
  })

  it('exposes wiki page identifiers for localized CMS routes', () => {
    expect(matchLocalizedWikiRouteByPath('/mentions-legales')).toEqual({
      routeName: 'legal-notice',
      locale: 'fr-FR',
      pageId: LOCALIZED_WIKI_PATHS['legal-notice']['fr-FR'].pageId,
    })
    expect(matchLocalizedWikiRouteByPath('/legal-notice')).toEqual({
      routeName: 'legal-notice',
      locale: 'en-US',
      pageId: LOCALIZED_WIKI_PATHS['legal-notice']['en-US'].pageId,
    })
    expect(matchLocalizedWikiRouteByPath('/politique-confidentialite')).toEqual(
      {
        routeName: 'data-privacy',
        locale: 'fr-FR',
        pageId: LOCALIZED_WIKI_PATHS['data-privacy']['fr-FR'].pageId,
      }
    )
    expect(matchLocalizedWikiRouteByPath('/data-privacy')).toEqual({
      routeName: 'data-privacy',
      locale: 'en-US',
      pageId: LOCALIZED_WIKI_PATHS['data-privacy']['en-US'].pageId,
    })
    expect(matchLocalizedWikiRouteByPath('/team')).toBeNull()
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

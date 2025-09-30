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
    expect(normalizeLocale('es-ES')).toBe('en-US')
    expect(normalizeLocale(undefined)).toBe('en-US')
  })

  it('resolves localized static paths', () => {
    expect(resolveLocalizedRoutePath('team', 'fr-FR')).toBe('/equipe')
    expect(resolveLocalizedRoutePath('team', 'en-US')).toBe('/team')
    expect(resolveLocalizedRoutePath('xwiki-fullpage', 'fr-FR')).toBe('/mentions-legales')
    expect(resolveLocalizedRoutePath('xwiki-fullpage', 'en-US')).toBe('/legal-notice')
  })

  it('matches paths back to their localized routes', () => {
    expect(matchLocalizedRouteByPath('/equipe')).toEqual({ routeName: 'team', locale: 'fr-FR' })
    expect(matchLocalizedRouteByPath('/team')).toEqual({ routeName: 'team', locale: 'en-US' })
    expect(matchLocalizedRouteByPath('/mentions-legales')).toEqual({
      routeName: 'xwiki-fullpage',
      locale: 'fr-FR',
    })
    expect(matchLocalizedRouteByPath('/legal-notice')).toEqual({
      routeName: 'xwiki-fullpage',
      locale: 'en-US',
    })
    expect(matchLocalizedRouteByPath('/unknown')).toBeNull()
  })

  it('exposes wiki page identifiers for localized CMS routes', () => {
    expect(matchLocalizedWikiRouteByPath('/mentions-legales')).toEqual({
      routeName: 'xwiki-fullpage',
      locale: 'fr-FR',
      pageId: LOCALIZED_WIKI_PATHS['xwiki-fullpage']['fr-FR'].pageId,
    })
    expect(matchLocalizedWikiRouteByPath('/legal-notice')).toEqual({
      routeName: 'xwiki-fullpage',
      locale: 'en-US',
      pageId: LOCALIZED_WIKI_PATHS['xwiki-fullpage']['en-US'].pageId,
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

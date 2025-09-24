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
    expect(resolveLocalizedRoutePath('blog', 'fr-FR')).toBe('/notre-blog')
    expect(resolveLocalizedRoutePath('blog', 'en-US')).toBe('/our-blog')
    expect(resolveLocalizedRoutePath('contact', 'fr-FR')).toBe('/contact')
    expect(resolveLocalizedRoutePath('impact-score', 'en-US')).toBe('/impact-score')
    expect(resolveLocalizedRoutePath('ecoscore', 'fr-FR')).toBe('/ecoscore')
  })

  it('resolves localized dynamic paths', () => {
    expect(resolveLocalizedRoutePath('blog-slug', 'fr-FR', { slug: 'article-test' })).toBe(
      '/notre-blog/article-test',
    )
    expect(resolveLocalizedRoutePath('blog-slug', 'en-US', { slug: 'article-test' })).toBe(
      '/our-blog/article-test',
    )
  })

  it('falls back to default paths when no mapping exists', () => {
    expect(resolveLocalizedRoutePath('privacy', 'fr-FR')).toBe('/privacy')
    expect(resolveLocalizedRoutePath('account', 'en-US')).toBe('/account')
  })

  it('throws when required params are missing', () => {
    expect(() => resolveLocalizedRoutePath('blog-slug', 'fr-FR')).toThrowError(
      'Missing parameter "slug"',
    )
  })

  it('builds a compatible i18n pages configuration', () => {
    const config = buildI18nPagesConfig()

    Object.entries(LOCALIZED_ROUTE_PATHS).forEach(([routeName, locales]) => {
      expect(config[routeName]).toEqual(locales)
    })
  })
})

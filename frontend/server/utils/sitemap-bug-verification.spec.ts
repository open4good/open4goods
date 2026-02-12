import { describe, it, expect } from 'vitest'
import { getPublicSitemapUrlsForDomainLanguage } from './sitemap-local-files'

describe('sitemap-local-files bug verification', () => {
  it('does not crash on invalid origin', () => {
    // This should not throw
    const urls = getPublicSitemapUrlsForDomainLanguage('fr', 'invalid-origin')

    // It should return an empty array (filtered out) or handle it gracefully
    // The implementation filters out nulls.
    expect(urls).toEqual([])
  })

  it('works with valid origin', () => {
    const mockConfig = {
      sitemapLocalFiles: {
        fr: ['/tmp/test.xml'],
      },
    }
    // @ts-expect-error - Partial config
    const urls = getPublicSitemapUrlsForDomainLanguage(
      'fr',
      'https://nudger.fr',
      mockConfig
    )
    expect(urls).toContain('https://nudger.fr/sitemap/fr/test.xml')
  })
})

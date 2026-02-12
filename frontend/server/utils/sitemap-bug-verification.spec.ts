
import { describe, it, expect, vi } from 'vitest'

// We need to mock #imports because it is a Nuxt alias
vi.mock('#imports', () => ({
  useRuntimeConfig: () => ({
    sitemapLocalFiles: {
      fr: ['/tmp/test.xml']
    }
  })
}))

describe('sitemap-local-files bug verification', () => {
    it('does not crash on invalid origin', async () => {
        const { getPublicSitemapUrlsForDomainLanguage } = await import('./sitemap-local-files')
        
        // This should not throw
        const urls = getPublicSitemapUrlsForDomainLanguage('fr', 'invalid-origin')
        
        // It should return an empty array (filtered out) or handle it gracefully
        // The implementation filters out nulls.
        expect(urls).toEqual([])
    })

    it('works with valid origin', async () => {
        const { getPublicSitemapUrlsForDomainLanguage } = await import('./sitemap-local-files')
         const urls = getPublicSitemapUrlsForDomainLanguage('fr', 'https://nudger.fr')
         expect(urls).toContain('https://nudger.fr/sitemap/fr/test.xml')
    })
})

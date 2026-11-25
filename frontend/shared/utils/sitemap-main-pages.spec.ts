import { describe, expect, it, vi } from 'vitest'

const loadModule = async () => {
  // Ensure module-level caches are rebuilt for each scenario
  vi.resetModules()

  return import('./sitemap-main-pages')
}

describe('getMainPagePathsForDomainLanguage', () => {
  it('returns localized french paths', async () => {
    const { getMainPagePathsForDomainLanguage } = await loadModule()
    const paths = getMainPagePathsForDomainLanguage('fr')

    expect(paths).toContain('/')
    expect(paths).toContain('/equipe')
    expect(paths).toContain('/partenaires')
    expect(paths).toContain('/mentions-legales')
    expect(paths).toContain('/politique-confidentialite')
    expect(paths).toContain('/classement-et-referencement')
    expect(paths).not.toContain('/team')
    expect(paths).toContain('/impact-score')
    expect(paths).toContain('/impact-score/versions')
    expect(paths).toContain('/opendata/gtin')
    expect(paths).toContain('/opendata/isbn')
    expect(paths).toContain('/opensource')
    expect(paths).not.toContain('/offline')
  })

  it('drops excluded runtime routes even when configured with trailing slashes', async () => {
    const previousStaticRoutes = process.env.NUXT_STATIC_MAIN_PAGE_ROUTES
    process.env.NUXT_STATIC_MAIN_PAGE_ROUTES = JSON.stringify([
      '/offline/',
      '/team',
    ])

    const { getMainPagePathsForDomainLanguage } = await loadModule()
    const paths = getMainPagePathsForDomainLanguage('fr')

    expect(paths).toContain('/equipe')
    expect(paths).not.toContain('/offline')
    expect(paths).not.toContain('/offline/')

    process.env.NUXT_STATIC_MAIN_PAGE_ROUTES = previousStaticRoutes
  })
})

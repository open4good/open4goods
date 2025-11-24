import { describe, expect, it } from 'vitest'

import { getMainPagePathsForDomainLanguage } from './sitemap-main-pages'

describe('getMainPagePathsForDomainLanguage', () => {
  it('returns unique english paths', () => {
    const paths = getMainPagePathsForDomainLanguage('en')
    const unique = new Set(paths)

    expect(paths.length).toBe(unique.size)
    expect(paths).toContain('/')
    expect(paths).toContain('/team')
    expect(paths).toContain('/partners')
    expect(paths).toContain('/legal-notice')
    expect(paths).toContain('/data-privacy')
    expect(paths).toContain('/impact-score')
    expect(paths).toContain('/opendata/gtin')
    expect(paths).toContain('/opendata/isbn')
    expect(paths).toContain('/opensource')
    expect(paths).not.toContain('/offline')
  })

  it('returns localized french paths', () => {
    const paths = getMainPagePathsForDomainLanguage('fr')

    expect(paths).toContain('/')
    expect(paths).toContain('/equipe')
    expect(paths).toContain('/partenaires')
    expect(paths).toContain('/mentions-legales')
    expect(paths).toContain('/politique-confidentialite')
    expect(paths).not.toContain('/team')
    expect(paths).toContain('/impact-score')
    expect(paths).toContain('/opendata/gtin')
    expect(paths).toContain('/opendata/isbn')
    expect(paths).toContain('/opensource')
    expect(paths).not.toContain('/offline')
  })
})

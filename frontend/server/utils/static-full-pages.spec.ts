import { describe, expect, it } from 'vitest'

import { getStaticFullPage } from './static-full-pages'

describe('getStaticFullPage', () => {
  it('returns the real legal-notice page with its metadata', () => {
    const result = getStaticFullPage(
      'webpages:default:legal-notice:WebHome',
      'fr'
    )
    expect(result).not.toBeNull()
    expect(result?.pageTitle).toBe('Les mentions légales et les CGU de Nudger')
    expect(result?.metaTitle).toBe('Mentions légales | Nudger')
    expect(result?.width).toBe('container-semi-fluid')
    expect(result?.htmlContent).toContain(
      '<h2>1. Présentation du site et de l’éditeur</h2>'
    )
  })

  it('carries the Amazon Associates disclosure verbatim in the legal notice (GOU-1396)', () => {
    const result = getStaticFullPage(
      'webpages:default:legal-notice:WebHome',
      'fr'
    )
    expect(result?.htmlContent).toContain(
      'En tant que Partenaire Amazon, je réalise un bénéfice sur les achats remplissant les conditions requises.'
    )
  })

  it('links the affiliate-links paragraph to the ranking and listing page (GOU-1995)', () => {
    const result = getStaticFullPage(
      'webpages:default:legal-notice:WebHome',
      'fr'
    )
    expect(result?.htmlContent).toContain(
      '<a href="https://nudger.fr/classement-et-referencement">Classement et référencement</a>'
    )
  })

  it('identifies the registered publisher for the Google CSS review (GOU-2021)', () => {
    const result = getStaticFullPage(
      'webpages:default:legal-notice:WebHome',
      'fr'
    )
    const html = result?.htmlContent ?? ''
    expect(html).toContain('SIRET : 945 307 593 00018')
    expect(html).toContain('8 Venelle de Keralloche, 29200 Brest, France')
    expect(html).toContain(
      '<a href="mailto:goulven@nudger.fr">goulven@nudger.fr</a>'
    )
    expect(html).not.toContain('immatriculation')
    expect(html).not.toContain('gmail')
  })

  it('names the same registered publisher in the data-privacy page (GOU-2021)', () => {
    const html =
      getStaticFullPage('webpages:default:data-privacy:WebHome', 'fr')
        ?.htmlContent ?? ''
    expect(html).toContain('SIRET : 945 307 593 00018')
    expect(html).not.toContain('immatriculation')
  })

  it('returns the real data-privacy page with its metadata', () => {
    const result = getStaticFullPage(
      'webpages:default:data-privacy:WebHome',
      'fr'
    )
    expect(result).not.toBeNull()
    expect(result?.pageTitle).toBe('Politique de confidentialité')
    expect(result?.width).toBe('container-fluid')
    expect(result?.htmlContent).toContain(
      'Règlement Général sur la Protection des Données'
    )
  })

  it('falls back to French when English has no translation', () => {
    const result = getStaticFullPage(
      'webpages:default:legal-notice:WebHome',
      'en'
    )
    expect(result?.htmlContent).toContain('Présentation du site')
  })

  it('returns null for a page id with no static entry, so callers fall through to the live fetch', () => {
    expect(
      getStaticFullPage('webpages:default:some-unmapped-page:WebHome', 'fr')
    ).toBeNull()
  })
})

import { describe, expect, it } from 'vitest'

import { getStaticFullPage } from './static-full-pages'

describe('getStaticFullPage', () => {
  it('returns the real legal-notice page with its metadata', () => {
    const result = getStaticFullPage('webpages:default:legal-notice:WebHome', 'fr')
    expect(result).not.toBeNull()
    expect(result?.pageTitle).toBe('Les mentions légales et les CGU de Nudger')
    expect(result?.metaTitle).toBe('Mentions légales | Nudger')
    expect(result?.width).toBe('container-semi-fluid')
    expect(result?.editLink).toBeNull()
    expect(result?.htmlContent).toContain('<h2>1. Présentation du site et de l’éditeur</h2>')
  })

  it('returns the real data-privacy page with its metadata', () => {
    const result = getStaticFullPage('webpages:default:data-privacy:WebHome', 'fr')
    expect(result).not.toBeNull()
    expect(result?.pageTitle).toBe('Politique de confidentialité')
    expect(result?.width).toBe('container-fluid')
    expect(result?.htmlContent).toContain('Règlement Général sur la Protection des Données')
  })

  it('falls back to French when English has no translation', () => {
    const result = getStaticFullPage('webpages:default:legal-notice:WebHome', 'en')
    expect(result?.htmlContent).toContain('Présentation du site')
  })

  it('returns null for a page id with no static entry, so callers fall through to the live fetch', () => {
    expect(getStaticFullPage('webpages:default:some-unmapped-page:WebHome', 'fr')).toBeNull()
  })
})

import { describe, expect, it } from 'vitest'
import { buildGuideArticleJsonLd } from './guide-jsonld'

describe('buildGuideArticleJsonLd', () => {
  it('builds an Article authored by the Nudger organization', () => {
    const json = buildGuideArticleJsonLd({
      url: 'https://nudger.fr/lave-vaisselles/meilleur-lave-vaisselle',
      headline: 'Meilleur lave-vaisselle 2026',
      image: 'https://nudger.fr/img.jpg',
      datePublished: '2026-05-01',
      dateModified: '2026-05-31',
      origin: 'https://nudger.fr',
    })

    expect(json['@type']).toBe('Article')
    expect(json.author).toEqual({
      '@type': 'Organization',
      name: 'Nudger',
      url: 'https://nudger.fr',
    })
    expect(json.datePublished).toBe('2026-05-01')
    expect(json.dateModified).toBe('2026-05-31')
    expect(json.image).toEqual(['https://nudger.fr/img.jpg'])
  })

  it('falls back to the modification date for datePublished', () => {
    const json = buildGuideArticleJsonLd({
      url: 'https://nudger.fr/x',
      headline: 'x',
      dateModified: '2026-05-31',
      origin: 'https://nudger.fr',
    })

    expect(json.datePublished).toBe('2026-05-31')
  })
})

import { describe, expect, it } from 'vitest'

import {
  attributedProductSources,
  declaresAttribution,
  indexProductSources,
} from './useProductSources'

describe('useProductSources', () => {
  it('credits only the sources declaring a licence or an attribution', () => {
    const sources = [
      { name: 'fr-indice-durabilite', license: 'Licence Ouverte 2.0' },
      { name: 'rakuten.com', portalUrl: 'https://www.rakuten.com/' },
      { name: 'open-prices', license: ' ', attribution: 'Contributors' },
      { name: 'icecat.biz', license: '  ', attribution: '' },
      { name: ' ', license: 'Licence Ouverte 2.0' },
    ]

    expect(
      attributedProductSources(sources).map(source => source.name)
    ).toEqual(['fr-indice-durabilite', 'open-prices'])
    expect(attributedProductSources(null)).toEqual([])
    expect(declaresAttribution(undefined)).toBe(false)
  })

  it('indexes the sources by normalised name', () => {
    const index = indexProductSources([
      { name: ' Data-Gouv ', license: 'Licence Ouverte 2.0' },
      { name: 'data-gouv', license: 'ignored duplicate' },
      { name: '' },
    ])

    expect(index.size).toBe(1)
    expect(index.get('data-gouv')?.license).toBe('Licence Ouverte 2.0')
  })
})

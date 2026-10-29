import { describe, it, expect } from 'vitest'
import { buildCategoryItemListJsonLd } from './category-jsonld'

describe('category-jsonld', () => {
  const base = {
    name: 'Lave-vaisselles',
    numberOfItems: 42,
    url: 'https://nudger.fr/lave-vaisselles',
  }

  it('emits ListItems with only type, position and absolute url', () => {
    const result = buildCategoryItemListJsonLd({
      ...base,
      productUrls: ['https://nudger.fr/a', null, 'https://nudger.fr/b'],
    })

    expect(result?.itemListElement).toEqual([
      { '@type': 'ListItem', position: 1, url: 'https://nudger.fr/a' },
      { '@type': 'ListItem', position: 2, url: 'https://nudger.fr/b' },
    ])
    const json = JSON.stringify(result)
    expect(json).not.toContain('"Product"')
    expect(json).not.toContain('"Offer"')
    expect(json).not.toContain('"item"')
    expect(result?.numberOfItems).toBe(42)
  })

  it('returns null when no product url is available', () => {
    expect(buildCategoryItemListJsonLd({ ...base, productUrls: [null] })).toBe(
      null
    )
  })
})

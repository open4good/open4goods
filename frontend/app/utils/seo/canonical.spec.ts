import { describe, expect, it } from 'vitest'
import { buildCanonicalUrl } from './canonical'

const origin = 'https://nudger.fr'

describe('buildCanonicalUrl', () => {
  it('drops query string and hash', () => {
    expect(
      buildCanonicalUrl('/blog/mon-article?utm_source=x#top', origin)
    ).toBe('https://nudger.fr/blog/mon-article')
  })

  it('drops the trailing slash but keeps the root', () => {
    expect(buildCanonicalUrl('/impact-score/', origin)).toBe(
      'https://nudger.fr/impact-score'
    )
    expect(buildCanonicalUrl('/?a=1', origin)).toBe('https://nudger.fr/')
    expect(buildCanonicalUrl('', origin)).toBe('https://nudger.fr/')
  })

  it('returns null for an invalid origin', () => {
    expect(buildCanonicalUrl('/x', 'not a url')).toBeNull()
  })
})

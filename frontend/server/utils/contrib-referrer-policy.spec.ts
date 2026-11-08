import { describe, expect, it } from 'vitest'

import { resolveContribReferrerPolicy } from './contrib-referrer-policy'

describe('resolveContribReferrerPolicy', () => {
  it('sends the origin to the Amazon search (GOU-1396)', () => {
    expect(
      resolveContribReferrerPolicy(
        'https://www.amazon.fr/s?k=4006381333931&tag=nudger-21'
      )
    ).toBe('strict-origin-when-cross-origin')
    expect(resolveContribReferrerPolicy('https://amazon.fr/s?k=1')).toBe(
      'strict-origin-when-cross-origin'
    )
  })

  it.each([
    'https://www.awin1.com/cread.php?awinmid=1',
    'https://www.darty.com/p/1',
    'https://www.amazon.com/s?k=1',
    'https://amazon.fr.evil.test/s?k=1',
    'https://notamazon.fr/s?k=1',
    'http://www.amazon.fr/s?k=1',
    'not a url',
    '',
  ])('keeps no-referrer for %s', location => {
    expect(resolveContribReferrerPolicy(location)).toBe('no-referrer')
  })
})

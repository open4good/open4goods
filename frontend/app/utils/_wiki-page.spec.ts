import { describe, expect, it } from 'vitest'

import { normalizeWikiPageId } from './_wiki-page'

describe('normalizeWikiPageId', () => {
  it('returns null for empty values', () => {
    expect(normalizeWikiPageId(null)).toBeNull()
    expect(normalizeWikiPageId(undefined)).toBeNull()
    expect(normalizeWikiPageId('  ')).toBeNull()
  })

  it('returns trimmed relative ids as-is', () => {
    expect(normalizeWikiPageId('verticals/tv/technologies-tv/WebHome')).toBe(
      'verticals/tv/technologies-tv/WebHome',
    )
  })

  it('removes leading slashes', () => {
    expect(normalizeWikiPageId('/verticals/tv/technologies-tv/WebHome')).toBe(
      'verticals/tv/technologies-tv/WebHome',
    )
  })

  it('decodes percent-encoded segments', () => {
    expect(
      normalizeWikiPageId('verticals%2Ftv%2Ftechnologies-tv%2FWebHome'),
    ).toBe('verticals/tv/technologies-tv/WebHome')
  })

  it('strips backend origin prefixes', () => {
    expect(
      normalizeWikiPageId(
        'https://wiki.example/pages/verticals%2Ftv%2Ftechnologies-tv%2FWebHome',
      ),
    ).toBe('verticals/tv/technologies-tv/WebHome')
  })

  it('strips explicit pages prefix', () => {
    expect(
      normalizeWikiPageId('pages/verticals/tv/technologies-tv/WebHome'),
    ).toBe('verticals/tv/technologies-tv/WebHome')
  })
})

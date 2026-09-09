import { describe, expect, it } from 'vitest'

import { getStaticContentBloc } from './static-content-blocs'

describe('getStaticContentBloc', () => {
  it('returns HTML-escaped, paragraph-wrapped content for a known bloc id', () => {
    const result = getStaticContentBloc('pages:team:goulven-furet-title:', 'fr')
    expect(result).toBe('<p>CEO / CTO</p>')
  })

  it('falls back to French when the requested language has no translation', () => {
    const result = getStaticContentBloc('pages:partners:wekey', 'en')
    expect(result).toContain('Wekey accompagne')
  })

  it('returns the real English translation when one was authored', () => {
    const result = getStaticContentBloc('pages:partners:ecotree', 'en')
    expect(result).toContain('EcoTree enables')
  })

  it('returns null for a bloc id with no static entry, so callers fall through to the live fetch', () => {
    expect(getStaticContentBloc('pages:some:unmapped-bloc:', 'fr')).toBeNull()
  })

  it('HTML-escapes a literal ampersand in the source content', () => {
    expect(getStaticContentBloc('pages:team:laurent-blondel-title:', 'fr')).toBe(
      '<p>Développeur &amp; intégrateur frontend</p>'
    )
  })
})

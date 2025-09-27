import { describe, expect, it } from 'vitest'

import { _slugify } from './_slugify'

describe('_slugify', () => {
  it('normalizes accents and punctuation', () => {
    expect(_slugify('Bérangère Leven')).toBe('berangere-leven')
  })

  it('collapses whitespace and symbols into single hyphen', () => {
    expect(_slugify('  Candide   Chérel!! ')).toBe('candide-cherel')
  })

  it('uses fallback when input yields empty slug', () => {
    expect(_slugify('!!!', { fallback: 'member' })).toBe('member')
  })
})

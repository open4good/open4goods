import { describe, expect, it } from 'vitest'

import { normalizeLayout } from '~/composables/cms/useFullPage'

describe('useFullPage layout normalization', () => {
  it('keeps supported layout values', () => {
    expect(normalizeLayout('layout3')).toBe('layout3')
  })

  it('falls back to the default layout for unsupported values', () => {
    expect(normalizeLayout('unsupported')).toBe('layout1')
    expect(normalizeLayout(undefined)).toBe('layout1')
  })
})

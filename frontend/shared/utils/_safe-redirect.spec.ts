import { describe, expect, it } from 'vitest'

import { isSafeRedirectTarget } from './_safe-redirect'

describe('isSafeRedirectTarget', () => {
  it('accepts root-relative paths', () => {
    expect(isSafeRedirectTarget('/account')).toBe(true)
    expect(isSafeRedirectTarget('/')).toBe(true)
  })

  it('rejects absolute URLs', () => {
    expect(isSafeRedirectTarget('https://evil.example/phish')).toBe(false)
    expect(isSafeRedirectTarget('http://evil.example')).toBe(false)
  })

  it('rejects protocol-relative URLs', () => {
    expect(isSafeRedirectTarget('//evil.example')).toBe(false)
  })

  it('rejects non-string values', () => {
    expect(isSafeRedirectTarget(undefined)).toBe(false)
    expect(isSafeRedirectTarget(null)).toBe(false)
    expect(isSafeRedirectTarget(42)).toBe(false)
    expect(isSafeRedirectTarget(['/account'])).toBe(false)
  })
})

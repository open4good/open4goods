import { describe, expect, it } from 'vitest'

import { hasAdminAccess } from './_roles'

describe('hasAdminAccess', () => {
  it('returns false when roles are empty or undefined', () => {
    expect(hasAdminAccess()).toBe(false)
    expect(hasAdminAccess([])).toBe(false)
  })

  it('matches lowercase admin role', () => {
    expect(hasAdminAccess(['admin'])).toBe(true)
  })

  it('matches uppercase admin role', () => {
    expect(hasAdminAccess(['ADMIN'])).toBe(true)
  })

  it('matches prefixed admin role', () => {
    expect(hasAdminAccess(['ROLE_ADMIN'])).toBe(true)
  })

  it('ignores roles without admin privileges', () => {
    expect(hasAdminAccess(['user'])).toBe(false)
    expect(hasAdminAccess(['domain'])).toBe(false)
  })
})

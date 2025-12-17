import { describe, expect, it } from 'vitest'

import { hasAdminAccess } from './_roles'

describe('hasAdminAccess', () => {
  it('returns false when roles are empty or undefined', () => {
    expect(hasAdminAccess()).toBe(false)
    expect(hasAdminAccess([])).toBe(false)
  })

  it('matches configured roles regardless of casing', () => {
    expect(
      hasAdminAccess(['content_editor'], { allowedRoles: ['CONTENT_EDITOR'] })
    ).toBe(true)
  })

  it('allows XWIKIADMINGROUP by default', () => {
    expect(hasAdminAccess(['XWIKIADMINGROUP'])).toBe(true)
    expect(hasAdminAccess(['xwikiadmingroup'])).toBe(true)
  })

  it('ignores roles without admin privileges', () => {
    expect(
      hasAdminAccess(['user'], { allowedRoles: ['ROLE_SITEEDITOR'] })
    ).toBe(false)
    expect(
      hasAdminAccess(['domain'], { allowedRoles: ['ROLE_SITEEDITOR'] })
    ).toBe(false)
  })

  it('rejects users without configured admin roles', () => {
    expect(
      hasAdminAccess(['content-editor'], { allowedRoles: ['ROLE_SITEEDITOR'] })
    ).toBe(false)
  })
})

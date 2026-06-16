import { describe, it, expect } from 'vitest'
import type { AuthSession, AuthUser, AuthOrganization, OrgRole } from './useAuthSession'

describe('AuthSession types', () => {
  it('accepts a fully populated session', () => {
    const user: AuthUser = {
      id: 'user-1',
      email: 'alice@example.com',
      displayName: 'Alice',
      avatarUrl: 'https://example.com/avatar.png',
      platformAdmin: false,
    }
    const org: AuthOrganization = {
      id: 'org-1',
      name: 'Acme',
      slug: 'acme',
      balanceCredits: 2500,
    }
    const role: OrgRole = 'OWNER'

    const session: AuthSession = { user, organization: org, role }

    expect(session.user.platformAdmin).toBe(false)
    expect(session.organization?.balanceCredits).toBe(2500)
    expect(session.role).toBe('OWNER')
  })

  it('accepts a session without an organization (new user before org creation)', () => {
    const user: AuthUser = {
      id: 'user-2',
      email: 'bob@example.com',
      displayName: 'Bob',
      avatarUrl: null,
      platformAdmin: true,
    }
    const session: AuthSession = { user, organization: null, role: null }

    expect(session.user.platformAdmin).toBe(true)
    expect(session.organization).toBeNull()
    expect(session.role).toBeNull()
  })

  it('recognises all valid OrgRole values', () => {
    const roles: OrgRole[] = ['OWNER', 'ADMIN', 'DEVELOPER', 'BILLING']
    expect(roles).toHaveLength(4)
  })
})

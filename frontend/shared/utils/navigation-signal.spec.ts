import { describe, expect, it } from 'vitest'

import {
  buildNavigationSignalCookie,
  NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS,
} from './navigation-signal'

describe('buildNavigationSignalCookie', () => {
  it('builds a first-party Lax cookie, Secure on https', () => {
    expect(buildNavigationSignalCookie(true)).toBe(
      `nudger_nav=1; Path=/; Max-Age=${NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS}; SameSite=Lax; Secure`
    )
    expect(buildNavigationSignalCookie(false)).not.toContain('Secure')
  })
})

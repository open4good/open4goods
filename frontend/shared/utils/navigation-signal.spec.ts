import { describe, expect, it } from 'vitest'

import {
  buildNavigationSignalCookie,
  isSiteReferer,
  NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS,
} from './navigation-signal'

describe('isSiteReferer', () => {
  it.each([
    'https://nudger.fr/fr/produit/123',
    'https://beta.nudger.fr/',
    'https://nudger.com/en/product',
    'https://www.nudger.com/',
    'http://localhost:4100/produit',
    'https://NUDGER.FR/',
  ])('accepts the site referer %s', referer => {
    expect(isSiteReferer(referer, 'nudger.fr')).toBe(true)
  })

  it('accepts a referer from the host serving the request', () => {
    expect(
      isSiteReferer('https://preview.example/p', 'preview.example:443')
    ).toBe(true)
  })

  it.each([
    ['missing', undefined],
    ['empty', ''],
    ['foreign host', 'https://evil.example/nudger.fr'],
    ['look-alike host', 'https://nudger.fr.evil.example/'],
    ['sub-domain not listed', 'https://fake.nudger.fr/'],
    ['not a URL', 'nudger.fr'],
    ['non-http scheme', 'android-app://nudger.fr/'],
  ])('rejects a %s referer', (_name, referer) => {
    expect(isSiteReferer(referer, 'nudger.fr')).toBe(false)
  })

  it('reads the first value of a repeated header', () => {
    expect(
      isSiteReferer(['https://nudger.fr/', 'https://evil.example/'], null)
    ).toBe(true)
  })
})

describe('buildNavigationSignalCookie', () => {
  it('builds a first-party Lax cookie, Secure on https', () => {
    expect(buildNavigationSignalCookie(true)).toBe(
      `nudger_nav=1; Path=/; Max-Age=${NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS}; SameSite=Lax; Secure`
    )
    expect(buildNavigationSignalCookie(false)).not.toContain('Secure')
  })
})

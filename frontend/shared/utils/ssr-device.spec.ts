import { describe, expect, it } from 'vitest'

import { isDesktopRequest } from './ssr-device'

const IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15'
const DESKTOP =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0'

describe('isDesktopRequest', () => {
  it('follows the User-Agent without a Cloudflare device class', () => {
    expect(isDesktopRequest({ userAgent: IPHONE })).toBe(false)
    expect(isDesktopRequest({ userAgent: DESKTOP })).toBe(true)
    expect(isDesktopRequest({})).toBe(true)
  })

  it('prefers CF-Device-Type, the device class of the edge cache key', () => {
    expect(
      isDesktopRequest({ cfDeviceType: 'desktop', userAgent: IPHONE })
    ).toBe(true)
    expect(
      isDesktopRequest({ cfDeviceType: 'mobile', userAgent: DESKTOP })
    ).toBe(false)
    expect(
      isDesktopRequest({ cfDeviceType: 'Tablet', userAgent: DESKTOP })
    ).toBe(false)
  })

  it('ignores an unknown device class', () => {
    expect(isDesktopRequest({ cfDeviceType: 'tv', userAgent: IPHONE })).toBe(
      false
    )
  })
})

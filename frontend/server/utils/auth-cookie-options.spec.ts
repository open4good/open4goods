import type { H3Event } from 'h3'
import { describe, expect, it } from 'vitest'

import { buildAuthCookieOptions } from './auth-cookie-options'

const createEvent = (headers: Record<string, string> = {}, encrypted = false): H3Event => ({
  node: {
    req: {
      headers,
      connection: encrypted ? { encrypted: true } : undefined,
    },
  },
} as unknown as H3Event)

describe('buildAuthCookieOptions', () => {
  it('returns lax cookies for HTTP requests to support local development', () => {
    const options = buildAuthCookieOptions(createEvent())

    expect(options).toMatchObject({
      httpOnly: true,
      secure: false,
      sameSite: 'lax',
      path: '/',
    })
  })

  it('enforces secure cookies when the request is served over HTTPS', () => {
    const options = buildAuthCookieOptions(createEvent({ 'x-forwarded-proto': 'https' }, true))

    expect(options).toMatchObject({
      httpOnly: true,
      secure: true,
      sameSite: 'none',
      path: '/',
    })
  })
})

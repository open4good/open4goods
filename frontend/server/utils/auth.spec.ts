import { createHmac } from 'node:crypto'

import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest'

import { requireAdminAccess, verifyAccessToken } from './auth'

const getCookieMock = vi.hoisted(() => vi.fn())
const createErrorMock = vi.hoisted(() => vi.fn())
const runtimeConfig = vi.hoisted(() => ({
  public: { tokenCookieName: 'access_token' },
  jwtSecret: '',
}))

vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  createError: createErrorMock,
  getCookie: getCookieMock,
}))

vi.mock('#imports', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  createError: createErrorMock,
  getCookie: getCookieMock,
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('nuxt/app', () => ({ useRuntimeConfig: () => runtimeConfig }))
vi.mock('#app/nuxt', () => ({ useRuntimeConfig: () => runtimeConfig }))

const SECRET = 'test-jwt-secret-at-least-32-characters-long'
const COOKIE_NAME = 'access_token'

const base64Url = (value: object | string) =>
  Buffer.from(typeof value === 'string' ? value : JSON.stringify(value)).toString(
    'base64url'
  )

const sign = (
  payload: Record<string, unknown>,
  secret = SECRET,
  header: Record<string, unknown> = { alg: 'HS256', typ: 'JWT' }
) => {
  const headerB64 = base64Url(header)
  const payloadB64 = base64Url(payload)
  const signature = createHmac('sha256', secret)
    .update(`${headerB64}.${payloadB64}`)
    .digest('base64url')
  return `${headerB64}.${payloadB64}.${signature}`
}

const futureExp = () => Math.floor(Date.now() / 1000) + 3600
const pastExp = () => Math.floor(Date.now() / 1000) - 3600

describe('verifyAccessToken', () => {
  it('returns the payload for a validly signed, non-expired token', () => {
    const token = sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() })
    expect(verifyAccessToken(token, SECRET)).toEqual(
      expect.objectContaining({ roles: ['XWIKIADMINGROUP'] })
    )
  })

  it('rejects a token signed with a different secret', () => {
    const token = sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() }, 'wrong-secret')
    expect(verifyAccessToken(token, SECRET)).toBeNull()
  })

  it('rejects a tampered payload', () => {
    const token = sign({ roles: ['USER'], exp: futureExp() })
    const [headerB64, , signatureB64] = token.split('.')
    const tampered = `${headerB64}.${base64Url({ roles: ['XWIKIADMINGROUP'], exp: futureExp() })}.${signatureB64}`
    expect(verifyAccessToken(tampered, SECRET)).toBeNull()
  })

  it('rejects an expired token', () => {
    const token = sign({ roles: ['XWIKIADMINGROUP'], exp: pastExp() })
    expect(verifyAccessToken(token, SECRET)).toBeNull()
  })

  it('rejects a malformed token', () => {
    expect(verifyAccessToken('not-a-jwt', SECRET)).toBeNull()
  })

  it('rejects when no secret is configured', () => {
    const token = sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() })
    expect(verifyAccessToken(token, '')).toBeNull()
  })
})

describe('requireAdminAccess', () => {
  const event = {} as Parameters<typeof requireAdminAccess>[0]

  const originalEnvSecret = process.env.FRONT_SECURITY_JWT_SECRET

  beforeEach(() => {
    getCookieMock.mockReset()
    createErrorMock.mockReset()
    createErrorMock.mockImplementation(
      (input: { statusCode: number; statusMessage: string }) =>
        Object.assign(new Error(input.statusMessage), input)
    )
    runtimeConfig.jwtSecret = ''
    delete process.env.FRONT_SECURITY_JWT_SECRET
  })

  afterEach(() => {
    if (originalEnvSecret === undefined) {
      delete process.env.FRONT_SECURITY_JWT_SECRET
    } else {
      process.env.FRONT_SECURITY_JWT_SECRET = originalEnvSecret
    }
  })

  it('throws 401 when there is no access token cookie', () => {
    getCookieMock.mockReturnValue(undefined)

    expect(() =>
      requireAdminAccess(event, { cookieName: COOKIE_NAME, secret: SECRET })
    ).toThrow(expect.objectContaining({ statusCode: 401 }))
  })

  it('throws 401 when the token signature is invalid', () => {
    getCookieMock.mockReturnValue(sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() }, 'wrong-secret'))

    expect(() =>
      requireAdminAccess(event, { cookieName: COOKIE_NAME, secret: SECRET })
    ).toThrow(expect.objectContaining({ statusCode: 401 }))
  })

  it('throws 403 when the token is valid but the role is not admin', () => {
    getCookieMock.mockReturnValue(sign({ roles: ['USER'], exp: futureExp() }))

    expect(() =>
      requireAdminAccess(event, { cookieName: COOKIE_NAME, secret: SECRET })
    ).toThrow(expect.objectContaining({ statusCode: 403 }))
  })

  it('does not throw when the token is valid and the role is admin', () => {
    getCookieMock.mockReturnValue(sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() }))

    expect(() =>
      requireAdminAccess(event, { cookieName: COOKIE_NAME, secret: SECRET })
    ).not.toThrow()
  })

  it('throws 500 when no secret is configured anywhere (runtime config nor env)', () => {
    getCookieMock.mockReturnValue(sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() }))

    expect(() => requireAdminAccess(event, { cookieName: COOKIE_NAME })).toThrow(
      expect.objectContaining({ statusCode: 500 })
    )
  })

  it('falls back to FRONT_SECURITY_JWT_SECRET when runtime config has no jwtSecret baked in', () => {
    process.env.FRONT_SECURITY_JWT_SECRET = SECRET
    getCookieMock.mockReturnValue(sign({ roles: ['XWIKIADMINGROUP'], exp: futureExp() }))

    expect(() => requireAdminAccess(event, { cookieName: COOKIE_NAME })).not.toThrow()
  })
})

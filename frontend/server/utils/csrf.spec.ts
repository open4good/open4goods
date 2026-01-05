import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'

import { assertCsrfToken, assertSameOrigin, ensureCsrfCookie } from './csrf'
import { CSRF_COOKIE_NAME, CSRF_HEADER_NAME } from '~~/shared/utils/csrf'

const getCookieMock = vi.hoisted(() => vi.fn())
const setCookieMock = vi.hoisted(() => vi.fn())
const getRequestHeaderMock = vi.hoisted(() => vi.fn())
const createErrorMock = vi.hoisted(() => vi.fn())

vi.mock('node:crypto', () => ({
  randomUUID: () => 'csrf-token-123',
  default: {
    randomUUID: () => 'csrf-token-123',
  },
}))

vi.mock('h3', async importOriginal => ({
  ...(await importOriginal<typeof import('h3')>()),
  createError: createErrorMock,
  getCookie: getCookieMock,
  getRequestHeader: getRequestHeaderMock,
  setCookie: setCookieMock,
}))

describe('csrf utilities', () => {
  const event = {} as Parameters<typeof ensureCsrfCookie>[0]
  const originalEnv = process.env.NODE_ENV

  beforeEach(() => {
    getCookieMock.mockReset()
    setCookieMock.mockReset()
    getRequestHeaderMock.mockReset()
    createErrorMock.mockReset()
    process.env.NODE_ENV = 'test'
  })

  afterEach(() => {
    process.env.NODE_ENV = originalEnv
  })

  it('reuses an existing CSRF cookie', () => {
    getCookieMock.mockReturnValue('existing-token')

    const token = ensureCsrfCookie(event)

    expect(token).toBe('existing-token')
    expect(setCookieMock).not.toHaveBeenCalled()
  })

  it('sets a CSRF cookie when missing', () => {
    getCookieMock.mockReturnValue(undefined)

    const token = ensureCsrfCookie(event)

    expect(token).toBe('csrf-token-123')
    expect(setCookieMock).toHaveBeenCalledWith(event, CSRF_COOKIE_NAME, token, {
      httpOnly: false,
      sameSite: 'lax',
      secure: false,
      path: '/',
      maxAge: 60 * 60 * 24,
    })
  })

  it('allows same-origin requests', () => {
    getRequestHeaderMock.mockImplementation((_, name: string) => {
      if (name === 'origin') {
        return 'https://nudger.fr'
      }
      if (name === 'host') {
        return 'nudger.fr'
      }
      return undefined
    })

    expect(() => assertSameOrigin(event)).not.toThrow()
  })

  it('rejects cross-site requests', () => {
    const error = new Error('Cross-site request blocked')
    createErrorMock.mockReturnValue(error)
    getRequestHeaderMock.mockImplementation((_, name: string) => {
      if (name === 'origin') {
        return 'https://evil.example'
      }
      if (name === 'host') {
        return 'nudger.fr'
      }
      return undefined
    })

    expect(() => assertSameOrigin(event)).toThrow(error)
  })

  it('accepts matching CSRF header and cookie', () => {
    getCookieMock.mockReturnValue('token-123')
    getRequestHeaderMock.mockImplementation((_, name: string) => {
      if (name === CSRF_HEADER_NAME) {
        return 'token-123'
      }
      return undefined
    })

    expect(() => assertCsrfToken(event)).not.toThrow()
  })

  it('rejects missing CSRF header', () => {
    const error = new Error('Invalid CSRF token')
    createErrorMock.mockReturnValue(error)
    getCookieMock.mockReturnValue('token-123')
    getRequestHeaderMock.mockReturnValue(undefined)

    expect(() => assertCsrfToken(event)).toThrow(error)
  })
})

import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'

import {
  assertCsrfToken,
  assertSameOrigin,
  ensureCsrfContextToken,
} from './csrf'
import { CSRF_HEADER_NAME } from '~~/shared/utils/csrf'

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
  const event = { context: {} } as unknown as Parameters<
    typeof ensureCsrfContextToken
  >[0]
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

  it('reuses an existing CSRF cookie without setting one', () => {
    getCookieMock.mockReturnValue('existing-token')

    const token = ensureCsrfContextToken(event)

    expect(token).toBe('existing-token')
    expect(setCookieMock).not.toHaveBeenCalled()
    // Token should be stored in event context so SSR plugin can read it
    expect(
      (event as unknown as { context: Record<string, unknown> }).context
        .csrfToken
    ).toBe('existing-token')
  })

  it('never sets a CSRF cookie when missing, so anonymous HTML stays cacheable', () => {
    getCookieMock.mockReturnValue(undefined)

    const token = ensureCsrfContextToken(event)

    expect(token).toBe('csrf-token-123')
    // GOU-2700: a Set-Cookie on an anonymous page keeps the CDN from caching it.
    expect(setCookieMock).not.toHaveBeenCalled()
    // The per-render token lives only in the server context for internal SSR calls
    expect(
      (event as unknown as { context: Record<string, unknown> }).context
        .csrfToken
    ).toBe('csrf-token-123')
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
    expect(createErrorMock).toHaveBeenCalledWith({
      statusCode: 403,
      statusMessage: 'Invalid CSRF token.',
    })
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'

type MiddlewareHandler = (typeof import('./csrf'))['default']
type MockEvent = {
  path: string
  method: string
  context: Record<string, unknown>
}

const ensureCsrfCookieMock = vi.hoisted(() => vi.fn())
const assertSameOriginMock = vi.hoisted(() => vi.fn())
const assertCsrfTokenMock = vi.hoisted(() => vi.fn())
const shouldValidateCsrfMock = vi.hoisted(() => vi.fn())

vi.mock('~~/server/utils/csrf', () => ({
  ensureCsrfCookie: ensureCsrfCookieMock,
  assertSameOrigin: assertSameOriginMock,
  assertCsrfToken: assertCsrfTokenMock,
  shouldValidateCsrf: shouldValidateCsrfMock,
}))

const buildEvent = (path: string, method = 'GET'): MockEvent => ({
  path,
  method,
  context: {},
})

describe('server/middleware/csrf', () => {
  let handler: MiddlewareHandler

  beforeEach(async () => {
    vi.clearAllMocks()
    vi.resetModules()
    vi.stubGlobal('defineEventHandler', (fn: MiddlewareHandler) => fn)
    ensureCsrfCookieMock.mockImplementation((event: MockEvent) => {
      event.context.csrfToken = 'generated-token'
      return 'generated-token'
    })
    shouldValidateCsrfMock.mockReturnValue(true)

    handler = (await import('./csrf')).default
  })

  it('ensures the CSRF cookie/context token even for page (non-/api) requests', () => {
    const event = buildEvent('/smartphones')

    handler(event)

    // Regression guard: page rendering issues internal POSTs to /api/* through the
    // same H3 event, which read event.context.csrfToken. If this token is never
    // populated for the page request itself, those internal calls are sent without
    // a token and fail CSRF validation (403) during SSR.
    expect(ensureCsrfCookieMock).toHaveBeenCalledWith(event)
    expect(event.context.csrfToken).toBe('generated-token')
  })

  it('does not run origin/token validation for page requests', () => {
    const event = buildEvent('/smartphones')

    handler(event)

    expect(assertSameOriginMock).not.toHaveBeenCalled()
    expect(assertCsrfTokenMock).not.toHaveBeenCalled()
  })

  it('validates origin and CSRF token for unsafe /api requests', () => {
    const event = buildEvent('/api/products/search', 'POST')

    handler(event)

    expect(ensureCsrfCookieMock).toHaveBeenCalledWith(event)
    expect(assertSameOriginMock).toHaveBeenCalledWith(event)
    expect(assertCsrfTokenMock).toHaveBeenCalledWith(event)
  })

  it('skips origin/token validation for safe-method /api requests', () => {
    shouldValidateCsrfMock.mockReturnValue(false)
    const event = buildEvent('/api/products/fields/sortable/123', 'GET')

    handler(event)

    expect(assertSameOriginMock).not.toHaveBeenCalled()
    expect(assertCsrfTokenMock).not.toHaveBeenCalled()
  })
})

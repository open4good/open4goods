// @vitest-environment node
// Plain Node environment: no Nuxt app nor ambient Nuxt context exists here,
// as in an SSR caller that has already awaited (GOU-1226).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { createCsrfFetch } from './_csrf-fetch'
import type { CsrfFetchDependencies } from './_csrf-fetch'

const fetchMock = vi.fn()

const lastRequestHeaders = () => {
  const init = fetchMock.mock.calls.at(-1)?.[1] as RequestInit | undefined
  return new Headers(init?.headers)
}

const build = (overrides: Partial<CsrfFetchDependencies> = {}) =>
  createCsrfFetch({
    isServer: true,
    readCookie: () => undefined,
    writeCookie: vi.fn(),
    getServerContext: () => ({ csrfToken: 'server-token' }),
    ...overrides,
  })

describe('createCsrfFetch', () => {
  beforeEach(() => {
    fetchMock.mockImplementation(
      async () =>
        new Response('{}', {
          status: 200,
          headers: { 'content-type': 'application/json' },
        })
    )
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    fetchMock.mockReset()
  })

  it('sends the request token on an SSR POST /api issued after an await', async () => {
    const csrfFetch = build()

    // First call, then a second one once the caller has awaited.
    await csrfFetch('/api/products/search', { method: 'POST', body: {} })
    await new Promise(resolve => setTimeout(resolve, 0))
    await csrfFetch('/api/products/search', { method: 'POST', body: {} })

    expect(fetchMock).toHaveBeenCalledTimes(2)
    const headers = lastRequestHeaders()
    expect(headers.get('x-csrf-token')).toBe('server-token')
    expect(headers.get('cookie')).toBe('csrf_token=server-token')
  })

  it('prefers the incoming cookie over the request context on the server', async () => {
    const csrfFetch = build({ readCookie: () => 'cookie-token' })

    await csrfFetch('/api/products/search', { method: 'POST', body: {} })

    const headers = lastRequestHeaders()
    expect(headers.get('x-csrf-token')).toBe('cookie-token')
    expect(headers.get('cookie')).toBe('csrf_token=cookie-token')
  })

  it('sends no token on the server when none is known', async () => {
    const writeCookie = vi.fn()
    const csrfFetch = build({ getServerContext: () => undefined, writeCookie })

    await csrfFetch('/api/products/search', { method: 'POST', body: {} })

    expect(lastRequestHeaders().has('x-csrf-token')).toBe(false)
    expect(writeCookie).not.toHaveBeenCalled()
  })

  it('leaves safe methods and non-API requests untouched', async () => {
    const getServerContext = vi.fn(() => ({ csrfToken: 'server-token' }))
    const csrfFetch = build({ getServerContext })

    await csrfFetch('/api/products/search')
    expect(lastRequestHeaders().has('x-csrf-token')).toBe(false)

    await csrfFetch('/other', { method: 'POST', body: {} })
    expect(lastRequestHeaders().has('x-csrf-token')).toBe(false)

    expect(getServerContext).not.toHaveBeenCalled()
  })

  it('creates and stores a token in the browser when the cookie is missing', async () => {
    const writeCookie = vi.fn()
    const getServerContext = vi.fn()
    const csrfFetch = build({ isServer: false, writeCookie, getServerContext })

    await csrfFetch('/api/products/search', { method: 'POST', body: {} })

    const headers = lastRequestHeaders()
    const token = headers.get('x-csrf-token')
    expect(token).toBeTruthy()
    expect(writeCookie).toHaveBeenCalledWith(token)
    expect(headers.has('cookie')).toBe(false)
    expect(getServerContext).not.toHaveBeenCalled()
  })
})

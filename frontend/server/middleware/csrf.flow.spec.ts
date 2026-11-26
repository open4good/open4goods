// @vitest-environment node
// End-to-end CSRF flow over a real H3 app (GOU-2700): page GETs issue no
// cookie, while contact/feedback POSTs keep working from the browser and from
// the SSR render.
import { createApp, eventHandler, toWebHandler } from 'h3'
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import { createCsrfFetch } from '~/utils/_csrf-fetch'

type Handler = Parameters<typeof eventHandler>[0]

let webHandler: (request: Request) => Promise<Response>
let pageContext: Record<string, unknown> = {}

beforeAll(async () => {
  vi.stubGlobal('defineEventHandler', (fn: Handler) => eventHandler(fn))
  const csrfMiddleware = (await import('./csrf')).default

  const app = createApp()
  app.use(csrfMiddleware)
  app.use(
    '/api/contact',
    eventHandler(() => ({ success: true }))
  )
  app.use(
    '/api/feedback/vote',
    eventHandler(() => ({ success: true }))
  )
  app.use(
    '/fiche',
    eventHandler(event => {
      pageContext = event.context
      return '<html></html>'
    })
  )
  webHandler = toWebHandler(app)
})

const request = (path: string, init?: RequestInit) =>
  webHandler(new Request(`http://localhost${path}`, init))

describe('CSRF token on demand', () => {
  const forward = (input: RequestInfo | URL, init?: RequestInit) =>
    request(String(input), init)
  const fetchMock = vi.fn(forward)

  beforeEach(() => {
    pageContext = {}
    fetchMock.mockImplementation(forward)
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    fetchMock.mockClear()
    vi.unstubAllGlobals()
    vi.stubGlobal('defineEventHandler', (fn: Handler) => eventHandler(fn))
  })

  it('sets no cookie on an anonymous page GET', async () => {
    const response = await request('/fiche')

    expect(response.status).toBe(200)
    expect(response.headers.get('set-cookie')).toBeNull()
    // The render still gets a token for its internal POSTs.
    expect(typeof pageContext.csrfToken).toBe('string')
  })

  it('sets no cookie on an API GET either', async () => {
    const response = await request('/api/contact')

    expect(response.headers.get('set-cookie')).toBeNull()
  })

  it('accepts a browser contact POST whose token was created on demand', async () => {
    let browserCookie: string | undefined
    const csrfFetch = createCsrfFetch({
      isServer: false,
      readCookie: () => browserCookie,
      writeCookie: token => {
        browserCookie = token
      },
      getServerContext: () => undefined,
    })
    // The browser sends back the cookie it has just written.
    fetchMock.mockImplementation((input, init) => {
      const headers = new Headers(init?.headers)
      if (browserCookie) {
        headers.set('cookie', `csrf_token=${browserCookie}`)
      }
      return request(String(input), { ...init, headers })
    })

    await expect(
      csrfFetch('/api/contact', { method: 'POST', body: { message: 'hi' } })
    ).resolves.toEqual({ success: true })
    await expect(
      csrfFetch('/api/feedback/vote', {
        method: 'POST',
        body: { issueId: '1' },
      })
    ).resolves.toEqual({ success: true })
    expect(browserCookie).toBeTruthy()
  })

  it('accepts an SSR POST signed with the per-render context token', async () => {
    await request('/fiche')
    const csrfFetch = createCsrfFetch({
      isServer: true,
      readCookie: () => undefined,
      writeCookie: vi.fn(),
      getServerContext: () => pageContext,
    })

    await expect(
      csrfFetch('/api/contact', { method: 'POST', body: {} })
    ).resolves.toEqual({ success: true })
  })

  it('still rejects an unsafe call without a matching cookie', async () => {
    const noCookie = await request('/api/contact', {
      method: 'POST',
      headers: { 'x-csrf-token': 'forged' },
    })
    const mismatch = await request('/api/contact', {
      method: 'POST',
      headers: { 'x-csrf-token': 'forged', cookie: 'csrf_token=other' },
    })

    expect(noCookie.status).toBe(403)
    expect(mismatch.status).toBe(403)
  })
})

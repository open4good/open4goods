// @vitest-environment node
import { createApp, eventHandler, toWebHandler } from 'h3'
import { describe, expect, it } from 'vitest'

import {
  applyHtmlCacheHeaders,
  PRIVATE_HTML_CACHE_CONTROL,
  resolveHtmlCacheHeaders,
} from './html-cache-control'
import type { HtmlCacheRequest } from './html-cache-control'

const settings = {
  sMaxAge: 600,
  staleWhileRevalidate: 3600,
  authCookieNames: ['access_token', 'refresh_token'],
}

const anonymous: HtmlCacheRequest = {
  method: 'GET',
  statusCode: 200,
  cookies: {},
  setsCookie: false,
  pageOptOut: false,
  degraded: false,
}

describe('resolveHtmlCacheHeaders', () => {
  it('lets shared caches keep the page of an anonymous visitor', () => {
    expect(resolveHtmlCacheHeaders(anonymous, settings)).toEqual({
      cacheControl:
        'public, max-age=0, s-maxage=600, stale-while-revalidate=3600',
      cdnCacheControl: 'max-age=600, stale-while-revalidate=3600',
      vary: 'Host, X-Forwarded-Host, CF-Device-Type, User-Agent, Cookie',
    })
  })

  it('keeps cookies that do not change the render cacheable', () => {
    const headers = resolveHtmlCacheHeaders(
      {
        ...anonymous,
        cookies: {
          csrf_token: 'abc',
          'open4goods-preferred-theme': 'light',
        },
      },
      settings
    )
    expect(headers.cacheControl).toContain('public')
  })

  it.each([
    ['an access token', { cookies: { access_token: 'jwt' } }],
    ['a refresh token only', { cookies: { refresh_token: 'r' } }],
    ['the dark theme', { cookies: { 'open4goods-preferred-theme': 'dark' } }],
    ['a Hotjar recording', { cookies: { hotjar_record: '1' } }],
    ['a Set-Cookie in the response', { setsCookie: true }],
    ['a page opt-out', { pageOptOut: true }],
    ['a POST', { method: 'POST' }],
  ])('is private, no-store with %s', (_label, override) => {
    expect(
      resolveHtmlCacheHeaders({ ...anonymous, ...override }, settings)
    ).toEqual({ cacheControl: PRIVATE_HTML_CACHE_CONTROL })
  })

  it('never lets an error page be cached', () => {
    expect(
      resolveHtmlCacheHeaders({ ...anonymous, statusCode: 404 }, settings)
    ).toEqual({ cacheControl: 'no-store' })
  })

  it('never lets a page degraded by a failed internal call be cached', () => {
    expect(
      resolveHtmlCacheHeaders({ ...anonymous, degraded: true }, settings)
    ).toEqual({ cacheControl: 'no-store' })
  })
})

const render = async (
  init: RequestInit = {},
  page: { headers?: Record<string, string>; setCookie?: boolean } = {}
) => {
  const app = createApp()
  app.use(
    eventHandler(event => {
      if (page.setCookie) {
        event.node.res.setHeader('set-cookie', 'x=1')
      }
      applyHtmlCacheHeaders(
        event,
        {
          statusCode: 200,
          headers: page.headers ?? {
            'content-type': 'text/html;charset=utf-8',
          },
        },
        settings
      )
      return '<html></html>'
    })
  )
  return toWebHandler(app)(new Request('http://localhost/fiche', init))
}

describe('applyHtmlCacheHeaders', () => {
  it('serves an anonymous product page as public, with no Set-Cookie', async () => {
    const response = await render()

    expect(response.headers.get('set-cookie')).toBeNull()
    expect(response.headers.get('cache-control')).toMatch(
      /^public, .*s-maxage=600/
    )
    expect(response.headers.get('cloudflare-cdn-cache-control')).toBe(
      'max-age=600, stale-while-revalidate=3600'
    )
  })

  it('serves a signed-in page as private, no-store', async () => {
    const response = await render({ headers: { cookie: 'access_token=jwt' } })

    expect(response.headers.get('cache-control')).toBe('private, no-store')
    expect(response.headers.get('cloudflare-cdn-cache-control')).toBeNull()
  })

  it('does not share a page that sets a cookie', async () => {
    const response = await render({}, { setCookie: true })

    expect(response.headers.get('cache-control')).toBe('private, no-store')
  })

  it('leaves non-HTML renders (payloads, islands) untouched', async () => {
    const response = await render(
      {},
      { headers: { 'content-type': 'application/json' } }
    )

    expect(response.headers.get('cache-control')).toBeNull()
  })
})

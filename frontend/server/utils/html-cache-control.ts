import type { H3Event } from 'h3'
import { getResponseHeader, parseCookies, setResponseHeader } from 'h3'

import { THEME_PREFERENCE_KEY } from '~~/shared/constants/theme'
import { HTML_CACHE_DEGRADED_CONTEXT_KEY } from '~~/shared/utils/html-cache'
import { HOTJAR_RECORDING_COOKIE_NAME } from '~~/shared/utils/hotjar-recording'

export const PRIVATE_HTML_CACHE_CONTROL = 'private, no-store'
export const UNCACHEABLE_HTML_CACHE_CONTROL = 'no-store'

/**
 * What the shared HTML of a public page varies on, besides the URL:
 * the domain (locale), the device class (category page layout) and cookies
 * (auth, theme). Cloudflare ignores `Vary`: its cache rule carries the same
 * split (docs/operations/html-edge-cache.md).
 */
export const PUBLIC_HTML_VARY =
  'Host, X-Forwarded-Host, CF-Device-Type, User-Agent, Cookie'

export interface HtmlCacheSettings {
  /** Edge TTL of an anonymous page, in seconds. */
  sMaxAge: number
  /** Window during which the edge may serve a stale page while it revalidates, in seconds. */
  staleWhileRevalidate: number
  /** Cookies that mean a signed-in visitor (access and refresh tokens). */
  authCookieNames: string[]
}

export interface HtmlCacheRequest {
  method: string
  statusCode: number
  cookies: Record<string, string | undefined>
  /** The response already sets a cookie: it belongs to this visitor only. */
  setsCookie: boolean
  /** The page opted out of shared caching (`definePageMeta({ htmlCache: false })`). */
  pageOptOut: boolean
  /** An internal call failed during the render: the HTML may be degraded. */
  degraded: boolean
}

export interface HtmlCacheHeaders {
  cacheControl: string
  /** Cloudflare-only directive (RFC 9213 targeted cache control). */
  cdnCacheControl?: string
  vary?: string
}

const CACHEABLE_METHODS = new Set(['GET', 'HEAD'])

/**
 * Cookies that change the server-rendered HTML for a visitor without an
 * account: the dark theme (light is the default render) and the Hotjar
 * recording chip.
 */
const hasPersonalisedRenderCookie = (cookies: HtmlCacheRequest['cookies']) =>
  cookies[THEME_PREFERENCE_KEY] === 'dark' ||
  Boolean(cookies[HOTJAR_RECORDING_COOKIE_NAME])

/**
 * Cache policy of a server-rendered page (GOU-2700): shared caching for
 * anonymous visitors only, `private, no-store` as soon as the HTML may belong
 * to one visitor.
 */
export const resolveHtmlCacheHeaders = (
  request: HtmlCacheRequest,
  settings: HtmlCacheSettings
): HtmlCacheHeaders => {
  const isSignedIn = settings.authCookieNames.some(name =>
    Boolean(request.cookies[name])
  )

  if (
    !CACHEABLE_METHODS.has(request.method.toUpperCase()) ||
    isSignedIn ||
    request.setsCookie ||
    request.pageOptOut ||
    hasPersonalisedRenderCookie(request.cookies)
  ) {
    return { cacheControl: PRIVATE_HTML_CACHE_CONTROL }
  }

  if (request.statusCode !== 200 || request.degraded) {
    return { cacheControl: UNCACHEABLE_HTML_CACHE_CONTROL }
  }

  const { sMaxAge, staleWhileRevalidate } = settings

  return {
    // Browsers always revalidate (max-age=0), so a visitor who signs in never
    // reuses the anonymous page from the browser cache.
    cacheControl: `public, max-age=0, s-maxage=${sMaxAge}, stale-while-revalidate=${staleWhileRevalidate}`,
    // Cloudflare disables stale-while-revalidate when s-maxage is present: its
    // own header carries the same TTL without s-maxage.
    cdnCacheControl: `max-age=${sMaxAge}, stale-while-revalidate=${staleWhileRevalidate}`,
    vary: PUBLIC_HTML_VARY,
  }
}

const isHtmlResponse = (headers: Record<string, unknown> | undefined) => {
  const contentType = Object.entries(headers ?? {}).find(
    ([name]) => name.toLowerCase() === 'content-type'
  )?.[1]
  return typeof contentType === 'string' && contentType.includes('text/html')
}

export interface RenderedPage {
  statusCode?: number
  headers?: Record<string, unknown>
}

/** Applies the cache policy to a rendered HTML page; other renders are left untouched. */
export const applyHtmlCacheHeaders = (
  event: H3Event,
  response: RenderedPage,
  settings: HtmlCacheSettings
) => {
  if (!isHtmlResponse(response.headers)) {
    return
  }

  const responseSetCookie = Object.keys(response.headers ?? {}).some(
    name => name.toLowerCase() === 'set-cookie'
  )
  const eventSetCookie = getResponseHeader(event, 'set-cookie')

  const headers = resolveHtmlCacheHeaders(
    {
      method: event.method,
      statusCode: response.statusCode ?? event.node.res.statusCode ?? 200,
      cookies: parseCookies(event),
      setsCookie:
        responseSetCookie ||
        (Array.isArray(eventSetCookie)
          ? eventSetCookie.length > 0
          : Boolean(eventSetCookie)),
      pageOptOut: event.context.htmlCachePrivate === true,
      degraded: event.context[HTML_CACHE_DEGRADED_CONTEXT_KEY] === true,
    },
    settings
  )

  setResponseHeader(event, 'Cache-Control', headers.cacheControl)
  if (headers.cdnCacheControl) {
    setResponseHeader(
      event,
      'Cloudflare-CDN-Cache-Control',
      headers.cdnCacheControl
    )
  }
  if (headers.vary) {
    setResponseHeader(event, 'Vary', headers.vary)
  }
}

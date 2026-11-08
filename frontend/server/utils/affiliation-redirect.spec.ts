import { describe, expect, it, vi } from 'vitest'
import type { H3Event } from 'h3'

import {
  MISSING_NAVIGATION_SIGNAL_MESSAGE,
  resolveAffiliationContent,
  resolveAffiliationOrigin,
  resolveAffiliationRedirect,
  sendContribPlainError,
} from './affiliation-redirect'

const eventWith = (headers: Record<string, string | string[]>) =>
  ({ node: { req: { headers } } }) as unknown as H3Event

describe('resolveAffiliationOrigin', () => {
  it('returns the path only when the Referer has the request host', () => {
    expect(
      resolveAffiliationOrigin(
        eventWith({
          host: 'nudger.fr',
          referer: 'https://nudger.fr/fours/mon-four?a=1#x',
        })
      )
    ).toBe('/fours/mon-four')
  })

  it('prefers x-forwarded-host over host', () => {
    expect(
      resolveAffiliationOrigin(
        eventWith({
          host: 'internal:3000',
          'x-forwarded-host': 'nudger.fr',
          referer: 'https://nudger.fr/fours/a',
        })
      )
    ).toBe('/fours/a')
  })

  it('truncates the path to 200 characters', () => {
    const path = `/${'a'.repeat(300)}`
    const origin = resolveAffiliationOrigin(
      eventWith({ host: 'nudger.fr', referer: `https://nudger.fr${path}` })
    )
    expect(origin).toHaveLength(200)
  })

  it.each([
    ['other host', { host: 'nudger.fr', referer: 'https://evil.example/x' }],
    ['absent', { host: 'nudger.fr' }],
    ['invalid', { host: 'nudger.fr', referer: 'not a url' }],
  ])('returns nothing for %s referer', (_label, headers) => {
    expect(resolveAffiliationOrigin(eventWith(headers))).toBeUndefined()
  })
})

describe('sendContribPlainError', () => {
  const run = async (error: unknown) => {
    const headers: Record<string, string> = {}
    const res = {
      statusCode: 0,
      setHeader: (k: string, v: string) => {
        headers[k.toLowerCase()] = v
      },
      getHeader: (k: string) => headers[k.toLowerCase()],
      end: (b: string) => {
        res.body = b
      },
      body: '',
    }
    const event = { node: { req: { headers: {} }, res } } as unknown as H3Event
    await sendContribPlainError(event, error)
    return { res, headers }
  }

  it('explains the missing navigation signal in plain text', async () => {
    const { res, headers } = await run({
      statusCode: 403,
      statusMessage: MISSING_NAVIGATION_SIGNAL_MESSAGE,
    })

    expect(res.statusCode).toBe(403)
    expect(res.body).toBe(`${MISSING_NAVIGATION_SIGNAL_MESSAGE}.`)
    expect(headers['content-type']).toContain('text/plain')
    expect(headers['cache-control']).toBe('no-store')
    expect(headers.location).toBeUndefined()
  })

  it('keeps Forbidden for robot and empty user agent refusals', async () => {
    const { res } = await run({ statusCode: 403, statusMessage: 'Forbidden' })

    expect(res.body).toBe('Forbidden')
  })
})

describe('resolveAffiliationContent', () => {
  const withUrl = (url: string) =>
    ({ node: { req: { url, headers: {} } }, path: url }) as unknown as H3Event

  it('accepts an article or guide path', () => {
    expect(
      resolveAffiliationContent(withUrl('/contrib/tok?from=%2Fblog%2Fmon-slug'))
    ).toBe('/blog/mon-slug')
    expect(
      resolveAffiliationContent(withUrl('/contrib/tok?from=/guides/a/b'))
    ).toBe('/guides/a/b')
  })

  it.each([
    ['other section', '/fours/x'],
    ['absolute url', 'https://nudger.fr/blog/x'],
    ['protocol-relative', '//blog/x'],
    ['too long', `/blog/${'a'.repeat(200)}`],
    ['query', '/blog/x?y=1'],
    ['fragment', '/blog/x#y'],
    ['space', '/blog/x y'],
    ['index only', '/blog/'],
  ])('ignores %s', (_label, value) => {
    expect(
      resolveAffiliationContent(
        withUrl(`/contrib/tok?from=${encodeURIComponent(value)}`)
      )
    ).toBeUndefined()
  })

  it('ignores a missing from', () => {
    expect(resolveAffiliationContent(withUrl('/contrib/tok'))).toBeUndefined()
  })
})

describe('resolveAffiliationRedirect guards with from', () => {
  it('still refuses robots even with a valid from', async () => {
    const event = {
      node: {
        req: {
          url: '/contrib/tok?from=%2Fblog%2Fx',
          method: 'GET',
          headers: { host: 'nudger.fr', 'user-agent': 'Googlebot/2.1' },
        },
      },
      path: '/contrib/tok?from=%2Fblog%2Fx',
      context: { params: { token: 'tok' } },
    } as unknown as H3Event
    await expect(resolveAffiliationRedirect(event)).rejects.toMatchObject({
      statusCode: 403,
    })
  })

  it('still refuses a missing navigation cookie even with a valid from', async () => {
    vi.stubGlobal('useRuntimeConfig', () => ({}))
    const event = {
      node: {
        req: {
          url: '/contrib/tok?from=%2Fblog%2Fx',
          method: 'GET',
          headers: {
            host: 'nudger.fr',
            'user-agent': 'Mozilla/5.0 (X11; Linux x86_64) Firefox/130.0',
          },
        },
      },
      path: '/contrib/tok?from=%2Fblog%2Fx',
      context: { params: { token: 'tok' } },
    } as unknown as H3Event
    await expect(resolveAffiliationRedirect(event)).rejects.toMatchObject({
      statusCode: 403,
      statusMessage: MISSING_NAVIGATION_SIGNAL_MESSAGE,
    })
    vi.unstubAllGlobals()
  })
})

import { describe, expect, it } from 'vitest'
import type { H3Event } from 'h3'

import {
  MISSING_NAVIGATION_SIGNAL_MESSAGE,
  resolveAffiliationOrigin,
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

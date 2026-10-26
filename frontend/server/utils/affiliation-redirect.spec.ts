import { describe, expect, it } from 'vitest'
import type { H3Event } from 'h3'

import { resolveAffiliationOrigin } from './affiliation-redirect'

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

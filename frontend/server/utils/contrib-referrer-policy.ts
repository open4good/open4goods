import { setHeader } from 'h3'
import type { H3Event } from 'h3'

/**
 * Referrer-Policy of the /contrib/ redirect, chosen by target host.
 *
 * Merchants and networks get no Referer (GOU-1184). Amazon is the only
 * exception: the Associates agreement (§6(v)) forbids hiding the site the
 * click comes from, so the followed redirect sends the origin only
 * (`https://nudger.fr/`), never the page path (GOU-1396).
 */
export const CONTRIB_DEFAULT_REFERRER_POLICY = 'no-referrer'
export const CONTRIB_AMAZON_REFERRER_POLICY = 'strict-origin-when-cross-origin'

/** Hosts that receive the origin: amazon.fr and its sub-domains. */
const ORIGIN_REFERRER_HOSTS = ['amazon.fr']

const isOriginReferrerHost = (host: string): boolean =>
  ORIGIN_REFERRER_HOSTS.some(
    allowed => host === allowed || host.endsWith(`.${allowed}`)
  )

/**
 * @param location redirect target returned by front-api
 * @returns `strict-origin-when-cross-origin` for an https amazon.fr target,
 * `no-referrer` otherwise (including an unreadable URL)
 */
export const resolveContribReferrerPolicy = (location: string): string => {
  try {
    const url = new URL(location)
    if (
      url.protocol === 'https:' &&
      isOriginReferrerHost(url.hostname.toLowerCase())
    ) {
      return CONTRIB_AMAZON_REFERRER_POLICY
    }
  } catch {
    // Unreadable target: keep the default.
  }

  return CONTRIB_DEFAULT_REFERRER_POLICY
}

/**
 * Headers of a successful /contrib/ resolution, shared by the page route and
 * /api/contrib/: never cached nor indexed, Referer policy chosen by target.
 */
export const setContribRedirectHeaders = (
  event: H3Event,
  location: string
): void => {
  setHeader(event, 'X-Robots-Tag', 'noindex, nofollow')
  setHeader(event, 'Referrer-Policy', resolveContribReferrerPolicy(location))
  setHeader(event, 'Cache-Control', 'no-store')
}

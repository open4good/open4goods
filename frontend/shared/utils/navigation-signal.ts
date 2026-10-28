import { HOST_DOMAIN_LANGUAGE_MAP, normalizeHost } from './domain-language'

/**
 * First-party technical cookie proving that a page of the site was loaded in
 * this browser before an affiliate link (/contrib/<token>) is followed
 * (GOU-1184). It carries no personal data (constant value) and only serves
 * fraud prevention on affiliate redirects, so it is exempt from consent.
 */
export const NAVIGATION_SIGNAL_COOKIE_NAME = 'nudger_nav'
export const NAVIGATION_SIGNAL_COOKIE_VALUE = '1'
export const NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60

/**
 * Builds the `document.cookie` assignment written by the client plugin.
 */
export const buildNavigationSignalCookie = (secure: boolean): string =>
  [
    `${NAVIGATION_SIGNAL_COOKIE_NAME}=${NAVIGATION_SIGNAL_COOKIE_VALUE}`,
    'Path=/',
    `Max-Age=${NAVIGATION_SIGNAL_COOKIE_MAX_AGE_SECONDS}`,
    'SameSite=Lax',
    ...(secure ? ['Secure'] : []),
  ].join('; ')

/**
 * Whether a raw `Referer` header points to a page of the site: one of the
 * hosts known by `resolveDomainLanguage`, or the host serving the request.
 */
export const isSiteReferer = (
  rawReferer: string | string[] | undefined,
  requestHost?: string | string[] | null
): boolean => {
  const referer = Array.isArray(rawReferer) ? rawReferer[0] : rawReferer

  if (!referer) {
    return false
  }

  let refererHost: string

  try {
    const url = new URL(referer)

    if (url.protocol !== 'https:' && url.protocol !== 'http:') {
      return false
    }

    refererHost = url.hostname.toLowerCase()
  } catch {
    return false
  }

  if (Object.hasOwn(HOST_DOMAIN_LANGUAGE_MAP, refererHost)) {
    return true
  }

  return refererHost === normalizeHost(requestHost)
}

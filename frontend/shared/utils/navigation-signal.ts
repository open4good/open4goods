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

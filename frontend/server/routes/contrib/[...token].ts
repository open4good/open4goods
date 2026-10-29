import { defineEventHandler, sendRedirect, setHeader } from 'h3'

import {
  resolveAffiliationRedirect,
  sendContribPlainError,
} from '../../utils/affiliation-redirect'
import { resolveContribReferrerPolicy } from '../../utils/contrib-referrer-policy'

// Catch-all so a token whose "/" was decoded (/contrib/a/b) is still served here
// and never falls through to the HTML catch-all page.
export default defineEventHandler(async event => {
  setHeader(event, 'X-Robots-Tag', 'noindex, nofollow')

  try {
    const redirect = await resolveAffiliationRedirect(event)

    // The visitor's Referer reaches nudger.fr only: the followed redirect to the
    // network or merchant carries none, whatever policy the link used (GOU-1184).
    // Amazon gets the origin only (Associates agreement §6(v), GOU-1396).
    setHeader(
      event,
      'Referrer-Policy',
      resolveContribReferrerPolicy(redirect.location)
    )
    setHeader(event, 'Cache-Control', 'no-store')

    return sendRedirect(event, redirect.location, redirect.statusCode)
  } catch (error) {
    return sendContribPlainError(event, error)
  }
})

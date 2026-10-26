import { defineEventHandler, sendRedirect, setHeader } from 'h3'

import {
  resolveAffiliationRedirect,
  sendContribPlainError,
} from '../../utils/affiliation-redirect'

// Catch-all so a token whose "/" was decoded (/contrib/a/b) is still served here
// and never falls through to the HTML catch-all page.
export default defineEventHandler(async event => {
  setHeader(event, 'X-Robots-Tag', 'noindex, nofollow')

  try {
    const redirect = await resolveAffiliationRedirect(event)

    return sendRedirect(event, redirect.location, redirect.statusCode)
  } catch (error) {
    return sendContribPlainError(event, error)
  }
})

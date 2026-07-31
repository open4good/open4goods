import { defineEventHandler, sendRedirect, setHeader } from 'h3'

import { resolveAffiliationRedirect } from '../../utils/affiliation-redirect'

export default defineEventHandler(async event => {
  const redirect = await resolveAffiliationRedirect(event)

  setHeader(event, 'X-Robots-Tag', 'noindex, nofollow')

  return sendRedirect(event, redirect.location, redirect.statusCode)
})

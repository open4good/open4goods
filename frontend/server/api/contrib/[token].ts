import { defineEventHandler } from 'h3'

import {
  resolveAffiliationRedirect,
  sendContribPlainError,
} from '../../utils/affiliation-redirect'
import { setContribRedirectHeaders } from '../../utils/contrib-referrer-policy'

export default defineEventHandler(async event => {
  try {
    const redirect = await resolveAffiliationRedirect(event)
    setContribRedirectHeaders(event, redirect.location)
    return redirect
  } catch (error) {
    return sendContribPlainError(event, error)
  }
})

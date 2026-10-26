import { defineEventHandler } from 'h3'

import {
  resolveAffiliationRedirect,
  sendContribPlainError,
} from '../../utils/affiliation-redirect'

export default defineEventHandler(async event => {
  try {
    return await resolveAffiliationRedirect(event)
  } catch (error) {
    return sendContribPlainError(event, error)
  }
})

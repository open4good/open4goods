import { defineEventHandler, getRequestURL } from 'h3'

import {
  assertCsrfToken,
  assertSameOrigin,
  ensureCsrfCookie,
  shouldValidateCsrf,
} from '~~/server/utils/csrf'

const API_PREFIX = '/api'

export default defineEventHandler(event => {
  const { pathname } = getRequestURL(event)

  // Fix SWR handler error: prevent setting cookies on assistant-configs endpoint
  // which is cached via SWR and conflicts with late header modifications
  if (!pathname.includes('/assistant-configs')) {
    ensureCsrfCookie(event)
  }
  if (!pathname.startsWith(API_PREFIX)) {
    return
  }

  if (!shouldValidateCsrf(event)) {
    return
  }

  assertSameOrigin(event)
  assertCsrfToken(event)
})

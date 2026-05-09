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

  if (!pathname.startsWith(API_PREFIX)) {
    return
  }

  if (!shouldValidateCsrf(event)) {
    return
  }

  ensureCsrfCookie(event)
  assertSameOrigin(event)
  assertCsrfToken(event)
})

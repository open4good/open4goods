import {
  assertCsrfToken,
  assertSameOrigin,
  ensureCsrfCookie,
  shouldValidateCsrf,
} from '~~/server/utils/csrf'

const API_PREFIX = '/api'

export default defineEventHandler(event => {
  const pathname = event.path ?? event.node.req.url ?? '/'

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

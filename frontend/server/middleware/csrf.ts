import {
  assertCsrfToken,
  assertSameOrigin,
  ensureCsrfCookie,
  shouldValidateCsrf,
} from '~~/server/utils/csrf'

const API_PREFIX = '/api'

export default defineEventHandler(event => {
  // Always ensure the cookie/context token exists, even for page (non-/api) requests.
  // SSR page rendering issues internal POSTs to /api/* (e.g. product search) through
  // the same H3 event; those calls read event.context.csrfToken. If we only ran this
  // for /api requests, that context would never be populated during page rendering
  // and the internal POST would be sent without a token, failing CSRF validation.
  ensureCsrfCookie(event)

  const pathname = event.path ?? event.node.req.url ?? '/'

  if (!pathname.startsWith(API_PREFIX)) {
    return
  }

  if (!shouldValidateCsrf(event)) {
    return
  }

  assertSameOrigin(event)
  assertCsrfToken(event)
})

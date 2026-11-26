import {
  assertCsrfToken,
  assertSameOrigin,
  ensureCsrfContextToken,
  shouldValidateCsrf,
} from '~~/server/utils/csrf'

const API_PREFIX = '/api'

export default defineEventHandler(event => {
  const pathname = event.path ?? event.node.req.url ?? '/'

  if (!pathname.startsWith(API_PREFIX)) {
    // SSR page rendering issues internal POSTs to /api/* (e.g. product search)
    // that read event.context.csrfToken. The token stays in the server context:
    // no cookie is set, so anonymous HTML remains cacheable (GOU-2700).
    ensureCsrfContextToken(event)
    return
  }

  if (!shouldValidateCsrf(event)) {
    return
  }

  assertSameOrigin(event)
  assertCsrfToken(event)
})

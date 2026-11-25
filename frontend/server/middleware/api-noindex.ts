import { setResponseHeader } from 'h3'

const API_PREFIX = '/api/'

/** JSON endpoints must never end up in a search index (GOU-2696). */
export default defineEventHandler(event => {
  const pathname = event.path ?? event.node.req.url ?? '/'

  if (pathname === '/api' || pathname.startsWith(API_PREFIX)) {
    setResponseHeader(event, 'X-Robots-Tag', 'noindex')
  }
})

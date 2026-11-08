import { requireAdminAccess } from '~~/server/utils/auth'

const METRIKS_PREFIX = '/api/metriks'

export default defineEventHandler(event => {
  const pathname = event.path ?? event.node.req.url ?? '/'

  if (!pathname.startsWith(METRIKS_PREFIX)) {
    return
  }

  requireAdminAccess(event)
})

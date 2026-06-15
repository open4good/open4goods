/**
 * Resolves runtime API bases that may be absolute in production or relative in dev-proxy mode.
 */
export function resolveRuntimeUrl(baseUrl: string, path = ''): string {
  const normalizedBase = baseUrl.replace(/\/+$/, '') || '/'
  const normalizedPath = path ? `/${path.replace(/^\/+/, '')}` : ''

  if (normalizedBase.startsWith('/')) {
    return `${normalizedBase === '/' ? '' : normalizedBase}${normalizedPath || '/'}`
  }

  return new URL(normalizedPath || '/', normalizedBase).toString().replace(/\/+$/, '')
}

/**
 * Converts a browser-relative runtime base into an absolute URL for server-side fetch clients.
 */
export function resolveServerRuntimeBaseUrl(baseUrl: string, origin: string): string {
  const normalizedBase = baseUrl.replace(/\/+$/, '') || '/'

  if (normalizedBase.startsWith('/')) {
    return new URL(normalizedBase, origin).toString().replace(/\/+$/, '')
  }

  return normalizedBase
}

export default defineNuxtPlugin(() => {
  const globalObj = globalThis as typeof globalThis & {
    __apiTokenInstalled?: boolean
  }
  if (globalObj.__apiTokenInstalled) return
  globalObj.__apiTokenInstalled = true

  const config = useRuntimeConfig()
  const apiUrl = config.apiUrl
  const token = config.machineToken
  const originalFetch = globalObj.fetch
  const toUrl = (input: unknown): string => {
    if (typeof input === 'string') return input
    if (input instanceof URL) return input.toString()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (input as any)?.url ?? ''
  }
  const withToken = (init: RequestInit = {}): RequestInit => {
    const headers = new Headers(init.headers ?? {})
    headers.set('X-Shared-Token', token)
    return { ...init, headers }
  }
  globalObj.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = toUrl(input)

    // Allow internal relative URLs to be fetched serverside by resolving them to localhost
    if (url.startsWith('/')) {
      try {
        const { origin } = useRequestURL()
        const absoluteUrl = `${origin}${url}`
        return originalFetch(absoluteUrl, init)
      } catch {
        // If context is unavailable (e.g. call outside of request), fall back to localhost
        // This prevents the "Nuxt instance" error and "Invalid URL" error
        const port = process.env.PORT || process.env.NITRO_PORT || '3000'
        const host = process.env.HOST || 'localhost'
        const absoluteUrl = `http://${host}:${port}${url}`
        return originalFetch(absoluteUrl, init)
      }
    }

    const opts = token && url.startsWith(apiUrl) ? withToken(init) : init
    return originalFetch(input, opts)
  }
})

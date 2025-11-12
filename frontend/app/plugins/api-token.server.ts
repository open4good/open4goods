export default defineNuxtPlugin(() => {
  const globalObj = globalThis as typeof globalThis & {
    __apiTokenInstalled?: boolean
    $fetch?: typeof $fetch
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
    const opts = token && url.startsWith(apiUrl) ? withToken(init) : init
    return originalFetch(input, opts)
  }

  const original$fetch = globalObj.$fetch
  if (original$fetch) {
    const wrapped = ((input: unknown, init?: RequestInit) => {
      const url = toUrl(input)
      const opts = token && url.startsWith(apiUrl) ? withToken(init) : init
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return original$fetch(input as any, opts as any)
    }) as unknown as typeof $fetch
    wrapped.raw = original$fetch.raw
    wrapped.create = original$fetch.create
    globalObj.$fetch = wrapped
  }
})

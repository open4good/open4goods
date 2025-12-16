export default defineNuxtPlugin(() => {
  if (
    !import.meta.server ||
    (globalThis as { __fetchLoggerInstalled?: boolean }).__fetchLoggerInstalled
  ) {
    return
  }
  const config = useRuntimeConfig()
  const originalFetch = globalThis.fetch
  ;(globalThis as { __fetchLoggerInstalled?: boolean }).__fetchLoggerInstalled =
    true
  globalThis.fetch = async (
    input: RequestInfo | URL,
    init?: RequestInit
  ): Promise<Response> => {
    const url =
      typeof input === 'string'
        ? input
        : input instanceof URL
          ? input.toString()
          : input.url
    if (url.startsWith(config.apiUrl)) {
      const method = init?.method ?? 'GET'
      console.log(`[nuxt] ${method} ${url}`)
    }
    return originalFetch(input, init)
  }
})

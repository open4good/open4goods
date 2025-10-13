import { useRequestHeaders } from '#app'

const HEADER_NAME = 'x-forwarded-host'

const normalizeForwardedHost = (rawValue: string | string[] | undefined): string | null => {
  if (!rawValue) {
    return null
  }

  const value = Array.isArray(rawValue) ? rawValue[0] : rawValue
  if (!value) {
    return null
  }
  const normalized = value.split(',')[0]?.trim().toLowerCase()

  return normalized ? normalized : null
}

const withForwardedHost = (forwardedHost: string, init?: RequestInit): RequestInit => {
  const headers = new Headers(init?.headers ?? {})

  headers.set(HEADER_NAME, forwardedHost)
  headers.set('host', forwardedHost)

  return { ...init, headers }
}

const wrapFetchWithForwardedHost = (baseFetch: typeof $fetch, forwardedHost: string): typeof $fetch => {
  const forwardedFetch = (async (input: Parameters<typeof $fetch>[0], init?: RequestInit) => {
    const options = withForwardedHost(forwardedHost, init)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return baseFetch(input as any, options as any)
  }) as typeof $fetch

  if (typeof baseFetch.raw === 'function') {
    forwardedFetch.raw = (async (input: Parameters<typeof $fetch.raw>[0], init?: RequestInit) => {
      const options = withForwardedHost(forwardedHost, init)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return baseFetch.raw(input as any, options as any)
    }) as typeof $fetch.raw
  }

  if (typeof baseFetch.create === 'function') {
    forwardedFetch.create = ((defaults?: Parameters<typeof baseFetch.create>[0]) => {
      const created = baseFetch.create(defaults as never)
      return wrapFetchWithForwardedHost(created, forwardedHost)
    }) as typeof baseFetch.create
  }

  return forwardedFetch
}

type FetchCapableNuxtApp = { $fetch: typeof $fetch }

export const installForwardedHostFetch = (nuxtApp: FetchCapableNuxtApp): void => {
  const requestHeaders = useRequestHeaders(['x-forwarded-host', 'host'])
  const forwardedHost =
    normalizeForwardedHost(requestHeaders['x-forwarded-host']) ?? normalizeForwardedHost(requestHeaders.host)

  if (!forwardedHost) {
    return
  }

  const globalScope = globalThis as typeof globalThis & { $fetch?: typeof $fetch }
  const baseFetch = (nuxtApp.$fetch ?? globalScope.$fetch ?? $fetch) as typeof $fetch
  const forwardedFetch = wrapFetchWithForwardedHost(baseFetch, forwardedHost)

  nuxtApp.$fetch = forwardedFetch
  globalScope.$fetch = forwardedFetch
}

export default defineNuxtPlugin((nuxtApp) => {
  installForwardedHostFetch(nuxtApp as unknown as FetchCapableNuxtApp)
})

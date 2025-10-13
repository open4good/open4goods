import { afterEach, describe, expect, it, vi } from 'vitest'

import { installForwardedHostFetch } from '../server-fetch-forwarded-host.server'

const useRequestHeadersMock = vi.hoisted(() => vi.fn())

vi.mock('#app', async () => {
  const actual = await vi.importActual<typeof import('#app')>('#app')

  return {
    ...actual,
    useRequestHeaders: useRequestHeadersMock,
  }
})

describe('server fetch forwarded host plugin', () => {
  const originalGlobalFetch = globalThis.$fetch

  const createBaseFetch = () => {
    const fetchSpy = vi
      .fn(async (_input: Parameters<typeof $fetch>[0], init?: RequestInit) => ({ init }))
      .mockName('base$fetch')
    const rawSpy = vi
      .fn(async (_input: Parameters<typeof $fetch.raw>[0], init?: RequestInit) => ({ init }))
      .mockName('base$fetch.raw')

    const baseFetch = fetchSpy as unknown as typeof $fetch
    baseFetch.raw = rawSpy as unknown as typeof $fetch.raw
    baseFetch.create = vi.fn(() => baseFetch).mockName('base$fetch.create') as unknown as typeof baseFetch.create

    return { baseFetch, fetchSpy, rawSpy }
  }

  afterEach(() => {
    useRequestHeadersMock.mockReset()
    globalThis.$fetch = originalGlobalFetch
  })

  it('wraps $fetch to forward the normalized host header', async () => {
    const { baseFetch, fetchSpy, rawSpy } = createBaseFetch()
    const nuxtApp = { $fetch: baseFetch }

    globalThis.$fetch = baseFetch

    useRequestHeadersMock.mockReturnValue({
      'x-forwarded-host': 'Example.COM:3000, backup.example',
      host: undefined,
    })

    installForwardedHostFetch(nuxtApp)

    expect(useRequestHeadersMock).toHaveBeenCalledWith(['x-forwarded-host', 'host'])

    const wrappedFetch = nuxtApp.$fetch as typeof $fetch

    await wrappedFetch('https://service.test', { headers: { 'x-custom-header': 'value' } })

    expect(fetchSpy).toHaveBeenCalledTimes(1)

    const forwardedInit = fetchSpy.mock.calls[0]?.[1] as RequestInit | undefined
    const forwardedHeaders = new Headers((forwardedInit?.headers ?? {}) as HeadersInit)

    expect(forwardedHeaders.get('x-forwarded-host')).toBe('example.com:3000')
    expect(forwardedHeaders.get('host')).toBe('example.com:3000')
    expect(forwardedHeaders.get('x-custom-header')).toBe('value')
    expect(globalThis.$fetch).toBe(nuxtApp.$fetch)

    await wrappedFetch.raw('https://service.test/raw')
    const rawCall = rawSpy.mock.calls[0]?.[1] as RequestInit | undefined
    const rawHeaders = new Headers((rawCall?.headers ?? {}) as HeadersInit)
    expect(rawHeaders.get('x-forwarded-host')).toBe('example.com:3000')
    expect(rawHeaders.get('host')).toBe('example.com:3000')
  })
})

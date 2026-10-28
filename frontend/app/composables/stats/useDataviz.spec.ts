import { useNuxtApp } from '#imports'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useDataviz } from './useDataviz'

describe('useDataviz', () => {
  const fetchMock = vi.fn()
  // globalThis.$fetch is swapped on every SSR request: the composable must use
  // the client of its own nuxtApp (GOU-1228).
  const globalFetchMock = vi.fn(() =>
    Promise.reject(new Error('globalThis.$fetch must not be used'))
  )
  const nuxtApp = useNuxtApp()
  const originalFetch = nuxtApp.$fetch

  beforeEach(() => {
    nuxtApp.$fetch = fetchMock as unknown as typeof nuxtApp.$fetch
    vi.stubGlobal('$fetch', globalFetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    nuxtApp.$fetch = originalFetch
    fetchMock.mockReset()
    globalFetchMock.mockClear()
  })

  it('posts chart queries through the request client', async () => {
    fetchMock.mockResolvedValue({ ok: true })
    const request = { chartId: 'c1' } as never

    const result = await useDataviz('phones').fetchChartData(request)

    expect(result).toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/stats/dataviz/chart',
      expect.objectContaining({
        method: 'POST',
        params: { verticalId: 'phones' },
        body: request,
      })
    )
    expect(globalFetchMock).not.toHaveBeenCalled()
  })
})

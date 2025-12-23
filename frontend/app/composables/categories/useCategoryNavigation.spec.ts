import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('useCategoryNavigation composable', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.resetModules()
    fetchMock.mockReset()
    vi.stubGlobal('$fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads the navigation tree and caches the payload', async () => {
    const navigationResponse = {
      category: { title: 'Root' },
      childCategories: [],
    }

    fetchMock.mockResolvedValueOnce(navigationResponse)

    const { useCategoryNavigation } = await import('./useCategoryNavigation')
    const { fetchNavigation, navigation, loading, error } =
      useCategoryNavigation()

    const result = await fetchNavigation()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/categories/navigation',
      expect.objectContaining({ params: undefined })
    )
    expect(result).toEqual(navigationResponse)
    expect(navigation.value).toEqual(navigationResponse)
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  it('captures errors when the navigation endpoint fails', async () => {
    fetchMock.mockRejectedValueOnce(new Error('Network error'))

    const { useCategoryNavigation } = await import('./useCategoryNavigation')
    const { fetchNavigation, error, navigation } = useCategoryNavigation()

    await fetchNavigation({ path: 'electronics' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/categories/navigation',
      expect.objectContaining({ params: { path: 'electronics' } })
    )
    expect(error.value).toBe('Network error')
    expect(navigation.value).toBeNull()
  })

  it('reuses the cached navigation payload when the server TTL has not expired', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2024-01-01T00:00:00Z'))

    const navigationResponse = {
      category: { title: 'Cached root' },
      childCategories: [],
    }

    const getNavigationMock = vi.fn().mockResolvedValue(navigationResponse)

    vi.doMock('~~/shared/api-client/services/categories.services', () => ({
      useCategoriesService: () => ({ getNavigation: getNavigationMock }),
    }))

    const caches = new Map<string, { payload: unknown; expiresAt: number }>()

    vi.doMock('nitropack/runtime/internal/cache', () => ({
      cachedEventHandler: (
        handler: (event: unknown) => unknown | Promise<unknown>,
        options?: {
          name?: string
          maxAge?: number
          getKey?: (event: unknown) => string
        }
      ) => {
        const name = options?.name ?? 'default'
        const maxAgeMs = (options?.maxAge ?? 0) * 1000

        return async (event: unknown) => {
          const keySegment = options?.getKey?.(event) ?? ''
          const cacheKey = `${name}:${keySegment}`
          const now = Date.now()
          const cached = caches.get(cacheKey)

          if (cached && cached.expiresAt > now) {
            return cached.payload
          }

          const payload = await handler(event)
          caches.set(cacheKey, { payload, expiresAt: now + maxAgeMs })
          return payload
        }
      },
    }))

    vi.doMock('h3', () => ({
      getQuery: (event: { context?: { query?: Record<string, unknown> } }) =>
        event.context?.query ?? {},
      setResponseHeader: (
        event: { node: { res: { headers: Record<string, string> } } },
        name: string,
        value: string
      ) => {
        event.node.res.headers[name.toLowerCase()] = value
      },
      createError: (input: unknown) => ({
        ...(typeof input === 'object' && input ? input : {}),
        isCreateError: true,
      }),
    }))

    const { default: handler } =
      await import('../../../server/api/categories/navigation.get')

    const event = {
      context: { query: {} },
      node: {
        req: { headers: { host: 'nudger.fr' } },
        res: { headers: {} as Record<string, string> },
      },
    }

    const firstResponse = await handler(event as never)
    expect(firstResponse).toEqual(navigationResponse)
    expect(getNavigationMock).toHaveBeenCalledTimes(1)

    const secondResponse = await handler(event as never)
    expect(secondResponse).toEqual(navigationResponse)
    expect(getNavigationMock).toHaveBeenCalledTimes(1)

    vi.doUnmock('~~/shared/api-client/services/categories.services')
    vi.doUnmock('nitropack/runtime/internal/cache')
    vi.doUnmock('h3')
    vi.resetModules()

    vi.useRealTimers()
  })
})

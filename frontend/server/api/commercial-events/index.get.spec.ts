import { beforeEach, describe, expect, it, vi } from 'vitest'

type CommercialEventsRouteHandler = (typeof import('./index.get'))['default']

const listCommercialEventsMock = vi.hoisted(() => vi.fn())
const useCommercialEventsServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ listCommercialEvents: listCommercialEventsMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())

vi.mock('~~/shared/api-client/services/commercial-events.services', () => ({
  useCommercialEventsService: useCommercialEventsServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: vi.fn(),
}))

vi.mock('h3', async importOriginal => {
  const actual = await importOriginal<typeof import('h3')>()

  return {
    ...actual,
    createError: (input: unknown) => ({
      ...(typeof input === 'object' && input ? input : {}),
      isCreateError: true,
    }),
  }
})

vi.mock('nitropack/runtime/internal/cache', () => ({
  cachedEventHandler: (
    handler: (event: unknown) => unknown | Promise<unknown>,
    options?: {
      name?: string
      maxAge?: number
      getKey?: (event: unknown) => string
    }
  ) => {
    const cache = new Map<string, { payload: unknown; expiresAt: number }>()
    const name = options?.name ?? 'default'
    const maxAgeMs = (options?.maxAge ?? 0) * 1000

    return async (event: unknown) => {
      const key = `${name}:${options?.getKey?.(event) ?? ''}`
      const now = Date.now()
      const cached = cache.get(key)

      if (cached && cached.expiresAt > now) {
        return cached.payload
      }

      const payload = await handler(event)
      cache.set(key, {
        payload,
        expiresAt: now + maxAgeMs,
      })

      return payload
    }
  },
}))

describe('server/api/commercial-events/index.get', () => {
  let handler: CommercialEventsRouteHandler

  beforeEach(async () => {
    vi.resetModules()
    listCommercialEventsMock.mockReset()
    useCommercialEventsServiceMock.mockClear()
    resolveDomainLanguageMock.mockImplementation((host?: string) => ({
      domainLanguage: host === 'nudger.fr' ? 'fr' : 'en',
    }))
    setDomainLanguageCacheHeadersMock.mockReset()

    handler = (await import('./index.get')).default
  })

  it('fetches commercial events with a 24-hour cache policy', async () => {
    const payload = [{ title: 'Promo', startsAt: '2026-01-01' }]
    listCommercialEventsMock.mockResolvedValue(payload)

    const event = {
      context: {},
      node: {
        req: { headers: { host: 'nudger.fr' } },
      },
    } as unknown as Parameters<CommercialEventsRouteHandler>[0]

    const response = await handler(event)

    expect(response).toEqual(payload)
    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.fr')
    expect(useCommercialEventsServiceMock).toHaveBeenCalledWith('fr')
    expect(setDomainLanguageCacheHeadersMock).toHaveBeenCalledWith(
      event,
      'public, max-age=86400, s-maxage=86400'
    )
  })

  it('keeps independent cache entries per domain language', async () => {
    listCommercialEventsMock.mockResolvedValue([{ title: 'Promo' }])

    const frEvent = {
      context: {},
      node: {
        req: { headers: { host: 'nudger.fr' } },
      },
    } as unknown as Parameters<CommercialEventsRouteHandler>[0]

    const enEvent = {
      context: {},
      node: {
        req: { headers: { host: 'nudger.org' } },
      },
    } as unknown as Parameters<CommercialEventsRouteHandler>[0]

    await handler(frEvent)
    await handler(frEvent)
    await handler(enEvent)

    expect(listCommercialEventsMock).toHaveBeenCalledTimes(2)
    expect(useCommercialEventsServiceMock).toHaveBeenNthCalledWith(1, 'fr')
    expect(useCommercialEventsServiceMock).toHaveBeenNthCalledWith(2, 'en')
  })
})

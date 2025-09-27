import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type TeamRouteHandler = typeof import('./team')['default']

const runtimeConfig = vi.hoisted(() => ({
  apiUrl: '',
  machineToken: '',
}))

const setResponseHeaderMock = vi.hoisted(() => vi.fn())

vi.mock('#imports', () => ({
  defineEventHandler: (fn: TeamRouteHandler) => fn,
  setResponseHeader: setResponseHeaderMock,
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('nuxt/app', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

vi.mock('#app/nuxt', () => ({
  useRuntimeConfig: () => runtimeConfig,
}))

describe('GET /api/team Nitro endpoint', () => {
  const fetchMock = vi.fn<Parameters<typeof fetch>, Promise<Response>>()
  let handler: TeamRouteHandler

  beforeEach(async () => {
    vi.resetModules()
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          cores: [],
          contributors: [],
        }),
        {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }
      )
    )

    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('defineEventHandler', (fn: TeamRouteHandler) => fn)

    runtimeConfig.apiUrl = 'https://backend.example.test'
    runtimeConfig.machineToken = 'test-token-123'
    setResponseHeaderMock.mockReset()
    vi.stubGlobal('setResponseHeader', setResponseHeaderMock)
    process.env.API_URL = 'https://backend.example.test'
    process.env.MACHINE_TOKEN = 'test-token-123'

    handler = (await import('./team')).default
  })

  afterEach(() => {
    vi.resetAllMocks()
    vi.unstubAllGlobals()
  })

  it('forwards the shared token header to the backend request', async () => {
    const event = {
      node: {
        req: {
          headers: {
            host: 'nudger.com',
          },
        },
        res: {},
      },
    } as unknown as Parameters<typeof handler>[0]

    await handler(event)

    expect(fetchMock).toHaveBeenCalledOnce()
    const [, init] = fetchMock.mock.calls[0]
    const headers = (init?.headers ?? {}) as Record<string, string>

    expect(headers['X-Shared-Token']).toBe('test-token-123')
  })
})

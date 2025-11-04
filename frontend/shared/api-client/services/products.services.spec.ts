import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const createBackendApiConfigMock = vi.hoisted(() =>
  vi.fn(() => ({
    basePath: 'https://backend.example',
    headers: { 'X-Shared-Token': 'token-123' },
  })),
)

vi.mock('./createBackendApiConfig', () => ({
  createBackendApiConfig: createBackendApiConfigMock,
}))

describe('useProductService.triggerReviewGeneration', () => {
  let fetchMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.resetModules()
    createBackendApiConfigMock.mockClear()
    process.env.VITEST = 'true'
    fetchMock = vi
      .fn<Parameters<typeof fetch>, ReturnType<typeof fetch>>()
      .mockResolvedValue(
        new Response('123', {
          status: 200,
          headers: { 'content-type': 'application/json' },
        }),
      )
    vi.stubGlobal('fetch', fetchMock as unknown as typeof fetch)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    delete process.env.VITEST
  })

  it('calls the backend review endpoint with the expected path and headers', async () => {
    const { useProductService } = await import('./products.services')
    const service = useProductService('fr')

    const scheduledGtin = await service.triggerReviewGeneration(
      '8806095491219',
      'captcha-token',
    )

    expect(createBackendApiConfigMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledTimes(1)

    const [url, init] = fetchMock.mock.calls[0] ?? []

    expect(url).toBe(
      'https://backend.example/products/8806095491219/review?hcaptchaResponse=captcha-token&domainLanguage=fr',
    )
    expect(init).toMatchObject({
      method: 'POST',
      headers: { 'X-Shared-Token': 'token-123' },
    })
    expect(scheduledGtin).toBe(123)
  })

  it('throws a ResponseError when the backend rejects the request', async () => {
    const response = new Response('Not Found', { status: 404, statusText: 'Not Found' })
    fetchMock.mockResolvedValueOnce(response)

    const { useProductService } = await import('./products.services')
    const service = useProductService('en')
    const apiModule = await import('..')

    await expect(
      service.triggerReviewGeneration(8806095491219, 'captcha-token'),
    ).rejects.toBeInstanceOf(apiModule.ResponseError)
  })
})

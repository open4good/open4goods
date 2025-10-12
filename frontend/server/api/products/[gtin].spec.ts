import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { ProductDto } from '~~/shared/api-client'

type ProductsRouteHandler = typeof import('./[gtin]')['default']

const getProductByGtinMock = vi.hoisted(() => vi.fn<Promise<ProductDto>, [string]>())
const useProductsServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getProductByGtin: getProductByGtinMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() => vi.fn(() => ({ domainLanguage: 'en' as const })))
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)
const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string; cause?: unknown }) => ({
    ...input,
    isCreateError: true,
  }))
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: ProductsRouteHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: createErrorMock,
}))

vi.mock('~~/shared/api-client/services/products.services', () => ({
  useProductsService: useProductsServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

describe('server/api/products/[gtin]', () => {
  let handler: ProductsRouteHandler
  let consoleErrorSpy: ReturnType<typeof vi.spyOn>

  beforeEach(async () => {
    vi.resetModules()

    getProductByGtinMock.mockReset()
    useProductsServiceMock.mockReturnValue({ getProductByGtin: getProductByGtinMock })
    resolveDomainLanguageMock.mockReturnValue({ domainLanguage: 'en' })
    extractBackendErrorDetailsMock.mockResolvedValue({
      statusCode: 500,
      statusMessage: 'Backend failure',
      statusText: 'Internal Server Error',
      bodyText: undefined,
      isResponseError: false,
      logMessage: 'HTTP 500 - Backend failure',
    })
    getRouterParamMock.mockImplementation((event, name) => {
      const context = (event as { context?: { params?: Record<string, string | undefined> } }).context
      return context?.params?.[name]
    })
    createErrorMock.mockImplementation((input) => ({ ...input, isCreateError: true }))

    consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    vi.stubGlobal('defineEventHandler', (fn: ProductsRouteHandler) => fn)
    vi.stubGlobal('getRouterParam', getRouterParamMock)
    vi.stubGlobal('createError', createErrorMock)

    handler = (await import('./[gtin]')).default
  })

  afterEach(() => {
    consoleErrorSpy.mockRestore()
    vi.clearAllMocks()
    vi.unstubAllGlobals()
  })

  it('fetches product details by GTIN', async () => {
    const product: ProductDto = {
      id: 'abc123',
      gtin: 123456,
      name: 'Example product',
    }
    getProductByGtinMock.mockResolvedValue(product)

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: '123456' } },
    } as unknown as Parameters<ProductsRouteHandler>[0]

    const response = await handler(event)

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('nudger.example')
    expect(useProductsServiceMock).toHaveBeenCalledWith('en')
    expect(getProductByGtinMock).toHaveBeenCalledWith('123456')
    expect(response).toEqual(product)
  })

  it('uses the forwarded host header when present', async () => {
    const product: ProductDto = {
      id: 'gtin789',
      gtin: 789012,
      name: 'Forwarded host product',
    }
    getProductByGtinMock.mockResolvedValue(product)

    const event = {
      node: {
        req: {
          headers: {
            host: 'nudger.example',
            'x-forwarded-host': 'beta.nudger.example',
          },
        },
      },
      context: { params: { gtin: '789012' } },
    } as unknown as Parameters<ProductsRouteHandler>[0]

    const response = await handler(event)

    expect(resolveDomainLanguageMock).toHaveBeenCalledWith('beta.nudger.example')
    expect(response).toEqual(product)
  })

  it('throws a 400 error when the GTIN param is missing', async () => {
    getRouterParamMock.mockReturnValueOnce(undefined)

    const event = {
      node: {
        req: { headers: { host: 'nudger.example' } },
      },
      context: { params: { gtin: undefined } },
    } as unknown as Parameters<ProductsRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
    expect(getProductByGtinMock).not.toHaveBeenCalled()
  })

  it('throws a 400 error when the GTIN format is invalid', async () => {
    const event = {
      node: {
        req: { headers: { host: 'nudger.example' } },
      },
      context: { params: { gtin: '12ab34' } },
    } as unknown as Parameters<ProductsRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
    expect(getProductByGtinMock).not.toHaveBeenCalled()
  })

  it('logs backend failures and rethrows with backend metadata', async () => {
    const backendFailure = new Error('backend error')
    getProductByGtinMock.mockRejectedValueOnce(backendFailure)
    extractBackendErrorDetailsMock.mockResolvedValueOnce({
      statusCode: 502,
      statusMessage: 'Bad Gateway',
      statusText: 'Bad Gateway',
      bodyText: 'Upstream failure',
      isResponseError: true,
      logMessage: 'HTTP 502 - Bad Gateway',
    })

    const event = {
      node: {
        req: {
          headers: { host: 'nudger.example' },
        },
      },
      context: { params: { gtin: '987654' } },
    } as unknown as Parameters<ProductsRouteHandler>[0]

    await expect(handler(event)).rejects.toMatchObject({
      statusCode: 502,
      statusMessage: 'Bad Gateway',
    })

    expect(extractBackendErrorDetailsMock).toHaveBeenCalledWith(backendFailure)
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Error fetching product detail:',
      'HTTP 502 - Bad Gateway',
      expect.objectContaining({ statusCode: 502 })
    )
  })
})

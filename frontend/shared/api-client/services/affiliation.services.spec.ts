import { beforeEach, describe, expect, it, vi } from 'vitest'

const redirectGetRawMock = vi.fn()
const redirectPostRawMock = vi.fn()
const AffiliationApiMock = vi.fn()
const createBackendApiConfigMock = vi.fn()

interface ResponseErrorHeaders {
  get(name: string): string | null
}

class MockHeaders implements ResponseErrorHeaders {
  constructor(
    private readonly entries: Record<string, string | undefined> = {}
  ) {}

  get(name: string): string | null {
    const value = this.entries[name]

    return value ?? null
  }
}

vi.mock('..', () => {
  class MockResponseError extends Error {
    response: { status: number; headers: ResponseErrorHeaders }

    constructor(
      status: number,
      headers: Record<string, string | undefined> = {}
    ) {
      super(`Response returned status code ${status}`)
      this.name = 'ResponseError'
      this.response = {
        status,
        headers: new MockHeaders(headers),
      }
    }
  }

  return {
    AffiliationApi: AffiliationApiMock,
    RedirectGetDomainLanguageEnum: { Fr: 'fr', En: 'en' } as const,
    RedirectPostDomainLanguageEnum: { Fr: 'fr', En: 'en' } as const,
    ResponseError: MockResponseError,
  }
})

vi.mock('./createBackendApiConfig', () => ({
  createBackendApiConfig: createBackendApiConfigMock,
}))

type ResponseErrorConstructor = new (
  status: number,
  headers?: Record<string, string | undefined>
) => Error & {
  response: {
    status: number
    headers: ResponseErrorHeaders
  }
}

let useAffiliationService: (typeof import('./affiliation.services'))['useAffiliationService']
let ResponseErrorCtor: ResponseErrorConstructor

describe('useAffiliationService.resolveRedirect', () => {
  beforeEach(async () => {
    vi.resetModules()
    redirectGetRawMock.mockReset()
    redirectPostRawMock.mockReset()
    AffiliationApiMock.mockReset()
    createBackendApiConfigMock.mockReset()
    createBackendApiConfigMock.mockReturnValue({ basePath: 'https://api.test' })
    AffiliationApiMock.mockImplementation(() => ({
      redirectGetRaw: redirectGetRawMock,
      redirectPostRaw: redirectPostRawMock,
    }))
    ;({ useAffiliationService } = await import('./affiliation.services'))
    const apiModule = await import('..')
    ResponseErrorCtor =
      apiModule.ResponseError as unknown as ResponseErrorConstructor
  })

  it('returns redirect details for GET requests', async () => {
    redirectGetRawMock.mockRejectedValueOnce(
      new ResponseErrorCtor(301, { Location: 'https://example.com/get' })
    )

    const affiliationService = useAffiliationService('en')
    const response = await affiliationService.resolveRedirect({
      token: 'token-123',
      userAgent: 'Mozilla/5.0',
      method: 'GET',
    })

    expect(createBackendApiConfigMock).toHaveBeenCalledTimes(1)
    expect(AffiliationApiMock).toHaveBeenCalledWith({
      basePath: 'https://api.test',
    })
    expect(redirectGetRawMock).toHaveBeenCalledTimes(1)
    expect(redirectGetRawMock).toHaveBeenCalledWith(
      {
        token: 'token-123',
        domainLanguage: 'en',
        userAgent: 'Mozilla/5.0',
      },
      expect.any(Function)
    )

    const initOverride = redirectGetRawMock.mock.calls[0]?.[1]

    expect(initOverride).toBeDefined()

    const overriddenInit = await initOverride!({
      init: { headers: { foo: 'bar' } },
    })

    expect(overriddenInit).toEqual({
      headers: { foo: 'bar' },
      redirect: 'manual',
    })
    expect(response).toEqual({
      statusCode: 301,
      location: 'https://example.com/get',
    })
  })

  it('returns redirect details for POST requests', async () => {
    redirectPostRawMock.mockRejectedValueOnce(
      new ResponseErrorCtor(301, { Location: 'https://example.com/post' })
    )

    const affiliationService = useAffiliationService('fr')
    const response = await affiliationService.resolveRedirect({
      token: 'token-456',
      method: 'POST',
    })

    expect(redirectPostRawMock).toHaveBeenCalledTimes(1)
    expect(redirectPostRawMock).toHaveBeenCalledWith(
      {
        token: 'token-456',
        domainLanguage: 'fr',
        userAgent: undefined,
      },
      expect.any(Function)
    )
    expect(response).toEqual({
      statusCode: 301,
      location: 'https://example.com/post',
    })
  })

  it('throws when the redirect response is missing the Location header', async () => {
    redirectGetRawMock.mockRejectedValueOnce(new ResponseErrorCtor(301))

    const affiliationService = useAffiliationService('en')

    await expect(
      affiliationService.resolveRedirect({
        token: 'missing-location',
        method: 'GET',
      })
    ).rejects.toThrow('Redirect response is missing the Location header.')
  })

  it('rethrows unexpected redirect status codes', async () => {
    redirectGetRawMock.mockRejectedValueOnce(new ResponseErrorCtor(400))

    const affiliationService = useAffiliationService('en')

    await expect(
      affiliationService.resolveRedirect({ token: 'bad-status', method: 'GET' })
    ).rejects.toBeInstanceOf(ResponseErrorCtor)
  })

  it('rethrows unknown errors', async () => {
    const unknownError = new Error('network-failure')
    redirectGetRawMock.mockRejectedValueOnce(unknownError)

    const affiliationService = useAffiliationService('en')

    await expect(
      affiliationService.resolveRedirect({
        token: 'unknown-error',
        method: 'GET',
      })
    ).rejects.toBe(unknownError)
  })
})

import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  RedirectGetDomainLanguageEnum,
  RedirectPostDomainLanguageEnum,
  ResponseError,
} from '..'

const redirectGetRawMock = vi.hoisted(() => vi.fn())
const redirectPostRawMock = vi.hoisted(() => vi.fn())
const createBackendApiConfigMock = vi.hoisted(() => vi.fn(() => ({ basePath: 'https://backend.test' })))

vi.mock('../createBackendApiConfig', () => ({
  createBackendApiConfig: createBackendApiConfigMock,
}))

vi.mock('..', async () => {
  const actual = await vi.importActual<typeof import('..')>('..')

  class MockAffiliationApi {
    constructor(..._args: unknown[]) {}

    redirectGetRaw = redirectGetRawMock
    redirectPostRaw = redirectPostRawMock
  }

  return {
    ...actual,
    AffiliationApi: MockAffiliationApi as unknown as typeof actual.AffiliationApi,
  }
})

let useAffiliationService: typeof import('./affiliation.services')['useAffiliationService']

describe('useAffiliationService', () => {
  beforeEach(async () => {
    vi.resetModules()
    redirectGetRawMock.mockReset()
    redirectPostRawMock.mockReset()
    createBackendApiConfigMock.mockClear()

    process.env.VITEST = 'true'

    ;({ useAffiliationService } = await import('./affiliation.services'))
  })

  it('returns redirect metadata for GET requests', async () => {
    const redirectLocation = 'https://redirect.example.test'

    redirectGetRawMock.mockRejectedValueOnce(
      new ResponseError(
        new Response(null, {
          status: 301,
          headers: { Location: redirectLocation },
        })
      )
    )

    const service = useAffiliationService('en')
    const result = await service.resolveRedirect({
      token: 'token-123',
      method: 'GET',
    })

    expect(result).toEqual({ statusCode: 301, location: redirectLocation })
    expect(redirectGetRawMock).toHaveBeenCalledWith({
      token: 'token-123',
      domainLanguage: RedirectGetDomainLanguageEnum.En,
      userAgent: undefined,
    })
  })

  it('returns redirect metadata for POST requests and forwards user agent', async () => {
    const redirectLocation = 'https://redirect.example.test/post'

    redirectPostRawMock.mockRejectedValueOnce(
      new ResponseError(
        new Response(null, {
          status: 301,
          headers: { Location: redirectLocation },
        })
      )
    )

    const service = useAffiliationService('fr')
    const result = await service.resolveRedirect({
      token: 'token-xyz',
      method: 'POST',
      userAgent: 'Mozilla/5.0',
    })

    expect(result).toEqual({ statusCode: 301, location: redirectLocation })
    expect(redirectPostRawMock).toHaveBeenCalledWith({
      token: 'token-xyz',
      domainLanguage: RedirectPostDomainLanguageEnum.Fr,
      userAgent: 'Mozilla/5.0',
    })
  })

  it('throws when the redirect response is missing the Location header', async () => {
    redirectGetRawMock.mockRejectedValueOnce(
      new ResponseError(
        new Response(null, {
          status: 301,
        })
      )
    )

    const service = useAffiliationService('en')

    await expect(
      service.resolveRedirect({ token: 'token-123', method: 'GET' })
    ).rejects.toThrow('Redirect response is missing the Location header.')
  })

  it('rethrows backend errors that are not redirects', async () => {
    const backendError = new ResponseError(
      new Response(null, {
        status: 500,
      })
    )

    redirectGetRawMock.mockRejectedValueOnce(backendError)

    const service = useAffiliationService('en')

    await expect(
      service.resolveRedirect({ token: 'token-123', method: 'GET' })
    ).rejects.toBe(backendError)
  })
})

afterAll(() => {
  delete process.env.VITEST
})

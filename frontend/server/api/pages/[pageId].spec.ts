import { beforeEach, describe, expect, it, vi } from 'vitest'

type PageHandler = (typeof import('./[pageId]'))['default']

const getPageMock = vi.hoisted(() => vi.fn())
const usePagesServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getPage: getPageMock }))
)
const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'fr' as const }))
)
const extractBackendErrorDetailsMock = vi.hoisted(() => vi.fn())
const setDomainLanguageCacheHeadersMock = vi.hoisted(() => vi.fn())
const getRouterParamMock = vi.hoisted(() =>
  vi.fn<(event: unknown, name: string) => string | undefined>()
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: PageHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: (input: { statusCode: number; statusMessage: string }) => ({
    ...input,
    isCreateError: true,
  }),
}))

vi.mock('~~/shared/api-client/services/pages.services', () => ({
  usePagesService: usePagesServiceMock,
}))

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/log-backend-error', () => ({
  extractBackendErrorDetails: extractBackendErrorDetailsMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

describe('server/api/pages/[pageId]', () => {
  let handler: PageHandler

  beforeEach(async () => {
    vi.resetModules()
    getPageMock.mockReset()
    getRouterParamMock.mockReset()
    handler = (await import('./[pageId]')).default
  })

  const event = {
    node: { req: { headers: { host: 'nudger.fr' } } },
  } as unknown as Parameters<PageHandler>[0]

  it('serves the real static legal-notice page without calling the live XWiki backend', async () => {
    getRouterParamMock.mockReturnValue(
      encodeURIComponent('webpages:default:legal-notice:WebHome')
    )

    const response = await handler(event)

    expect(response.pageTitle).toBe('Les mentions légales et les CGU de Nudger')
    expect(usePagesServiceMock).not.toHaveBeenCalled()
    expect(getPageMock).not.toHaveBeenCalled()
  })

  it('falls through to the live backend for a page id with no static entry', async () => {
    getRouterParamMock.mockReturnValue(
      encodeURIComponent('webpages:default:impact-score-ia:WebHome')
    )
    getPageMock.mockResolvedValue({ htmlContent: '<p>from xwiki</p>' })

    const response = await handler(event)

    expect(usePagesServiceMock).toHaveBeenCalledWith('fr')
    expect(getPageMock).toHaveBeenCalledWith(
      'webpages:default:impact-score-ia:WebHome'
    )
    expect(response.htmlContent).toBe('<p>from xwiki</p>')
  })

  it('rejects a request with no page id', async () => {
    getRouterParamMock.mockReturnValue(undefined)

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })
})

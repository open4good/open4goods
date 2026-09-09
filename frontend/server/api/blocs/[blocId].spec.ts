import { beforeEach, describe, expect, it, vi } from 'vitest'

type BlocHandler = (typeof import('./[blocId]'))['default']

const getBlocMock = vi.hoisted(() => vi.fn())
const useContentServiceMock = vi.hoisted(() =>
  vi.fn(() => ({ getBloc: getBlocMock }))
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
  defineEventHandler: (fn: BlocHandler) => fn,
  getRouterParam: getRouterParamMock,
  createError: (input: { statusCode: number; statusMessage: string }) => ({
    ...input,
    isCreateError: true,
  }),
}))

vi.mock('~~/shared/api-client/services/content.services', () => ({
  useContentService: useContentServiceMock,
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

describe('server/api/blocs/[blocId]', () => {
  let handler: BlocHandler

  beforeEach(async () => {
    vi.resetModules()
    getBlocMock.mockReset()
    getRouterParamMock.mockReset()
    handler = (await import('./[blocId]')).default
  })

  const event = {
    node: { req: { headers: { host: 'nudger.fr' } } },
  } as unknown as Parameters<BlocHandler>[0]

  it('serves real static content without calling the live XWiki backend', async () => {
    getRouterParamMock.mockReturnValue('pages:team:goulven-furet-title:')

    const response = await handler(event)

    expect(response).toEqual({
      blocId: 'pages:team:goulven-furet-title:',
      htmlContent: '<p>CEO / CTO</p>',
      editLink: null,
    })
    expect(useContentServiceMock).not.toHaveBeenCalled()
    expect(getBlocMock).not.toHaveBeenCalled()
  })

  it('falls through to the live backend for a bloc id with no static entry', async () => {
    getRouterParamMock.mockReturnValue('pages:legal-notice:WebHome')
    getBlocMock.mockResolvedValue({
      blocId: 'pages:legal-notice:WebHome',
      htmlContent: '<p>from xwiki</p>',
      editLink: 'https://wiki.nudger.fr/edit',
    })

    const response = await handler(event)

    expect(useContentServiceMock).toHaveBeenCalledWith('fr')
    expect(getBlocMock).toHaveBeenCalledWith('pages:legal-notice:WebHome')
    expect(response.htmlContent).toBe('<p>from xwiki</p>')
  })

  it('rejects a request with no bloc id', async () => {
    getRouterParamMock.mockReturnValue(undefined)

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })
})

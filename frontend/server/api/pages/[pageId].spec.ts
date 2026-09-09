import { beforeEach, describe, expect, it, vi } from 'vitest'

type PageHandler = (typeof import('./[pageId]'))['default']

const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'fr' as const }))
)
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

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

describe('server/api/pages/[pageId]', () => {
  let handler: PageHandler

  beforeEach(async () => {
    vi.resetModules()
    getRouterParamMock.mockReset()
    handler = (await import('./[pageId]')).default
  })

  const event = {
    node: { req: { headers: { host: 'nudger.fr' } } },
  } as unknown as Parameters<PageHandler>[0]

  it('serves the real static legal-notice page', async () => {
    getRouterParamMock.mockReturnValue(
      encodeURIComponent('webpages:default:legal-notice:WebHome')
    )

    const response = await handler(event)

    expect(response.pageTitle).toBe('Les mentions légales et les CGU de Nudger')
  })

  it('404s a page id with no static entry -- there is no live XWiki backend to fall back to', async () => {
    getRouterParamMock.mockReturnValue(
      encodeURIComponent('webpages:default:impact-score-ia:WebHome')
    )

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 404 })
  })

  it('rejects a request with no page id', async () => {
    getRouterParamMock.mockReturnValue(undefined)

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })
})

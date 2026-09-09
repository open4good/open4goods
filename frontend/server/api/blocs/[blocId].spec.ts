import { beforeEach, describe, expect, it, vi } from 'vitest'

type BlocHandler = (typeof import('./[blocId]'))['default']

const resolveDomainLanguageMock = vi.hoisted(() =>
  vi.fn(() => ({ domainLanguage: 'fr' as const }))
)
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

vi.mock('~~/shared/utils/domain-language', () => ({
  resolveDomainLanguage: resolveDomainLanguageMock,
}))

vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: setDomainLanguageCacheHeadersMock,
}))

describe('server/api/blocs/[blocId]', () => {
  let handler: BlocHandler

  beforeEach(async () => {
    vi.resetModules()
    getRouterParamMock.mockReset()
    handler = (await import('./[blocId]')).default
  })

  const event = {
    node: { req: { headers: { host: 'nudger.fr' } } },
  } as unknown as Parameters<BlocHandler>[0]

  it('serves real static content for a bloc id in the static map', async () => {
    getRouterParamMock.mockReturnValue('pages:team:goulven-furet-title:')

    const response = await handler(event)

    expect(response).toEqual({
      blocId: 'pages:team:goulven-furet-title:',
      htmlContent: '<p>CEO / CTO</p>',
      editLink: null,
    })
  })

  it('404s a bloc id with no static entry -- there is no live XWiki backend to fall back to', async () => {
    getRouterParamMock.mockReturnValue('pages:unknown:bloc')

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 404 })
  })

  it('rejects a request with no bloc id', async () => {
    getRouterParamMock.mockReturnValue(undefined)

    await expect(handler(event)).rejects.toMatchObject({ statusCode: 400 })
  })
})

import { beforeEach, describe, expect, it, vi } from 'vitest'

const sendRedirectMock = vi.hoisted(() => vi.fn())
const setHeaderMock = vi.hoisted(() => vi.fn())
const createErrorMock = vi.hoisted(() =>
  vi.fn((input: { statusCode: number; statusMessage: string }) => input)
)

vi.mock('h3', () => ({
  defineEventHandler: (fn: unknown) => fn,
  sendRedirect: sendRedirectMock,
  setHeader: setHeaderMock,
  createError: createErrorMock,
}))

describe('SEO remediation routes', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('redirects the legacy product sitemap to the localized sitemap', async () => {
    const handler = (await import('./sitemap/product-pages.xml.get')).default
    const event = {} as Parameters<typeof handler>[0]

    await handler(event)

    expect(sendRedirectMock).toHaveBeenCalledWith(
      event,
      '/sitemap/fr/product-pages.xml',
      301
    )
  })

  it('retires the stale blog RSS feed with 410', async () => {
    const handler = (await import('./blog/rss.get')).default

    expect(() => handler({} as Parameters<typeof handler>[0])).toThrow()
    expect(createErrorMock).toHaveBeenCalledWith({
      statusCode: 410,
      statusMessage: 'RSS feed retired',
    })
  })

  it('exposes the canonical sitemap in robots.txt', async () => {
    const handler = (await import('./robots.txt.get')).default
    const event = {} as Parameters<typeof handler>[0]

    const response = await handler(event)

    expect(setHeaderMock).toHaveBeenCalledWith(
      event,
      'Content-Type',
      'text/plain; charset=utf-8'
    )
    expect(response).toContain('Sitemap: https://nudger.fr/sitemap_index.xml')
  })

  it('blocks affiliation redirect URLs in robots.txt', async () => {
    const handler = (await import('./robots.txt.get')).default

    const response = await handler({} as Parameters<typeof handler>[0])

    expect(response).toContain('Disallow: /contrib')
  })
})

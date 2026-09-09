import { beforeEach, describe, expect, it, vi } from 'vitest'

const setResponseHeaderMock = vi.hoisted(() => vi.fn())
const getRequestURLMock = vi.hoisted(() =>
  vi.fn(() => new URL('https://nudger.fr/blog/rss'))
)
const listBlogDocsMock = vi.hoisted(() => vi.fn())

vi.mock('h3', () => ({
  defineEventHandler: (fn: unknown) => fn,
  setResponseHeader: setResponseHeaderMock,
  getRequestURL: getRequestURLMock,
}))
vi.mock('../../utils/cache-headers', () => ({
  setDomainLanguageCacheHeaders: vi.fn(),
}))
vi.mock('../../utils/blog-content', () => ({
  listBlogDocs: listBlogDocsMock,
}))

describe('blog RSS feed', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('builds a valid RSS 2.0 feed with absolute links from the request origin', async () => {
    listBlogDocsMock.mockResolvedValue([
      {
        path: '/blog/fr/a-quoi-sert-l-esg',
        title: 'À quoi sert l’ESG ?',
        description: 'Une description',
        author: 'o4g',
        tags: ['ESG'],
        date: '2026-08-06T03:37:30Z',
      },
    ])

    const handler = (await import('./rss.get')).default
    const xml = await handler({} as Parameters<typeof handler>[0])

    expect(xml).toContain('<rss version="2.0">')
    expect(xml).toContain('<link>https://nudger.fr/blog/a-quoi-sert-l-esg</link>')
    expect(xml).toContain('<category>ESG</category>')
    expect(xml).toContain('<title>À quoi sert l’ESG ?</title>')
    expect(setResponseHeaderMock).toHaveBeenCalledWith(
      {},
      'Content-Type',
      'application/rss+xml; charset=UTF-8'
    )
  })

  it('omits optional item fields that are absent instead of emitting empty tags', async () => {
    listBlogDocsMock.mockResolvedValue([
      { path: '/blog/fr/no-extras', title: 'No extras', description: '', author: '', tags: [], date: null },
    ])

    const handler = (await import('./rss.get')).default
    const xml = await handler({} as Parameters<typeof handler>[0])

    expect(xml).not.toContain('<description></description>')
    expect(xml).not.toContain('<pubDate>')
    expect(xml).not.toContain('<author></author>')
  })
})

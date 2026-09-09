import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import sitemapPlugin from './sitemap-blog-posts'
import { listBlogDocs } from '~~/server/utils/blog-content'
import type { BlogContentDoc } from '~~/server/utils/blog-content'

vi.mock('~~/server/utils/blog-content', async () => {
  const actual = await vi.importActual<
    typeof import('~~/server/utils/blog-content')
  >('~~/server/utils/blog-content')
  return {
    ...actual,
    listBlogDocs: vi.fn(),
  }
})

const doc = (overrides: Partial<BlogContentDoc>): BlogContentDoc => ({
  path: '/blog/fr/some-post',
  title: 'Some post',
  description: 'A description',
  author: 'o4g',
  language: 'fr',
  tags: [],
  date: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
  draft: false,
  published: true,
  ...overrides,
})

describe('sitemap-blog-posts plugin', () => {
  let mockHook: Mock
  let mockCtx: {
    event: Record<string, unknown>
    sitemapName: string
    sources: Array<{ urls: Array<{ loc: string; lastmod?: string }> }>
  }

  beforeEach(() => {
    mockHook = vi.fn()
    mockCtx = { event: {}, sitemapName: 'blog-posts.xml', sources: [] }
    vi.clearAllMocks()
  })

  const setRequestHost = (host: string) => {
    mockCtx.event = { node: { req: { headers: { host } } } }
  }

  const runPlugin = () => {
    const nitroApp = {
      hooks: { hook: mockHook },
    } as unknown as { hooks: { hook: Mock } }
    sitemapPlugin(nitroApp)
    return mockHook.mock.calls[0]?.[1]
  }

  it('ignores hook calls for other sitemaps', async () => {
    const callback = runPlugin()
    mockCtx.sitemapName = 'main-pages.xml'

    await callback(mockCtx)

    expect(listBlogDocs).not.toHaveBeenCalled()
    expect(mockCtx.sources).toHaveLength(0)
  })

  it('adds one source with a url per published post on the fr domain', async () => {
    const callback = runPlugin()
    setRequestHost('nudger.fr')
    vi.mocked(listBlogDocs).mockResolvedValue([
      doc({ path: '/blog/fr/hello-world', updatedAt: '2026-02-01T00:00:00Z' }),
      doc({ path: '/blog/fr/second-post', updatedAt: null }),
    ])

    await callback(mockCtx)

    expect(mockCtx.sources).toHaveLength(1)
    expect(mockCtx.sources[0]?.urls).toEqual([
      { loc: '/blog/hello-world', lastmod: '2026-02-01T00:00:00Z' },
      { loc: '/blog/second-post', lastmod: '2026-01-01T00:00:00Z' },
    ])
  })

  it('skips the english domain, which has no blog content yet', async () => {
    const callback = runPlugin()
    setRequestHost('nudger.com')

    await callback(mockCtx)

    expect(listBlogDocs).not.toHaveBeenCalled()
    expect(mockCtx.sources).toHaveLength(0)
  })
})

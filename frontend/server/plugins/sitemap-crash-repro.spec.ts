import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import sitemapPlugin from './sitemap-index'
import { getLocalSitemapFileDescriptorsForDomainLanguage } from '~~/server/utils/sitemap-local-files'

const accessMock = vi.hoisted(() => vi.fn())

// Mock dependencies
vi.mock('node:fs/promises', () => ({
  __esModule: true,
  access: accessMock,
  default: { access: accessMock },
}))

vi.mock('~~/server/utils/sitemap-local-files', () => ({
  getLocalSitemapFileDescriptorsForDomainLanguage: vi.fn(),
}))

describe('sitemap-index plugin', () => {
  let mockHook: Mock
  let mockCtx: {
    event: Record<string, unknown>
    sitemaps: Array<{ sitemap: string }>
  }

  beforeEach(() => {
    mockHook = vi.fn()
    mockCtx = {
      event: {},
      sitemaps: [],
    }
    vi.clearAllMocks()
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([])
    accessMock.mockResolvedValue(undefined)
  })

  const setRequestHost = (host: string, protocol = 'https') => {
    mockCtx.event = {
      node: {
        req: {
          headers: {
            host,
            'x-forwarded-proto': protocol,
          },
        },
      },
    }
  }

  const runPlugin = () => {
    const nitroApp = {
      hooks: {
        hook: mockHook,
      },
    } as unknown as { hooks: { hook: Mock } }
    sitemapPlugin(nitroApp)
    // Return the registered async callback
    return mockHook.mock.calls[0]?.[1]
  }

  it('handles standard nudger.fr request with existing file', async () => {
    const callback = runPlugin()

    setRequestHost('nudger.fr')
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([
      {
        filePath: '/opt/open4goods/sitemap/fr/product-pages.xml',
        fileName: 'product-pages.xml',
        publicPath: '/sitemap/fr/product-pages.xml',
      },
    ])
    accessMock.mockResolvedValue(undefined)

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0]?.sitemap).toBe(
      'https://nudger.fr/sitemap/fr/product-pages.xml'
    )
  })

  it('skips sitemaps whose file does not exist on disk', async () => {
    const callback = runPlugin()

    setRequestHost('nudger.fr')
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([
      {
        filePath: '/opt/open4goods/sitemap/fr/guides.xml',
        fileName: 'guides.xml',
        publicPath: '/sitemap/fr/guides.xml',
      },
    ])
    accessMock.mockRejectedValue(new Error('ENOENT'))

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(0)
  })

  it('handles empty sitemap list', async () => {
    const callback = runPlugin()

    setRequestHost('nudger.fr')
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([])

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(0)
  })

  it('handles unknown domain by falling back to default', async () => {
    const callback = runPlugin()

    setRequestHost('unknown.com')
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([
      {
        filePath: '/opt/open4goods/sitemap/default/product-pages.xml',
        fileName: 'product-pages.xml',
        publicPath: '/sitemap/default/product-pages.xml',
      },
    ])
    accessMock.mockResolvedValue(undefined)

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0]?.sitemap).toBe(
      'https://unknown.com/sitemap/default/product-pages.xml'
    )
  })

  it('falls back to the canonical site URL when request headers are unavailable', async () => {
    const callback = runPlugin()

    mockCtx.event = {}
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([
      {
        filePath: '/opt/open4goods/sitemap/fr/product-pages.xml',
        fileName: 'product-pages.xml',
        publicPath: '/sitemap/fr/product-pages.xml',
      },
    ])
    accessMock.mockResolvedValue(undefined)

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0]?.sitemap).toBe(
      'https://nudger.fr/sitemap/fr/product-pages.xml'
    )
  })

  it('does not add duplicates', async () => {
    const callback = runPlugin()

    setRequestHost('nudger.fr')
    vi.mocked(getLocalSitemapFileDescriptorsForDomainLanguage).mockReturnValue([
      {
        filePath: '/opt/open4goods/sitemap/fr/product-pages.xml',
        fileName: 'product-pages.xml',
        publicPath: '/sitemap/fr/product-pages.xml',
      },
    ])
    accessMock.mockResolvedValue(undefined)
    mockCtx.sitemaps = [{ sitemap: 'https://nudger.fr/sitemap/fr/product-pages.xml' }]

    await callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
  })
})

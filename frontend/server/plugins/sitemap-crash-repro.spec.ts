import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock dependencies
vi.mock('h3', () => ({
  getRequestURL: vi.fn(),
  createError: vi.fn(),
  defineEventHandler: vi.fn(),
}))

vi.mock('#imports', () => ({
  useRuntimeConfig: vi.fn(),
}))

// Import the plugin
import sitemapPlugin from './sitemap-index'
import { getRequestURL } from 'h3'
import { useRuntimeConfig } from '#imports'

describe('sitemap-index plugin', () => {
  let mockHook: any
  let mockCtx: any

  beforeEach(() => {
    mockHook = vi.fn()
    mockCtx = {
      event: {},
      sitemaps: [],
    }
    vi.clearAllMocks()
  })

  const runPlugin = () => {
    const nitroApp = {
      hooks: {
        hook: mockHook,
      },
    } as any
    sitemapPlugin(nitroApp)
    // Return the registered callback
    return mockHook.mock.calls[0][1]
  }

  it('handles standard nudger.fr request', () => {
    const callback = runPlugin()

    // Mock getRequestURL
    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))

    // Mock runtime config
    vi.mocked(useRuntimeConfig).mockReturnValue({
      sitemapLocalFiles: {
        fr: ['/tmp/sitemap.xml'],
      },
    })

    callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0].sitemap).toBe(
      'https://nudger.fr/sitemap/fr/sitemap.xml'
    )
  })

  it('handles missing sitemapLocalFiles configuration cleanly', () => {
    const callback = runPlugin()

    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    // Returns object but missing sitemapLocalFiles
    vi.mocked(useRuntimeConfig).mockReturnValue({})

    callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(0)
  })

  it('handles unknown domain by falling back to default', () => {
    const callback = runPlugin()

    vi.mocked(getRequestURL).mockReturnValue(new URL('https://unknown.com'))
    vi.mocked(useRuntimeConfig).mockReturnValue({
      sitemapLocalFiles: {
        fr: ['/tmp/sitemap-fr.xml'],
        en: ['/tmp/sitemap-en.xml'],
      },
    })

    callback(mockCtx)

    // unknown falls back to 'fr', so we expect fr sitemaps
    // Origin is preserved as unknown.com?
    // getPublicSitemapUrlsForDomainLanguage uses origin from requestURL
    expect(mockCtx.sitemaps[0].sitemap).toContain(
      'https://unknown.com/sitemap/fr/sitemap-fr.xml'
    )
  })

  it('handles origin being null? (impossible for URL object)', () => {
    // URL object always has origin.
  })

  it('handles sitemapLocalFiles being null', () => {
    const callback = runPlugin()
    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(useRuntimeConfig).mockReturnValue({
      sitemapLocalFiles: null,
    })
    callback(mockCtx)
    expect(mockCtx.sitemaps).toHaveLength(0)
  })

  it('handles sitemapLocalFiles[domain] being non-array', () => {
    const callback = runPlugin()
    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(useRuntimeConfig).mockReturnValue({
      sitemapLocalFiles: {
        fr: 'not-an-array',
      },
    })
    callback(mockCtx)
    expect(mockCtx.sitemaps).toHaveLength(0) // Should be handled by Array.isArray check
  })

  it('handles sitemapLocalFiles[domain] containing non-strings', () => {
    const callback = runPlugin()
    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(useRuntimeConfig).mockReturnValue({
      sitemapLocalFiles: {
        fr: [null, 123, {}],
      },
    })
    callback(mockCtx)
    expect(mockCtx.sitemaps).toHaveLength(0)
  })
})

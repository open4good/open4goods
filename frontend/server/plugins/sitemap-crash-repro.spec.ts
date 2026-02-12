import { describe, it, expect, vi, beforeEach, type Mock } from 'vitest'
import { getRequestURL } from 'h3'
import sitemapPlugin from './sitemap-index'
import { getPublicSitemapUrlsForDomainLanguage } from '~~/server/utils/sitemap-local-files'

// Mock dependencies
vi.mock('h3', () => ({
  getRequestURL: vi.fn(),
  createError: vi.fn(),
  defineEventHandler: vi.fn(),
}))

vi.mock('~~/server/utils/sitemap-local-files', () => ({
  getPublicSitemapUrlsForDomainLanguage: vi.fn(),
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
    vi.mocked(getPublicSitemapUrlsForDomainLanguage).mockReturnValue([])
  })

  const runPlugin = () => {
    const nitroApp = {
      hooks: {
        hook: mockHook,
      },
    } as unknown as { hooks: { hook: Mock } }
    sitemapPlugin(nitroApp)
    // Return the registered callback
    return mockHook.mock.calls[0]?.[1]
  }

  it('handles standard nudger.fr request', () => {
    const callback = runPlugin()

    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(getPublicSitemapUrlsForDomainLanguage).mockReturnValue([
      'https://nudger.fr/sitemap/fr/sitemap.xml',
    ])

    callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0]?.sitemap).toBe(
      'https://nudger.fr/sitemap/fr/sitemap.xml'
    )
  })

  it('handles empty sitemap list', () => {
    const callback = runPlugin()

    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(getPublicSitemapUrlsForDomainLanguage).mockReturnValue([])

    callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(0)
  })

  it('handles unknown domain by falling back to default', () => {
    const callback = runPlugin()

    vi.mocked(getRequestURL).mockReturnValue(new URL('https://unknown.com'))
    vi.mocked(getPublicSitemapUrlsForDomainLanguage).mockReturnValue([
      'https://unknown.com/sitemap/fr/sitemap-fr.xml',
    ])

    callback(mockCtx)

    expect(mockCtx.sitemaps).toHaveLength(1)
    expect(mockCtx.sitemaps[0]?.sitemap).toBe(
      'https://unknown.com/sitemap/fr/sitemap-fr.xml'
    )
  })

  it('handles sitemapLocalFiles returning null/undefined logic (simulated by empty array)', () => {
    const callback = runPlugin()
    vi.mocked(getRequestURL).mockReturnValue(new URL('https://nudger.fr'))
    vi.mocked(getPublicSitemapUrlsForDomainLanguage).mockReturnValue([])
    callback(mockCtx)
    expect(mockCtx.sitemaps).toHaveLength(0)
  })
})

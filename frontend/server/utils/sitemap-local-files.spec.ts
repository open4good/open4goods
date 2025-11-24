import { afterEach, describe, expect, it, vi } from 'vitest'

const buildRuntimeConfig = () => ({
  sitemapLocalFiles: {
    fr: [
      '/opt/open4goods/sitemap/fr/blog-posts.xml ',
      '/opt/open4goods/sitemap/fr/blog-posts.xml',
      '/opt/open4goods/sitemap/fr/category-pages.xml',
      42,
      '',
    ],
  },
})

afterEach(() => {
  vi.clearAllMocks()
  vi.resetModules()
})

describe('sitemap-local-files helpers', () => {
  it('exposes deduplicated descriptors and public URLs', async () => {
    const runtimeConfig = buildRuntimeConfig()

    const {
      getLocalSitemapFileDescriptorsForDomainLanguage,
      getPublicSitemapUrlsForDomainLanguage,
      getLocalSitemapFilePath,
    } = await import('~~/server/utils/sitemap-local-files')

    const descriptors = getLocalSitemapFileDescriptorsForDomainLanguage('fr', runtimeConfig)

    expect(descriptors).toEqual([
      {
        fileName: 'blog-posts.xml',
        filePath: '/opt/open4goods/sitemap/fr/blog-posts.xml',
        publicPath: '/sitemap/fr/blog-posts.xml',
      },
      {
        fileName: 'category-pages.xml',
        filePath: '/opt/open4goods/sitemap/fr/category-pages.xml',
        publicPath: '/sitemap/fr/category-pages.xml',
      },
    ])

    expect(getPublicSitemapUrlsForDomainLanguage('fr', 'https://nudger.fr', runtimeConfig)).toEqual([
      'https://nudger.fr/sitemap/fr/blog-posts.xml',
      'https://nudger.fr/sitemap/fr/category-pages.xml',
    ])

    expect(getLocalSitemapFilePath('fr', 'blog-posts.xml', runtimeConfig)).toBe(
      '/opt/open4goods/sitemap/fr/blog-posts.xml',
    )
    expect(getLocalSitemapFilePath('fr', 'missing.xml', runtimeConfig)).toBeNull()
  })
})

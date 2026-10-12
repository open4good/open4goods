import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  blogDocToSitemapUrl,
  findBlogDocBySlug,
  listBlogDocs,
  resolveRelativeImageSrc,
  toBlogPostDto,
  toBlogTagDtos,
  type BlogContentDoc,
} from './blog-content'

const queryCollectionMock = vi.hoisted(() => vi.fn())

vi.mock('@nuxt/content/server', () => ({
  queryCollection: queryCollectionMock,
}))

const doc = (overrides: Partial<BlogContentDoc>): BlogContentDoc => ({
  path: '/blog/fr/some-post',
  title: 'Some post',
  description: 'A description',
  author: 'o4g',
  language: 'fr',
  tags: ['ESG'],
  date: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
  draft: false,
  published: true,
  rawbody: '---\ntitle: x\n---\nHello',
  ...overrides,
})

const fakeEvent = {} as Parameters<typeof listBlogDocs>[0]

describe('blog-content', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listBlogDocs', () => {
    it('drops drafts and unpublished posts, sorted newest first', async () => {
      const all = vi.fn().mockResolvedValue([
        doc({ path: '/blog/fr/oldest', date: '2025-01-01T00:00:00Z' }),
        doc({ path: '/blog/fr/draft', draft: true }),
        doc({ path: '/blog/fr/unpublished', published: false }),
        doc({ path: '/blog/fr/newest', date: '2026-06-01T00:00:00Z' }),
      ])
      queryCollectionMock.mockReturnValue({ all })

      const result = await listBlogDocs(fakeEvent)

      expect(result.map(d => d.path)).toEqual(['/blog/fr/newest', '/blog/fr/oldest'])
      expect(queryCollectionMock).toHaveBeenCalledWith(fakeEvent, 'blog')
    })
  })

  describe('findBlogDocBySlug', () => {
    it('matches on the last path segment regardless of language prefix depth', async () => {
      const all = vi.fn().mockResolvedValue([
        doc({ path: '/blog/fr/a-quoi-sert-l-esg' }),
      ])
      queryCollectionMock.mockReturnValue({ all })

      const found = await findBlogDocBySlug(fakeEvent, 'a-quoi-sert-l-esg')
      expect(found?.path).toBe('/blog/fr/a-quoi-sert-l-esg')

      const notFound = await findBlogDocBySlug(fakeEvent, 'does-not-exist')
      expect(notFound).toBeNull()
    })
  })

  describe('toBlogPostDto', () => {
    it('maps dates to epoch ms and slug to the url field', async () => {
      const dto = await toBlogPostDto(
        doc({ path: '/blog/fr/hello-world', rawbody: '---\ntitle: x\n---\nHello **world**' })
      )

      expect(dto.url).toBe('hello-world')
      expect(dto.createdMs).toBe(Date.parse('2026-01-01T00:00:00Z'))
      expect(dto.modifiedMs).toBe(Date.parse('2026-01-02T00:00:00Z'))
      expect(dto.body).toContain('<strong>world</strong>')
    })

    it('unwraps a bare [assistant] marker out of its markdown-it paragraph wrapper', async () => {
      const dto = await toBlogPostDto(
        doc({ rawbody: '---\ntitle: x\n---\nBefore\n\n[assistant]\n\nAfter' })
      )

      expect(dto.body).toContain('[assistant]')
      expect(dto.body).not.toContain('<p>[assistant]</p>')
    })

    it('rewrites relative inline image references in the rendered body', async () => {
      const dto = await toBlogPostDto(
        doc({
          path: '/blog/fr/some-post',
          rawbody: '---\ntitle: Some post\n---\n![](cover.png)',
        })
      )

      expect(dto.body).toContain('/images/blog/some-post/cover.png')
    })
  })

  describe('blogDocToSitemapUrl', () => {
    it('builds a /blog/<slug> loc regardless of the path prefix depth', () => {
      const url = blogDocToSitemapUrl(
        doc({ path: '/blog/fr/a-quoi-sert-l-esg', updatedAt: '2026-03-01T00:00:00Z' })
      )
      expect(url).toEqual({
        loc: '/blog/a-quoi-sert-l-esg',
        lastmod: '2026-03-01T00:00:00Z',
      })
    })

    it('falls back to date when updatedAt is absent', () => {
      const url = blogDocToSitemapUrl(doc({ updatedAt: null, date: '2026-01-05T00:00:00Z' }))
      expect(url.lastmod).toBe('2026-01-05T00:00:00Z')
    })
  })

  describe('toBlogTagDtos', () => {
    it('counts tag occurrences across docs', () => {
      const result = toBlogTagDtos([
        doc({ tags: ['ESG', 'News'] }),
        doc({ tags: ['ESG'] }),
      ])

      expect(result).toEqual(
        expect.arrayContaining([
          { name: 'ESG', count: 2 },
          { name: 'News', count: 1 },
        ])
      )
    })
  })
})

describe('resolveRelativeImageSrc', () => {
  it('rewrites a bare filename to the slug image folder', () => {
    const html = '<p><img src="tv-planet-bad.jpeg" alt=""></p>'
    expect(resolveRelativeImageSrc(html, 'impact-ecologique-des-televiseurs')).toBe(
      '<p><img src="/images/blog/impact-ecologique-des-televiseurs/tv-planet-bad.jpeg" alt=""></p>'
    )
  })

  it('leaves a root-relative path untouched', () => {
    const html = '<img src="/images/blog/some-slug/already-rooted.png">'
    expect(resolveRelativeImageSrc(html, 'some-slug')).toBe(html)
  })

  it('leaves an absolute URL untouched', () => {
    const html = '<img src="https://cdn.example.com/image.png">'
    expect(resolveRelativeImageSrc(html, 'some-slug')).toBe(html)
  })

  it('leaves a protocol-relative URL untouched', () => {
    const html = '<img src="//cdn.example.com/image.png">'
    expect(resolveRelativeImageSrc(html, 'some-slug')).toBe(html)
  })

  it('leaves a data: URI untouched', () => {
    const html = '<img src="data:image/png;base64,abc123">'
    expect(resolveRelativeImageSrc(html, 'some-slug')).toBe(html)
  })

  it('leaves a fragment-only reference untouched', () => {
    const html = '<img src="#some-anchor">'
    expect(resolveRelativeImageSrc(html, 'some-slug')).toBe(html)
  })
})

/**
 * Guards GOU-407: a Markdown source file referencing an inline image by filename only (or by a
 * path that does not exist under public/images/blog/<slug>/) renders a broken `<img>` once served,
 * because the browser resolves it against the page URL instead of the image folder shipped with
 * the post.
 */
describe('blog content image coverage', () => {
  const contentDir = resolve(process.cwd(), 'content/blog/fr')
  const publicDir = resolve(process.cwd(), 'public')

  const markdownFiles = readdirSync(contentDir).filter(name =>
    name.endsWith('.md')
  )

  const extractInlineImageRefs = (rawbody: string): string[] => {
    const refs: string[] = []
    for (const match of rawbody.matchAll(/!\[[^\]]*\]\(([^)\s]+)\)/g)) {
      refs.push(match[1])
    }
    return refs
  }

  it('found the blog Markdown source files', () => {
    expect(markdownFiles.length).toBeGreaterThan(0)
  })

  it.each(markdownFiles)(
    'every inline/cover image of %s exists under public/images/blog/<slug>/',
    fileName => {
      const slug = fileName.replace(/\.md$/, '')
      const rawbody = readFileSync(resolve(contentDir, fileName), 'utf-8')

      const frontMatterImageMatch = rawbody.match(/^image:\s*(\S+)\s*$/m)
      const inlineRefs = extractInlineImageRefs(rawbody)
      const refs = frontMatterImageMatch
        ? [frontMatterImageMatch[1], ...inlineRefs]
        : inlineRefs

      for (const ref of refs) {
        if (/^https?:\/\//i.test(ref)) {
          continue
        }

        const rootRelativePath = ref.startsWith('/')
          ? ref
          : `/images/blog/${slug}/${ref}`

        expect(existsSync(resolve(publicDir, `.${rootRelativePath}`))).toBe(
          true
        )
      }
    }
  )
})

import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

import MarkdownIt from 'markdown-it'
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

    const draftAndUnpublished = () =>
      queryCollectionMock.mockReturnValue({
        all: vi.fn().mockResolvedValue([
          doc({ path: '/blog/fr/en-relecture', draft: true }),
          doc({ path: '/blog/fr/retire', draft: true, published: false }),
        ]),
      })

    it('does not find a draft by default (prod: 404)', async () => {
      draftAndUnpublished()

      expect(await findBlogDocBySlug(fakeEvent, 'en-relecture')).toBeNull()
    })

    it('finds a draft with includeDrafts (beta review), never an unpublished post', async () => {
      draftAndUnpublished()

      const found = await findBlogDocBySlug(fakeEvent, 'en-relecture', { includeDrafts: true })
      expect(found?.draft).toBe(true)
      expect(await findBlogDocBySlug(fakeEvent, 'retire', { includeDrafts: true })).toBeNull()
    })

    it('keeps drafts out of listBlogDocs', async () => {
      draftAndUnpublished()

      expect(await listBlogDocs(fakeEvent)).toEqual([])
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

  describe('product widget directive', () => {
    const render = async (body: string) =>
      (await toBlogPostDto(doc({ rawbody: `---\ntitle: x\n---\n${body}` }))).body

    it('renders the directive and its closing line as a validated marker', async () => {
      const body = await render(
        'Before\n\n::guide-product-grid{vertical="tv" subcategory="tv_55_inches" top="3"}\n::\n\nAfter'
      )

      expect(body).toBe(
        '<p>Before</p>\n<div data-product-widget="tv|tv_55_inches|3"></div>\n<p>After</p>\n'
      )
    })

    it('renders the tv-size-calculator directive as a validated marker', async () => {
      const body = await render(
        'Before\n\n::tv-size-calculator{default-distance="2.5" default-resolution="full-hd"}\n::\n\nAfter'
      )

      expect(body).toBe(
        '<p>Before</p>\n<div data-tv-size-calculator="2.5|full-hd"></div>\n<p>After</p>\n'
      )
    })

    it('renders the three TV widgets of the Black Friday article', async () => {
      const rawbody = readFileSync(
        resolve(
          process.cwd(),
          'content/blog/fr/black-friday-tv-reperer-fausses-promotions.md'
        ),
        'utf-8'
      )
      const dto = await toBlogPostDto(
        doc({
          path: '/blog/fr/black-friday-tv-reperer-fausses-promotions',
          rawbody,
        })
      )

      const markers = dto.body.match(/data-product-widget="[^"]*"/g)
      expect(markers).toEqual([
        'data-product-widget="tv|tv_43_inches|3"',
        'data-product-widget="tv|tv_55_inches|3"',
        'data-product-widget="tv|tv_65_inches|3"',
      ])
    })

    it('accepts a directive without closing line, right after a paragraph', async () => {
      const body = await render(
        'Before\n::guide-product-grid{vertical="oven"}\nAfter'
      )

      expect(body).toBe(
        '<p>Before</p>\n<div data-product-widget="oven||3"></div>\n<p>After</p>\n'
      )
    })

    it('renders nothing for invalid attributes, without breaking the body', async () => {
      const body = await render(
        'Before\n\n::guide-product-grid{vertical="<img src=x onerror=alert(1)>"}\n::\n\nAfter'
      )

      expect(body).toBe('<p>Before</p>\n<p>After</p>\n')
    })

    it('leaves a malformed directive as escaped text', async () => {
      const body = await render('::guide-product-grid{vertical="tv"')

      expect(body).not.toContain('data-product-widget')
      expect(body).toContain('::guide-product-grid{vertical=&quot;tv&quot;')
    })

    it('keeps the directive literal inside a code block', async () => {
      const body = await render(
        '```md\n::guide-product-grid{vertical="tv"}\n```'
      )

      expect(body).not.toContain('data-product-widget')
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

/**
 * GOU-2225: the product widget rule must not change the HTML of posts that do not use it.
 */
describe('blog content without product widget', () => {
  const contentDir = resolve(process.cwd(), 'content/blog/fr')
  const plain = new MarkdownIt({ html: false, linkify: true })
  const files = readdirSync(contentDir).filter(
    name =>
      name.endsWith('.md') &&
      !readFileSync(resolve(contentDir, name), 'utf-8').includes(
        '::guide-product-grid'
      )
  )

  it.each(files)('%s renders as plain markdown-it', async fileName => {
    const rawbody = readFileSync(resolve(contentDir, fileName), 'utf-8')
    const slug = fileName.replace(/\.md$/, '')
    const dto = await toBlogPostDto(doc({ path: `/blog/fr/${slug}`, rawbody }))
    const expected = resolveRelativeImageSrc(
      plain
        .render(rawbody.replace(/^---\n[\s\S]*?\n---\n/, ''))
        .replace('<p>[assistant]</p>', '[assistant]'),
      slug
    )

    expect(dto.body).toBe(expected)
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

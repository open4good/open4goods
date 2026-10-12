import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { resolveRelativeImageSrc, toBlogPostDto } from './blog-content'
import type { BlogContentDoc } from './blog-content'

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
})

describe('toBlogPostDto', () => {
  const doc = (overrides: Partial<BlogContentDoc>): BlogContentDoc => ({
    path: '/blog/fr/some-post',
    title: 'Some post',
    description: 'A description',
    author: 'o4g',
    language: 'fr',
    tags: [],
    date: '2026-01-01T00:00:00Z',
    updatedAt: null,
    draft: false,
    published: true,
    rawbody: '',
    ...overrides,
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

import type { H3Event } from 'h3'
import MarkdownIt from 'markdown-it'
import { queryCollection } from '@nuxt/content/server'

import type { BlogPostDto, BlogTagDto } from '~~/shared/api-client'

const markdown = new MarkdownIt({ html: false, linkify: true })

export interface BlogContentDoc {
  path: string
  title: string
  description: string
  author: string
  language: string
  tags: string[]
  date: string | null
  updatedAt: string | null
  draft: boolean
  published: boolean
  image?: string
  rawbody: string
}

export const slugFromPath = (path: string): string => path.split('/').filter(Boolean).pop() ?? path

const toEpochMs = (value: string | null): number | null => {
  if (!value) {
    return null
  }
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? null : parsed
}

/**
 * Markdown source files reference their inline images by filename only (e.g. `![](tv-planet.jpeg)`),
 * relative to the image folder exported alongside the post rather than to the rendered `/blog/<slug>`
 * route. Left as-is, the browser resolves them against the page URL and requests `/blog/<file>`,
 * which hits the `[slug].vue` catch-all instead of the image. Absolute URLs, already-rooted paths
 * (starting with a single `/`), other URI schemes (`data:`, `mailto:`, …) and fragment-only
 * references (`#…`) are left untouched.
 */
export function resolveRelativeImageSrc(html: string, slug: string): string {
  return html.replace(
    /(<img[^>]*\ssrc=")([^"]+)(")/g,
    (match, prefix: string, src: string, suffix: string) => {
      if (src.startsWith('/') || src.startsWith('#') || /^[a-z][a-z0-9+.-]*:/i.test(src)) {
        return match
      }
      return `${prefix}/images/blog/${slug}/${src}${suffix}`
    }
  )
}

/**
 * Renders a post's Markdown body to the sanitizer-ready HTML string BlogPostDto.body has always
 * carried. Rendered from the collection's `rawbody` (the source file as stored in the content
 * database) rather than its parsed AST -- the AST is shaped for <ContentRenderer>, not for producing
 * a plain HTML string, and the exported Markdown has no macros or components that would need MDC's
 * renderer over a plain CommonMark one. Never read content/ from disk: the deployed .output bundle
 * ships without it. A stray `[assistant]` marker (see quelle-taille-de-tv-choisir-pour-son-salon.md)
 * is unwrapped from the `<p>` markdown-it puts around it, so TheArticle.vue's
 * `content.split('[assistant]')` still finds a clean split point.
 */
function renderBody(raw: string, slug: string): string {
  const body = raw.replace(/^---\n[\s\S]*?\n---\n/, '')
  const rendered = markdown
    .render(body)
    .replace('<p>[assistant]</p>', '[assistant]')
  return resolveRelativeImageSrc(rendered, slug)
}

/**
 * All published, non-draft posts from the `blog` Nuxt Content collection, newest first -- the same
 * ordering BlogService.updateBlogPosts() used (sorted by creation date descending).
 */
export async function listBlogDocs(event: H3Event): Promise<BlogContentDoc[]> {
  const all = (await queryCollection(event, 'blog').all()) as unknown as Array<
    BlogContentDoc & { path: string }
  >
  return all
    .filter(doc => doc.published && !doc.draft)
    .sort((a, b) => (toEpochMs(b.date) ?? 0) - (toEpochMs(a.date) ?? 0))
}

export async function findBlogDocBySlug(
  event: H3Event,
  slug: string
): Promise<BlogContentDoc | null> {
  const docs = await listBlogDocs(event)
  return docs.find(doc => slugFromPath(doc.path) === slug) ?? null
}

export async function toBlogPostDto(doc: BlogContentDoc): Promise<BlogPostDto> {
  const slug = slugFromPath(doc.path)
  return {
    url: slug,
    title: doc.title,
    author: doc.author,
    summary: doc.description,
    body: renderBody(doc.rawbody, slug),
    category: doc.tags,
    image: doc.image,
    createdMs: toEpochMs(doc.date) ?? undefined,
    modifiedMs: toEpochMs(doc.updatedAt) ?? undefined,
  }
}

export interface BlogSitemapUrl {
  loc: string
  lastmod?: string
}

/**
 * Maps a blog doc to the shape @nuxtjs/sitemap expects for a source entry -- see
 * server/plugins/sitemap-blog-posts.ts. `updatedAt` wins over `date` for `lastmod` since it
 * reflects the last real edit, matching rss.get.ts's own preference order for freshness signals.
 */
export function blogDocToSitemapUrl(doc: BlogContentDoc): BlogSitemapUrl {
  return {
    loc: `/blog/${slugFromPath(doc.path)}`,
    lastmod: doc.updatedAt ?? doc.date ?? undefined,
  }
}

export function toBlogTagDtos(docs: BlogContentDoc[]): BlogTagDto[] {
  const counts = new Map<string, number>()
  for (const doc of docs) {
    for (const tag of doc.tags) {
      counts.set(tag, (counts.get(tag) ?? 0) + 1)
    }
  }
  return Array.from(counts, ([name, count]) => ({ name, count }))
}

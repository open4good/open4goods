import type { H3Event } from 'h3'
import MarkdownIt from 'markdown-it'
import { queryCollection } from '@nuxt/content/server'

import type { BlogPostDto, BlogTagDto } from '~~/shared/api-client'
import {
  PRODUCT_WIDGET_DIRECTIVE,
  parseProductWidgetAttributes,
  renderProductWidgetMarker,
  TV_CALCULATOR_DIRECTIVE,
  parseTvSizeCalculatorAttributes,
  renderTvSizeCalculatorMarker,
  type BlogProductWidget,
  type BlogTvSizeCalculator,
} from '~~/shared/utils/blog-product-widget'

/**
 * Block rule for the `::guide-product-grid{…}` directive (same syntax as the guides, see
 * docs/markdown-mapping.md). It takes the directive line and its optional closing `::` line, and
 * emits a `product_widget` token holding the validated values (null when invalid: nothing is
 * rendered). Any other line, including a malformed directive, stays ordinary Markdown.
 */
function productWidgetPlugin(md: MarkdownIt): void {
  const directives = [
    {
      rule: 'product_widget',
      pattern: PRODUCT_WIDGET_DIRECTIVE,
      parse: (attributes: string) => parseProductWidgetAttributes(attributes),
      render: (meta: unknown) =>
        renderProductWidgetMarker(meta as BlogProductWidget),
    },
    {
      // `::tv-size-calculator{…}` (GOU-2713), never invalid: bad values fall back to the defaults.
      rule: 'tv_size_calculator',
      pattern: TV_CALCULATOR_DIRECTIVE,
      parse: (attributes: string) =>
        parseTvSizeCalculatorAttributes(attributes),
      render: (meta: unknown) =>
        renderTvSizeCalculatorMarker(meta as BlogTvSizeCalculator),
    },
  ]
  for (const { rule, pattern, parse, render } of directives) {
    md.block.ruler.before(
      'paragraph',
      rule,
      (state, startLine, endLine, silent) => {
        if (state.sCount[startLine]! - state.blkIndent >= 4) {
          return false
        }
        const lineText = (line: number) =>
          state.src.slice(
            state.bMarks[line]! + state.tShift[line]!,
            state.eMarks[line]
          )
        const match = pattern.exec(lineText(startLine))
        if (!match) {
          return false
        }
        if (silent) {
          return true
        }
        let nextLine = startLine + 1
        if (nextLine < endLine && /^::\s*$/.test(lineText(nextLine))) {
          nextLine += 1
        }
        const token = state.push(rule, '', 0)
        token.block = true
        token.map = [startLine, nextLine]
        token.meta = parse(match[1] ?? '')
        state.line = nextLine
        return true
      },
      { alt: ['paragraph', 'reference', 'blockquote', 'list'] }
    )
    md.renderer.rules[rule] = (tokens, idx) => {
      const meta = tokens[idx]?.meta
      return meta ? `${render(meta)}\n` : ''
    }
  }
}

const markdown = new MarkdownIt({ html: false, linkify: true }).use(
  productWidgetPlugin
)

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

export const slugFromPath = (path: string): string =>
  path.split('/').filter(Boolean).pop() ?? path

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
      if (
        src.startsWith('/') ||
        src.startsWith('#') ||
        /^[a-z][a-z0-9+.-]*:/i.test(src)
      ) {
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
 * a plain HTML string. The only component a post can hold is the product widget directive, handled
 * by productWidgetPlugin above, so MDC's renderer is not needed. Never read content/ from disk: the deployed .output bundle
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
 * BlogPostDto plus `draft`, set only when a draft is served for review (see findBlogDocBySlug).
 * The generated BlogPostDto mirrors front-api and has no such field.
 */
export type BlogArticleDto = BlogPostDto & { draft?: boolean }

async function queryBlogDocs(
  event: H3Event,
  includeDrafts: boolean
): Promise<BlogContentDoc[]> {
  const all = (await queryCollection(event, 'blog').all()) as unknown as Array<
    BlogContentDoc & { path: string }
  >
  return all
    .filter(doc => doc.published && (includeDrafts || !doc.draft))
    .sort((a, b) => (toEpochMs(b.date) ?? 0) - (toEpochMs(a.date) ?? 0))
}

/**
 * All published, non-draft posts from the `blog` Nuxt Content collection, newest first -- the same
 * ordering BlogService.updateBlogPosts() used (sorted by creation date descending). Every listing
 * (articles, tags, RSS, sitemap) goes through it, so a draft never appears in one.
 */
export async function listBlogDocs(event: H3Event): Promise<BlogContentDoc[]> {
  return queryBlogDocs(event, false)
}

export interface FindBlogDocOptions {
  /**
   * Also return a `draft: true` post, so that it can be reviewed at its URL. Only the article route
   * sets it, from the `blogShowDrafts` runtime config (NUXT_BLOG_SHOW_DRAFTS=true on beta).
   * A post with `published: false` is never returned.
   */
  includeDrafts?: boolean
}

export async function findBlogDocBySlug(
  event: H3Event,
  slug: string,
  { includeDrafts = false }: FindBlogDocOptions = {}
): Promise<BlogContentDoc | null> {
  const docs = await queryBlogDocs(event, includeDrafts)
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

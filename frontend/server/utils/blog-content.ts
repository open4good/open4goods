import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

import type { H3Event } from 'h3'
import MarkdownIt from 'markdown-it'
import { queryCollection } from '@nuxt/content/server'

import type { BlogPostDto, BlogTagDto } from '~~/shared/api-client'

// scripts/migrate/xwiki_blog_export.py writes here; kept in sync with frontend/content.config.ts's
// BLOG_DIR so the raw Markdown body can be read directly (see renderBody for why).
const BLOG_CONTENT_DIR = fileURLToPath(new URL('../../content/blog', import.meta.url))

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
}

const slugFromPath = (path: string): string => path.split('/').filter(Boolean).pop() ?? path

const toEpochMs = (value: string | null): number | null => {
  if (!value) {
    return null
  }
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? null : parsed
}

/**
 * Renders a post's Markdown body to the sanitizer-ready HTML string BlogPostDto.body has always
 * carried. Read from the raw file rather than the collection's parsed AST -- the AST is shaped for
 * <ContentRenderer>, not for producing a plain HTML string, and the exported Markdown has no macros
 * or components that would need MDC's renderer over a plain CommonMark one. A stray `[assistant]`
 * marker (see quelle-taille-de-tv-choisir-pour-son-salon.md) is unwrapped from the `<p>` markdown-it
 * puts around it, so TheArticle.vue's `content.split('[assistant]')` still finds a clean split point.
 */
async function renderBody(language: string, slug: string): Promise<string> {
  const filePath = `${BLOG_CONTENT_DIR}/${language}/${slug}.md`
  const raw = await readFile(filePath, 'utf-8')
  const body = raw.replace(/^---\n[\s\S]*?\n---\n/, '')
  return markdown.render(body).replace('<p>[assistant]</p>', '[assistant]')
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
    body: await renderBody(doc.language, slug),
    category: doc.tags,
    image: doc.image,
    editLink: undefined,
    createdMs: toEpochMs(doc.date) ?? undefined,
    modifiedMs: toEpochMs(doc.updatedAt) ?? undefined,
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

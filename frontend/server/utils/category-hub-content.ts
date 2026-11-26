import type { H3Event } from 'h3'
import MarkdownIt from 'markdown-it'
import { queryCollection } from '@nuxt/content/server'

import {
  validateCategoryHubDoc,
  type CategoryHubDoc,
  type CategoryHubDto,
} from '~~/shared/utils/category-hub'

// Simple Markdown only: raw HTML in a hub file is escaped, not rendered.
const markdown = new MarkdownIt({ html: false, linkify: true })

export interface FindCategoryHubOptions {
  /**
   * Also return a `published: false` file, for review. Only set from the
   * `categoryHubsPreview` runtime config (NUXT_CATEGORY_HUBS_PREVIEW=true on beta or dev).
   */
  includeUnpublished?: boolean
}

/**
 * Hub file of a vertical in the given language, from the `categoryHubs` Nuxt Content
 * collection. Null when there is no file, when it is not published (outside preview)
 * or when its format is invalid: the category page then renders no block.
 */
export async function findCategoryHubDoc(
  event: H3Event,
  verticalId: string,
  language: string,
  { includeUnpublished = false }: FindCategoryHubOptions = {}
): Promise<CategoryHubDoc | null> {
  const docs = (await queryCollection(event, 'categoryHubs')
    .where('verticalId', '=', verticalId)
    .where('language', '=', language)
    .all()) as unknown as CategoryHubDoc[]

  const doc = docs.find(item => item.published || includeUnpublished)
  if (!doc) {
    return null
  }

  const errors = validateCategoryHubDoc(doc)
  if (errors.length) {
    console.error(
      `[category-hubs] Invalid hub file for vertical "${verticalId}" (${language}): ${errors.join('; ')}`
    )
    return null
  }
  return doc
}

export const toCategoryHubDto = (doc: CategoryHubDoc): CategoryHubDto => ({
  verticalId: doc.verticalId,
  language: doc.language,
  published: doc.published,
  title: doc.title?.trim() || null,
  introHtml: markdown.render(doc.intro.trim()),
  faq: doc.faq.map(item => ({
    question: item.question.trim(),
    answer: item.answer.trim(),
  })),
  subCategories: doc.subCategories ?? [],
  guides: (doc.guides ?? []).map(guide => ({
    slug: guide.slug.trim(),
    label: guide.label.trim(),
  })),
})

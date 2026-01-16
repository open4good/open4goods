import MiniSearch from 'minisearch'
import { getQuery, getRequestHeader, type H3Event } from 'h3'

import {
  normalizeBasePath,
  normalizeDocsLocale,
  useDocsContent,
  type DocsSearchSection,
} from '~/composables/useDocsContent'

type SearchResult = {
  path: string
  title: string
  excerpt: string
  score: number
  tags: string[]
}

type SearchIndexCache = {
  index: MiniSearch<DocsSearchSection>
  entries: Map<string, DocsSearchSection>
}

const searchIndexes = new Map<string, SearchIndexCache>()
const MAX_RESULTS = 20

const buildCacheKey = (
  event: H3Event,
  locale: string,
  basePath: string
) => {
  const host =
    getRequestHeader(event, 'x-forwarded-host') ??
    getRequestHeader(event, 'host') ??
    'unknown'

  return `${host}:${locale}:${basePath}`
}

const buildSearchIndex = (sections: DocsSearchSection[]) => {
  const index = new MiniSearch<DocsSearchSection>({
    idField: 'path',
    fields: ['title', 'content', 'tags'],
    storeFields: ['path', 'title', 'description', 'tags', 'content'],
    searchOptions: {
      boost: { title: 3, tags: 2 },
      prefix: true,
      fuzzy: 0.2,
    },
  })

  index.addAll(sections)

  return index
}

const buildExcerpt = (entry: DocsSearchSection): string => {
  if (entry.description) {
    return entry.description
  }

  if (!entry.content) {
    return ''
  }

  return entry.content.slice(0, 160)
}

export default defineEventHandler(async event => {
  const query = getQuery(event)
  const rawQuery = String(query.query ?? '').trim()

  if (!rawQuery) {
    return [] as SearchResult[]
  }

  const locale = normalizeDocsLocale(String(query.locale ?? ''))
  const basePath = normalizeBasePath(String(query.basePath ?? '/docs'))
  const tags = String(query.tags ?? '')
    .split(',')
    .map(tag => tag.trim())
    .filter(Boolean)

  const { getSearchSections } = useDocsContent()
  const cacheKey = buildCacheKey(event, locale, basePath)
  let cached = searchIndexes.get(cacheKey)

  if (!cached) {
    const sections = await getSearchSections({ locale, basePath })

    if (!sections.length) {
      return []
    }

    const index = buildSearchIndex(sections)
    cached = {
      index,
      entries: new Map(sections.map(section => [section.path, section])),
    }

    searchIndexes.set(cacheKey, cached)
  }

  const results = cached.index.search(rawQuery, { boost: { title: 3 } })
  const filteredResults = results
    .filter(result => {
      if (tags.length === 0) {
        return true
      }

      const entry = cached?.entries.get(result.id)
      return tags.every(tag => entry?.tags.includes(tag))
    })
    .slice(0, MAX_RESULTS)

  return filteredResults.map(result => {
    const entry = cached?.entries.get(result.id)
    return {
      path: result.id,
      title: entry?.title ?? result.id,
      excerpt: entry ? buildExcerpt(entry) : '',
      score: result.score,
      tags: entry?.tags ?? [],
    }
  })
})

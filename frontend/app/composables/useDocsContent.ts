import { computed } from 'vue'

import {
  DEFAULT_DOMAIN_LANGUAGE,
  getDomainLanguageFromHostname,
} from '~~/shared/utils/domain-language'

export type DocsLocale = 'en' | 'fr'

export type DocsDoc = {
  path: string
  title?: string | null
  description?: string | null
  tags?: string[] | null
  icon?: string | null
  weight?: number | null
  updatedAt?: string | null
  draft?: boolean | null
  body?: unknown
}

export type DocsNavigationNode = {
  id: string
  title: string
  path?: string
  icon?: string | null
  children: DocsNavigationNode[]
}

export type DocsSearchSection = {
  path: string
  title: string
  description?: string | null
  tags: string[]
  content: string
}

const DEFAULT_DOCS_LOCALE: DocsLocale = 'en'
const DEFAULT_DOCS_BASE_PATH = '/docs'
const INVALID_PATH_PATTERN = /(\.\.|\\|%00)/u

const toPlainText = (value: unknown): string => {
  if (!value) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (Array.isArray(value)) {
    return value.map(item => toPlainText(item)).join(' ')
  }

  if (typeof value === 'object') {
    return Object.values(value as Record<string, unknown>)
      .map(item => toPlainText(item))
      .join(' ')
  }

  return String(value)
}

const extractPlainText = (value: unknown): string =>
  toPlainText(value)
    .replace(/\s+/g, ' ')
    .replace(/[#*_>`~[\]()-]/g, ' ')
    .trim()

const sanitizePathInput = (value?: string | null): string => {
  if (!value) {
    return ''
  }

  const [withoutQueryHash] = value.split(/[?#]/u)
  const normalizedSlashes = withoutQueryHash.replace(/\\/g, '/')
  const collapsed = normalizedSlashes.replace(/\/{2,}/g, '/').trim()

  if (!collapsed || INVALID_PATH_PATTERN.test(collapsed)) {
    return ''
  }

  return collapsed
}

const normalizePathSegment = (value?: string | null): string => {
  const sanitized = sanitizePathInput(value)

  if (!sanitized) {
    return ''
  }

  return sanitized.replace(/^\/+/, '').replace(/\/+$/, '')
}

const useSafeRequestURL = (): URL | null => {
  try {
    return useRequestURL()
  } catch {
    if (import.meta.client && typeof window !== 'undefined') {
      try {
        return new URL(window.location.href)
      } catch {
        return null
      }
    }

    return null
  }
}

export const normalizeDocsLocale = (
  locale?: string | null | undefined
): DocsLocale => {
  if (locale === 'fr' || locale === 'en') {
    return locale
  }

  return DEFAULT_DOCS_LOCALE
}

export const resolveLocaleFromRequest = (): DocsLocale => {
  const requestURL = useSafeRequestURL()
  const domainLanguage = getDomainLanguageFromHostname(
    requestURL?.hostname ?? null
  ).domainLanguage

  return normalizeDocsLocale(domainLanguage ?? DEFAULT_DOMAIN_LANGUAGE)
}

export const normalizeBasePath = (basePath?: string | null): string => {
  const sanitized = sanitizePathInput(basePath ?? DEFAULT_DOCS_BASE_PATH)

  if (!sanitized) {
    return DEFAULT_DOCS_BASE_PATH
  }

  const normalized = sanitized.startsWith('/') ? sanitized : `/${sanitized}`

  if (!normalized.startsWith(DEFAULT_DOCS_BASE_PATH)) {
    return DEFAULT_DOCS_BASE_PATH
  }

  return normalized === '/'
    ? DEFAULT_DOCS_BASE_PATH
    : normalized.replace(/\/+$/, '')
}

export const normalizeSlugOrPath = (slugOrPath?: string | null): string =>
  normalizePathSegment(slugOrPath)

export const resolveDocPath = ({
  locale,
  slugOrPath,
  basePath,
}: {
  locale?: string | null
  slugOrPath: string
  basePath?: string | null
}): string => {
  const normalizedLocale = normalizeDocsLocale(locale)
  const normalizedBasePath = normalizeBasePath(basePath)
  const normalizedSlug = normalizeSlugOrPath(slugOrPath)

  const basePrefix = normalizedBasePath.replace(/\/+$/, '')
  let slugPath = normalizedSlug ? `/${normalizedSlug}` : ''

  if (slugPath.startsWith(`${basePrefix}/`)) {
    slugPath = slugPath.slice(basePrefix.length)
  } else if (slugPath === basePrefix) {
    slugPath = ''
  }

  const localePrefixMatch = slugPath.match(/^\/(en|fr)(\/|$)/)

  if (localePrefixMatch) {
    slugPath = slugPath.replace(/^\/(en|fr)/, '')
  }

  if (slugPath.startsWith(`/${normalizedLocale}/`)) {
    slugPath = slugPath.slice(normalizedLocale.length + 2)
  } else if (slugPath === `/${normalizedLocale}`) {
    slugPath = ''
  } else {
    slugPath = slugPath.replace(/^\/+/, '')
  }

  const resolvedSlug = slugPath || 'index'

  return `${basePrefix}/${normalizedLocale}/${resolvedSlug}`
}

export const isAllowedPath = (path: string, basePath?: string | null) => {
  const normalizedBasePath = normalizeBasePath(basePath)
  return path.startsWith(`${normalizedBasePath}/`)
}

const deriveTitleFromPath = (path: string): string => {
  const segments = path.split('/').filter(Boolean)
  const lastSegment = segments[segments.length - 1] ?? 'Document'

  return lastSegment
    .replace(/[-_]/g, ' ')
    .replace(/\b\w/g, char => char.toUpperCase())
}

const buildNormalizedDoc = (doc: DocsDoc): DocsDoc => {
  const titleCandidate =
    doc.title ??
    (doc.body &&
      typeof doc.body === 'object' &&
      (doc.body as { toc?: { links?: Array<{ text?: string }> } }).toc
        ?.links?.[0]?.text) ??
    null

  const derivedTitle = titleCandidate || deriveTitleFromPath(doc.path)
  const derivedDescription =
    doc.description?.trim() ||
    extractPlainText(doc.body).slice(0, 180).trim() ||
    null

  return {
    ...doc,
    title: derivedTitle,
    description: derivedDescription,
    tags: (doc.tags ?? []).filter(Boolean),
  }
}

const isDraftAllowed = () => import.meta.dev

const filterDrafts = (docs: DocsDoc[]) =>
  docs.filter(doc => (doc.draft ? isDraftAllowed() : true))

const sortDocs = (docs: DocsDoc[]) =>
  [...docs].sort((a, b) => {
    const weightA = a.weight ?? 0
    const weightB = b.weight ?? 0

    if (weightA !== weightB) {
      return weightA - weightB
    }

    return (a.title ?? '').localeCompare(b.title ?? '')
  })

const getDocPathInfo = (docPath: string, basePath?: string | null) => {
  const normalizedBasePath = normalizeBasePath(basePath)
  const segments = docPath.split('/').filter(Boolean)
  const baseSegments = normalizedBasePath.split('/').filter(Boolean)
  const localeIndex = baseSegments.length
  const locale = segments[localeIndex] as DocsLocale | undefined
  const slugSegments = segments.slice(localeIndex + 1)

  return {
    basePath: normalizedBasePath,
    locale,
    slug: slugSegments.join('/'),
  }
}

export const useDocsContent = () => {
  const requestURL = useSafeRequestURL()

  const defaultLocale = computed(() => resolveLocaleFromRequest())

  const buildCanonicalUrl = ({
    baseUrl,
    docPath,
  }: {
    baseUrl?: string | null
    docPath: string
  }): string | null => {
    const origin = baseUrl ?? requestURL?.origin

    if (!origin) {
      return null
    }

    const normalizedPath = sanitizePathInput(docPath)

    if (!normalizedPath) {
      return null
    }

    try {
      return new URL(normalizedPath, origin).toString()
    } catch {
      return null
    }
  }

  const buildHreflangLinks = ({
    docPath,
    baseUrl,
    availableLocales,
  }: {
    docPath: string
    baseUrl?: string | null
    availableLocales: DocsLocale[]
  }): Array<{ hreflang: string; href: string }> => {
    const origin = baseUrl ?? requestURL?.origin

    if (!origin) {
      return []
    }

    const { basePath, slug } = getDocPathInfo(docPath)

    return availableLocales.map(locale => ({
      hreflang: locale,
      href: new URL(`${basePath}/${locale}/${slug}`, origin).toString(),
    }))
  }

  const getDocByPath = async ({
    path,
    fields,
  }: {
    path: string
    fields?: string[]
  }) => {
    if (!isAllowedPath(path)) {
      return null
    }

    const query = queryCollection('docs').where('path', '=', path)
    const resolvedQuery =
      fields && fields.length > 0 ? query.select(fields) : query

    const doc = (await resolvedQuery.first()) as DocsDoc | null

    if (!doc) {
      return null
    }

    if (doc.draft && !isDraftAllowed()) {
      return null
    }

    return buildNormalizedDoc(doc)
  }

  const listDocs = async ({
    locale,
    basePath,
  }: {
    locale?: DocsLocale | null
    basePath?: string | null
  }) => {
    const resolvedLocale = normalizeDocsLocale(locale ?? defaultLocale.value)
    const resolvedBasePath = normalizeBasePath(basePath)
    const prefix = `${resolvedBasePath}/${resolvedLocale}/`
    const docs = (await queryCollection('docs')
      .where('path', 'LIKE', `${prefix}%`)
      .all()) as DocsDoc[]

    return sortDocs(filterDrafts(docs)).map(buildNormalizedDoc)
  }

  const getNavigationTree = async ({
    locale,
    basePath,
  }: {
    locale?: DocsLocale | null
    basePath?: string | null
  }) => {
    const docs = await listDocs({ locale, basePath })
    const resolvedLocale = normalizeDocsLocale(locale ?? defaultLocale.value)
    const resolvedBasePath = normalizeBasePath(basePath)
    const root: DocsNavigationNode = {
      id: `${resolvedBasePath}/${resolvedLocale}`,
      title: resolvedLocale.toUpperCase(),
      children: [],
    }

    const nodeMap = new Map<string, DocsNavigationNode>()
    nodeMap.set(root.id, root)

    docs.forEach(doc => {
      const { slug } = getDocPathInfo(doc.path, resolvedBasePath)
      const segments = slug.split('/').filter(Boolean)
      let currentId = root.id

      segments.forEach((segment, index) => {
        const parent = nodeMap.get(currentId)

        if (!parent) {
          return
        }

        const nextId = `${currentId}/${segment}`
        let node = nodeMap.get(nextId)

        if (!node) {
          node = {
            id: nextId,
            title: segment
              .replace(/[-_]/g, ' ')
              .replace(/\b\w/g, char => char.toUpperCase()),
            children: [],
          }
          parent.children.push(node)
          nodeMap.set(nextId, node)
        }

        if (index === segments.length - 1) {
          node.path = doc.path
          node.icon = doc.icon
          node.title = doc.title ?? node.title
        }

        currentId = nextId
      })
    })

    return root
  }

  const getSearchSections = async ({
    locale,
    basePath,
  }: {
    locale?: DocsLocale | null
    basePath?: string | null
  }): Promise<DocsSearchSection[]> => {
    const docs = await listDocs({ locale, basePath })

    return docs.map(doc => ({
      path: doc.path,
      title: doc.title ?? deriveTitleFromPath(doc.path),
      description: doc.description ?? null,
      tags: doc.tags ?? [],
      content: extractPlainText(doc.body ?? ''),
    }))
  }

  return {
    defaultLocale,
    buildCanonicalUrl,
    buildHreflangLinks,
    getDocByPath,
    getNavigationTree,
    getSearchSections,
    listDocs,
  }
}

export type ContentScope = 'public' | 'admin'

export interface ContentCatalogItem {
  id: string
  title: string
  description: string
  path: string
  locale: string
  slug: string
  section: string
  tags: string[]
  scope: ContentScope
  content: string
}


const extractText = (value: unknown): string => {
  if (typeof value === 'string') {
    return value
  }

  if (Array.isArray(value)) {
    return value.map((item) => extractText(item)).join(' ')
  }

  if (value && typeof value === 'object') {
    return Object.values(value as Record<string, unknown>)
      .map((item) => extractText(item))
      .join(' ')
  }

  return ''
}

interface PagesCollectionItem {
  id: string
  path: string
  title: string
  description: string
  tags: string[]
  scope: ContentScope
  body: unknown
}

export function useContentCatalog() {
  const { locale, defaultLocale } = useI18n()

  const { data, pending, error, refresh } = useAsyncData(
    () => `content-catalog:${locale.value}`,
    async () => {
      const docs = await $fetch<PagesCollectionItem[]>('/api/content/catalog')

      return docs
        .map((doc: PagesCollectionItem) => {
          const fullPath = doc.path || ''
          const pathSegments = fullPath.split('/').filter(Boolean)
          const localizedSegment = pathSegments[0] || defaultLocale
          const slugParts = pathSegments.slice(1)

          return {
            id: doc.id || fullPath,
            title: doc.title || slugParts.at(-1) || 'Untitled',
            description: doc.description || '',
            path: fullPath,
            locale: localizedSegment,
            slug: slugParts.join('/'),
            section: slugParts[0] || 'root',
            tags: Array.isArray(doc.tags) ? doc.tags : [],
            scope: doc.scope === 'admin' ? 'admin' : 'public',
            content: extractText(doc.body)
          } satisfies ContentCatalogItem
        })
        .filter((doc: ContentCatalogItem) => doc.locale === locale.value)
        .sort((left: ContentCatalogItem, right: ContentCatalogItem) => left.slug.localeCompare(right.slug))
    },
    {
      watch: [locale]
    }
  )

  return {
    items: data,
    pending,
    error,
    refresh
  }
}

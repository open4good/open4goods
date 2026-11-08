/**
 * SEO composable for PageHeader component
 *
 * Automatically generates:
 * - Meta tags (title, description, OG tags)
 * - Structured data (JSON-LD for WebPage, Article, BreadcrumbList)
 * - Canonical URL
 *
 * @module components/shared/header/composables
 */

import type {
  PageHeaderBreadcrumbItem,
  PageHeaderHeadingLevel,
  PageHeaderSchemaType,
} from '../types'

export interface UseHeaderSeoOptions {
  /** Page title */
  title: string
  /** Page subtitle */
  subtitle?: string
  /** Page description */
  description?: string
  /** Heading level (h1-h6) */
  headingLevel: PageHeaderHeadingLevel
  /** Schema.org type */
  schemaType?: PageHeaderSchemaType
  /** Breadcrumb navigation items */
  breadcrumbs?: PageHeaderBreadcrumbItem[]
  /** Open Graph image URL */
  ogImage?: string
}

/**
 * Unhead options for the header's head entries. They are fallbacks: a page
 * that declares its own title, description or canonical keeps them, because
 * a child component's entry would otherwise win the tag deduplication.
 */
export const HEADER_SEO_HEAD_OPTIONS = { tagPriority: 'low' } as const

/**
 * Removes trailing slashes from a pathname, keeping the root path as `/`.
 */
export const normalizeCanonicalPathname = (pathname: string): string =>
  pathname.replace(/\/+$/u, '') || '/'

/**
 * Generates SEO metadata and structured data for page headers
 */
export function useHeaderSeo(options: UseHeaderSeoOptions) {
  const { locale } = useI18n()
  const requestURL = useRequestURL()

  // Canonical URL: keep origin + pathname and drop query/hash noise and the
  // trailing slash, so `/foo/` does not declare itself as a second canonical.
  const canonicalUrl = computed(() => {
    const url = new URL(
      normalizeCanonicalPathname(requestURL.pathname),
      requestURL.origin
    )
    return url.toString()
  })

  // Meta description (subtitle > description fallback)
  const metaDescription = computed(
    () => options.subtitle || options.description || ''
  )

  // Structured Data: Breadcrumb List (if breadcrumbs provided)
  const breadcrumbJsonLd = computed(() => {
    if (!options.breadcrumbs?.length) {
      return null
    }

    return {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: options.breadcrumbs.map((item, index) => ({
        '@type': 'ListItem',
        position: index + 1,
        name: item.label,
        item: item.href
          ? new URL(item.href, requestURL.origin).toString()
          : undefined,
      })),
    }
  })

  // Structured Data: Main entity (WebPage, Article, etc.)
  const mainEntityJsonLd = computed(() => {
    const schemaType = options.schemaType ?? 'WebPage'

    const baseSchema = {
      '@context': 'https://schema.org',
      '@type': schemaType,
      name: options.title,
      description: metaDescription.value,
      url: canonicalUrl.value,
      inLanguage: locale.value,
    }

    // Add breadcrumb reference if available
    if (breadcrumbJsonLd.value) {
      return {
        ...baseSchema,
        breadcrumb: breadcrumbJsonLd.value,
      }
    }

    return baseSchema
  })

  // Set SEO meta tags (fallbacks, see HEADER_SEO_HEAD_OPTIONS)
  useSeoMeta(
    {
      title: () => options.title,
      description: () => metaDescription.value || undefined,
      ogTitle: () => options.title,
      ogDescription: () => metaDescription.value || undefined,
      ogUrl: () => canonicalUrl.value,
      ogType: () =>
        options.schemaType === 'Article' ? 'article' : ('website' as const),
      ogImage: () => options.ogImage,
      ogLocale: () => locale.value.replace('-', '_'),
    },
    HEADER_SEO_HEAD_OPTIONS
  )

  // Set head tags (canonical + JSON-LD, fallbacks as well)
  useHead(
    () => ({
      link: [{ rel: 'canonical', href: canonicalUrl.value }],
      script: [
        {
          key: 'page-header-main-entity-jsonld',
          type: 'application/ld+json',
          innerHTML: JSON.stringify(mainEntityJsonLd.value),
        },
        // Separate breadcrumb JSON-LD only if not included in main entity
        ...(breadcrumbJsonLd.value && !options.schemaType
          ? [
              {
                key: 'page-header-breadcrumb-jsonld',
                type: 'application/ld+json',
                innerHTML: JSON.stringify(breadcrumbJsonLd.value),
              },
            ]
          : []),
      ],
    }),
    HEADER_SEO_HEAD_OPTIONS
  )

  return {
    canonicalUrl,
    metaDescription,
    breadcrumbJsonLd,
    mainEntityJsonLd,
  }
}

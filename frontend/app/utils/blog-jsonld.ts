import { compactJsonLd, type JsonLdValue } from './product-jsonld'

export interface BlogJsonLdSiteInfo {
  name: string
  origin: string
  logoUrl?: string
  sameAs?: string[]
}

export interface BlogJsonLdBreadcrumb {
  name: string
  link?: string
}

export interface BlogJsonLdPosting {
  headline?: string
  description?: string
  url?: string
  image?: string | string[]
  datePublished?: string
  dateModified?: string
  author?: string
}

interface BlogJsonLdBaseInput {
  canonicalUrl: string
  locale: string
  site: BlogJsonLdSiteInfo
  breadcrumbs: BlogJsonLdBreadcrumb[]
}

export interface BlogJsonLdCollectionInput extends BlogJsonLdBaseInput {
  pageTitle: string
  description?: string
  about?: string
  posts: BlogJsonLdPosting[]
}

export interface BlogJsonLdArticleInput extends BlogJsonLdBaseInput {
  pageTitle: string
  description?: string
  article: BlogJsonLdPosting
}

const isNonEmptyString = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0

const normalizeString = (value: unknown): string | undefined =>
  isNonEmptyString(value) ? value.trim() : undefined

const toAbsoluteUrl = (origin: string, value: string): string | undefined => {
  try {
    return new URL(value, origin).toString()
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to build absolute URL for blog JSON-LD.', error)
    }
    return undefined
  }
}

const buildOrganizationEntry = (site: BlogJsonLdSiteInfo) =>
  compactJsonLd({
    '@type': 'Organization',
    '@id': `${site.origin}#organization`,
    name: normalizeString(site.name),
    url: site.origin,
    logo: normalizeString(site.logoUrl),
    sameAs: site.sameAs
      ?.map(value => normalizeString(value))
      .filter((value): value is string => Boolean(value)),
  })

const buildBreadcrumbEntry = (
  canonicalUrl: string,
  siteOrigin: string,
  breadcrumbs: BlogJsonLdBreadcrumb[]
) =>
  compactJsonLd({
    '@type': 'BreadcrumbList',
    '@id': `${canonicalUrl}#breadcrumb`,
    itemListElement: breadcrumbs.map((crumb, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: normalizeString(crumb.name),
      item: crumb.link ? toAbsoluteUrl(siteOrigin, crumb.link) : undefined,
    })),
  })

const buildBlogPostingEntry = (siteOrigin: string, post: BlogJsonLdPosting) => {
  const url = normalizeString(post.url)
  const absoluteUrl = url ? toAbsoluteUrl(siteOrigin, url) ?? url : undefined

  return compactJsonLd({
    '@type': 'BlogPosting',
    '@id': absoluteUrl ? `${absoluteUrl}#blogposting` : undefined,
    headline: normalizeString(post.headline),
    description: normalizeString(post.description),
    url: absoluteUrl,
    image: Array.isArray(post.image)
      ? post.image
          .map(value => normalizeString(value))
          .filter((value): value is string => Boolean(value))
      : normalizeString(post.image),
    datePublished: normalizeString(post.datePublished),
    dateModified: normalizeString(post.dateModified),
    author: normalizeString(post.author)
      ? {
          '@type': 'Person',
          name: normalizeString(post.author),
        }
      : undefined,
  })
}

const buildWebsiteEntry = (site: BlogJsonLdSiteInfo) =>
  compactJsonLd({
    '@type': 'WebSite',
    '@id': `${site.origin}#website`,
    url: site.origin,
    name: normalizeString(site.name),
    publisher: { '@id': `${site.origin}#organization` },
  })

export const buildBlogCollectionJsonLd = (
  input: BlogJsonLdCollectionInput
): Record<string, JsonLdValue> | null => {
  const organizationEntry = buildOrganizationEntry(input.site)
  const breadcrumbEntry = buildBreadcrumbEntry(
    input.canonicalUrl,
    input.site.origin,
    input.breadcrumbs
  )
  const websiteEntry = buildWebsiteEntry(input.site)

  const pageEntry = compactJsonLd({
    '@type': 'CollectionPage',
    '@id': `${input.canonicalUrl}#webpage`,
    url: input.canonicalUrl,
    name: normalizeString(input.pageTitle),
    description: normalizeString(input.description),
    inLanguage: normalizeString(input.locale),
    isPartOf: { '@id': `${input.site.origin}#website` },
    about: normalizeString(input.about),
  })

  const blogEntry = compactJsonLd({
    '@type': 'Blog',
    '@id': `${input.canonicalUrl}#blog`,
    name: normalizeString(input.pageTitle),
    publisher: { '@id': `${input.site.origin}#organization` },
  })

  const blogPosts = input.posts
    .map(post => buildBlogPostingEntry(input.site.origin, post))
    .filter(Boolean)

  const graphEntries = compactJsonLd([
    organizationEntry,
    websiteEntry,
    breadcrumbEntry,
    blogEntry,
    pageEntry
      ? {
          ...pageEntry,
          mainEntity: blogEntry ? { '@id': `${input.canonicalUrl}#blog` } : undefined,
          hasPart: blogPosts.length ? blogPosts : undefined,
        }
      : undefined,
    ...blogPosts,
  ]) as JsonLdValue

  const graph = compactJsonLd({
    '@context': 'https://schema.org',
    '@graph': graphEntries,
  })

  return (graph as Record<string, JsonLdValue>) ?? null
}

export const buildBlogArticleJsonLd = (
  input: BlogJsonLdArticleInput
): Record<string, JsonLdValue> | null => {
  const organizationEntry = buildOrganizationEntry(input.site)
  const breadcrumbEntry = buildBreadcrumbEntry(
    input.canonicalUrl,
    input.site.origin,
    input.breadcrumbs
  )
  const websiteEntry = buildWebsiteEntry(input.site)

  const webPageEntry = compactJsonLd({
    '@type': 'WebPage',
    '@id': `${input.canonicalUrl}#webpage`,
    url: input.canonicalUrl,
    name: normalizeString(input.pageTitle),
    description: normalizeString(input.description),
    inLanguage: normalizeString(input.locale),
    isPartOf: { '@id': `${input.site.origin}#website` },
  })

  const blogPostingEntry = buildBlogPostingEntry(input.site.origin, {
    ...input.article,
    url: input.article.url ?? input.canonicalUrl,
  })

  const graphEntries = compactJsonLd([
    organizationEntry,
    websiteEntry,
    breadcrumbEntry,
    blogPostingEntry
      ? {
          ...blogPostingEntry,
          mainEntityOfPage: { '@id': `${input.canonicalUrl}#webpage` },
          publisher: { '@id': `${input.site.origin}#organization` },
        }
      : undefined,
    webPageEntry
      ? {
          ...webPageEntry,
          mainEntity: blogPostingEntry
            ? { '@id': blogPostingEntry['@id'] }
            : undefined,
        }
      : undefined,
  ]) as JsonLdValue

  const graph = compactJsonLd({
    '@context': 'https://schema.org',
    '@graph': graphEntries,
  })

  return (graph as Record<string, JsonLdValue>) ?? null
}

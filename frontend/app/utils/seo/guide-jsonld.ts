export interface GuideArticleJsonLdInput {
  url: string
  headline: string
  description?: string | null
  image?: string | null
  datePublished?: string | null
  dateModified?: string | null
  inLanguage?: string | null
  /** Site origin, used to reference the Nudger organization. */
  origin: string
}

/**
 * schema.org `Article` of a buying guide. The author is the Nudger
 * organization, not a person.
 */
export const buildGuideArticleJsonLd = (input: GuideArticleJsonLdInput) => {
  const modified = input.dateModified ?? input.datePublished ?? undefined
  const published = input.datePublished ?? modified

  return {
    '@context': 'https://schema.org',
    '@type': 'Article',
    mainEntityOfPage: { '@type': 'WebPage', '@id': input.url },
    url: input.url,
    headline: input.headline,
    ...(input.description ? { description: input.description } : {}),
    ...(input.image ? { image: [input.image] } : {}),
    ...(published ? { datePublished: published } : {}),
    ...(modified ? { dateModified: modified } : {}),
    ...(input.inLanguage ? { inLanguage: input.inLanguage } : {}),
    author: {
      '@type': 'Organization',
      name: 'Nudger',
      url: input.origin,
    },
    publisher: { '@id': `${input.origin}#organization` },
  }
}

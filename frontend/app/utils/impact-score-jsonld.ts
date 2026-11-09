export interface ImpactScoreMethodJsonLdInput {
  type: 'TechArticle' | 'WebPage'
  name: string
  description: string
  url?: string | null
  dateModified?: string
  version?: string
  inLanguage?: string
  aboutName?: string
}

/**
 * JSON-LD of the Impact Score method pages. Deliberately never emits
 * `Rating` / `AggregateRating`: the Impact Score is a computed indicator, not a
 * customer review.
 */
export const buildImpactScoreMethodJsonLd = (
  input: ImpactScoreMethodJsonLdInput
): Record<string, unknown> => ({
  '@context': 'https://schema.org',
  '@type': input.type,
  name: input.name,
  headline: input.name,
  description: input.description,
  ...(input.url ? { url: input.url } : {}),
  ...(input.dateModified ? { dateModified: input.dateModified } : {}),
  ...(input.version ? { version: input.version } : {}),
  ...(input.inLanguage ? { inLanguage: input.inLanguage } : {}),
  ...(input.aboutName
    ? { about: { '@type': 'Thing', name: input.aboutName } }
    : {}),
})

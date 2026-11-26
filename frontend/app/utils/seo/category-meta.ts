import type {
  VerticalConfigFullDto,
  VerticalSubCategoryDto,
} from '~~/shared/api-client'

export interface CategorySeoMetaOptions {
  category?: VerticalConfigFullDto | null
  subCategory?: VerticalSubCategoryDto | null
  siteName: string
}

export interface CategorySeoMeta {
  title: string
  description: string
  ogTitle: string
  ogDescription: string
}

const stripMarkdownSyntax = (value?: string | null) =>
  (value ?? '')
    .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[`*_~>#]+/g, '')
    .replace(/\s+/g, ' ')
    .trim()

export const META_DESCRIPTION_MAX_LENGTH = 155

/**
 * Clamp a meta description to the length Google shows, cutting on a word
 * boundary and ending with an ellipsis.
 */
export const clampMetaDescription = (
  value: string,
  maxLength = META_DESCRIPTION_MAX_LENGTH
) => {
  if (value.length <= maxLength) {
    return value
  }

  const cut = value.slice(0, maxLength - 1)
  const lastSpace = cut.lastIndexOf(' ')
  const base = lastSpace > maxLength / 2 ? cut.slice(0, lastSpace) : cut

  return `${base.replace(/[\s,;:.\-–]+$/, '')}…`
}

const firstText = (...values: Array<string | null | undefined>) =>
  values
    .find(value => typeof value === 'string' && value.trim().length > 0)
    ?.trim() ?? ''

/**
 * Resolve category page SEO metadata with backend fields first and visible
 * content fields as resilience fallbacks.
 */
export const resolveCategorySeoMeta = ({
  category,
  subCategory,
  siteName,
}: CategorySeoMetaOptions): CategorySeoMeta => {
  const title = firstText(
    subCategory?.metaTitle,
    subCategory?.h1Title,
    category?.verticalMetaTitle,
    category?.verticalHomeTitle,
    siteName
  )

  const description = stripMarkdownSyntax(
    firstText(
      subCategory?.metaDescription,
      subCategory?.description,
      category?.verticalMetaDescription,
      category?.verticalHomeDescription
    )
  )

  const ogTitle = firstText(
    subCategory?.metaOpenGraphTitle,
    subCategory?.metaTitle,
    subCategory?.h1Title,
    category?.verticalMetaOpenGraphTitle,
    title
  )

  const ogDescription = stripMarkdownSyntax(
    firstText(
      subCategory?.metaOpenGraphDescription,
      subCategory?.metaDescription,
      subCategory?.description,
      category?.verticalMetaOpenGraphDescription,
      description
    )
  )

  return {
    title,
    description: clampMetaDescription(description),
    ogTitle,
    ogDescription: clampMetaDescription(ogDescription),
  }
}

export interface CategoryEcoscoreSeoMetaOptions {
  /** Localised title, built from the category label. */
  title: string
  /** Localised description. Never the long category description. */
  description: string
}

/**
 * SEO metadata of the `/{vertical}/ecoscore` page. It gets its own title and
 * description so it does not duplicate the category page (GOU-2695).
 */
export const resolveCategoryEcoscoreSeoMeta = ({
  title,
  description,
}: CategoryEcoscoreSeoMetaOptions): CategorySeoMeta => {
  const cleanTitle = stripMarkdownSyntax(title)
  const cleanDescription = clampMetaDescription(
    stripMarkdownSyntax(description)
  )

  return {
    title: cleanTitle,
    description: cleanDescription,
    ogTitle: cleanTitle,
    ogDescription: cleanDescription,
  }
}

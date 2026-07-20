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

const firstText = (...values: Array<string | null | undefined>) =>
  values.find(value => typeof value === 'string' && value.trim().length > 0)
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
    description,
    ogTitle,
    ogDescription,
  }
}

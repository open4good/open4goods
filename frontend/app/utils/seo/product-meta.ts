export interface ProductMetaTemplates {
  withImpactFull: string
  withImpactCompact: string
  withImpactMinimal: string
  withoutImpactFull: string
  withoutImpactCompact: string
  withoutImpactMinimal: string
}

export interface ProductMetaDescriptionTemplates {
  withImpact: string
  withImpactNoBrand: string
  withImpactVertical: string
  withImpactVerticalNoBrand: string
  withoutImpact: string
  withoutImpactNoBrand: string
  withoutImpactVertical: string
  withoutImpactVerticalNoBrand: string
}

export interface ProductMetaBuildOptions {
  productName: string
  brandModel: string
  score: number | null
  verticalTitle?: string
  maxTitleLength?: number
  maxDescriptionLength?: number
  titleTemplates: ProductMetaTemplates
  descriptionTemplates: ProductMetaDescriptionTemplates
}

const DEFAULT_TITLE_LENGTH = 60
const DEFAULT_DESCRIPTION_LENGTH = 160

const normalizeText = (value: string): string =>
  value.trim().replace(/\s+/g, ' ')

const formatScore = (score: number | null): string => {
  if (typeof score !== 'number') {
    return ''
  }

  return Number(score.toFixed(1)).toString()
}

const truncateAtWordBoundary = (value: string, maxLength: number): string => {
  const normalized = normalizeText(value)

  if (normalized.length <= maxLength) {
    return normalized
  }

  const safeLimit = Math.max(1, maxLength - 1)
  const truncated = normalized.slice(0, safeLimit)
  const lastSpace = truncated.lastIndexOf(' ')
  const compact =
    lastSpace > Math.floor(safeLimit * 0.6)
      ? truncated.slice(0, lastSpace)
      : truncated

  return `${compact.trimEnd()}…`
}

const resolveTitleCandidates = (options: ProductMetaBuildOptions): string[] => {
  const productName = normalizeText(options.productName)
  const brandModel = normalizeText(options.brandModel)
  const score = formatScore(options.score)

  if (!productName.length) {
    return [brandModel]
  }

  if (score) {
    return [
      options.titleTemplates.withImpactFull,
      options.titleTemplates.withImpactCompact,
      options.titleTemplates.withImpactMinimal,
    ]
      .map(template =>
        template
          .replaceAll('{productName}', productName)
          .replaceAll('{score}', score)
          .replaceAll('{brandModel}', brandModel)
      )
      .map(normalizeText)
      .filter(Boolean)
  }

  return [
    options.titleTemplates.withoutImpactFull,
    options.titleTemplates.withoutImpactCompact,
    options.titleTemplates.withoutImpactMinimal,
  ]
    .map(template =>
      template
        .replaceAll('{productName}', productName)
        .replaceAll('{brandModel}', brandModel)
    )
    .map(normalizeText)
    .filter(Boolean)
}

/**
 * Build localized SEO metadata for product pages with explicit variants when
 * an impact score exists and when it does not.
 */
export const buildProductMeta = (options: ProductMetaBuildOptions) => {
  const maxTitleLength = options.maxTitleLength ?? DEFAULT_TITLE_LENGTH
  const maxDescriptionLength =
    options.maxDescriptionLength ?? DEFAULT_DESCRIPTION_LENGTH
  const score = formatScore(options.score)
  const productName = normalizeText(options.productName)
  const brandModel = normalizeText(options.brandModel)
  const verticalTitle = normalizeText(options.verticalTitle ?? '')

  const titleCandidates = resolveTitleCandidates(options)
  const matchingTitle =
    titleCandidates.find(candidate => candidate.length <= maxTitleLength) ??
    titleCandidates.at(-1) ??
    productName

  const title = truncateAtWordBoundary(matchingTitle, maxTitleLength)

  const sameProductAndBrandModel =
    brandModel.length > 0 && productName.toLowerCase() === brandModel.toLowerCase()

  const descriptionTemplate = score
    ? verticalTitle
      ? sameProductAndBrandModel
        ? options.descriptionTemplates.withImpactVerticalNoBrand
        : options.descriptionTemplates.withImpactVertical
      : sameProductAndBrandModel
        ? options.descriptionTemplates.withImpactNoBrand
        : options.descriptionTemplates.withImpact
    : verticalTitle
      ? sameProductAndBrandModel
        ? options.descriptionTemplates.withoutImpactVerticalNoBrand
        : options.descriptionTemplates.withoutImpactVertical
      : sameProductAndBrandModel
        ? options.descriptionTemplates.withoutImpactNoBrand
        : options.descriptionTemplates.withoutImpact

  const description = truncateAtWordBoundary(
    descriptionTemplate
      .replaceAll('{productName}', productName)
      .replaceAll('{score}', score)
      .replaceAll('{brandModel}', brandModel)
      .replaceAll('{verticalTitle}', verticalTitle),
    maxDescriptionLength
  )

  return {
    title,
    description,
  }
}

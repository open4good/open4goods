import type {
  FieldMetadataDto,
  ProductFieldOptionsResponse,
} from '~~/shared/api-client'

type FieldMetadataGroup = FieldMetadataDto[]

const normalizeText = (value: string | null | undefined): string => {
  return String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ')
    .toLowerCase()
}

const buildFieldDeduplicationKey = (field: FieldMetadataDto): string => {
  const mappingKey = normalizeText(field.mapping)
  if (mappingKey) {
    return `mapping:${mappingKey}`
  }

  const titleKey = normalizeText(field.title)
  if (titleKey) {
    return `title:${titleKey}`
  }

  return `fallback:${normalizeText(JSON.stringify(field))}`
}

const computeFieldQualityScore = (field: FieldMetadataDto): number => {
  let score = 0

  if (normalizeText(field.mapping)) {
    score += 4
  }

  if (normalizeText(field.description)) {
    score += 2
  }

  if (normalizeText(field.title)) {
    score += 1
  }

  return score
}

/**
 * Deduplicates field metadata entries using `mapping` as primary key and
 * normalized title as fallback key.
 */
export const deduplicateFieldMetadataList = (
  fields: FieldMetadataGroup | null | undefined
): FieldMetadataGroup => {
  if (!fields?.length) {
    return []
  }

  const deduplicated: FieldMetadataDto[] = []
  const indexByKey = new Map<string, number>()

  fields.forEach(field => {
    const key = buildFieldDeduplicationKey(field)
    const currentIndex = indexByKey.get(key)

    if (currentIndex == null) {
      indexByKey.set(key, deduplicated.length)
      deduplicated.push(field)
      return
    }

    const existing = deduplicated[currentIndex]
    const nextScore = computeFieldQualityScore(field)
    const existingScore = computeFieldQualityScore(existing)

    if (nextScore > existingScore) {
      deduplicated[currentIndex] = field
    }
  })

  return deduplicated
}

/**
 * Returns normalized field option groups to prevent duplicate filter/sort
 * entries in category pages.
 */
export const normalizeFieldOptionsResponse = (
  options: ProductFieldOptionsResponse | null | undefined
): ProductFieldOptionsResponse | null => {
  if (!options) {
    return null
  }

  return {
    global: deduplicateFieldMetadataList(options.global),
    impact: deduplicateFieldMetadataList(options.impact),
    technical: deduplicateFieldMetadataList(options.technical),
  }
}

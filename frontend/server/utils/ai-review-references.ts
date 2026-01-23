import type { AiReviewDto, AiReviewSourceDto } from '~~/shared/api-client'

const referencePattern = /\[(\s*\d+(?:\s*,\s*\d+)*)\]/g

const buildReferenceAnchor = (referenceNumber: number): string =>
  `<a class="review-ref" href="#review-ref-${referenceNumber}">[${referenceNumber}]</a>`

const collectAvailableReferences = (
  sources?: AiReviewSourceDto[] | null
): Set<number> => {
  const available = new Set<number>()

  sources?.forEach(source => {
    if (typeof source.number === 'number' && Number.isFinite(source.number)) {
      available.add(source.number)
    }
  })

  return available
}

const replaceReferences = (content: string, available: Set<number>): string =>
  content.replace(referencePattern, (_, group: string) => {
    const references = group
      .split(',')
      .map(value => Number.parseInt(value.trim(), 10))
      .filter(value => Number.isFinite(value) && available.has(value))

    if (!references.length) {
      return ''
    }

    return references.map(buildReferenceAnchor).join(', ')
  })

const replaceIfString = (
  content: string | null | undefined,
  available: Set<number>
): string | undefined => {
  if (typeof content !== 'string') {
    return content ?? undefined
  }

  return replaceReferences(content, available)
}

const replaceArray = (
  values: Array<string> | null | undefined,
  available: Set<number>
): Array<string> | undefined => {
  if (!Array.isArray(values)) {
    return values ?? undefined
  }

  return values
    .map(value => replaceIfString(value, available) ?? '')
    .filter(value => value.length > 0)
}

export const applyAiReviewReferenceLinks = (
  review: AiReviewDto | null | undefined
): void => {
  if (!review) {
    return
  }

  const available = collectAvailableReferences(review.sources)

  review.description = replaceIfString(review.description, available)
  review.shortDescription = replaceIfString(review.shortDescription, available)
  review.mediumTitle = replaceIfString(review.mediumTitle, available)
  review.shortTitle = replaceIfString(review.shortTitle, available)
  review.technicalReview = replaceIfString(review.technicalReview, available)
  review.technicalReviewNovice = replaceIfString(
    review.technicalReviewNovice,
    available
  )
  review.technicalReviewIntermediate = replaceIfString(
    review.technicalReviewIntermediate,
    available
  )
  review.technicalReviewAdvanced = replaceIfString(
    review.technicalReviewAdvanced,
    available
  )
  review.technicalShortReview = replaceIfString(
    review.technicalShortReview,
    available
  )
  review.ecologicalReview = replaceIfString(review.ecologicalReview, available)
  review.ecologicalReviewNovice = replaceIfString(
    review.ecologicalReviewNovice,
    available
  )
  review.ecologicalReviewIntermediate = replaceIfString(
    review.ecologicalReviewIntermediate,
    available
  )
  review.ecologicalReviewAdvanced = replaceIfString(
    review.ecologicalReviewAdvanced,
    available
  )
  review.communityReviewNovice = replaceIfString(
    review.communityReviewNovice,
    available
  )
  review.communityReviewIntermediate = replaceIfString(
    review.communityReviewIntermediate,
    available
  )
  review.communityReviewAdvanced = replaceIfString(
    review.communityReviewAdvanced,
    available
  )
  review.summary = replaceIfString(review.summary, available)
  review.dataQuality = replaceIfString(review.dataQuality, available)
  review.technicalOneline = replaceIfString(review.technicalOneline, available)
  review.ecologicalOneline = replaceIfString(review.ecologicalOneline, available)
  review.communityOneline = replaceIfString(review.communityOneline, available)
  review.pros = replaceArray(review.pros, available)
  review.cons = replaceArray(review.cons, available)
}

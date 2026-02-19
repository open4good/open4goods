import type {
  AggregationBucketDto,
  AggregationResponseDto,
} from '~~/shared/api-client'
import {
  hasRenderableFacetLabel,
  normalizeFacetLabel,
} from '~~/shared/utils/facet-normalization'

type FacetIssueKind =
  | 'empty_label'
  | 'taxonomy_duplicate'
  | 'source_collision'
  | 'duplicate_bucket'

const TAXONOMY_FIELDS = new Set(['googleTaxonomyId'])
const SOURCE_FIELDS = new Set(['datasource', 'offers.datasource'])

const cloneBucket = (bucket: AggregationBucketDto): AggregationBucketDto => ({
  key: bucket.key,
  to: bucket.to,
  count: bucket.count,
  missing: bucket.missing,
})

/**
 * Sanitizes term buckets so UI never receives blank labels nor duplicated labels
 * that only differ by case/accents/plural forms.
 */
export const sanitizeFacetAggregations = (
  aggregations: AggregationResponseDto[] | null | undefined
): AggregationResponseDto[] => {
  if (!aggregations?.length) {
    return []
  }

  return aggregations.map(aggregation => {
    const buckets = aggregation.buckets ?? []
    if (!buckets.length) {
      return aggregation
    }

    const deduplicated = new Map<string, AggregationBucketDto>()

    buckets.forEach(bucket => {
      if (bucket.missing) {
        return
      }

      const rawKey = String(bucket.key ?? '')
      if (!hasRenderableFacetLabel(rawKey)) {
        return
      }

      const normalizedKey = normalizeFacetLabel(rawKey)
      const current = deduplicated.get(normalizedKey)
      if (!current) {
        deduplicated.set(normalizedKey, cloneBucket(bucket))
        return
      }

      deduplicated.set(normalizedKey, {
        ...current,
        count: (current.count ?? 0) + (bucket.count ?? 0),
      })
    })

    return {
      ...aggregation,
      buckets: Array.from(deduplicated.values()),
    }
  })
}

/**
 * Emits quality warnings on suspicious facet data before UI publication.
 */
export const logFacetQualityIssues = (
  aggregations: AggregationResponseDto[] | null | undefined
) => {
  if (!aggregations?.length) {
    return
  }

  aggregations.forEach(aggregation => {
    const field = aggregation.field ?? ''
    const buckets = aggregation.buckets ?? []
    const seen = new Map<string, Set<string>>()

    buckets.forEach(bucket => {
      if (bucket.missing) {
        return
      }

      const rawKey = String(bucket.key ?? '')
      if (!hasRenderableFacetLabel(rawKey)) {
        console.warn('[facet-quality] Empty facet label removed', {
          field,
          issue: 'empty_label' as FacetIssueKind,
        })
        return
      }

      const normalized = normalizeFacetLabel(rawKey)
      const rawSet = seen.get(normalized) ?? new Set<string>()
      rawSet.add(rawKey)
      seen.set(normalized, rawSet)

      if (rawSet.size > 1) {
        const issue: FacetIssueKind = TAXONOMY_FIELDS.has(field)
          ? 'taxonomy_duplicate'
          : SOURCE_FIELDS.has(field)
            ? 'source_collision'
            : 'duplicate_bucket'

        console.warn('[facet-quality] Duplicate facet labels detected', {
          field,
          issue,
          labels: Array.from(rawSet),
        })
      }
    })
  })
}

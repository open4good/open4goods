import { describe, expect, it, vi } from 'vitest'
import type { AggregationResponseDto } from '~~/shared/api-client'
import {
  logFacetQualityIssues,
  sanitizeFacetAggregations,
} from './facet-quality'

describe('sanitizeFacetAggregations', () => {
  it('drops empty labels and merges duplicated normalized labels', () => {
    const aggregations: AggregationResponseDto[] = [
      {
        field: 'datasource',
        buckets: [
          { key: ' Amazon ', count: 2 },
          { key: 'amazon', count: 1 },
          { key: ' ', count: 5 },
        ],
      },
    ]

    const sanitized = sanitizeFacetAggregations(aggregations)

    expect(sanitized[0]?.buckets).toEqual([{ key: ' Amazon ', count: 3 }])
  })
})

describe('logFacetQualityIssues', () => {
  it('warns when duplicate taxonomy labels are detected', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})

    logFacetQualityIssues([
      {
        field: 'googleTaxonomyId',
        buckets: [
          { key: 'Télévision', count: 1 },
          { key: 'televisions', count: 1 },
        ],
      },
    ])

    expect(warnSpy).toHaveBeenCalled()
    warnSpy.mockRestore()
  })
})

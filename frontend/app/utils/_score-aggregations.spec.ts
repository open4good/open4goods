import { describe, expect, it } from 'vitest'
import { AggTypeEnum } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'
import { buildScoreAggregations } from './_score-aggregations'

describe('buildScoreAggregations', () => {
  it('builds range aggregations for provided scores using absolute values', () => {
    const aggs = buildScoreAggregations(['DURABILITY', 'REPAIRABILITY'])

    expect(aggs).toEqual([
      {
        name: 'score_DURABILITY',
        field: 'scores.DURABILITY.value',
        type: AggTypeEnum.Range,
        step: 0.5,
      },
      {
        name: 'score_REPAIRABILITY',
        field: 'scores.REPAIRABILITY.value',
        type: AggTypeEnum.Range,
        step: 0.5,
      },
    ])
  })

  it('uses the ecoscore constant for the ECOSCORE aggregation', () => {
    const aggs = buildScoreAggregations(['ECOSCORE'])

    expect(aggs).toEqual([
      {
        name: 'score_ECOSCORE',
        field: ECOSCORE_RELATIVE_FIELD,
        type: AggTypeEnum.Range,
        step: 0.5,
      },
    ])
  })
})

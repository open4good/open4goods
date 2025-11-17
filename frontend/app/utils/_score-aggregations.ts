import { AggTypeEnum, type Agg } from '~~/shared/api-client'
import { ECOSCORE_RELATIVE_FIELD } from '~/constants/scores'

export const buildScoreAggregations = (scoreIds: readonly string[]): Agg[] => {
  return scoreIds
    .map((scoreId) => (typeof scoreId === 'string' ? scoreId.trim() : ''))
    .filter((scoreId) => scoreId.length)
    .map((scoreId) => {
      const normalizedId = scoreId.toUpperCase()
      const field = normalizedId === 'ECOSCORE'
        ? ECOSCORE_RELATIVE_FIELD
        : `scores.${scoreId}.value`

      return {
        name: `score_${scoreId}`,
        field,
        type: AggTypeEnum.Range,
        step: 0.5,
      }
    })
}

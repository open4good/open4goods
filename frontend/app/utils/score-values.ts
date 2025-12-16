import type { ProductScoreDto } from 'shared/api-client'

export type ScoreValueSource =
  | 'value'
  | 'relative'
  | 'legacyRelative'
  | 'percent'
  | 'on20'

export interface ResolvedScoreValue {
  value: number
  source: ScoreValueSource
}

export const resolveScoreNumericValue = (
  score: ProductScoreDto | null | undefined
): ResolvedScoreValue | null => {
  if (!score) {
    return null
  }

  const { value, relativ, percent, on20 } = score

  if (typeof value === 'number' && Number.isFinite(value)) {
    return { value, source: 'value' }
  }

  const relativeValue = relativ?.value
  if (typeof relativeValue === 'number' && Number.isFinite(relativeValue)) {
    return { value: relativeValue, source: 'relative' }
  }

  const legacyRelative = (score as { relative?: { value?: number | null } })
    .relative?.value
  if (typeof legacyRelative === 'number' && Number.isFinite(legacyRelative)) {
    return { value: legacyRelative, source: 'legacyRelative' }
  }

  if (typeof percent === 'number' && Number.isFinite(percent)) {
    return { value: percent, source: 'percent' }
  }

  if (typeof on20 === 'number' && Number.isFinite(on20)) {
    return { value: on20, source: 'on20' }
  }

  return null
}

export const extractScoreValue = (
  score: ProductScoreDto | null | undefined
): number | null => {
  return resolveScoreNumericValue(score)?.value ?? null
}

import type { ProductAttributeSourceDto } from '~~/shared/api-client'

export type DistributionBucket = {
  label: string
  value: number
}

export type ScoreAbsoluteStats = {
  min?: number | null
  max?: number | null
  avg?: number | null
  count?: number | null
  value?: number | null
  stdDev?: number | null
}

export type ScoreNormalizationParams = {
  sigmaK?: number | null
  fixedMin?: number | null
  fixedMax?: number | null
  quantileLow?: number | null
  quantileHigh?: number | null
  mapping?: Record<string, number> | null
  constantValue?: number | null
  threshold?: number | null
  greaterIsPass?: boolean | null
}

export type ScoreNormalizationConfig = {
  method?: string | null
  params?: ScoreNormalizationParams | null
}

export type ScoreScoringConfig = {
  scale?: { min?: number | null; max?: number | null } | null
  normalization?: ScoreNormalizationConfig | null
  transform?: string | null
  missingValuePolicy?: string | null
  degenerateDistributionPolicy?: string | null
}

export type ScoreView = {
  id: string
  label: string
  description?: string | null
  relativeValue: number | null
  value?: number | null
  participateInScores?: string[] | null
  participateInACV?: string[] | null
  attributeValue?: string | null
  attributeSuffix?: string | null
  attributeSourcing?: ProductAttributeSourceDto | null
  absoluteValue?: string | number | null
  absolute?: ScoreAbsoluteStats | null
  coefficient?: number | null
  percent?: number | null
  ranking?: number | string | null
  letter?: string | null
  on20?: number | null
  distribution?: DistributionBucket[]
  energyLetter?: string | null
  energyClassDisplay?: string | null
  energyClassImage?: string | null
  metadatas?: Record<string, string> | null
  unit?: string | null
  aggregates?: Record<string, number> | null
  userBetterIs?: 'GREATER' | 'LOWER' | null
  impactBetterIs?: 'GREATER' | 'LOWER' | null
  scoring?: ScoreScoringConfig | null
  importanceDescription?: string | null
  virtual?: boolean
}

export type RankingInfo = {
  position: number
  total: number
  globalBest?: { fullSlug: string; bestName: string }
  globalBetter?: { fullSlug: string; bestName: string }
}

export type CountryInfo = {
  name: string
  flag?: string | null
}

import type { InjectionKey, Ref } from 'vue'
import type { ProductAttributeSourceDto } from '~~/shared/api-client'

/**
 * Methodology page of the product's category, provided by the ImpactScore card
 * to the criterion explanations it renders several levels below.
 */
export const impactMethodologyHrefKey: InjectionKey<Ref<string>> = Symbol(
  'impactMethodologyHref'
)

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
  icon?: string | null
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
  /** V2: criterion of the active matrix with no value on this product. */
  notFilled?: boolean
  numericMapping?: Record<string, number> | null
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

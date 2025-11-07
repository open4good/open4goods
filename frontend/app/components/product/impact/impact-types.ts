export type DistributionBucket = {
  label: string
  value: number
}

export type ScoreView = {
  id: string
  label: string
  description?: string | null
  relativeValue: number | null
  value?: number | null
  absoluteValue?: string | number | null
  coefficient?: number | null
  percent?: number | null
  ranking?: number | string | null
  letter?: string | null
  on20?: number | null
  distribution?: DistributionBucket[]
  energyLetter?: string | null
  metadatas?: Record<string, string> | null
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

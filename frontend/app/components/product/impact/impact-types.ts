export type DistributionBucket = {
  label: string
  value: number
}

export type ScoreView = {
  id: string
  label: string
  description?: string | null
  relativeValue: number | null
  absoluteValue?: string | number | null
  percent?: number | null
  ranking?: number | string | null
  letter?: string | null
  distribution?: DistributionBucket[]
  energyLetter?: string | null
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

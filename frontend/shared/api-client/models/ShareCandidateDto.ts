import type { ProductAggregatedPriceDto } from './ProductAggregatedPriceDto'
import { mapValues } from '../runtime'

export interface ShareCandidateDto {
  productId: string
  name: string
  image?: string | null
  ecoScore?: number | null
  impactScore?: number | null
  bestPrice?: ProductAggregatedPriceDto | null
  confidence?: number | null
}

export function ShareCandidateDtoFromJSON(json: any): ShareCandidateDto {
  return mapValues(json)
}

export function ShareCandidateDtoToJSON(value?: ShareCandidateDto | null): any {
  if (value === undefined) {
    return undefined
  }
  if (value === null) {
    return null
  }
  return {
    ...value,
  }
}

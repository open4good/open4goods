import { mapValues } from '../runtime'
import type { ShareCandidateDto } from './ShareCandidateDto'
import type { ShareExtractionDto } from './ShareExtractionDto'
import type { ShareResolutionStatus } from './ShareResolutionStatus'

export interface ShareResolutionResponseDto {
  token: string
  status: ShareResolutionStatus
  originUrl: string
  startedAt: string
  resolvedAt?: string | null
  extracted?: ShareExtractionDto | null
  candidates: ShareCandidateDto[]
  message?: string | null
}

export function ShareResolutionResponseDtoFromJSON(
  json: any
): ShareResolutionResponseDto {
  return mapValues(json)
}

export function ShareResolutionResponseDtoToJSON(
  value?: ShareResolutionResponseDto | null
): any {
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

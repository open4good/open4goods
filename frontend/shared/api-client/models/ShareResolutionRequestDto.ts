import { mapValues } from '../runtime'

export interface ShareResolutionRequestDto {
  url: string
  title?: string | null
  text?: string | null
}

export function ShareResolutionRequestDtoFromJSON(
  json: any
): ShareResolutionRequestDto {
  return mapValues(json)
}

export function ShareResolutionRequestDtoToJSON(
  value?: ShareResolutionRequestDto | null
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

import { mapValues } from '../runtime'

export interface ShareExtractionDto {
  gtin?: string | null
  query?: string | null
}

export function ShareExtractionDtoFromJSON(json: any): ShareExtractionDto {
  return mapValues(json)
}

export function ShareExtractionDtoToJSON(
  value?: ShareExtractionDto | null
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

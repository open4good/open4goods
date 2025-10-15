import { decompressFromBase64, compressToBase64 } from 'lz-string'
import type { FilterRequestDto, SortRequestDto } from '~~/shared/api-client'

export type CategoryViewMode = 'cards' | 'list' | 'table'

export interface CategoryHashState {
  filters?: FilterRequestDto
  search?: string
  sort?: SortRequestDto
  pageNumber?: number
  view?: CategoryViewMode
  activeSubsets?: string[]
  impactExpanded?: boolean
  technicalExpanded?: boolean
}

const removeUndefined = <T extends object>(value: T): Partial<T> => {
  return (Object.keys(value) as Array<keyof T>).reduce<Partial<T>>((accumulator, key) => {
    const entryValue = value[key]

    if (entryValue === undefined) {
      return accumulator
    }

    if (Array.isArray(entryValue) && entryValue.length === 0) {
      return accumulator
    }

    if (
      entryValue &&
      typeof entryValue === 'object' &&
      !Array.isArray(entryValue) &&
      Object.keys(entryValue as Record<string, unknown>).length === 0
    ) {
      return accumulator
    }

    ;(accumulator as Record<keyof T, T[keyof T]>)[key] = entryValue
    return accumulator
  }, {})
}

export const serializeCategoryHashState = (state: CategoryHashState): string => {
  const cleaned = removeUndefined(state)

  if (!Object.keys(cleaned).length) {
    return ''
  }

  const rawPayload = JSON.stringify(cleaned)
  return compressToBase64(rawPayload)
}

export const deserializeCategoryHashState = (payload: string | null | undefined): CategoryHashState | null => {
  if (!payload || payload.trim().length === 0) {
    return null
  }

  try {
    const json = decompressFromBase64(payload)
    if (!json) {
      return null
    }

    const parsed = JSON.parse(json) as CategoryHashState
    return parsed ?? null
  } catch (error) {
    if (import.meta.server) {
      console.error('Failed to decode category hash state payload.', error)
    }

    return null
  }
}

export const buildCategoryHash = (state: CategoryHashState): string => {
  const serialized = serializeCategoryHashState(state)
  return serialized ? `#${serialized}` : ''
}

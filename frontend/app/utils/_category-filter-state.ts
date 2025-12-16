import LZString from 'lz-string'
import type { FilterRequestDto, SortRequestDto } from '~~/shared/api-client'

const { decompressFromBase64, compressToBase64 } = LZString

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

const HASH_PREFIX = 'state-'

const toBase64Url = (value: string): string => {
  return value.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/u, '')
}

const fromBase64Url = (value: string): string => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const paddingLength = normalized.length % 4
  const padding = paddingLength === 0 ? '' : '='.repeat(4 - paddingLength)
  return `${normalized}${padding}`
}

const removeUndefined = <T extends object>(value: T): Partial<T> => {
  return (Object.keys(value) as Array<keyof T>).reduce<Partial<T>>(
    (accumulator, key) => {
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
    },
    {}
  )
}

export const serializeCategoryHashState = (
  state: CategoryHashState
): string => {
  const cleaned = removeUndefined(state)

  if (!Object.keys(cleaned).length) {
    return ''
  }

  const rawPayload = JSON.stringify(cleaned)
  return compressToBase64(rawPayload)
}

export const deserializeCategoryHashState = (
  payload: string | null | undefined
): CategoryHashState | null => {
  if (!payload || payload.trim().length === 0) {
    return null
  }

  let base64Payload: string

  if (payload.startsWith(HASH_PREFIX)) {
    const trimmed = payload.slice(HASH_PREFIX.length)
    if (!trimmed) {
      return null
    }

    base64Payload = fromBase64Url(trimmed)
  } else {
    try {
      base64Payload = decodeURIComponent(payload)
    } catch (error) {
      if (import.meta.server) {
        console.error('Failed to decode category hash state payload.', error)
      }

      return null
    }
  }

  try {
    const json = decompressFromBase64(base64Payload)
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
  if (!serialized) {
    return ''
  }

  const safePayload = toBase64Url(serialized)
  return `#${HASH_PREFIX}${safePayload}`
}

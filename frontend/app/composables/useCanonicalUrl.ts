import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import { useRequestURL, useRoute } from '#imports'

const useSafeRequestURL = (): URL | null => {
  try {
    return useRequestURL()
  } catch {
    if (import.meta.client && typeof window !== 'undefined' && window.location) {
      try {
        return new URL(window.location.href)
      } catch {
        return null
      }
    }

    return null
  }
}

export const useCanonicalUrl = (
  path?: MaybeRefOrGetter<string | null | undefined>,
) => {
  const route = useRoute()
  const requestURL = useSafeRequestURL()

  return computed(() => {
    const origin = requestURL?.origin

    if (!origin) {
      return null
    }

    const rawPath = path ? toValue(path) ?? '' : route.fullPath ?? ''
    const [pathWithoutHash] = rawPath.split('#')
    const normalizedPath = pathWithoutHash && pathWithoutHash.length > 0 ? pathWithoutHash : '/'

    try {
      return new URL(normalizedPath, origin).toString()
    } catch {
      return null
    }
  })
}

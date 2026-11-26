import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import { useRequestURL, useRoute } from '#imports'
import { buildCanonicalUrl } from '~/utils/seo/canonical'

const useSafeRequestURL = (): URL | null => {
  try {
    return useRequestURL()
  } catch {
    if (
      import.meta.client &&
      typeof window !== 'undefined' &&
      window.location
    ) {
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
  path?: MaybeRefOrGetter<string | null | undefined>
) => {
  const route = useRoute()
  const requestURL = useSafeRequestURL()

  return computed(() => {
    const origin = requestURL?.origin

    if (!origin) {
      return null
    }

    const rawPath = path ? (toValue(path) ?? '') : (route.fullPath ?? '')

    return buildCanonicalUrl(rawPath, origin)
  })
}

import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import type { XwikiContentBlocDto } from '~~/shared/api-client'

/**
 * Composable to retrieve dynamic content blocs with SSR-aware caching.
 */
export const useContentBloc = async (
  blocId: MaybeRefOrGetter<string | null | undefined>
) => {
  const key = computed(() => {
    const id = toValue(blocId)
    return id ? `content-bloc:${id}` : 'content-bloc:empty'
  })

  const asyncState = await useAsyncData<XwikiContentBlocDto | null>(
    () => key.value,
    async () => {
      const id = toValue(blocId)
      if (!id) {
        return null
      }

      const headers = useRequestHeaders(['host', 'x-forwarded-host'])
      return $fetch<XwikiContentBlocDto>(`/api/blocs/${id}`, {
        headers,
      })
    },
    {
      server: true,
      watch: [() => toValue(blocId)],
      default: () => null,
    }
  )

  const htmlContent = computed(() => asyncState.data.value?.htmlContent ?? '')
  const editLink = computed(() => asyncState.data.value?.editLink)

  return {
    htmlContent,
    editLink,
    pending: asyncState.pending,
    error: asyncState.error,
    refresh: asyncState.refresh,
  }
}

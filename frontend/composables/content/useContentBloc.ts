import type { XwikiContentBlocDto } from '~/src/api'

/**
 * Composable to retrieve dynamic content blocs
 */
export const useContentBloc = () => {
  const htmlContent = ref<string>('')
  const editLink = ref<string | undefined>(undefined)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchBloc = async (blocId: string) => {
    loading.value = true
    error.value = null
    try {
      const bloc = await $fetch<XwikiContentBlocDto>(`/api/blocs/${blocId}`)
      htmlContent.value = bloc.htmlContent ?? ''
      editLink.value = bloc.editLink
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch content'
    } finally {
      loading.value = false
    }
  }

  return {
    htmlContent: readonly(htmlContent),
    editLink: readonly(editLink),
    loading: readonly(loading),
    error: readonly(error),
    fetchBloc,
  }
}

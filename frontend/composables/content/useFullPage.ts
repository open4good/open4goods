import type { FullPage } from '~/src/api'

/**
 * Composable to retrieve full XWiki pages
 */
export const useFullPage = () => {
  const htmlContent = ref<string>('')
  const editLink = ref<string | undefined>(undefined)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchPage = async (xwikiPageId: string) => {
    loading.value = true
    error.value = null
    try {
      const page = await $fetch<FullPage>(`/api/pages/${xwikiPageId}`)
      htmlContent.value = page.htmlContent ?? ''
      const link = page.wikiPage?.xwikiAbsoluteUrl
      editLink.value = link?.replace('/view/', '/edit/')
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch page'
    } finally {
      loading.value = false
    }
  }

  return {
    htmlContent: readonly(htmlContent),
    editLink: readonly(editLink),
    loading: readonly(loading),
    error: readonly(error),
    fetchPage,
  }
}

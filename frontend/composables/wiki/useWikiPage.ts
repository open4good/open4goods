interface FullPageDto {
  metaTitle?: string
  metaDescription?: string
  pageTitle?: string
  html?: string
  width?: string
  editLink?: string
}

/**
 * Composable to retrieve full XWiki pages
 */
export const useWikiPage = () => {
  const data = ref<FullPageDto | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchPage = async (slug: string) => {
    loading.value = true
    error.value = null
    try {
      data.value = await $fetch<FullPageDto>(`/api/pages/${slug}`)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch page'
    } finally {
      loading.value = false
    }
  }

  return {
    data: readonly(data),
    loading: readonly(loading),
    error: readonly(error),
    fetchPage,
  }
}

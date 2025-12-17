import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const SUPPORTED_WIDTHS = new Set([
  'container',
  'container-fluid',
  'container-semi-fluid',
])

type SupportedWidth = 'container' | 'container-fluid' | 'container-semi-fluid'

export const useFullPage = async (
  pageId: MaybeRefOrGetter<string | null | undefined>
) => {
  const key = computed(() => {
    const id = toValue(pageId)
    return id ? `full-page:${id}` : 'full-page:empty'
  })

  const asyncState = await useAsyncData<CmsFullPage | null>(
    () => key.value,
    async () => {
      const id = toValue(pageId)
      if (!id) {
        return null
      }

      const encodedId = encodeURIComponent(id)
      const headers = useRequestHeaders(['host', 'x-forwarded-host'])
      return await $fetch<CmsFullPage>(`/api/pages/${encodedId}`, {
        headers,
      })
    },
    {
      server: true,
      default: () => null,
    }
  )

  const page = computed(() => asyncState.data.value)

  const width = computed<SupportedWidth>(() => {
    const rawWidth = page.value?.width ?? 'container'
    return (
      SUPPORTED_WIDTHS.has(rawWidth) ? rawWidth : 'container'
    ) as SupportedWidth
  })

  const pageTitle = computed(
    () => page.value?.pageTitle ?? page.value?.title ?? ''
  )
  const metaTitle = computed(() => page.value?.metaTitle ?? pageTitle.value)
  const metaDescription = computed(() => page.value?.metaDescription ?? '')
  const htmlContent = computed(() => page.value?.htmlContent ?? '')
  const editLink = computed(() => page.value?.editLink ?? null)

  return {
    page,
    width,
    pageTitle,
    metaTitle,
    metaDescription,
    htmlContent,
    editLink,
    pending: asyncState.pending,
    error: asyncState.error,
    refresh: asyncState.refresh,
  }
}

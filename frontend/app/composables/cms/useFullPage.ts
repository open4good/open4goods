import { computed, toValue, watchEffect, type MaybeRefOrGetter } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const SUPPORTED_WIDTHS = new Set(['container', 'container-fluid', 'container-semi-fluid'])

type SupportedWidth = 'container' | 'container-fluid' | 'container-semi-fluid'

interface PageProperties {
  width?: string
  pageTitle?: string
  metaTitle?: string
  metaDescription?: string
}

export const useFullPage = async (
  pageId: MaybeRefOrGetter<string | null | undefined>,
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
      return await $fetch<CmsFullPage>(`/api/pages/${encodedId}`)
    },
    {
      server: true,
      default: () => null,
    },
  )

  const lastResolvedPageKey = `cms-full-page:last-resolved:${key.value}`
  const lastResolvedPage = useState<CmsFullPage | null>(
    lastResolvedPageKey,
    () => asyncState.data.value ?? null,
  )

  watchEffect(() => {
    const resolvedPage = asyncState.data.value
    if (asyncState.pending.value && resolvedPage === null) {
      return
    }

    lastResolvedPage.value = resolvedPage
  })

  const page = computed(() => asyncState.data.value ?? lastResolvedPage.value)
  const properties = computed<PageProperties>(() => ({ ...(page.value?.properties ?? {}) }))

  const width = computed<SupportedWidth>(() => {
    const rawWidth = properties.value.width ?? 'container'
    return (SUPPORTED_WIDTHS.has(rawWidth) ? rawWidth : 'container') as SupportedWidth
  })

  const pageTitle = computed(() => properties.value.pageTitle ?? page.value?.wikiPage?.title ?? '')
  const metaTitle = computed(() => properties.value.metaTitle ?? pageTitle.value)
  const metaDescription = computed(() => properties.value.metaDescription ?? '')
  const htmlContent = computed(() => {
    const content = page.value?.htmlContent

    if (typeof content !== 'string') {
      return undefined
    }

    return content.trim() === '' ? undefined : content
  })
  const editLink = computed(() => page.value?.editLink ?? null)

  return {
    data: asyncState.data,
    page,
    properties,
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

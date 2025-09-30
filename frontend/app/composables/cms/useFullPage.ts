import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const SUPPORTED_LAYOUTS = new Set(['layout1'])
const SUPPORTED_WIDTHS = new Set(['container', 'container-fluid', 'container-semi-fluid'])

type SupportedLayout = 'layout1'
type SupportedWidth = 'container' | 'container-fluid' | 'container-semi-fluid'

interface PageProperties {
  layout?: string
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
      watch: [() => toValue(pageId)],
      default: () => null,
    },
  )

  const page = computed(() => asyncState.data.value)
  const properties = computed<PageProperties>(() => ({ ...(page.value?.properties ?? {}) }))

  const requestedLayout = computed(() => (properties.value.layout ?? 'layout1').toLowerCase())
  const layout = computed<SupportedLayout>(() => {
    const normalized = requestedLayout.value
    return (SUPPORTED_LAYOUTS.has(normalized) ? normalized : 'layout1') as SupportedLayout
  })

  const width = computed<SupportedWidth>(() => {
    const rawWidth = properties.value.width ?? 'container'
    return (SUPPORTED_WIDTHS.has(rawWidth) ? rawWidth : 'container') as SupportedWidth
  })

  const pageTitle = computed(() => properties.value.pageTitle ?? page.value?.wikiPage?.title ?? '')
  const metaTitle = computed(() => properties.value.metaTitle ?? pageTitle.value)
  const metaDescription = computed(() => properties.value.metaDescription ?? '')
  const htmlContent = computed(() => page.value?.htmlContent ?? '')
  const editLink = computed(() => page.value?.editLink ?? null)

  return {
    page,
    properties,
    layout,
    requestedLayout,
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

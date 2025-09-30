import { computed, toValue, type MaybeRefOrGetter } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const DEFAULT_LAYOUT = 'layout1'

const SUPPORTED_LAYOUT_VALUES = [
  'layout1',
  'layout2',
  'layout3',
  'layout4',
  'layout5',
  'layout6',
  'layout7',
  'layout8',
  'layout9',
  'layout10',
  'layout11',
  'layout12',
] as const

const SUPPORTED_WIDTH_VALUES = ['container', 'container-fluid', 'container-semi-fluid'] as const

export type SupportedLayout = (typeof SUPPORTED_LAYOUT_VALUES)[number]
type SupportedWidth = (typeof SUPPORTED_WIDTH_VALUES)[number]

export const SUPPORTED_LAYOUTS = new Set<SupportedLayout>(SUPPORTED_LAYOUT_VALUES)
const SUPPORTED_WIDTHS = new Set<SupportedWidth>(SUPPORTED_WIDTH_VALUES)

const isSupportedLayout = (layout: string): layout is SupportedLayout =>
  SUPPORTED_LAYOUTS.has(layout as SupportedLayout)

const isSupportedWidth = (width: string): width is SupportedWidth =>
  SUPPORTED_WIDTHS.has(width as SupportedWidth)

export const normalizeLayout = (layout: string | null | undefined): SupportedLayout => {
  const normalized = (layout ?? DEFAULT_LAYOUT).toLowerCase()
  return isSupportedLayout(normalized) ? normalized : DEFAULT_LAYOUT
}

const normalizeWidth = (width: string | null | undefined): SupportedWidth => {
  const normalized = (width ?? 'container').toLowerCase()
  return isSupportedWidth(normalized) ? normalized : 'container'
}

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
      default: () => null,
    },
  )

  const page = computed(() => asyncState.data.value)
  const properties = computed<PageProperties>(() => ({ ...(page.value?.properties ?? {}) }))

  const requestedLayout = computed(() => (properties.value.layout ?? DEFAULT_LAYOUT).toLowerCase())
  const layout = computed<SupportedLayout>(() => normalizeLayout(requestedLayout.value))

  const width = computed<SupportedWidth>(() => normalizeWidth(properties.value.width))

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

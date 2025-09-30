import { computed, shallowRef, toValue, watch, type MaybeRefOrGetter } from 'vue'
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
  const normalizedId = computed(() => {
    const rawId = toValue(pageId)
    if (!rawId) {
      return ''
    }

    return String(rawId).trim()
  })

  if (!normalizedId.value) {
    throw createError({ statusCode: 404, statusMessage: 'CMS page not found' })
  }

  const fallbackPage: CmsFullPage = {
    htmlContent: '',
    properties: {},
    editLink: null,
  }

  const asyncState = await useAsyncData<CmsFullPage>(
    () => `fullpage:${normalizedId.value}`,
    async () => {
      const id = normalizedId.value
      if (!id) {
        throw createError({ statusCode: 404, statusMessage: 'CMS page not found' })
      }

      const encodedId = encodeURIComponent(id)
      return await $fetch<CmsFullPage>(`/api/pages/${encodedId}`)
    },
    {
      server: true,
      lazy: false,
      watch: [normalizedId],
      default: () => ({ ...fallbackPage }),
    },
  )

  const lastResolvedPage = shallowRef(asyncState.data.value ?? fallbackPage)

  watch(
    () => [asyncState.pending.value, asyncState.error.value, asyncState.data.value] as const,
    ([isPending, currentError, currentData]) => {
      if (!isPending && !currentError && currentData) {
        lastResolvedPage.value = currentData
      }
    },
    { immediate: true },
  )

  const page = computed(() => {
    if (asyncState.pending.value) {
      return lastResolvedPage.value
    }

    return asyncState.data.value ?? lastResolvedPage.value ?? fallbackPage
  })

  const properties = computed<PageProperties>(() => ({ ...(page.value?.properties ?? {}) }))

  const width = computed<SupportedWidth>(() => {
    const rawWidth = properties.value.width ?? 'container'
    return (SUPPORTED_WIDTHS.has(rawWidth) ? rawWidth : 'container') as SupportedWidth
  })

  const pageTitle = computed(() => properties.value.pageTitle ?? page.value?.wikiPage?.title ?? '')
  const metaTitle = computed(() => properties.value.metaTitle ?? pageTitle.value)
  const metaDescription = computed(() => properties.value.metaDescription ?? '')
  const editLink = computed(() => page.value?.editLink ?? null)

  return {
    ...asyncState,
    page,
    properties,
    width,
    pageTitle,
    metaTitle,
    metaDescription,
    editLink,
  }
}

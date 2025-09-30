import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { nextTick, ref, type Ref } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const useAsyncDataMock = vi.hoisted(() => vi.fn())
const stateStore = vi.hoisted(() => new Map<string, Ref<unknown>>())
const useStateMock = vi.hoisted(() =>
  vi.fn(<T>(key: string, init: () => T) => {
    const store = stateStore as Map<string, Ref<unknown>>
    if (!store.has(key)) {
      store.set(key, ref(init()) as Ref<unknown>)
    }

    return store.get(key) as Ref<T>
  }),
)

vi.mock('#app', () => ({
  useAsyncData: useAsyncDataMock,
}))

vi.mock('nuxt/app', () => ({
  useAsyncData: useAsyncDataMock,
}))

vi.mock('#app/composables/asyncData', () => ({
  useAsyncData: useAsyncDataMock,
}))

vi.mock('#imports', () => ({
  useState: useStateMock,
}))

describe('useFullPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>
  let latestExecute: (() => Promise<void>) | null
  let latestData: Ref<CmsFullPage | null> | null

  beforeEach(() => {
    latestExecute = null
    latestData = null
    fetchMock = vi.fn().mockResolvedValue({
      htmlContent: '<p>Server HTML</p>',
      properties: {},
      editLink: null,
      wikiPage: {
        title: 'Title',
      },
    } as CmsFullPage)

    ;(globalThis as Record<string, unknown>).$fetch = fetchMock

    useAsyncDataMock.mockImplementation(async (_key, handler, _options) => {
      const data = ref<CmsFullPage | null>(null)
      latestData = data
      const pending = ref(false)
      const error = ref<Error | null>(null)
      const refresh = vi.fn()
      latestExecute = async () => {
        data.value = (await handler()) as CmsFullPage | null
      }

      await latestExecute()

      return { data, pending, error, refresh }
    })
  })

  afterEach(() => {
    useAsyncDataMock.mockReset()
    fetchMock.mockReset()
    delete (globalThis as Record<string, unknown>).$fetch
    ;(stateStore as Map<string, Ref<unknown>>).clear()
    useStateMock.mockClear()
    vi.resetModules()
  })

  it('refetches page content when the page identifier becomes available', async () => {
    const pageId = ref<string | null>(null)

    const { useFullPage } = await import('./useFullPage')
    const { htmlContent } = await useFullPage(pageId)

    expect(useAsyncDataMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).not.toHaveBeenCalled()

    const [, , options] = useAsyncDataMock.mock.calls[0] ?? []
    const watchSources = options?.watch ?? []
    expect(watchSources).toHaveLength(1)
    const [watchSource] = watchSources
    expect(watchSource?.()).toBeNull()

    pageId.value = 'Space/Sub Page'
    expect(watchSource?.()).toBe('Space/Sub Page')

    // simulate the watcher triggering a refetch
    await latestExecute?.()

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/pages/Space%2FSub%20Page')

    expect(htmlContent.value).toBe('<p>Server HTML</p>')
  })

  it('keeps previously fetched content while awaiting hydration data', async () => {
    const pageId = ref<string>('Space/Sub Page')

    const { useFullPage } = await import('./useFullPage')
    const { htmlContent, pageTitle, metaTitle } = await useFullPage(pageId)

    expect(htmlContent.value).toBe('<p>Server HTML</p>')
    expect(pageTitle.value).toBe('Title')
    expect(metaTitle.value).toBe('Title')

    expect(latestData).not.toBeNull()
    latestData!.value = null
    await nextTick()

    expect(htmlContent.value).toBe('<p>Server HTML</p>')
    expect(pageTitle.value).toBe('Title')
    expect(metaTitle.value).toBe('Title')
  })
})

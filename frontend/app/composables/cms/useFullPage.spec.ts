import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { ref } from 'vue'
import type { CmsFullPage } from '~~/shared/api-client/services/pages.services'

const useAsyncDataMock = vi.hoisted(() => vi.fn())

vi.mock('#app', () => ({
  useAsyncData: useAsyncDataMock,
}))

vi.mock('nuxt/app', () => ({
  useAsyncData: useAsyncDataMock,
}))

vi.mock('#app/composables/asyncData', () => ({
  useAsyncData: useAsyncDataMock,
}))

describe('useFullPage', () => {
  let fetchMock: ReturnType<typeof vi.fn>
  let latestExecute: (() => Promise<void>) | null

  beforeEach(() => {
    latestExecute = null
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
})

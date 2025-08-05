import { beforeEach, afterEach, describe, expect, test, vi } from 'vitest'
import type { Mock } from 'vitest'
import { useFullPage } from './useFullPage'

const mockPage = {
  htmlContent: '<p>Hello page</p>',
  wikiPage: { xwikiAbsoluteUrl: 'https://example.com/bin/view/Main/WebHome' },
}

describe('useFullPage', () => {
  beforeEach(() => {
    vi.stubGlobal('$fetch', vi.fn().mockResolvedValue(mockPage))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  test('fetchPage retrieves content and edit link', async () => {
    const { fetchPage, htmlContent, editLink, loading, error } = useFullPage()
    await fetchPage('Main.WebHome')
    expect($fetch).toHaveBeenCalledWith('/api/pages/Main.WebHome')
    expect(htmlContent.value).toBe(mockPage.htmlContent)
    expect(editLink.value).toBe('https://example.com/bin/edit/Main/WebHome')
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  test('sets error when fetch fails', async () => {
    const fetchMock = $fetch as unknown as Mock
    fetchMock.mockRejectedValueOnce(new Error('fail'))
    const { fetchPage, error, loading } = useFullPage()
    await fetchPage('id')
    expect(error.value).toBe('fail')
    expect(loading.value).toBe(false)
  })
})

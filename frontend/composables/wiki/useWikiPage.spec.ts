import { beforeEach, describe, expect, test, vi } from 'vitest'
import { useWikiPage } from './useWikiPage'

const mockPage = {
  metaTitle: 't',
  metaDescription: 'd',
  pageTitle: 'p',
  html: '<p>Hi</p>',
  width: 'full',
  editLink: '/edit',
}

// Mock $fetch
vi.stubGlobal('$fetch', vi.fn().mockResolvedValue(mockPage))
const fetchMock = $fetch as unknown as ReturnType<typeof vi.fn>

describe('useWikiPage', () => {
  beforeEach(() => {
    fetchMock.mockClear()
  })

  test('fetches wiki page and exposes data', async () => {
    const { data, fetchPage, error } = useWikiPage()
    await fetchPage('Main.WebHome')
    expect($fetch).toHaveBeenCalledWith('/api/pages/Main.WebHome')
    expect(data.value).toEqual(mockPage)
    expect(error.value).toBeNull()
  })

  test('handles fetch error', async () => {
    fetchMock.mockRejectedValueOnce(new Error('fail'))
    const { fetchPage, error } = useWikiPage()
    await fetchPage('id')
    expect(error.value).toBe('fail')
  })
})

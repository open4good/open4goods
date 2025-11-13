import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('useCategoryNavigation composable', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.resetModules()
    fetchMock.mockReset()
    vi.stubGlobal('$fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads the navigation tree and caches the payload', async () => {
    const navigationResponse = {
      category: { title: 'Root' },
      childCategories: [],
    }

    fetchMock.mockResolvedValueOnce(navigationResponse)

    const { useCategoryNavigation } = await import('./useCategoryNavigation')
    const { fetchNavigation, navigation, loading, error } = useCategoryNavigation()

    const result = await fetchNavigation()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/categories/navigation',
      expect.objectContaining({ params: undefined }),
    )
    expect(result).toEqual(navigationResponse)
    expect(navigation.value).toEqual(navigationResponse)
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  it('captures errors when the navigation endpoint fails', async () => {
    fetchMock.mockRejectedValueOnce(new Error('Network error'))

    const { useCategoryNavigation } = await import('./useCategoryNavigation')
    const { fetchNavigation, error, navigation } = useCategoryNavigation()

    await fetchNavigation({ path: 'electronics' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/categories/navigation',
      expect.objectContaining({ params: { path: 'electronics' } }),
    )
    expect(error.value).toBe('Network error')
    expect(navigation.value).toBeNull()
  })
})

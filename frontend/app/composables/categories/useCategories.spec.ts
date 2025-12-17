import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('useCategories composable', () => {
  const fetchMock = vi.fn()

  beforeEach(async () => {
    vi.resetModules()
    fetchMock.mockReset()
    vi.stubGlobal('$fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads category details, stores the active id and retains subsets', async () => {
    const categoriesResponse = [
      {
        id: 'category-1',
        verticalHomeUrl: '/dish-washers',
      },
    ]
    const detailResponse = {
      id: 'category-1',
      verticalHomeTitle: 'Dish washers',
      subsets: [
        {
          id: 'subset-1',
          title: 'Eco friendly',
        },
      ],
    }

    fetchMock.mockImplementation((request, init) => {
      if (request === '/api/categories') {
        expect(init).toEqual({ params: { onlyEnabled: true } })
        return Promise.resolve(categoriesResponse)
      }

      if (request === '/api/categories/category-1') {
        expect(init).toBeUndefined()
        return Promise.resolve(detailResponse)
      }

      throw new Error(`Unexpected request: ${String(request)}`)
    })

    const { useCategories } = await import('./useCategories')
    const { selectCategoryBySlug, currentCategory, activeCategoryId } =
      useCategories()

    const detail = await selectCategoryBySlug('dish-washers')

    expect(activeCategoryId.value).toBe('category-1')
    expect(detail).toEqual(detailResponse)
    expect(currentCategory.value?.subsets).toEqual(detailResponse.subsets)
  })

  it('raises a CategoryNotFoundError when the slug cannot be resolved', async () => {
    fetchMock.mockResolvedValueOnce([])

    const { useCategories } = await import('./useCategories')
    const { selectCategoryBySlug, error, currentCategory, activeCategoryId } =
      useCategories()

    await expect(selectCategoryBySlug('unknown-slug')).rejects.toMatchObject({
      name: 'CategoryNotFoundError',
      message: 'Category not found',
    })

    expect(error.value).toBe('Category not found')
    expect(currentCategory.value).toBeNull()
    expect(activeCategoryId.value).toBeNull()
  })
})

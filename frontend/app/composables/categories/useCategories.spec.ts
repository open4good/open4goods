import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('useCategories composable', () => {
  const fetchMock = vi.fn()
  const resolveOnlyEnabledParam = (
    init: unknown
  ): boolean | undefined => {
    if (!init || typeof init !== 'object') {
      return undefined
    }

    const params = (init as { params?: { onlyEnabled?: boolean } }).params
    return params?.onlyEnabled
  }

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

  it('loads disabled categories on fallback when slug is missing from the enabled list', async () => {
    const disabledCategory = {
      id: 'category-disabled',
      verticalHomeUrl: '/archived-category',
      enabled: false,
    }
    const detailResponse = {
      id: 'category-disabled',
      verticalHomeTitle: 'Archived',
    }

    fetchMock.mockImplementation((request, init) => {
      if (request === '/api/categories') {
        const onlyEnabled = resolveOnlyEnabledParam(init)

        if (onlyEnabled === true) {
          return Promise.resolve([])
        }

        if (onlyEnabled === false) {
          return Promise.resolve([disabledCategory])
        }
      }

      if (request === '/api/categories/category-disabled') {
        return Promise.resolve(detailResponse)
      }

      throw new Error(`Unexpected request: ${String(request)}`)
    })

    const { useCategories } = await import('./useCategories')
    const { selectCategoryBySlug, currentCategory, activeCategoryId } =
      useCategories()

    const detail = await selectCategoryBySlug('archived-category')

    expect(activeCategoryId.value).toBe('category-disabled')
    expect(detail).toEqual(detailResponse)
    expect(currentCategory.value?.enabled).toBeUndefined()
  })

  it('raises a CategoryNotFoundError when the slug cannot be resolved', async () => {
    fetchMock.mockImplementation((request, init) => {
      if (request === '/api/categories') {
        const onlyEnabled = resolveOnlyEnabledParam(init)

        if (onlyEnabled === true || onlyEnabled === false) {
          return Promise.resolve([])
        }
      }

      throw new Error(`Unexpected request: ${String(request)}`)
    })

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

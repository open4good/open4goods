import { ref } from 'vue'
import type { VerticalConfigDto } from '~~/shared/api-client'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMock = vi.fn()
let stateStore: Map<string, ReturnType<typeof ref>>

vi.mock('#imports', () => ({
  useState: (key: string, init: () => unknown) => {
    if (!stateStore.has(key)) {
      stateStore.set(key, ref(init()))
    }

    return stateStore.get(key)
  },
  useRequestHeaders: () => undefined,
}))

vi.mock('#app', () => ({
  useState: (key: string, init: () => unknown) => {
    if (!stateStore.has(key)) {
      stateStore.set(key, ref(init()))
    }

    return stateStore.get(key)
  },
  useRequestHeaders: () => undefined,
}))

describe('useCategories composable', () => {
  beforeEach(async () => {
    vi.resetModules()
    fetchMock.mockReset()
    stateStore = new Map()
    const globalObject = globalThis as Record<string, unknown>
    globalObject['categories-list-cache'] = {}
    globalObject['category-detail-cache'] = {}
    vi.stubGlobal('$fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    stateStore.clear()
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
        expect(init).toEqual({})
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

  it('loads disabled categories when the slug is present in the list', async () => {
    const disabledCategory = {
      id: 'category-disabled',
      verticalHomeUrl: 'archived-category',
      enabled: false,
    }
    const detailResponse = {
      id: 'category-disabled',
      verticalHomeTitle: 'Archived',
      enabled: false,
    }

    fetchMock.mockImplementation((request, init) => {
      if (request === '/api/categories') {
        expect(init).toEqual({})
        return Promise.resolve([
          disabledCategory as unknown as VerticalConfigDto,
        ])
      }

      if (request === '/api/categories/category-disabled') {
        return Promise.resolve(detailResponse)
      }

      throw new Error(`Unexpected request: ${String(request)}`)
    })

    const { useCategories } = await import('./useCategories')
    const { selectCategoryBySlug, currentCategory, activeCategoryId } =
      useCategories()

    const categoriesState = stateStore.get('categories-list')
    if (categoriesState) {
      categoriesState.value = [disabledCategory as unknown as VerticalConfigDto]
      expect(categoriesState.value).toEqual([
        disabledCategory as unknown as VerticalConfigDto,
      ])
    }
    const detail = await selectCategoryBySlug('archived-category')

    expect(activeCategoryId.value).toBe('category-disabled')
    expect(detail).toEqual(detailResponse)
    expect(currentCategory.value?.enabled).toBe(false)
  })

  it('raises a CategoryNotFoundError when the slug cannot be resolved', async () => {
    fetchMock.mockImplementation((request, init) => {
      if (request === '/api/categories') {
        expect(init).toEqual({})
        return Promise.resolve([])
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

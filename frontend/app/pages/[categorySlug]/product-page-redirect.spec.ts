import { mountSuspended, mockNuxtImport } from '@nuxt/test-utils/runtime'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const navigateToMock = vi.hoisted(() => vi.fn(() => Promise.resolve()))
const createErrorMock = vi.hoisted(() => {
  return vi.fn((input: { statusCode?: number; statusMessage?: string }) => {
    const error = new Error(input?.statusMessage ?? 'Error')
    Object.assign(error, input)
    throw error
  })
})

const route = {
  params: {
    categorySlug: 'Televiseurs',
    productSlug: '12345-Television-Samsung',
  },
}

mockNuxtImport('useRoute', () => () => route)
mockNuxtImport('navigateTo', () => navigateToMock)
mockNuxtImport('createError', () => createErrorMock)

describe('category product redirect page', () => {
  beforeEach(() => {
    navigateToMock.mockClear()
    createErrorMock.mockClear()
    route.params.categorySlug = 'Televiseurs'
    route.params.productSlug = '12345-Television-Samsung'
  })

  it('redirects matching product routes to the catch-all product page', async () => {
    const component = (await import('./product-page-redirect.vue')).default

    await mountSuspended(component)

    expect(navigateToMock).toHaveBeenCalledWith(
      {
        name: 'slug',
        params: { slug: ['televiseurs', '12345-television-samsung'] },
      },
      { replace: true, redirectCode: 301 },
    )
    expect(createErrorMock).not.toHaveBeenCalled()
  })

})

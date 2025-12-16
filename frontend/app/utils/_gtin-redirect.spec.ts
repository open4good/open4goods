import { describe, expect, it, vi } from 'vitest'

import type { ResolveGtinRedirectDependencies } from './_gtin-redirect'
import { resolveGtinRedirectTarget } from './_gtin-redirect'

describe('app/utils/_gtin-redirect', () => {
  const createDependencies = (
    overrides: Partial<ResolveGtinRedirectDependencies> = {}
  ): ResolveGtinRedirectDependencies => {
    const fetchProduct = vi.fn()
    const createError = vi.fn(
      (input: { statusCode: number; statusMessage: string; cause?: unknown }) =>
        Object.assign(new Error(input.statusMessage), {
          ...input,
          isCreateError: true,
        })
    )

    return {
      fetchProduct,
      createError,
      ...overrides,
    }
  }

  it('returns the canonical fullSlug path when the product exists', async () => {
    const dependencies = createDependencies({
      fetchProduct: vi
        .fn()
        .mockResolvedValue({ fullSlug: 'produits/example-product' }),
    })

    const target = await resolveGtinRedirectTarget('123456', dependencies)

    expect(dependencies.fetchProduct).toHaveBeenCalledWith('123456')
    expect(target).toBe('/produits/example-product')
  })

  it('strips query strings and fragments from the slug', async () => {
    const dependencies = createDependencies({
      fetchProduct: vi
        .fn()
        .mockResolvedValue({ fullSlug: '/produits/example?ref=123#section' }),
    })

    const target = await resolveGtinRedirectTarget('123456', dependencies)

    expect(target).toBe('/produits/example')
  })

  it('throws a 404 error when the backend does not find the product', async () => {
    const backendError = { statusCode: 404 }
    const dependencies = createDependencies({
      fetchProduct: vi.fn().mockRejectedValue(backendError),
    })

    await expect(
      resolveGtinRedirectTarget('123456', dependencies)
    ).rejects.toMatchObject({
      statusCode: 404,
      statusMessage: 'Product not found',
      isCreateError: true,
    })

    expect(dependencies.createError).toHaveBeenCalledWith({
      statusCode: 404,
      statusMessage: 'Product not found',
      cause: backendError,
    })
  })

  it('falls back to the raw product slug when the fullSlug is missing', async () => {
    const dependencies = createDependencies({
      fetchProduct: vi
        .fn()
        .mockResolvedValue({ slug: '123456-example-product ' }),
    })

    const target = await resolveGtinRedirectTarget('123456', dependencies)

    expect(target).toBe('/123456-example-product')
  })

  it('throws a 404 when the product does not expose a navigable slug', async () => {
    const dependencies = createDependencies({
      fetchProduct: vi.fn().mockResolvedValue({ fullSlug: '   ', slug: null }),
    })

    await expect(
      resolveGtinRedirectTarget('123456', dependencies)
    ).rejects.toMatchObject({
      statusCode: 404,
      statusMessage: 'Product not found',
      isCreateError: true,
    })

    expect(dependencies.createError).toHaveBeenCalledWith({
      statusCode: 404,
      statusMessage: 'Product not found',
    })
  })
})

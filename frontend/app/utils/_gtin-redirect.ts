import type { ProductDto } from '~~/shared/api-client'

export interface ResolveGtinRedirectDependencies {
  fetchProduct: (gtin: string) => Promise<ProductDto>
  createError: (input: { statusCode: number; statusMessage: string; cause?: unknown }) => Error
}

export const resolveGtinRedirectTarget = async (
  gtin: string,
  { fetchProduct, createError }: ResolveGtinRedirectDependencies,
  currentPath?: string
): Promise<string> => {
  let product: ProductDto
  try {
    product = await fetchProduct(gtin)
  } catch (error) {
    const statusCode =
      (error as { statusCode?: number })?.statusCode ??
      (error as { response?: { status?: number } })?.response?.status

    if (statusCode === 404) {
      throw createError({
        statusCode: 404,
        statusMessage: 'Product not found',
        cause: error,
      })
    }

    throw error
  }

  const normalizedFullSlug = product.fullSlug?.trim()
  const normalizedSlug = product.slug?.trim()
  const canonicalSlug = normalizedFullSlug || normalizedSlug

  if (canonicalSlug) {
    const [maybeBareSlug] = canonicalSlug.split(/[?#]/, 1)
    const bareFullSlug = (maybeBareSlug ?? '').trim()

    if (!bareFullSlug) {
      throw createError({
        statusCode: 404,
        statusMessage: 'Product not found',
      })
    }

    return bareFullSlug.startsWith('/') ? bareFullSlug : `/${bareFullSlug}`
  }

  const normalizedCurrentPath = currentPath?.trim()
  const fallbackPath = normalizedCurrentPath || `/${gtin}`

  return fallbackPath.startsWith('/') ? fallbackPath : `/${fallbackPath}`
}

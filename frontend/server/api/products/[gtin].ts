import { useProductService } from '~~/shared/api-client/services/product.services'
import type { ProductDto } from '~~/shared/api-client'
import { ProductIncludeEnum } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const PRODUCT_CACHE_CONTROL = 'public, max-age=300, s-maxage=300'

export default defineEventHandler(async (event): Promise<ProductDto> => {
  const gtinParam = getRouterParam(event, 'gtin')

  if (!gtinParam) {
    throw createError({ statusCode: 400, statusMessage: 'Product GTIN is required' })
  }

  const numericGtin = Number.parseInt(gtinParam, 10)

  if (!Number.isSafeInteger(numericGtin) || numericGtin < 0) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid GTIN parameter' })
  }

  setResponseHeader(event, 'Cache-Control', PRODUCT_CACHE_CONTROL)

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const productService = useProductService(domainLanguage)

  try {
    return await productService.getProduct(numericGtin, {
      include: [
        ProductIncludeEnum.Base,
        ProductIncludeEnum.Identity,
        ProductIncludeEnum.Names,
        ProductIncludeEnum.Scores,
      ],
    })
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching product detail', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

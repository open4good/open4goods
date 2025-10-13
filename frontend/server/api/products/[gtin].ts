import { createError, defineEventHandler, getRouterParam } from 'h3'
import { useProductService } from '~~/shared/api-client/services/products.services'
import type { ProductDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

export default defineEventHandler(async (event): Promise<ProductDto> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=300, s-maxage=300')

  const gtinParam = getRouterParam(event, 'gtin')

  if (!gtinParam) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Product GTIN is required',
    })
  }

  const parsedGtin = Number.parseInt(gtinParam, 10)

  if (!Number.isFinite(parsedGtin)) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Product GTIN must be numeric',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const productService = useProductService(domainLanguage)

  try {
    return await productService.getProductByGtin(parsedGtin)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching product:',
      backendError.logMessage,
      backendError
    )

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

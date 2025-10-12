import { useProductsService } from '~~/shared/api-client/services/products.services'
import type { ProductDto } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../utils/log-backend-error'

const GTIN_PATTERN = /^\d{6,}$/

export default defineEventHandler(async (event): Promise<ProductDto> => {
  const gtinParam = getRouterParam(event, 'gtin')
  if (!gtinParam) {
    throw createError({ statusCode: 400, statusMessage: 'Product GTIN is required' })
  }

  const decodedGtin = decodeURIComponent(gtinParam)

  if (!GTIN_PATTERN.test(decodedGtin)) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid GTIN parameter' })
  }

  const rawHost = event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const productsService = useProductsService(domainLanguage)

  try {
    return await productsService.getProductByGtin(decodedGtin)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching product detail:', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

import type { ProductFieldOptionsResponse } from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../../../utils/cache-headers'

export default defineEventHandler(
  async (event): Promise<ProductFieldOptionsResponse> => {
    setDomainLanguageCacheHeaders(event, 'public, max-age=600, s-maxage=600')

    const verticalIdParam = getRouterParam(event, 'verticalId')
    if (!verticalIdParam) {
      throw createError({
        statusCode: 400,
        statusMessage: 'verticalId parameter is required.',
      })
    }

    const verticalId = decodeURIComponent(verticalIdParam)

    const rawHost =
      event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
    const { domainLanguage } = resolveDomainLanguage(rawHost)

    const productService = useProductService(domainLanguage)

    try {
      return await productService.getSortableFieldsForVertical(verticalId)
    } catch (error) {
      const backendError = await extractBackendErrorDetails(error)
      console.error(
        'Error fetching sortable fields for vertical:',
        verticalId,
        backendError.logMessage,
        backendError
      )

      throw createError({
        statusCode: backendError.statusCode,
        statusMessage: backendError.statusMessage,
        cause: error,
      })
    }
  }
)

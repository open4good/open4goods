import {
  createError,
  defineEventHandler,
  getQuery,
} from 'h3'
import type { ProductDto } from '~~/shared/api-client'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { extractGtinParam } from '~~/shared/utils/_gtin'
import { deriveQueryFromUrl } from '~~/shared/utils/share-intent'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'
import { extractBackendErrorDetails } from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

const SEARCH_PAGE_SIZE = 6
const SLA_TIMEOUT_MS = 4000

const deriveSearchQuery = (
  query?: string | null,
  url?: string | null
): string | null => {
  if (query && query.trim().length) {
    return query.trim().slice(0, 160)
  }

  const fromUrl = deriveQueryFromUrl(url)
  return fromUrl ? fromUrl.slice(0, 160) : null
}

export default defineEventHandler(async event => {
  const { domainLanguage } = resolveDomainLanguage(event)
  setDomainLanguageCacheHeaders(event, 'private, no-store')

  const productService = useProductService(domainLanguage)
  const query = getQuery(event)

  const gtinParam = extractGtinParam(
    typeof query.gtin === 'string' ? query.gtin : null
  )
  const searchQuery = deriveSearchQuery(
    typeof query.q === 'string' ? query.q : null,
    typeof query.url === 'string' ? query.url : null
  )

  const origin = {
    gtin: gtinParam,
    query: searchQuery,
    url: typeof query.url === 'string' ? query.url : null,
    title: typeof query.title === 'string' ? query.title : null,
    text: typeof query.text === 'string' ? query.text : null,
  }

  const withTimeout = async <T>(promise: Promise<T>) => {
    let timeoutHandle: ReturnType<typeof setTimeout>
    const timeoutPromise = new Promise<null>(resolve => {
      timeoutHandle = setTimeout(() => resolve(null), SLA_TIMEOUT_MS)
    })

    const result = await Promise.race([promise, timeoutPromise])
    return { result, timeoutHandle: timeoutHandle! }
  }

  const response: {
    status: 'resolved' | 'empty' | 'error' | 'timeout'
    products: ProductDto[]
    primary: ProductDto | null
    origin: typeof origin
  } = {
    status: 'resolved',
    products: [],
    primary: null,
    origin,
  }

  try {
    if (gtinParam) {
      const { result, timeoutHandle } = await withTimeout(
        productService.getProductByGtin(gtinParam)
      )
      clearTimeout(timeoutHandle)

      if (result === null) {
        response.status = 'timeout'
      } else {
        response.products = result ? [result] : []
        response.primary = result ?? null
      }
    } else if (searchQuery) {
      const { result, timeoutHandle } = await withTimeout(
        productService.searchProducts({
          pageNumber: 0,
          pageSize: SEARCH_PAGE_SIZE,
          body: {
            query: searchQuery,
          },
        })
      )
      clearTimeout(timeoutHandle)

      if (result === null) {
        response.status = 'timeout'
      } else {
        response.products = result.products?.data ?? []
        response.primary = response.products[0] ?? null
      }
    } else {
      response.status = 'empty'
    }

    if (response.status === 'resolved' && response.products.length === 0) {
      response.status = 'empty'
    }
  } catch (error: unknown) {
    const details = await extractBackendErrorDetails(error)
    response.status = 'error'

    throw createError({
      statusCode: details.statusCode ?? 500,
      statusMessage: details.statusMessage ?? 'Share resolution failed',
      cause: error,
    })
  }

  return response
})

import {
  createError,
  defineEventHandler,
  getCookie,
  getRouterParam,
  readBody,
} from 'h3'
import { useProductService } from '~~/shared/api-client/services/products.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../../../utils/log-backend-error'

interface TriggerReviewPayload {
  hcaptchaResponse?: string
}

export default defineEventHandler(async event => {
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

  const runtimeConfig = useRuntimeConfig()
  const authToken = getCookie(event, runtimeConfig.public.tokenCookieName)
  const isAuthenticated = Boolean(authToken)

  const body = await readBody<TriggerReviewPayload | null>(event)
  const hcaptchaResponse = body?.hcaptchaResponse
  const force = isAuthenticated

  if (!hcaptchaResponse && !isAuthenticated) {
    throw createError({
      statusCode: 400,
      statusMessage: 'hCaptcha response is required',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const productService = useProductService(domainLanguage)

  try {
    await productService.triggerReviewGeneration(parsedGtin, hcaptchaResponse, {
      force,
      authToken,
    })
    event.node.res.statusCode = 202
    return { status: 'accepted' }
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error triggering AI review generation:',
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

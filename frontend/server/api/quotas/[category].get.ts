import { createError, defineEventHandler, getRouterParam } from 'h3'
import {
  IpQuotaStatusDtoCategoryEnum,
  type IpQuotaStatusDto,
} from '~~/shared/api-client'
import { useQuotaService } from '~~/shared/api-client/services/quota.services'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import {
  extractBackendErrorDetails,
  logBackendError,
} from '../../utils/log-backend-error'
import { setDomainLanguageCacheHeaders } from '../../utils/cache-headers'

const isQuotaCategory = (
  value: string
): value is IpQuotaStatusDtoCategoryEnum =>
  Object.values(IpQuotaStatusDtoCategoryEnum).includes(
    value as IpQuotaStatusDtoCategoryEnum
  )

export default defineEventHandler(async (event): Promise<IpQuotaStatusDto> => {
  setDomainLanguageCacheHeaders(event, 'no-store')

  const categoryParam = getRouterParam(event, 'category')
  if (!categoryParam || !isQuotaCategory(categoryParam)) {
    throw createError({
      statusCode: 400,
      statusMessage: 'Invalid quota category.',
    })
  }

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const quotaService = useQuotaService(domainLanguage)

  try {
    return await quotaService.getQuotaStatus(categoryParam)
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    logBackendError({
      namespace: 'quota:status',
      details: backendError,
    })

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

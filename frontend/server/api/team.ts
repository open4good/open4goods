import { useTeamService } from '~~/shared/api-client/services/team.services'
import type { TeamProperties } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { setDomainLanguageCacheHeaders } from '../utils/cache-headers'

import { extractBackendErrorDetails } from '../utils/log-backend-error'

export default defineEventHandler(async (event): Promise<TeamProperties> => {
  setDomainLanguageCacheHeaders(event, 'public, max-age=1800, s-maxage=1800')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)

  const teamService = useTeamService(domainLanguage)

  try {
    return await teamService.fetchTeam()
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error(
      'Error fetching team roster',
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

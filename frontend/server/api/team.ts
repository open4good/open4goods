import { useRuntimeConfig } from '#imports'
import { useTeamService } from '~~/shared/api-client/services/team.services'
import type { Member, TeamProperties } from '~~/shared/api-client'
import { resolveDomainLanguage } from '~~/shared/utils/domain-language'

import { extractBackendErrorDetails } from '../utils/log-backend-error'

const resolveImageUrl = (imageUrl: string | undefined, baseUrl: string | undefined): string | undefined => {
  if (!imageUrl) {
    return imageUrl
  }

  if (/^https?:\/\//i.test(imageUrl) || imageUrl.startsWith('//')) {
    return imageUrl
  }

  if (!baseUrl) {
    return imageUrl
  }

  try {
    return new URL(imageUrl, baseUrl).toString()
  } catch {
    return imageUrl
  }
}

const normalizeMemberImageUrls = (members: Member[], baseUrl: string | undefined): Member[] =>
  members.map((member) => ({
    ...member,
    imageUrl: resolveImageUrl(member.imageUrl, baseUrl),
  }))

export default defineEventHandler(async (event): Promise<TeamProperties> => {
  setResponseHeader(event, 'Cache-Control', 'public, max-age=1800, s-maxage=1800')

  const rawHost =
    event.node.req.headers['x-forwarded-host'] ?? event.node.req.headers.host
  const { domainLanguage } = resolveDomainLanguage(rawHost)
  const runtimeConfig = useRuntimeConfig(event)

  const teamService = useTeamService(domainLanguage)

  try {
    const roster = await teamService.fetchTeam()
    const baseUrl = runtimeConfig?.apiUrl

    return {
      ...roster,
      cores: normalizeMemberImageUrls(roster.cores, baseUrl),
      contributors: normalizeMemberImageUrls(roster.contributors, baseUrl),
    }
  } catch (error) {
    const backendError = await extractBackendErrorDetails(error)
    console.error('Error fetching team roster', backendError.logMessage, backendError)

    throw createError({
      statusCode: backendError.statusCode,
      statusMessage: backendError.statusMessage,
      cause: error,
    })
  }
})

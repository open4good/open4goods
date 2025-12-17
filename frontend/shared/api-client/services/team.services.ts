import { TeamApi, TeamDomainLanguageEnum } from '..'
import type { TeamProperties } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Team service responsible for fetching team roster data from the backend API.
 */
export const useTeamService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: TeamApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useTeamService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new TeamApi(createBackendApiConfig())
    }

    return api
  }

  const fetchTeam = async (): Promise<TeamProperties> => {
    const language =
      domainLanguage === 'fr'
        ? TeamDomainLanguageEnum.Fr
        : TeamDomainLanguageEnum.En

    try {
      return await resolveApi().team({ domainLanguage: language })
    } catch (error) {
      console.error('Error fetching team roster:', error)
      throw error
    }
  }

  return { fetchTeam }
}

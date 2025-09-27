import { TeamApi, Configuration, TeamDomainLanguageEnum } from '..'
import type { TeamProperties } from '..'
import type { DomainLanguage } from '../../utils/domain-language'

/**
 * Team service responsible for fetching team roster data from the backend API.
 */
export const useTeamService = (domainLanguage: DomainLanguage) => {
  const config = useRuntimeConfig()
  const apiConfig = new Configuration({ basePath: config.apiUrl })
  const api = new TeamApi(apiConfig)

  const fetchTeam = async (): Promise<TeamProperties> => {
    const language =
      domainLanguage === 'fr'
        ? TeamDomainLanguageEnum.Fr
        : TeamDomainLanguageEnum.En

    try {
      return await api.team({ domainLanguage: language })
    } catch (error) {
      console.error('Error fetching team roster:', error)
      throw error
    }
  }

  return { fetchTeam }
}

import { StatsApi, type CategoriesStatsDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Statistics service for fetching aggregated catalogue metrics.
 */
export const useStatsService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: StatsApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error('useStatsService() is only available on the server runtime.')
    }

    if (!api) {
      api = new StatsApi(createBackendApiConfig())
    }

    return api
  }

  const getCategoriesStats = async (): Promise<CategoriesStatsDto> => {
    try {
      return await resolveApi().categories({ domainLanguage })
    } catch (error) {
      console.error('Error fetching categories stats:', error)
      throw error
    }
  }

  return { getCategoriesStats }
}

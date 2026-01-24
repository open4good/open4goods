import { MetriksApi, type MetriksReportDto } from '..'
import type { DomainLanguage } from '../../utils/domain-language'
import { createBackendApiConfig } from './createBackendApiConfig'

/**
 * Metriks service for fetching aggregated KPI history.
 */
export const useMetriksService = (domainLanguage: DomainLanguage) => {
  const isVitest =
    typeof process !== 'undefined' && process.env?.VITEST === 'true'
  const isServerRuntime = import.meta.server || isVitest
  let api: MetriksApi | undefined

  const resolveApi = () => {
    if (!isServerRuntime) {
      throw new Error(
        'useMetriksService() is only available on the server runtime.'
      )
    }

    if (!api) {
      api = new MetriksApi(createBackendApiConfig())
    }

    return api
  }

  const getReport = async (
    limit: number,
    includePayload: boolean
  ): Promise<MetriksReportDto> => {
    try {
      return await resolveApi().report({
        domainLanguage,
        limit,
        includePayload,
      })
    } catch (error) {
      console.error('Error fetching metriks report:', error)
      throw error
    }
  }

  return { getReport }
}
